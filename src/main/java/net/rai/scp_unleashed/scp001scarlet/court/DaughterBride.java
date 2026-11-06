package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class DaughterBride extends CourtDaughter {
    public static final int Flail = MoveA;
    public static final int Grapple = MoveB;
    public static final int Vow = MoveC;
    public static final int Scream = MoveD;
    public static final double FlailRadius = 6.5;
    public static final double VowSlack = 10.0;
    public static final int VowTicks = 240;
    public static final int AnchorTicks = 600;
    public static final int AnchorRest = 900;
    public static final double AnchorRadius = 5.5;
    public static final double AnchorHealth = 40.0;
    public static final float SpectralAlpha = 0.55f;
    private static final RawAnimation DragLoop = RawAnimation.begin().thenLoop("drag");
    private static final EntityDataAccessor<Boolean> SpectralData = SynchedEntityData.defineId(DaughterBride.class,
            EntityDataSerializers.BOOLEAN);
    private static final BlockParticleOption Links = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHAIN.defaultBlockState());
    private final List<Bond> Bonds = new ArrayList<>();
    private final List<UUID> Anchors = new ArrayList<>();
    private final List<Scar> Trail = new ArrayList<>();
    private Vec3 GrappleAim = Vec3.ZERO;
    private int Rotation;
    private int AnchorLeft;
    private int AnchorWait;
    private double Spin;

    public record Bond(UUID First, @Nullable UUID Second, Vec3 Post) {
    }

    private record Scar(Vec3 Spot, int Until) {
    }

    private final java.util.Map<Bond, Integer> BondLeft = new java.util.HashMap<>();

    public DaughterBride(EntityType<? extends Monster> Type, Level World) {
        super(Type, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SpectralData, false);
    }

    public boolean spectral() {
        return entityData.get(SpectralData);
    }

    private void spectral(boolean Value) {
        if (spectral() != Value) {
            entityData.set(SpectralData, Value);
        }
    }

    @Override
    public int index() {
        return 1;
    }

    @Override
    public String key() {
        return "bride";
    }

    @Override
    protected BossEvent.BossBarColor barColour() {
        return BossEvent.BossBarColor.PINK;
    }

    @Override
    protected double nearRange() {
        return 3.0;
    }

    @Override
    protected double farRange() {
        return 5.5;
    }

    @Override
    protected int moveLength(int Move) {
        return switch (Move) {
            case Flail -> 36;
            case Grapple -> 44;
            case Vow -> 30;
            case Scream -> 40;
            default -> 20;
        };
    }

    @Override
    protected int shiftLength() {
        return 50;
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Entrance -> "entrance";
            case Death -> "death";
            case Shift, Scream -> "veil";
            case Flail -> "flail";
            case Grapple -> "grapple";
            case Vow -> "link";
            default -> null;
        };
    }

    @Override
    protected RawAnimation moving() {
        return dragging() ? DragLoop : WalkLoop;
    }

    public boolean dragging() {
        return phaseTwo() && lit();
    }

    public List<Bond> bonds() {
        return List.copyOf(Bonds);
    }

    @Override
    protected void chooseMove(LivingEntity Target) {
        Rotation++;
        double Distance = distanceTo(Target);
        int Move;
        if (Bonds.isEmpty() && Rotation % 4 == 2) {
            Move = Vow;
        } else if (Distance > 8.0) {
            Move = Grapple;
        } else if (Rotation % 5 == 4) {
            Move = Scream;
        } else {
            Move = Flail;
        }
        startMove(Move, Target);
        Cooldown = phaseTwo() ? 36 : 48;
    }

    @Override
    protected void moveTick(int Action, int Elapsed, @Nullable LivingEntity Target) {
        ServerLevel World = (ServerLevel) level();
        switch (Action) {
            case Flail -> flail(World, Elapsed, Target);
            case Grapple -> grapple(World, Elapsed, Target);
            case Vow -> vow(World, Elapsed, Target);
            case Scream -> scream(World, Elapsed);
            default -> {
            }
        }
    }

    private void flail(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        getNavigation().stop();
        if (Elapsed < 20) {
            face(Target);
        }
        if (Elapsed == 1) {
            CourtFx.sound(World, position(), "bride_chain", 2.0f, 0.7f);
        }
        if (Elapsed < 20 && Elapsed % 4 == 0) {
            Vec3 Facing = facing();
            for (int Step = -10; Step <= 10; Step++) {
                Vec3 Way = CourtFx.turn(Facing, Step * 10.0);
                Vec3 Spot = position().add(Way.scale(FlailRadius)).add(0.0, 0.15, 0.0);
                World.sendParticles(CourtFx.Crimson, Spot.x, Spot.y, Spot.z, 1, 0.05, 0.02, 0.05, 0.0);
            }
        }
        if (Elapsed == 20) {
            CourtFx.sound(World, position(), "bride_chain", 4.0f, 1.0f);
            Vec3 Facing = facing();
            for (int Step = -10; Step <= 10; Step += 2) {
                Vec3 Spot = position().add(CourtFx.turn(Facing, Step * 10.0).scale(FlailRadius * 0.7)).add(0.0, 1.0, 0.0);
                World.sendParticles(Links, Spot.x, Spot.y, Spot.z, 3, 0.3, 0.2, 0.3, 0.0);
            }
            for (LivingEntity Victim : CourtFx.victimsNear(World, position(), FlailRadius + 0.5)) {
                Vec3 Way = CourtFx.flat(Victim.position().subtract(position()));
                if (Way.dot(Facing) > -0.2 && CourtFx.strike(this, Victim, 10.0f)) {
                    CourtFx.push(Victim, position(), 1.0, 0.3);
                }
            }
        }
    }

    private void grapple(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        getNavigation().stop();
        face(Target);
        if (Elapsed == 1 && Target != null) {
            GrappleAim = Target.position().add(0.0, 1.0, 0.0);
            CourtFx.sound(World, position(), "bride_idle", 2.0f, 1.2f);
        }
        if (Elapsed < 20 && Elapsed % 3 == 0 && Target != null) {
            CourtFx.line(World, CourtFx.Violet, position().add(0.0, 3.5, 0.0), Target.position().add(0.0, 1.0, 0.0), 0.9, 0.03);
        }
        if (Elapsed == 20 && Target != null && Target.isAlive() && distanceTo(Target) < 20.0 && hasLineOfSight(Target)) {
            CourtFx.sound(World, Target.position(), "bride_grapple", 3.0f, 1.0f);
            CourtFx.line(World, Links, position().add(0.0, 3.5, 0.0), Target.position().add(0.0, 1.0, 0.0), 0.5, 0.05);
            Vec3 Pull = position().add(facing().scale(2.5)).subtract(Target.position());
            double Strength = Math.min(2.2, 0.25 + Pull.length() * 0.14);
            Vec3 Step = Pull.normalize().scale(Strength);
            Target.setDeltaMovement(Step.x, 0.35 + Math.max(0.0, Step.y), Step.z);
            Target.hurtMarked = true;
        }
        if (Elapsed > 20 && Elapsed < 32 && Elapsed % 3 == 0) {
            CourtFx.ring(World, CourtFx.Crimson, position().add(facing().scale(2.0)), 2.6, 16, 0.1);
        }
        if (Elapsed == 32) {
            CourtFx.sound(World, position(), "bride_chain", 3.0f, 0.8f);
            Vec3 Spot = position().add(facing().scale(2.0));
            CourtFx.crumble(World, CourtFx.solidAt(World, BlockPos.containing(Spot).below()), Spot, 20, 1.2, 0.1);
            for (LivingEntity Victim : CourtFx.victimsNear(World, Spot, 3.0)) {
                CourtFx.strike(this, Victim, 8.0f);
            }
        }
    }

    private void vow(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        getNavigation().stop();
        face(Target);
        if (Elapsed == 1) {
            spectral(true);
            CourtFx.sound(World, position(), "bride_link", 2.0f, 0.8f);
        }
        if (Elapsed >= moveLength(Vow) - 1) {
            spectral(false);
        }
        if (Elapsed < 15 && Elapsed % 3 == 0) {
            CourtFx.burst(World, CourtFx.Violet, position().add(0.0, 3.0, 0.0), 6, 0.6, 0.0);
        }
        if (Elapsed == 15 && Target != null) {
            bind(World, Target);
        }
    }

    public Bond bind(ServerLevel World, LivingEntity Target) {
        LivingEntity Partner = null;
        double Best = 24.0 * 24.0;
        for (Player Each : World.getEntitiesOfClass(Player.class, Target.getBoundingBox().inflate(24.0), CourtFx::validTarget)) {
            if (Each != Target && Each.distanceToSqr(Target) < Best) {
                Best = Each.distanceToSqr(Target);
                Partner = Each;
            }
        }
        Vec3 Post = Vec3.ZERO;
        if (Partner == null) {
            int Nearest = 0;
            double Closest = Double.MAX_VALUE;
            for (int Index = 0; Index < CourtArena.Pillars; Index++) {
                double Distance = Vec3.atBottomCenterOf(CourtArena.inward(Dais, Index, 2)).distanceToSqr(Target.position());
                if (Distance < Closest) {
                    Closest = Distance;
                    Nearest = Index;
                }
            }
            Post = Vec3.atBottomCenterOf(CourtArena.groundOr(World, CourtArena.inward(Dais, Nearest, 2), Dais));
        }
        Bond Made = new Bond(Target.getUUID(), Partner == null ? null : Partner.getUUID(), Post);
        Bonds.add(Made);
        BondLeft.put(Made, VowTicks);
        CourtFx.sound(World, Target.position(), "bride_link", 3.0f, 1.1f);
        return Made;
    }

    private void scream(ServerLevel World, int Elapsed) {
        getNavigation().stop();
        if (Elapsed == 1) {
            CourtFx.sound(World, position(), "bride_idle", 3.0f, 0.7f);
        }
        if (Elapsed < 24 && Elapsed % 3 == 0) {
            CourtFx.burst(World, CourtFx.Bone, position().add(0.0, getBbHeight() * 0.85, 0.0), 6, 0.5, 0.02);
            CourtFx.ring(World, CourtFx.Bone, position(), 7.0 * (Elapsed / 24.0), 24, 0.2);
        }
        if (Elapsed == 24) {
            CourtFx.sound(World, position(), "bride_scream", 6.0f, 1.0f);
            CourtFx.ring(World, ParticleTypes.CLOUD, position(), 4.0, 32, 0.4);
            CourtFx.ring(World, ParticleTypes.CLOUD, position(), 7.0, 48, 0.4);
            for (LivingEntity Victim : CourtFx.victimsNear(World, position(), 7.5)) {
                CourtFx.strike(this, Victim, 4.0f);
                CourtFx.push(Victim, position(), 1.3, 0.4);
            }
        }
    }

    @Override
    protected void shiftTick(int Elapsed) {
        ServerLevel World = (ServerLevel) level();
        getNavigation().stop();
        if (Elapsed == 1) {
            CourtFx.sound(World, position(), "bride_scream", 6.0f, 0.8f);
        }
        if (Elapsed % 4 == 0) {
            CourtFx.burst(World, CourtFx.Bone, position().add(0.0, getBbHeight() * 0.85, 0.0), 8, 0.6, 0.02);
        }
        if (Elapsed == 30) {
            raiseAnchors(World);
        }
    }

    public List<CourtProp> anchors() {
        List<CourtProp> Found = new ArrayList<>();
        if (level() instanceof ServerLevel World) {
            for (UUID Id : Anchors) {
                if (World.getEntity(Id) instanceof CourtProp Prop && Prop.isAlive() && !Prop.ending()) {
                    Found.add(Prop);
                }
            }
        }
        return Found;
    }

    public void raiseAnchors(ServerLevel World) {
        dropAnchors();
        CourtFx.sound(World, position(), "bride_chain", 6.0f, 0.6f);
        for (int Index = 0; Index < 2; Index++) {
            double Angle = Spin + Index * Math.PI;
            BlockPos Spot = BlockPos.containing(getX() + Math.cos(Angle) * AnchorRadius, getY(), getZ() + Math.sin(Angle) * AnchorRadius);
            CourtProp Anchor = CourtProp.spawn(World, ScarletCourtRegistry.ChainAnchorType.get(), Spot, AnchorTicks + 40);
            if (Anchor != null) {
                double Health = AnchorHealth * (1.0 + 0.5 * Math.max(0, Players - 1));
                Anchor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Health);
                Anchor.setHealth((float) Health);
                Anchor.owner(this);
                Anchors.add(Anchor.getUUID());
                CourtFx.crumble(World, CourtFx.solidAt(World, Spot.below()), Vec3.atBottomCenterOf(Spot), 20, 0.6, 0.2);
            }
        }
        AnchorLeft = AnchorTicks;
        lit(true);
    }

    public void dropAnchors() {
        for (CourtProp Anchor : anchors()) {
            Anchor.end();
        }
        Anchors.clear();
        lit(false);
    }

    @Override
    protected void arenaTick(ServerLevel World) {
        if (spectral() && action() != Vow) {
            spectral(false);
        }
        tickBonds(World);
        if (!phaseTwo() || dying()) {
            return;
        }
        if (lit()) {
            List<CourtProp> Live = anchors();
            if (Live.isEmpty() || --AnchorLeft <= 0) {
                dropAnchors();
                AnchorWait = AnchorRest;
                return;
            }
            Spin += Math.toRadians(4.5);
            for (int Index = 0; Index < Live.size(); Index++) {
                CourtProp Anchor = Live.get(Index);
                double Angle = Spin + Index * Math.PI;
                Vec3 Want = new Vec3(getX() + Math.cos(Angle) * AnchorRadius, getY(), getZ() + Math.sin(Angle) * AnchorRadius);
                BlockPos Ground = CourtArena.ground(World, BlockPos.containing(Want), 4);
                double Y = Ground != null ? Ground.getY() : getY();
                Anchor.moveTo(Want.x, Y, Want.z, Anchor.getYRot() + 9.0f, 0.0f);
                if (tickCount % 3 == 0) {
                    CourtFx.line(World, Links, position().add(0.0, 2.6, 0.0), Anchor.position().add(0.0, 0.5, 0.0), 0.7, 0.03);
                }
                if (tickCount % 5 == 0) {
                    Trail.add(new Scar(Anchor.position(), tickCount + 40));
                }
                for (LivingEntity Victim : CourtFx.victimsNear(World, Anchor.position().add(0.0, 0.5, 0.0), 1.4)) {
                    if (CourtFx.strike(this, Victim, 8.0f)) {
                        CourtFx.push(Victim, Anchor.position(), 0.9, 0.3);
                    }
                }
            }
        } else if (--AnchorWait <= 0 && action() == None && tickCount % 20 == 0) {
            startAction(Shift, shiftLength());
        }
        Iterator<Scar> Each = Trail.iterator();
        while (Each.hasNext()) {
            Scar Mark = Each.next();
            if (Mark.Until() < tickCount) {
                Each.remove();
                continue;
            }
            if (tickCount % 4 == 0) {
                World.sendParticles(ParticleTypes.SMALL_FLAME, Mark.Spot().x, Mark.Spot().y + 0.1, Mark.Spot().z, 1, 0.2, 0.0, 0.2, 0.0);
            }
            if (tickCount % 10 == 0) {
                for (LivingEntity Victim : CourtFx.victimsNear(World, Mark.Spot(), 0.9)) {
                    CourtFx.strike(World.damageSources().magic(), Victim, 2.0f);
                }
            }
        }
    }

    private void tickBonds(ServerLevel World) {
        Iterator<Bond> Each = Bonds.iterator();
        while (Each.hasNext()) {
            Bond Link = Each.next();
            int Left = BondLeft.getOrDefault(Link, 0) - 1;
            Entity First = World.getEntity(Link.First());
            Entity Second = Link.Second() == null ? null : World.getEntity(Link.Second());
            if (Left <= 0 || dying() || !(First instanceof LivingEntity A) || !A.isAlive() || Link.Second() != null && !(Second instanceof LivingEntity B && B.isAlive())) {
                Each.remove();
                BondLeft.remove(Link);
                continue;
            }
            BondLeft.put(Link, Left);
            Vec3 Other = Second != null ? Second.position() : Link.Post();
            if (Left % 5 == 0) {
                CourtFx.line(World, Links, A.position().add(0.0, 1.0, 0.0), Other.add(0.0, 1.0, 0.0), 0.8, 0.02);
                CourtFx.line(World, CourtFx.Violet, A.position().add(0.0, 1.0, 0.0), Other.add(0.0, 1.0, 0.0), 1.6, 0.02);
            }
            if (Left % 20 == 0 && A.position().distanceTo(Other) > VowSlack) {
                CourtFx.strike(World.damageSources().indirectMagic(this, this), A, 2.0f);
                if (Second instanceof LivingEntity B) {
                    CourtFx.strike(World.damageSources().indirectMagic(this, this), B, 2.0f);
                }
                CourtFx.sound(World, A.position(), "bride_link", 0.8f, 1.5f);
            }
        }
    }

    @Override
    protected void dyingTick(ServerLevel World, int Elapsed) {
        if (Elapsed < 40 && Elapsed % 4 == 0) {
            CourtFx.crumble(World, Blocks.CHAIN.defaultBlockState(), position().add(0.0, 2.5, 0.0), 12, 1.2, 1.2);
            CourtFx.sound(World, position(), "bride_chain", 1.0f, 0.6f + Elapsed * 0.01f);
        }
        if (Elapsed >= 36 && Elapsed % 3 == 0) {
            CourtFx.burst(World, CourtFx.Bone, position().add(0.0, getBbHeight() * (1.0 - (Elapsed - 36) / 44.0) * 0.9, 0.0), 10, 1.2, 0.0);
            CourtFx.burst(World, ParticleTypes.CLOUD, position().add(0.0, 1.0, 0.0), 2, 1.0, 0.0);
        }
    }

    @Override
    protected void onDying(ServerLevel World) {
        spectral(false);
        dropAnchors();
        Bonds.clear();
        BondLeft.clear();
        Trail.clear();
    }
}
