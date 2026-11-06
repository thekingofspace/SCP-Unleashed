package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.gateway.GatewayCurios;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp860Trip {
    public static final int ReentryDelay = 40;
    public static final int SafeEvery = 20;
    public static final int ArrivalRadius = 2;
    public static final float SwellVolume = 0.9f;
    private static final Map<UUID, Long> LeftAt = new ConcurrentHashMap<>();

    public enum Reason {
        Exit,
        Login,
        Command,
        Away
    }

    public record Spot(ServerLevel World, Vec3 Pos, float Yaw) {
    }

    private Scp860Trip() {
    }

    public static Scp860Record record(ServerPlayer User) {
        return Scp860Forest.record(User);
    }

    public static boolean resting(ServerPlayer User) {
        Long When = LeftAt.get(User.getUUID());
        return When != null && User.server.overworld().getGameTime() - When < ReentryDelay;
    }

    public static boolean enter(ServerPlayer User, Scp860DoorBlockEntity Door, Vec3 From) {
        if (!(Door.getLevel() instanceof ServerLevel Here)) {
            return false;
        }
        Direction Side = Door.sideOf(From);
        BlockPos Front = Door.getBlockPos().relative(Side, 1);
        Vec3 Safe = GatewayTravel.safeSpot(Here, Vec3.atBottomCenterOf(Front), EntityType.PLAYER);
        return enter(User, new Spot(Here, Safe, Side.toYRot()), Here.dimension(), Door.getBlockPos());
    }

    public static boolean enter(ServerPlayer User, Spot Return, @Nullable net.minecraft.resources.ResourceKey<Level> DoorDimension, @Nullable BlockPos DoorPos) {
        MinecraftServer Server = User.server;
        ServerLevel Forest = Scp860Forest.level(Server);
        Scp860Record Ledger = record(User);
        if (Forest == null || !User.isAlive() || User.isSpectator() || Ledger.Active || Scp860Forest.inside(User) || resting(User)) {
            return false;
        }
        Scp860Plan Layout = Scp860Forest.plan(Server);
        Scp860Builder.buildAround(Forest, Scp860Forest.baseX() + Layout.ArrivalX, Scp860Forest.baseZ() + Layout.ArrivalZ, ArrivalRadius);
        stash(User, Ledger);
        Ledger.Active = true;
        Ledger.Session = UUID.randomUUID();
        Ledger.ReturnDimension = Return.World().dimension();
        Ledger.ReturnPos = Return.Pos();
        Ledger.ReturnYaw = Return.Yaw();
        Ledger.DoorDimension = DoorDimension;
        Ledger.DoorPos = DoorPos;
        Ledger.Died = false;
        Ledger.Entered = Server.overworld().getGameTime();
        Scp860State.get(Server).countTrip();
        ServerLevel Here = User.serverLevel();
        Scp860Sounds.play(Here, User.position(), Scp860Registry.EnterLayers, SoundSource.PLAYERS, SwellVolume, 1.0f);
        Vec3 Arrival = Scp860Forest.arrival(Server, Scp860Forest.participants(Forest).size());
        Vec3 Landing = landing(Forest, Arrival);
        GatewayTravel.move(User, Forest, Landing, Layout.ArrivalYaw);
        Scp860Director.joined(User);
        return true;
    }

    public static Vec3 landing(ServerLevel Forest, Vec3 Arrival) {
        for (int Lift = 0; Lift <= 3; Lift++) {
            Vec3 Spot = Arrival.add(0.0, Lift, 0.0);
            if (Forest.noCollision(EntityType.PLAYER.getDimensions().makeBoundingBox(Spot))) {
                return Spot;
            }
        }
        return GatewayTravel.safeSpot(Forest, Arrival, EntityType.PLAYER);
    }

    public static void stash(ServerPlayer User, Scp860Record Ledger) {
        GatewayStash.settleHands(User);
        HolderLookup.Provider Lookup = User.registryAccess();
        Inventory Pockets = User.getInventory();
        ListTag Items = new ListTag();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.isEmpty()) {
                continue;
            }
            CompoundTag Row = new CompoundTag();
            Row.putInt("Slot", Slot);
            Row.put("Item", Stack.save(Lookup));
            Items.add(Row);
        }
        Ledger.Items = Items;
        Ledger.Curios = GatewayStash.curiosLoaded() ? GatewayCurios.capture(User, Lookup) : new ListTag();
        Pockets.clearContent();
        if (GatewayStash.curiosLoaded()) {
            GatewayCurios.wipe(User);
        }
        User.inventoryMenu.broadcastChanges();
    }

    public static int restore(ServerPlayer User) {
        Scp860Record Ledger = record(User);
        if (!Ledger.Active) {
            return 0;
        }
        HolderLookup.Provider Lookup = User.registryAccess();
        Inventory Pockets = User.getInventory();
        List<ItemStack> Later = new ArrayList<>();
        int Count = 0;
        for (Tag Raw : Ledger.Items) {
            CompoundTag Row = (CompoundTag) Raw;
            ItemStack Stack = ItemStack.parse(Lookup, Row.getCompound("Item")).orElse(ItemStack.EMPTY);
            if (Stack.isEmpty()) {
                continue;
            }
            Count++;
            int Slot = Row.getInt("Slot");
            if (Slot >= 0 && Slot < Pockets.getContainerSize() && Pockets.getItem(Slot).isEmpty()) {
                Pockets.setItem(Slot, Stack);
            } else {
                Later.add(Stack);
            }
        }
        if (GatewayStash.curiosLoaded()) {
            Later.addAll(GatewayCurios.restore(User, Ledger.Curios, Lookup));
        }
        boolean Died = Ledger.Died;
        int Levels = Ledger.DeathLevels;
        float Progress = Ledger.DeathProgress;
        int Total = Ledger.DeathTotal;
        Ledger.clearTrip();
        Ledger.Restores++;
        for (ItemStack Stack : Later) {
            Pockets.add(Stack);
            if (!Stack.isEmpty()) {
                ItemEntity Dropped = User.drop(Stack, false, false);
                if (Dropped != null) {
                    Dropped.setNoPickUpDelay();
                    Dropped.setTarget(User.getUUID());
                    Dropped.setUnlimitedLifetime();
                }
            }
        }
        if (Died && (Total > 0 || Levels > 0)) {
            if (User.totalExperience <= 0 && User.experienceLevel <= 0) {
                User.experienceLevel = Levels;
                User.experienceProgress = Progress;
                User.totalExperience = Total;
            } else {
                User.giveExperiencePoints(Math.max(0, Total));
            }
        }
        User.inventoryMenu.broadcastChanges();
        return Count;
    }

    public static Spot returnSpot(ServerPlayer User) {
        MinecraftServer Server = User.server;
        Scp860Record Ledger = record(User);
        if (Ledger.hasReturn()) {
            ServerLevel World = Server.getLevel(Ledger.ReturnDimension);
            if (usable(World)) {
                return new Spot(World, GatewayTravel.safeSpot(World, Ledger.ReturnPos, EntityType.PLAYER), Ledger.ReturnYaw);
            }
        }
        if (Ledger.hasSafe()) {
            ServerLevel World = Server.getLevel(Ledger.SafeDimension);
            if (usable(World)) {
                return new Spot(World, GatewayTravel.safeSpot(World, Ledger.SafePos, EntityType.PLAYER), User.getYRot());
            }
        }
        GatewayStash.Spot Respawn = GatewayTravel.respawnSpot(User);
        ServerLevel World = Server.getLevel(Respawn.Dimension());
        if (!usable(World)) {
            World = Server.overworld();
            return new Spot(World, Vec3.atBottomCenterOf(World.getSharedSpawnPos()), 0.0f);
        }
        return new Spot(World, Respawn.Pos(), Respawn.Yaw());
    }

    public static boolean usable(@Nullable ServerLevel World) {
        return World != null && !Scp860Forest.inside(World) && !GatewayDestinations.sealed(World.dimension());
    }

    public static boolean leave(ServerPlayer User, Reason Why) {
        Scp860Record Ledger = record(User);
        if (!Ledger.Active) {
            return false;
        }
        List<Scp860Watcher> Chasers = Why == Reason.Exit && Scp860Forest.inside(User) ? Scp860Watcher.chasers(User) : List.of();
        Spot Back = returnSpot(User);
        ServerLevel From = User.serverLevel();
        if (Scp860Forest.inside(From)) {
            Scp860Sounds.play(From, User.position(), Scp860Registry.LeaveLayers, SoundSource.PLAYERS, SwellVolume, 1.0f);
        }
        GatewayTravel.move(User, Back.World(), Back.Pos(), Back.Yaw());
        restore(User);
        Scp860Director.left(User);
        LeftAt.put(User.getUUID(), User.server.overworld().getGameTime());
        Scp860Sounds.play(Back.World(), Back.Pos(), Scp860Registry.LeaveLayers, SoundSource.PLAYERS, SwellVolume, 0.85f);
        int Index = 0;
        for (Scp860Watcher Chaser : Chasers) {
            Scp860Watcher.followOut(Chaser, User, Back, Index++);
        }
        return true;
    }

    public static void died(ServerPlayer User) {
        Scp860Record Ledger = record(User);
        if (!Ledger.Active) {
            return;
        }
        Ledger.Died = true;
        Ledger.DeathLevels = User.experienceLevel;
        Ledger.DeathProgress = User.experienceProgress;
        Ledger.DeathTotal = User.totalExperience;
        Scp860Director.left(User);
    }

    @Nullable
    public static DimensionTransition respawnTarget(ServerPlayer User) {
        Scp860Record Ledger = record(User);
        if (!Ledger.Active) {
            return null;
        }
        Spot Back = returnSpot(User);
        return new DimensionTransition(Back.World(), Back.Pos(), Vec3.ZERO, Back.Yaw(), 0.0f, DimensionTransition.DO_NOTHING);
    }

    public static int respawned(ServerPlayer User) {
        Scp860Record Ledger = record(User);
        if (!Ledger.Active) {
            return 0;
        }
        if (Scp860Forest.inside(User)) {
            Spot Back = returnSpot(User);
            GatewayTravel.move(User, Back.World(), Back.Pos(), Back.Yaw());
        }
        LeftAt.put(User.getUUID(), User.server.overworld().getGameTime());
        return restore(User);
    }

    public static boolean loggedIn(ServerPlayer User) {
        Scp860Record Ledger = record(User);
        if (Ledger.Active) {
            if (User.isDeadOrDying()) {
                return false;
            }
            return leave(User, Reason.Login);
        }
        if (Scp860Forest.strict(User.level()) && !Scp860Forest.exempt(User)) {
            Spot Back = returnSpot(User);
            GatewayTravel.move(User, Back.World(), Back.Pos(), Back.Yaw());
            return true;
        }
        return false;
    }

    public static void changedDimension(ServerPlayer User, net.minecraft.resources.ResourceKey<Level> From, net.minecraft.resources.ResourceKey<Level> To) {
        if (From == Scp860Forest.key() && To != Scp860Forest.key() && record(User).Active) {
            restore(User);
            Scp860Director.left(User);
        }
    }

    public static void track(ServerPlayer User) {
        if (User.tickCount % SafeEvery != 0 || Scp860Forest.inside(User) || GatewayDestinations.sealed(User.level().dimension())) {
            return;
        }
        if (!User.onGround() || User.isInWater() || User.isInLava() || User.isPassenger() || User.isSpectator() || User.isSleeping()) {
            return;
        }
        Scp860Record Ledger = record(User);
        Ledger.SafeDimension = User.level().dimension();
        Ledger.SafePos = User.position();
    }

    public static void forget(UUID Id) {
        LeftAt.remove(Id);
    }

    public static void forgetAll() {
        LeftAt.clear();
    }
}
