package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DaughterEyes extends CourtDaughter {
    public static final int Flare = MoveA;
    public static final int Beam = MoveB;
    public static final int Split = MoveC;
    public static final int Summon = MoveD;
    public static final int FlareWindup = 30;
    public static final double GazeCone = Math.cos(Math.toRadians(35.0));
    public static final float GazeDamage = 9.0f;
    public static final float BeamDamage = 6.0f;
    public static final double BeamLength = 24.0;
    public static final int Images = 3;
    public static final int ImageLife = 300;
    public static final double Hover = 1.2;
    private Vec3 BeamAim = new Vec3(0.0, 0.0, 1.0);
    private int Rotation;

    public DaughterEyes(EntityType<? extends Monster> Type, Level World) {
        super(Type, World);
        setNoGravity(true);
    }

    @Override
    public int index() {
        return 2;
    }

    @Override
    public String key() {
        return "eyes";
    }

    @Override
    protected BossEvent.BossBarColor barColour() {
        return BossEvent.BossBarColor.WHITE;
    }

    public boolean flaring() {
        return lit();
    }

    @Override
    protected double nearRange() {
        return 8.0;
    }

    @Override
    protected double farRange() {
        return 15.0;
    }

    @Override
    protected int moveLength(int Move) {
        return switch (Move) {
            case Flare -> 40;
            case Beam -> 70;
            case Split -> 30;
            case Summon -> 30;
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
            case Shift -> "open";
            case Flare -> "flare";
            case Beam -> "beam";
            case Split -> "split";
            case Summon -> "summon";
            default -> null;
        };
    }

    @Override
    protected void chooseMove(LivingEntity Target) {
        Rotation++;
        int Move;
        boolean Open = phaseTwo();
        if (Rotation % (Open ? 2 : 3) == 0) {
            Move = Flare;
        } else if (Rotation % 7 == 5 && imageCount() == 0) {
            Move = Split;
        } else if (Rotation % 5 == 4) {
            Move = Summon;
        } else {
            Move = Beam;
        }
        startMove(Move, Target);
        Cooldown = Open ? 30 : 46;
    }

    private int imageCount() {
        return level().getEntitiesOfClass(EyesImage.class, getBoundingBox().inflate(48.0)).size();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !dying()) {
            BlockPos Ground = CourtArena.ground((ServerLevel) level(), blockPosition(), 8);
            double Want = (Ground != null ? Ground.getY() : getY()) + Hover + Math.sin(tickCount * 0.05) * 0.25;
            Vec3 Motion = getDeltaMovement();
            setDeltaMovement(Motion.x, Motion.y * 0.6 + (Want - getY()) * 0.08, Motion.z);
        }
    }

    @Override
    protected void steer(@Nullable LivingEntity Target) {
        if (Target == null) {
            setDeltaMovement(getDeltaMovement().multiply(0.8, 1.0, 0.8));
            return;
        }
        getLookControl().setLookAt(Target, 20.0f, 20.0f);
        face(Target);
        Vec3 Centre = Vec3.atBottomCenterOf(Dais);
        Vec3 Way = CourtFx.flat(Target.position().subtract(position()));
        double Distance = distanceTo(Target);
        Vec3 Push = Vec3.ZERO;
        if (Distance > farRange()) {
            Push = Way.scale(0.035);
        } else if (Distance < nearRange()) {
            Push = Way.scale(-0.03);
        } else {
            Push = new Vec3(-Way.z, 0.0, Way.x).scale(0.015);
        }
        if (position().distanceTo(new Vec3(Centre.x, getY(), Centre.z)) > CourtArena.floorRadius() - 4) {
            Push = CourtFx.flat(Centre.subtract(position())).scale(0.04);
        }
        Vec3 Motion = getDeltaMovement();
        setDeltaMovement(Motion.x * 0.85 + Push.x, Motion.y, Motion.z * 0.85 + Push.z);
    }

    @Override
    protected void moveTick(int Action, int Elapsed, @Nullable LivingEntity Target) {
        ServerLevel World = (ServerLevel) level();
        setDeltaMovement(getDeltaMovement().multiply(0.7, 1.0, 0.7));
        face(Target);
        switch (Action) {
            case Flare -> flare(World, Elapsed);
            case Beam -> beam(World, Elapsed, Target);
            case Split -> split(World, Elapsed);
            case Summon -> summon(World, Elapsed);
            default -> {
            }
        }
    }

    private void flare(ServerLevel World, int Elapsed) {
        if (Elapsed == 1) {
            lit(true);
            CourtFx.sound(World, eyes(), "eyes_flare", 6.0f, 1.0f);
        }
        if (Elapsed < FlareWindup && Elapsed % 3 == 0) {
            CourtFx.burst(World, ParticleTypes.END_ROD, eyes(), 6, 1.4, 0.02);
            CourtFx.burst(World, CourtFx.Amber, eyes(), 8, 1.6, 0.0);
        }
        if (Elapsed == FlareWindup) {
            gaze(World);
        }
        if (Elapsed == FlareWindup + 4) {
            lit(false);
        }
    }

    public List<LivingEntity> gaze(ServerLevel World) {
        List<LivingEntity> Seared = new ArrayList<>();
        Vec3 Eyes = eyes();
        CourtFx.sound(World, Eyes, "eyes_flare", 6.0f, 1.4f);
        World.sendParticles(ParticleTypes.FLASH, Eyes.x, Eyes.y, Eyes.z, 1, 0.0, 0.0, 0.0, 0.0);
        for (LivingEntity Victim : CourtFx.victimsNear(World, Eyes, 40.0)) {
            Vec3 Look = Victim.getViewVector(1.0f);
            Vec3 Towards = Eyes.subtract(Victim.getEyePosition()).normalize();
            if (Look.dot(Towards) >= GazeCone && Victim.hasLineOfSight(this)) {
                if (CourtFx.strike(World.damageSources().indirectMagic(this, this), Victim, GazeDamage)) {
                    Victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true));
                    Victim.igniteForSeconds(2.0f);
                    Seared.add(Victim);
                    CourtFx.line(World, CourtFx.Amber, Eyes, Victim.getEyePosition(), 0.8, 0.02);
                }
            }
        }
        return Seared;
    }

    private void beam(ServerLevel World, int Elapsed, @Nullable LivingEntity Target) {
        Vec3 From = eyes();
        if (Elapsed == 1) {
            Vec3 Aim = Target != null ? Target.position().add(0.0, 1.0, 0.0) : From.add(facing().scale(10.0)).subtract(0.0, 4.0, 0.0);
            BeamAim = Aim.subtract(From).normalize();
            CourtFx.sound(World, From, "eyes_idle", 3.0f, 1.3f);
        }
        if (Elapsed < 20 && Elapsed % 3 == 0) {
            for (Vec3 Way : beams(-30.0)) {
                CourtFx.line(World, CourtFx.Amber, From, From.add(Way.scale(BeamLength)), 1.2, 0.0);
            }
        }
        if (Elapsed == 20) {
            CourtFx.sound(World, From, "eyes_beam", 5.0f, 1.0f);
        }
        if (Elapsed >= 20 && Elapsed <= 60) {
            double Angle = -30.0 + 60.0 * (Elapsed - 20) / 40.0;
            for (Vec3 Way : beams(Angle)) {
                Vec3 To = From.add(Way.scale(BeamLength));
                if (Elapsed % 2 == 0) {
                    CourtFx.line(World, ParticleTypes.END_ROD, From, To, 0.9, 0.0);
                    CourtFx.line(World, CourtFx.Amber, From, To, 0.7, 0.05);
                }
                if (Elapsed % 10 == 0) {
                    for (LivingEntity Victim : CourtFx.victims(World, new AABB(From, To).inflate(1.5))) {
                        if (CourtFx.segmentDistance(Victim.position().add(0.0, Victim.getBbHeight() * 0.5, 0.0), From, To) < 1.1) {
                            CourtFx.strike(World.damageSources().indirectMagic(this, this), Victim, BeamDamage);
                        }
                    }
                }
            }
        }
    }

    private List<Vec3> beams(double Angle) {
        List<Vec3> Ways = new ArrayList<>();
        Ways.add(CourtFx.turn(BeamAim, Angle).normalize());
        if (phaseTwo()) {
            Ways.add(CourtFx.turn(BeamAim, -Angle).normalize());
        }
        return Ways;
    }

    private void split(ServerLevel World, int Elapsed) {
        if (Elapsed == 1) {
            CourtFx.sound(World, eyes(), "eyes_idle", 3.0f, 0.8f);
        }
        if (Elapsed < 20 && Elapsed % 3 == 0) {
            CourtFx.burst(World, CourtFx.Amber, eyes(), 10, 2.0, 0.02);
        }
        if (Elapsed == 20) {
            scatterImages(World);
        }
    }

    public List<EyesImage> scatterImages(ServerLevel World) {
        List<EyesImage> Made = new ArrayList<>();
        CourtFx.sound(World, eyes(), "eyes_split", 6.0f, 1.0f);
        Vec3 Centre = position();
        int Mine = random.nextInt(Images + 1);
        double Turn = random.nextDouble() * Math.PI * 2.0;
        for (int Slot = 0; Slot <= Images; Slot++) {
            double Angle = Turn + Slot * Math.PI * 2.0 / (Images + 1);
            Vec3 Spot = Centre.add(Math.cos(Angle) * 6.0, 0.0, Math.sin(Angle) * 6.0);
            if (!CourtKeeper.free(World, Dais, Spot, getBbWidth(), getBbHeight())) {
                Vec3 Clear = CourtKeeper.clear(World, Dais, Spot, getBbWidth(), getBbHeight(), 5);
                Spot = Clear != null ? new Vec3(Clear.x, Math.max(Clear.y, Spot.y), Clear.z) : Centre;
            }
            CourtFx.burst(World, ParticleTypes.LARGE_SMOKE, Spot.add(0.0, 2.5, 0.0), 12, 0.8, 0.02);
            if (Slot == Mine) {
                moveTo(Spot.x, Math.max(getY(), Spot.y), Spot.z, getYRot(), 0.0f);
                continue;
            }
            EyesImage Image = ScarletCourtRegistry.EyesImageType.get().create(World);
            if (Image == null) {
                continue;
            }
            Image.moveTo(Spot.x, Math.max(getY(), Spot.y), Spot.z, getYRot(), 0.0f);
            Image.setYHeadRot(getYRot());
            Image.setYBodyRot(getYRot());
            Image.life(ImageLife);
            Image.addTag(CourtFx.Tag);
            if (getTarget() != null) {
                Image.setTarget(getTarget());
            }
            World.addFreshEntity(Image);
            ScarletCourt.Instance.adopt(Image);
            Made.add(Image);
        }
        return Made;
    }

    private void summon(ServerLevel World, int Elapsed) {
        if (Elapsed < 20 && Elapsed % 4 == 0) {
            CourtFx.burst(World, CourtFx.Amber, eyes().add(0.0, 1.0, 0.0), 6, 1.0, 0.0);
        }
        if (Elapsed == 20) {
            CourtFx.sound(World, eyes(), "eyeling_idle", 3.0f, 0.8f);
            int Count = 3 + Math.min(2, Players - 1);
            for (int Index = 0; Index < Count; Index++) {
                double Angle = Index * Math.PI * 2.0 / Count;
                BlockPos Spot = BlockPos.containing(getX() + Math.cos(Angle) * 2.5, getY() + 3.0, getZ() + Math.sin(Angle) * 2.5);
                CourtMinion Made = ScarletCourt.Instance.summon(World, CourtKind.Eyeling, Spot, true);
                if (Made != null && getTarget() != null) {
                    Made.setTarget(getTarget());
                }
            }
        }
    }

    @Override
    protected void shiftTick(int Elapsed) {
        ServerLevel World = (ServerLevel) level();
        setDeltaMovement(getDeltaMovement().multiply(0.5, 1.0, 0.5));
        if (Elapsed == 1) {
            CourtFx.sound(World, eyes(), "eyes_flare", 6.0f, 0.6f);
        }
        if (Elapsed % 4 == 0) {
            CourtFx.burst(World, CourtFx.Amber, eyes(), 16, 1.2, 0.04);
        }
        if (Elapsed == 30) {
            World.sendParticles(ParticleTypes.FLASH, eyes().x, eyes().y, eyes().z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void arenaTick(ServerLevel World) {
        if (tickCount % 10 == 0) {
            Vec3 Spot = Vec3.atBottomCenterOf(CourtArena.scatter(World, Dais, 3.0, CourtArena.floorRadius() - 4.0, random));
            World.sendParticles(CourtFx.Amber, Spot.x, Spot.y + 1.5 + random.nextDouble() * 3.0, Spot.z, 1, 0.3, 0.3, 0.3, 0.0);
        }
    }

    @Override
    protected void dyingTick(ServerLevel World, int Elapsed) {
        setDeltaMovement(0.0, -0.02, 0.0);
        if (Elapsed < 50 && Elapsed % 5 == 0) {
            CourtFx.sound(World, eyes(), "eyes_idle", 1.5f, 1.6f - Elapsed * 0.015f);
            CourtFx.burst(World, CourtFx.Amber, eyes().subtract(0.0, Elapsed * 0.05, 0.0), 6, 1.0, 0.0);
        }
        if (Elapsed >= 40 && Elapsed % 3 == 0) {
            CourtFx.burst(World, ParticleTypes.LARGE_SMOKE, position().add(0.0, 1.5, 0.0), 8, 1.2, 0.02);
            CourtFx.crumble(World, Blocks.BLACK_WOOL.defaultBlockState(), position().add(0.0, 2.0, 0.0), 10, 1.0, 1.0);
        }
    }

    @Override
    protected void onDying(ServerLevel World) {
        for (EyesImage Image : World.getEntitiesOfClass(EyesImage.class, getBoundingBox().inflate(64.0))) {
            Image.pop(false);
        }
    }
}
