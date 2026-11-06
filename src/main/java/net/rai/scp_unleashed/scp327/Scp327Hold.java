package net.rai.scp_unleashed.scp327;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.scp327.network.Scp327HeldPayload;
import net.rai.scp_unleashed.scp327.network.Scp327Net;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp327Hold {
    public static final ResourceLocation SpeedLock = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_hold_speed");
    public static final ResourceLocation JumpLock = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_hold_jump");
    public static final int AirDrain = 8;
    public static final int DrownInterval = 15;
    public static final float DrownDamage = 3.0f;
    public static final double Pull = 0.55;
    public static final double MaxPull = 0.6;
    public static final int StruggleNeeded = 14;
    public static final int StruggleGap = 3;
    public static final int StruggleDecay = 15;
    public static final double HaulSpeed = 0.22;
    public static final double StepLift = 0.42;
    public static final double StepUp = 1.05;
    public static final double Gravity = 0.08;
    public static final double RouteStep = 0.4;
    public static final double FloorGap = 0.05;
    public static final double[] UnstickReach = {0.3, 0.6, 1.0, 1.5};
    private static final Map<UUID, Grip> Grips = new HashMap<>();

    private Scp327Hold() {
    }

    public static void seize(Scp327Siren Siren, LivingEntity Victim) {
        Grip Held = new Grip(Siren.getUUID(), body(Victim));
        if (Victim instanceof Mob Body) {
            Held.NoAi = Body.isNoAi();
            Body.getNavigation().stop();
            Body.setTarget(null);
            Body.setNoAi(true);
        }
        Grips.put(Victim.getUUID(), Held);
        Victim.stopRiding();
        Victim.setSprinting(false);
        Victim.stopUsingItem();
        lock(Victim);
        if (Victim instanceof ServerPlayer User) {
            Scp327Net.send(User, new Scp327HeldPayload(true));
        }
    }

    public static boolean isHeld(@Nullable LivingEntity Victim) {
        return Victim != null && Grips.containsKey(Victim.getUUID());
    }

    @Nullable
    public static UUID holder(LivingEntity Victim) {
        Grip Held = Grips.get(Victim.getUUID());
        return Held == null ? null : Held.Siren;
    }

    public static boolean heldBy(LivingEntity Victim, Scp327Siren Siren) {
        Grip Held = Grips.get(Victim.getUUID());
        return Held != null && Siren.getUUID().equals(Held.Siren) && Held.Body.equals(body(Victim));
    }

    public static UUID body(LivingEntity Victim) {
        return Victim instanceof Player User ? ControlSessions.session(User).BodyId() : Victim.getUUID();
    }

    public static void release(LivingEntity Victim) {
        Grip Held = Grips.remove(Victim.getUUID());
        if (Held != null && Victim instanceof Mob Body) {
            Body.setNoAi(Held.NoAi);
        }
        unlock(Victim);
        if (Victim instanceof ServerPlayer User && !User.hasDisconnected()) {
            Scp327Net.send(User, new Scp327HeldPayload(false));
        }
    }

    public static void forget(UUID Victim) {
        Grips.remove(Victim);
    }

    public static void clear() {
        Grips.clear();
    }

    public static int struggle(LivingEntity Victim) {
        Grip Held = Grips.get(Victim.getUUID());
        if (Held == null) {
            return 0;
        }
        long Now = Victim.level().getGameTime();
        if (Now - Held.LastPress >= StruggleGap) {
            Held.LastPress = Now;
            Held.Struggle++;
        }
        return Held.Struggle;
    }

    public static int struggled(LivingEntity Victim) {
        Grip Held = Grips.get(Victim.getUUID());
        return Held == null ? 0 : Held.Struggle;
    }

    public static boolean brokeFree(LivingEntity Victim) {
        Grip Held = Grips.get(Victim.getUUID());
        if (Held == null) {
            return false;
        }
        long Now = Victim.level().getGameTime();
        if (Held.Struggle > 0 && Now - Held.LastDecay >= StruggleDecay) {
            Held.LastDecay = Now;
            Held.Struggle--;
        }
        return Held.Struggle >= StruggleNeeded;
    }

    public static void lock(LivingEntity Victim) {
        add(Victim, Attributes.MOVEMENT_SPEED, SpeedLock, -1.0);
        add(Victim, Attributes.JUMP_STRENGTH, JumpLock, -1.0);
    }

    public static void unlock(LivingEntity Victim) {
        remove(Victim, Attributes.MOVEMENT_SPEED, SpeedLock);
        remove(Victim, Attributes.JUMP_STRENGTH, JumpLock);
    }

    private static void add(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id, double Amount) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null && !Instance.hasModifier(Id)) {
            Instance.addTransientModifier(new AttributeModifier(Id, Amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void remove(LivingEntity Victim, Holder<Attribute> Kind, ResourceLocation Id) {
        AttributeInstance Instance = Victim.getAttribute(Kind);
        if (Instance != null) {
            Instance.removeModifier(Id);
        }
    }

    public static boolean clientDriven(LivingEntity Victim) {
        return Victim instanceof ServerPlayer && !(Victim instanceof FakePlayer);
    }

    public static void tether(ServerLevel World, LivingEntity Victim, Vec3 Point) {
        Vec3 Centre = Victim.position().add(0.0, Victim.getBbHeight() * 0.5, 0.0);
        Vec3 Target = safeTarget(World, Victim, Point);
        Vec3 Gap = Target.subtract(Centre);
        double Length = Gap.length();
        Vec3 Want = Length > 1.0e-4 ? Gap.scale(Math.min(MaxPull, Length * Pull) / Length) : Vec3.ZERO;
        Victim.fallDistance = 0.0f;
        Victim.setDeltaMovement(Want);
        if (clientDriven(Victim)) {
            Victim.hurtMarked = true;
            return;
        }
        Victim.move(MoverType.SELF, Want);
    }

    public static Vec3 safeTarget(ServerLevel World, LivingEntity Victim, Vec3 Point) {
        double Half = Victim.getBbHeight() * 0.5;
        BlockPos Column = BlockPos.containing(Point.x, Point.y, Point.z);
        int Surface = Scp327Waters.surfaceAt(World, Column);
        if (Surface == Scp327Waters.NoSurface) {
            Surface = Scp327Waters.surfaceAt(World, Column.above());
        }
        double Y = Point.y;
        if (Surface != Scp327Waters.NoSurface) {
            double Floor = Scp327Waters.floorAt(World, Column.getX(), Surface, Column.getZ());
            Y = Math.max(Y, Floor + Half + FloorGap);
        }
        Vec3 Target = new Vec3(Point.x, Y, Point.z);
        AABB Box = Victim.getDimensions(Victim.getPose()).makeBoundingBox(Target.subtract(0.0, Half, 0.0));
        if (!World.noCollision(Victim, Box)) {
            return Victim.position().add(0.0, Half, 0.0);
        }
        return Target;
    }

    public static void haul(ServerLevel World, LivingEntity Victim, Vec3 Entry) {
        Vec3 Here = Victim.position();
        double Dx = Entry.x - Here.x;
        double Dz = Entry.z - Here.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        double Pace = Math.min(HaulSpeed, Flat);
        Vec3 Step = Flat > 1.0e-3 ? new Vec3(Dx / Flat * Pace, 0.0, Dz / Flat * Pace) : Vec3.ZERO;
        AABB Box = Victim.getBoundingBox();
        boolean Blocked = !World.noCollision(Victim, Box.move(Step));
        boolean Climb = Blocked && World.noCollision(Victim, Box.move(0.0, StepUp, 0.0)) && World.noCollision(Victim, Box.move(0.0, StepUp, 0.0).move(Step));
        Victim.fallDistance = 0.0f;
        if (clientDriven(Victim)) {
            double Rise = Victim.onGround() ? (Climb ? StepLift : 0.0) : Math.max(-0.6, (Victim.getDeltaMovement().y - Gravity) * 0.98);
            Victim.setDeltaMovement(Step.x, Rise, Step.z);
            Victim.hurtMarked = true;
            return;
        }
        if (Climb) {
            Victim.move(MoverType.SELF, new Vec3(0.0, StepUp, 0.0));
        }
        double Fall = Victim.onGround() ? -Gravity : Math.max(-0.6, (Victim.getDeltaMovement().y - Gravity) * 0.98);
        if (wet(Victim)) {
            Fall = Math.max(Fall, -0.1);
        }
        Vec3 Motion = new Vec3(Step.x, Fall, Step.z);
        Victim.setDeltaMovement(Motion);
        Victim.move(MoverType.SELF, Motion);
    }

    public static boolean routeClear(ServerLevel World, LivingEntity Victim, Vec3 Entry) {
        Vec3 Here = Victim.position();
        double Dx = Entry.x - Here.x;
        double Dz = Entry.z - Here.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        int Count = Math.max(1, (int) Math.ceil(Flat / RouteStep));
        double Y = Here.y;
        for (int Index = 1; Index <= Count; Index++) {
            double Share = Index / (double) Count;
            double X = Here.x + Dx * Share;
            double Z = Here.z + Dz * Share;
            if (fits(World, Victim, X, Y, Z)) {
                continue;
            }
            double Raised = Math.floor(Y) + 1.0;
            if (fits(World, Victim, X, Raised, Z) && fits(World, Victim, Here.x + Dx * (Index - 1) / (double) Count, Raised, Here.z + Dz * (Index - 1) / (double) Count)) {
                Y = Raised;
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean fits(ServerLevel World, LivingEntity Victim, double X, double Y, double Z) {
        return World.noCollision(Victim, Victim.getDimensions(Victim.getPose()).makeBoundingBox(new Vec3(X, Y, Z)).deflate(0.01));
    }

    public static boolean stuck(ServerLevel World, LivingEntity Victim) {
        return Victim.isInWall() || !World.noCollision(Victim, Victim.getBoundingBox().deflate(0.02));
    }

    public static boolean unstick(ServerLevel World, LivingEntity Victim, Vec3 Toward) {
        AABB Box = Victim.getBoundingBox();
        Vec3 Lean = Toward.subtract(Victim.position()).multiply(1.0, 0.0, 1.0);
        Lean = Lean.lengthSqr() > 1.0e-4 ? Lean.normalize() : Vec3.ZERO;
        for (double Reach : UnstickReach) {
            Vec3[] Options = {new Vec3(0.0, Reach, 0.0), Lean.scale(Reach), Lean.scale(Reach).add(0.0, Reach, 0.0), new Vec3(Reach, 0.0, 0.0), new Vec3(-Reach, 0.0, 0.0),
                    new Vec3(0.0, 0.0, Reach), new Vec3(0.0, 0.0, -Reach), Lean.scale(-Reach).add(0.0, Reach, 0.0)};
            for (Vec3 Option : Options) {
                if (World.noCollision(Victim, Box.move(Option))) {
                    Vec3 Spot = Victim.position().add(Option);
                    if (clientDriven(Victim)) {
                        ((ServerPlayer) Victim).connection.teleport(Spot.x, Spot.y, Spot.z, Victim.getYRot(), Victim.getXRot());
                    } else {
                        Victim.setPos(Spot.x, Spot.y, Spot.z);
                    }
                    Victim.setDeltaMovement(Vec3.ZERO);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean wet(LivingEntity Victim) {
        if (Victim.isInWater()) {
            return true;
        }
        AABB Box = Victim.getBoundingBox();
        double Probe = Box.minY + 0.1;
        BlockPos Pos = BlockPos.containing(Victim.getX(), Probe, Victim.getZ());
        FluidState Liquid = Victim.level().getFluidState(Pos);
        return Liquid.is(FluidTags.WATER) && Probe < Pos.getY() + Liquid.getHeight(Victim.level(), Pos);
    }

    public static boolean eyesUnder(LivingEntity Victim) {
        Vec3 Eye = Victim.getEyePosition();
        BlockPos Pos = BlockPos.containing(Eye);
        FluidState Liquid = Victim.level().getFluidState(Pos);
        return Liquid.is(FluidTags.WATER) && Eye.y < Pos.getY() + Liquid.getHeight(Victim.level(), Pos);
    }

    public static boolean drown(LivingEntity Victim, DamageSource Source, int HeldTicks) {
        int Air = Victim.getAirSupply();
        Victim.setAirSupply(Math.max(0, Air - AirDrain));
        if (Victim.getAirSupply() > 0 || HeldTicks % DrownInterval != 0) {
            return false;
        }
        return Victim.hurt(Source, DrownDamage);
    }

    private static final class Grip {
        private final UUID Siren;
        private final UUID Body;
        private boolean NoAi;
        private int Struggle;
        private long LastPress = Long.MIN_VALUE / 2;
        private long LastDecay;

        private Grip(UUID Siren, UUID Body) {
            this.Siren = Siren;
            this.Body = Body;
        }
    }
}
