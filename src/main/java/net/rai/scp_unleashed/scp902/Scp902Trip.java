package net.rai.scp_unleashed.scp902;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp902.network.Scp902ScenePayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp902Trip {
    public static final AttributeModifier Trudge = new AttributeModifier(Scp902Registry.location("scp902_trudge"), -0.55, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final AttributeModifier Earthbound = new AttributeModifier(Scp902Registry.location("scp902_earthbound"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final float DepartVolume = 0.8f;
    public static final float FadeVolume = 0.7f;
    public static final int EvictEvery = 20;
    public static final int CopyDelay = 1;
    public static final int StuckTicks = 10;
    public static final double StuckMove = 0.02;
    public static final double DropLimit = 0.6;
    public static final double RescueStep = 0.5;
    public static final double RescueReach = 6.0;
    private static final Map<UUID, Scp902Copy> Waiting = new ConcurrentHashMap<>();

    private Scp902Trip() {
    }

    public static boolean eligible(ServerPlayer User) {
        Scp902State State = Scp902State.get(User.server);
        return Scp902Box.canUse(User) && !User.isSleeping() && !State.visited(User.getUUID()) && State.trip(User.getUUID()) == null
                && !Scp902Wastes.inside(User) && Scp902Wastes.level(User.server) != null;
    }

    public static boolean begin(ServerPlayer User) {
        if (!eligible(User)) {
            return false;
        }
        return start(User);
    }

    public static boolean start(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Scp902State State = Scp902State.get(Server);
        ServerLevel Wastes = Scp902Wastes.level(Server);
        if (Wastes == null || State.trip(User.getUUID()) != null || Scp902Wastes.inside(User)) {
            return false;
        }
        ServerLevel Here = User.serverLevel();
        Scp902Copy Copy = Scp902Copy.of(User);
        int Slot = State.freeSlot();
        Scp902State.Trip Made = new Scp902State.Trip(User.getUUID(), Here.dimension(), User.position(), User.getYRot(), User.getXRot(), Slot, Copy.getUUID());
        State.markVisited(User.getUUID());
        State.start(Made);
        Waiting.put(User.getUUID(), Copy);
        Scp902Wastes.build(Wastes, Slot);
        hold(Wastes, Scp902Wastes.arrival(Slot), Scp902Wastes.ArrivalTicketRadius);
        Here.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp902Registry.DepartSwell.get(), SoundSource.PLAYERS, DepartVolume, 1.0f);
        Here.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp902Registry.DepartAir.get(), SoundSource.PLAYERS, DepartVolume * 0.6f, 1.0f);
        send(User, Scp902ScenePayload.Depart, Scp902Scene.DepartTicks);
        return true;
    }

    public static void tick(MinecraftServer Server) {
        Scp902State State = Scp902State.get(Server);
        for (Scp902State.Trip Each : State.trips()) {
            ServerPlayer User = Server.getPlayerList().getPlayer(Each.Owner);
            if (User == null || User.isRemoved()) {
                continue;
            }
            Each.Ticks++;
            if (Each.Stage == Scp902State.StageLeaving) {
                if (Each.Ticks >= Scp902Scene.DepartTicks) {
                    arrive(User, Each);
                }
            } else if (Each.Stage == Scp902State.StageScene) {
                if (!Scp902Wastes.inside(User)) {
                    abandon(User, Each);
                    continue;
                }
                if (!Each.Placed && Each.Ticks >= CopyDelay) {
                    place(Server, User, Each);
                }
                keep(User, Each);
                guard(User, Each);
                if (Each.Hurried || Each.Ticks >= Scp902Scene.ReturnAt) {
                    home(User, Each);
                }
            }
        }
        Scp902Cleanup.tick(Server);
        if (Server.getTickCount() % EvictEvery == 0 && !Scp902Wastes.standingIn()) {
            ServerLevel Wastes = Scp902Wastes.level(Server);
            if (Wastes != null) {
                for (ServerPlayer User : new ArrayList<>(Wastes.players())) {
                    if (State.trip(User.getUUID()) == null && !Scp902Wastes.exempt(User)) {
                        sendToRespawn(User);
                    }
                }
            }
        }
    }

    public static boolean arrive(ServerPlayer User, Scp902State.Trip Each) {
        ServerLevel Wastes = Scp902Wastes.level(User.server);
        if (Wastes == null) {
            home(User, Each);
            return false;
        }
        Scp902Wastes.ensureBuilt(Wastes, Each.Slot);
        Vec3 Spot = Scp902Wastes.safeArrival(Wastes, Each.Slot);
        hold(Wastes, Spot, Scp902Wastes.ArrivalTicketRadius);
        ServerLevel Home = User.server.getLevel(Each.Dimension);
        if (Home != null) {
            hold(Home, Each.Spot, 1);
        }
        User.closeContainer();
        User.stopRiding();
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        User.teleportTo(Wastes, Spot.x, Spot.y, Spot.z, Scp902Wastes.ArrivalYaw, Scp902Wastes.ArrivalPitch);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
        Each.Stage = Scp902State.StageScene;
        Each.Ticks = 0;
        Each.Food = User.getFoodData().getFoodLevel();
        Each.Saturation = User.getFoodData().getSaturationLevel();
        slow(User, true);
        Scp902State.get(User.server).setDirty();
        send(User, Scp902ScenePayload.Arrive, Scp902Scene.ReturnAt);
        return true;
    }

    public static void hold(ServerLevel World, Vec3 Spot, int Radius) {
        BlockPos Centre = BlockPos.containing(Spot);
        World.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(Centre), Radius, Centre);
    }

    public static boolean place(MinecraftServer Server, ServerPlayer User, Scp902State.Trip Each) {
        Each.Placed = true;
        Scp902State.get(Server).setDirty();
        Scp902Copy Copy = Waiting.remove(Each.Owner);
        ServerLevel Home = Server.getLevel(Each.Dimension);
        if (Home == null) {
            return false;
        }
        if (Copy == null || Copy.level() != Home) {
            Copy = Scp902Copy.of(User, Home);
            Copy.stand(Each.Spot, Each.Yaw, Each.Pitch);
        }
        Copy.setUUID(Each.Copy);
        hold(Home, Each.Spot, 1);
        return Home.addFreshEntity(Copy);
    }

    public static void forgetAll() {
        Waiting.clear();
    }

    public static boolean waiting(UUID Owner) {
        return Waiting.containsKey(Owner);
    }

    public static boolean guard(ServerPlayer User, Scp902State.Trip Each) {
        if (Each.Stage != Scp902State.StageScene || Each.Ticks < Scp902Scene.ArriveFade || Each.Ticks > Scp902Scene.FinishAt || !Scp902Wastes.inside(User)) {
            Each.Still = 0;
            Each.LastSpot = null;
            return false;
        }
        Vec3 Now = User.position();
        double Along = Now.z - Scp902Wastes.centre(Each.Slot).getZ();
        boolean Sunk = Now.y < Scp902Wastes.ArrivalY - DropLimit;
        boolean Walking = Along > Scp902Wastes.WalkEndZ;
        if (Walking && Each.LastSpot != null && Now.distanceToSqr(Each.LastSpot) < StuckMove * StuckMove) {
            Each.Still++;
        } else {
            Each.Still = 0;
        }
        Each.LastSpot = Now;
        if (!Sunk && Each.Still <= StuckTicks) {
            return false;
        }
        rescue(User, Each);
        Each.Still = 0;
        Each.LastSpot = null;
        Each.Rescues++;
        return true;
    }

    public static void rescue(ServerPlayer User, Scp902State.Trip Each) {
        ServerLevel World = User.serverLevel();
        double From = Math.min(User.getZ(), Scp902Wastes.arrival(Each.Slot).z);
        Vec3 Landing = null;
        for (double Step = RescueStep; Step <= RescueReach; Step += RescueStep) {
            Vec3 Candidate = Scp902Wastes.pathPoint(Each.Slot, From - Step);
            if (Scp902Wastes.clear(World, Candidate)) {
                Landing = Candidate;
                break;
            }
        }
        if (Landing == null) {
            Scp902Wastes.carve(World, Each.Slot);
            Landing = Scp902Wastes.pathPoint(Each.Slot, From - RescueStep);
        }
        User.teleportTo(World, Landing.x, Landing.y, Landing.z, Scp902Wastes.ArrivalYaw, Scp902Wastes.ArrivalPitch);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    public static void keep(ServerPlayer User, Scp902State.Trip Each) {
        if (Each.Food >= 0) {
            User.getFoodData().setFoodLevel(Each.Food);
            User.getFoodData().setSaturation(Each.Saturation);
            User.getFoodData().setExhaustion(0.0f);
        }
        User.setAirSupply(User.getMaxAirSupply());
        User.clearFire();
        User.resetFallDistance();
        User.setTicksFrozen(0);
        if (User.isSprinting()) {
            User.setSprinting(false);
        }
        if (User.getHealth() < 1.0f) {
            User.setHealth(1.0f);
        }
    }

    public static void slow(ServerPlayer User, boolean Applied) {
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance Jump = User.getAttribute(Attributes.JUMP_STRENGTH);
        if (Speed != null) {
            Speed.removeModifier(Trudge.id());
            if (Applied) {
                Speed.addTransientModifier(Trudge);
            }
        }
        if (Jump != null) {
            Jump.removeModifier(Earthbound.id());
            if (Applied) {
                Jump.addTransientModifier(Earthbound);
            }
        }
    }

    public static void hurry(ServerPlayer User) {
        Scp902State.Trip Each = Scp902State.get(User.server).trip(User.getUUID());
        if (Each != null) {
            Each.Hurried = true;
        }
    }

    public static boolean home(ServerPlayer User) {
        Scp902State.Trip Each = Scp902State.get(User.server).trip(User.getUUID());
        if (Each == null) {
            return false;
        }
        home(User, Each);
        return true;
    }

    public static void home(ServerPlayer User, Scp902State.Trip Each) {
        MinecraftServer Server = User.server;
        slow(User, false);
        ServerLevel Home = Server.getLevel(Each.Dimension);
        if (Home == null || Scp902Wastes.inside(Home)) {
            sendToRespawn(User);
        } else {
            User.closeContainer();
            User.stopRiding();
            User.teleportTo(Home, Each.Spot.x, Each.Spot.y, Each.Spot.z, Each.Yaw, Each.Pitch);
            User.setDeltaMovement(Vec3.ZERO);
            User.resetFallDistance();
        }
        dismiss(Server, Each);
        Scp902State.get(Server).finish(Each.Owner);
        send(User, Scp902ScenePayload.Home, Scp902Scene.HomeFade);
    }

    public static void abandon(ServerPlayer User, Scp902State.Trip Each) {
        slow(User, false);
        dismiss(User.server, Each);
        Scp902State.get(User.server).finish(Each.Owner);
        send(User, Scp902ScenePayload.Clear, 0);
    }

    public static void dismiss(MinecraftServer Server, Scp902State.Trip Each) {
        Waiting.remove(Each.Owner);
        ServerLevel Home = Server.getLevel(Each.Dimension);
        if (Home == null) {
            return;
        }
        Entity Found = Home.getEntity(Each.Copy);
        if (Found instanceof Scp902Copy Copy) {
            Copy.discard();
        }
        if (Each.Placed) {
            Home.playSound(null, Each.Spot.x, Each.Spot.y + 1.0, Each.Spot.z, Scp902Registry.CopyFade.get(), SoundSource.PLAYERS, FadeVolume, 1.0f);
        }
    }

    @Nullable
    public static Scp902Copy copyOf(MinecraftServer Server, UUID Owner) {
        Scp902State.Trip Each = Scp902State.get(Server).trip(Owner);
        if (Each == null) {
            return null;
        }
        ServerLevel Home = Server.getLevel(Each.Dimension);
        return Home != null && Home.getEntity(Each.Copy) instanceof Scp902Copy Copy ? Copy : null;
    }

    public static void loggedIn(ServerPlayer User) {
        Scp902State.Trip Each = Scp902State.get(User.server).trip(User.getUUID());
        if (Each != null) {
            home(User, Each);
            return;
        }
        if (Scp902Wastes.inside(User) && !Scp902Wastes.standingIn() && !Scp902Wastes.exempt(User)) {
            sendToRespawn(User);
        }
    }

    public static void sendToRespawn(ServerPlayer User) {
        slow(User, false);
        DimensionTransition Respawn = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        ServerLevel World = Respawn.newLevel();
        Vec3 Spot = Respawn.pos();
        float Yaw = Respawn.yRot();
        if (Scp902Wastes.inside(World)) {
            World = User.server.overworld();
            BlockPos Shared = World.getSharedSpawnPos();
            Spot = Vec3.atBottomCenterOf(Shared);
            Yaw = 0.0f;
        }
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
        send(User, Scp902ScenePayload.Clear, 0);
    }

    public static void send(ServerPlayer User, int Kind, int Ticks) {
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new Scp902ScenePayload(Kind, Ticks));
        }
    }
}
