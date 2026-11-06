package net.rai.scp_unleashed.mtf;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MtfMissions {
    public enum Outcome {
        Called,
        Busy,
        NoSky,
        Failed
    }

    public static final String Prefix = "message.scp_unleashed.mtf.";
    public static final int AimInterval = 4;
    public static final double AimReach = 64.0;
    public static final double PickupReach = 48.0;
    public static final double InstantRing = 2.5;
    public static final int SquadCooldown = 6000;
    private static final Map<UUID, MtfMission> Missions = new LinkedHashMap<>();

    private MtfMissions() {
    }

    @Nullable
    public static MtfMission get(UUID Caller) {
        return Missions.get(Caller);
    }

    public static Collection<MtfMission> all() {
        return Collections.unmodifiableCollection(Missions.values());
    }

    public static boolean busy(UUID Caller) {
        return Missions.containsKey(Caller);
    }

    public static void clear() {
        Missions.clear();
    }

    public static long cooldownLeft(ServerPlayer Caller) {
        return Math.max(0L, Caller.getData(MtfRegistry.NextCall) - Caller.server.overworld().getGameTime());
    }

    private static void rest(MinecraftServer Server, UUID Caller) {
        ServerPlayer Online = Server.getPlayerList().getPlayer(Caller);
        if (Online == null) {
            return;
        }
        Online.setData(MtfRegistry.NextCall, Server.overworld().getGameTime() + SquadCooldown);
        Online.getCooldowns().addCooldown(MtfRegistry.SignalRadio.get(), SquadCooldown);
    }

    public static void cancel(MinecraftServer Server, UUID Caller) {
        MtfMission Mission = Missions.remove(Caller);
        if (Mission == null) {
            return;
        }
        rest(Server, Caller);
        ServerLevel World = Server.getLevel(Mission.dimension());
        if (World != null) {
            dismissAll(World, Mission);
        }
    }

    public static Outcome call(ServerPlayer Caller, MtfTier Tier) {
        if (busy(Caller.getUUID())) {
            return Outcome.Busy;
        }
        ServerLevel World = Caller.serverLevel();
        Optional<MtfLandingZone.Zone> Found = MtfLandingZone.find(World, Caller.position(), Caller.getYRot());
        if (Found.isEmpty()) {
            return Outcome.NoSky;
        }
        MtfMission Mission = new MtfMission(Caller.getUUID(), World.dimension(), Tier, Found.get(), World.getGameTime());
        Missions.put(Caller.getUUID(), Mission);
        if (MtfHelicopter.dispatch(World, Mission, false) == null) {
            Missions.remove(Caller.getUUID());
            return Outcome.Failed;
        }
        Caller.sendSystemMessage(Component.translatable(Prefix + "called", Tier.displayName(), Tier.size()).withStyle(ChatFormatting.GRAY));
        DiscordNotices.mtfCalled(Caller, Tier, Found.get().Ground());
        return Outcome.Called;
    }

    public static Outcome deployNow(ServerPlayer Caller, MtfTier Tier) {
        if (busy(Caller.getUUID())) {
            return Outcome.Busy;
        }
        ServerLevel World = Caller.serverLevel();
        MtfLandingZone.Zone Zone = MtfLandingZone.find(World, Caller.position(), Caller.getYRot())
                .orElse(new MtfLandingZone.Zone(Caller.blockPosition(), Caller.getY() + MtfLandingZone.DefaultHover, Caller.getYRot()));
        MtfMission Mission = new MtfMission(Caller.getUUID(), World.dimension(), Tier, Zone, World.getGameTime());
        Missions.put(Caller.getUUID(), Mission);
        for (int Index = 0; Index < Tier.size(); Index++) {
            MtfOperative Made = MtfRegistry.OperativeType.get().create(World);
            if (Made == null) {
                continue;
            }
            Made.configure(Caller.getUUID(), Tier, Tier.roles().get(Index), Index, World.getRandom());
            double Angle = Index * Math.PI * 2.0 / Tier.size();
            Made.moveTo(Caller.getX() + Math.cos(Angle) * InstantRing, Caller.getY(), Caller.getZ() + Math.sin(Angle) * InstantRing, Caller.getYRot(), 0.0f);
            Made.setState(MtfOperative.StateActive);
            if (World.addFreshEntity(Made)) {
                Mission.enlist(Made.getUUID());
            }
        }
        Mission.start(World.getGameTime());
        return Outcome.Called;
    }

    public static boolean requestExtraction(ServerPlayer Caller) {
        MtfMission Mission = get(Caller.getUUID());
        if (Mission == null || Mission.stage() == MtfMission.Stage.Extracting) {
            return false;
        }
        Mission.requestExtraction();
        return true;
    }

    public static void report(ServerPlayer Caller) {
        MtfMission Mission = get(Caller.getUUID());
        if (Mission == null) {
            Caller.displayClientMessage(Component.translatable(Prefix + "none").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Component Text = switch (Mission.stage()) {
            case Inbound -> Component.translatable(Prefix + "status.inbound", Mission.tier().displayName());
            case Deploying -> Component.translatable(Prefix + "status.deploying", Mission.tier().displayName());
            case Active -> Component.translatable(Prefix + "status.active", Mission.tier().displayName(), Mission.alive(), Mission.deployed(),
                    StringUtil.formatTickDuration(Mission.remaining(Caller.level().getGameTime()), Caller.level().tickRateManager().tickrate()));
            case Extracting -> Component.translatable(Prefix + "status.extracting", Mission.alive());
        };
        Caller.displayClientMessage(Text.copy().withStyle(ChatFormatting.GRAY), true);
    }

    public static void tick(MinecraftServer Server) {
        if (Missions.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, MtfMission>> Each = Missions.entrySet().iterator();
        while (Each.hasNext()) {
            MtfMission Mission = Each.next().getValue();
            ServerLevel World = Server.getLevel(Mission.dimension());
            ServerPlayer Caller = Server.getPlayerList().getPlayer(Mission.caller());
            if (World == null) {
                Each.remove();
                rest(Server, Mission.caller());
                continue;
            }
            if (Caller == null || Caller.level() != World) {
                dismissAll(World, Mission);
                if (Caller != null) {
                    Caller.sendSystemMessage(Component.translatable(Prefix + "lost_contact").withStyle(ChatFormatting.GRAY));
                }
                Each.remove();
                rest(Server, Mission.caller());
                continue;
            }
            if (!advance(World, Caller, Mission)) {
                Each.remove();
                rest(Server, Mission.caller());
            }
        }
    }

    private static boolean advance(ServerLevel World, ServerPlayer Caller, MtfMission Mission) {
        long Now = World.getGameTime();
        prune(World, Mission);
        if (Now % AimInterval == 0L) {
            trackAim(World, Caller, Mission, Now);
        }
        MtfHelicopter Ride = helicopter(World, Mission);
        switch (Mission.stage()) {
            case Inbound, Deploying -> {
                if (Ride == null) {
                    if (Mission.alive() == 0) {
                        dismissAll(World, Mission);
                        Caller.sendSystemMessage(Component.translatable(Prefix + "lost_contact").withStyle(ChatFormatting.GRAY));
                        return false;
                    }
                    Mission.start(Now);
                }
                if (Mission.extractRequested() || !Caller.isAlive()) {
                    return beginExtraction(World, Caller, Mission);
                }
            }
            case Active -> {
                if (Mission.alive() == 0) {
                    if (Mission.wipedOut()) {
                        Caller.sendSystemMessage(Component.translatable(Prefix + "lost", Mission.tier().displayName()).withStyle(ChatFormatting.RED));
                    } else {
                        Caller.sendSystemMessage(Component.translatable(Prefix + "lost_contact").withStyle(ChatFormatting.GRAY));
                    }
                    dismissAll(World, Mission);
                    return false;
                }
                if (Mission.extractRequested() || Now >= Mission.ends() || !Caller.isAlive()) {
                    return beginExtraction(World, Caller, Mission);
                }
            }
            case Extracting -> {
                if (Ride == null) {
                    finish(World, Caller, Mission);
                    return false;
                }
                Vec3 Point = regroupPoint(World, Mission);
                for (UUID Id : Mission.operatives()) {
                    if (World.getEntity(Id) instanceof MtfOperative Member && Member.state() == MtfOperative.StateActive) {
                        Member.regroup(Point.add(sideOffset(Mission, Member.slot())));
                    }
                }
            }
        }
        return true;
    }

    private static Vec3 regroupPoint(ServerLevel World, MtfMission Mission) {
        MtfLandingZone.Zone Zone = Mission.zone();
        return new Vec3(Zone.Ground().getX() + 0.5, Zone.Ground().getY(), Zone.Ground().getZ() + 0.5);
    }

    private static Vec3 sideOffset(MtfMission Mission, int Slot) {
        MtfLandingZone.Zone Zone = Mission.zone();
        int Side = Slot % 2;
        Vec3 Stand = MtfLandingZone.standPoint(Zone.hover(), Zone.Yaw(), Side);
        Vec3 Centre = Zone.hover();
        return new Vec3(Stand.x - Centre.x, 0.0, Stand.z - Centre.z);
    }

    private static void prune(ServerLevel World, MtfMission Mission) {
        List<UUID> Gone = new ArrayList<>();
        for (UUID Id : Mission.operatives()) {
            Entity Found = World.getEntity(Id);
            if (!(Found instanceof MtfOperative Member) || !Member.isAlive()) {
                Gone.add(Id);
            }
        }
        Mission.operatives().removeAll(Gone);
    }

    @Nullable
    public static MtfHelicopter helicopter(ServerLevel World, MtfMission Mission) {
        UUID Id = Mission.helicopter();
        if (Id == null) {
            return null;
        }
        return World.getEntity(Id) instanceof MtfHelicopter Found && Found.isAlive() ? Found : null;
    }

    private static void trackAim(ServerLevel World, ServerPlayer Caller, MtfMission Mission, long Now) {
        if (!Caller.isUsingItem()) {
            return;
        }
        ItemStack Used = Caller.getUseItem();
        if (!(Used.getItem() instanceof GunItem || Used.getItem() instanceof ProjectileWeaponItem || Used.getItem() instanceof TridentItem)) {
            return;
        }
        Vec3 Eye = Caller.getEyePosition();
        Vec3 End = Eye.add(Caller.getViewVector(1.0f).scale(AimReach));
        HitResult Wall = World.clip(new ClipContext(Eye, End, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Caller));
        if (Wall.getType() != HitResult.Type.MISS) {
            End = Wall.getLocation();
        }
        AABB Sweep = Caller.getBoundingBox().expandTowards(End.subtract(Eye)).inflate(1.0);
        EntityHitResult Hit = ProjectileUtil.getEntityHitResult(Caller, Eye, End, Sweep,
                Candidate -> Candidate instanceof LivingEntity && !Candidate.isSpectator() && !MtfFriends.isFriend(Caller.getUUID(), Candidate), AimReach * AimReach);
        if (Hit != null && Hit.getEntity() instanceof LivingEntity Aimed) {
            Mission.noteAimed(Aimed, Now);
        }
    }

    private static boolean beginExtraction(ServerLevel World, ServerPlayer Caller, MtfMission Mission) {
        Mission.setStage(MtfMission.Stage.Extracting);
        MtfHelicopter Existing = helicopter(World, Mission);
        if (Existing != null && !Existing.isDeparting()) {
            Existing.becomePickup(Mission.zone());
        } else {
            Mission.setZone(pickupZone(World, Caller, Mission));
            if (MtfHelicopter.dispatch(World, Mission, true) == null) {
                finish(World, Caller, Mission);
                return false;
            }
        }
        Vec3 Point = regroupPoint(World, Mission);
        for (UUID Id : Mission.operatives()) {
            if (World.getEntity(Id) instanceof MtfOperative Member) {
                Member.regroup(Point.add(sideOffset(Mission, Member.slot())));
            }
        }
        Caller.sendSystemMessage(Component.translatable(Prefix + "extraction", Mission.tier().displayName()).withStyle(ChatFormatting.GRAY));
        return true;
    }

    private static MtfLandingZone.Zone pickupZone(ServerLevel World, ServerPlayer Caller, MtfMission Mission) {
        List<Vec3> Candidates = new ArrayList<>();
        Vec3 Squad = null;
        for (UUID Id : Mission.operatives()) {
            if (World.getEntity(Id) instanceof MtfOperative Member && Member.onGround()) {
                Squad = Member.position();
                break;
            }
        }
        if (Caller.isAlive() && Caller.onGround() && (Squad == null || Squad.distanceTo(Caller.position()) < PickupReach)) {
            Candidates.add(Caller.position());
        }
        if (Squad != null) {
            Candidates.add(Squad);
        }
        Candidates.add(Vec3.atBottomCenterOf(Mission.zone().Ground()));
        for (Vec3 Spot : Candidates) {
            Optional<MtfLandingZone.Zone> Found = MtfLandingZone.find(World, Spot, Mission.zone().Yaw());
            if (Found.isPresent()) {
                return Found.get();
            }
        }
        return Mission.zone();
    }

    private static void finish(ServerLevel World, ServerPlayer Caller, MtfMission Mission) {
        int Stragglers = Mission.alive();
        dismissAll(World, Mission);
        if (Mission.deployed() == 0) {
            Caller.sendSystemMessage(Component.translatable(Prefix + "cancelled").withStyle(ChatFormatting.GRAY));
        } else if (Mission.boarded() > 0) {
            Caller.sendSystemMessage(Component.translatable(Prefix + "extracted", Mission.tier().displayName(), Mission.boarded(), Mission.deployed())
                    .withStyle(ChatFormatting.GRAY));
        } else if (Stragglers == 0 && Mission.wipedOut()) {
            Caller.sendSystemMessage(Component.translatable(Prefix + "lost", Mission.tier().displayName()).withStyle(ChatFormatting.RED));
        } else {
            Caller.sendSystemMessage(Component.translatable(Prefix + "lost_contact").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void dismissAll(ServerLevel World, MtfMission Mission) {
        for (UUID Id : List.copyOf(Mission.operatives())) {
            Entity Found = World.getEntity(Id);
            if (Found != null) {
                Found.discard();
            }
        }
        Mission.operatives().clear();
        MtfHelicopter Ride = helicopter(World, Mission);
        if (Ride != null) {
            Ride.discard();
        }
        Mission.setHelicopter(null);
    }

    public static void deploying(MtfHelicopter Ride, MtfMission Mission) {
        if (Mission.stage() == MtfMission.Stage.Inbound) {
            Mission.setStage(MtfMission.Stage.Deploying);
        }
        ServerPlayer Caller = caller(Ride);
        if (Caller != null) {
            Caller.displayClientMessage(Component.translatable(Prefix + "deploying", Mission.tier().displayName()).withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void deployed(MtfHelicopter Ride, MtfMission Mission) {
        if (Mission.stage() == MtfMission.Stage.Deploying || Mission.stage() == MtfMission.Stage.Inbound) {
            Mission.start(Ride.level().getGameTime());
            ServerPlayer Caller = caller(Ride);
            if (Caller != null) {
                Caller.displayClientMessage(Component.translatable(Prefix + "on_station", Mission.tier().displayName(),
                        StringUtil.formatTickDuration(Mission.tier().durationTicks(), Ride.level().tickRateManager().tickrate())).withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    public static void pickupReady(MtfHelicopter Ride, MtfMission Mission) {
        ServerPlayer Caller = caller(Ride);
        if (Caller != null) {
            Caller.displayClientMessage(Component.translatable(Prefix + "pickup").withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void departing(MtfHelicopter Ride) {
        UUID Id = Ride.callerId();
        MtfMission Mission = Id == null ? null : get(Id);
        if (Mission == null || !Ride.getUUID().equals(Mission.helicopter())) {
            return;
        }
        if (Mission.stage() != MtfMission.Stage.Extracting) {
            Mission.setHelicopter(null);
        }
    }

    public static void boarded(MtfOperative Member) {
        UUID Id = Member.callerId();
        MtfMission Mission = Id == null ? null : get(Id);
        if (Mission != null) {
            Mission.board(Member.getUUID());
        }
    }

    public static void fallen(MtfOperative Member) {
        UUID Id = Member.callerId();
        MtfMission Mission = Id == null ? null : get(Id);
        if (Mission != null) {
            Mission.fall(Member.getUUID());
        }
    }

    @Nullable
    private static ServerPlayer caller(MtfHelicopter Ride) {
        UUID Id = Ride.callerId();
        if (Id == null || Ride.level().getServer() == null) {
            return null;
        }
        return Ride.level().getServer().getPlayerList().getPlayer(Id);
    }

    public static void noteHit(ServerPlayer Caller, LivingEntity Victim) {
        MtfMission Mission = get(Caller.getUUID());
        if (Mission != null && Victim != Caller && !MtfFriends.isFriend(Caller.getUUID(), Victim)) {
            Mission.noteEngaged(Victim, Caller.level().getGameTime());
        }
    }

    public static void noteAttack(ServerPlayer Caller, LivingEntity Attacker) {
        MtfMission Mission = get(Caller.getUUID());
        if (Mission != null && !MtfFriends.isFriend(Caller.getUUID(), Attacker)) {
            Mission.noteAttacker(Attacker, Caller.level().getGameTime());
        }
    }

    public static void playLayers(ServerPlayer User, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            User.level().playSound(null, User.getX(), User.getEyeY(), User.getZ(), Layer.get(), SoundSource.PLAYERS, 0.8f, Pitch);
        }
    }
}
