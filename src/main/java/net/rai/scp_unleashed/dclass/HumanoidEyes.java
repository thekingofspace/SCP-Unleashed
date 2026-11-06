package net.rai.scp_unleashed.dclass;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class HumanoidEyes {
    public static final float YawRange = 45.0f;
    public static final float PitchRange = 32.0f;
    public static final float FocusCone = 50.0f;
    public static final float Saccade = 0.55f;
    public static final float LeadTurn = 2.5f;
    public static final float LeadScale = 9.0f;
    public static final int LeadTicks = 5;
    public static final float GlanceReach = 0.7f;
    public static final int BlinkTicks = 4;
    public static final double NoticeRange = 9.0;

    private float LookX;
    private float LookY;
    private float LookXO;
    private float LookYO;
    private float WantX;
    private float WantY;
    private int Hold;
    private int Rest;
    private int Lead;
    private float HeadYawO = Float.NaN;
    private float HeadPitchO;
    @Nullable
    private Entity Focus;
    private int BlinkClock = 40;
    private int Blinking;
    private boolean Again;
    private float Closed;
    private float ClosedO;
    private boolean Driven;

    public void blinkNow() {
        Driven = true;
        Blinking = BlinkTicks;
        Again = false;
    }

    public float lookX(float Partial) {
        return Mth.lerp(Partial, LookXO, LookX);
    }

    public float lookY(float Partial) {
        return Mth.lerp(Partial, LookYO, LookY);
    }

    public float closed(float Partial) {
        return Mth.lerp(Partial, ClosedO, Closed);
    }

    @Nullable
    public Entity focus() {
        return Focus;
    }

    public void tick(DClassLike Owner, float GlanceRate, boolean ForceClosed, boolean Wander) {
        LookXO = LookX;
        LookYO = LookY;
        ClosedO = Closed;
        RandomSource Random = Owner.getRandom();
        float HeadYaw = Owner.getYHeadRot();
        float HeadPitch = Owner.getXRot();
        float Turn = Float.isNaN(HeadYawO) ? 0.0f : Mth.wrapDegrees(HeadYaw - HeadYawO);
        float Nod = Float.isNaN(HeadYawO) ? 0.0f : HeadPitch - HeadPitchO;
        HeadYawO = HeadYaw;
        HeadPitchO = HeadPitch;
        if (ForceClosed) {
            Closed = Math.min(1.0f, Closed + 0.34f);
            LookX *= 0.8f;
            LookY *= 0.8f;
            Focus = null;
            return;
        }
        if (Math.abs(Turn) > LeadTurn || Math.abs(Nod) > LeadTurn) {
            Lead = LeadTicks;
            WantX = Mth.clamp(Turn / LeadScale, -1.0f, 1.0f);
            WantY = Mth.clamp(Nod / LeadScale, -1.0f, 1.0f);
            Focus = null;
            Hold = 0;
        } else if (Lead > 0) {
            Lead--;
            if (Lead == 0) {
                center(Random, GlanceRate);
            }
        } else if (Focus != null) {
            if (!Focus.isAlive() || Focus.distanceToSqr(Owner) > NoticeRange * NoticeRange || --Hold <= 0 || !aim(Owner, Focus.getEyePosition())) {
                center(Random, GlanceRate);
            }
        } else if (Hold > 0) {
            if (--Hold == 0) {
                center(Random, GlanceRate);
            }
        } else if (--Rest <= 0) {
            if (Wander) {
                glance(Owner, Random, GlanceRate);
            } else {
                center(Random, GlanceRate);
            }
        }
        LookX += (WantX - LookX) * Saccade;
        LookY += (WantY - LookY) * Saccade;
        if (Math.abs(WantX - LookX) < 0.02f) {
            LookX = WantX;
        }
        if (Math.abs(WantY - LookY) < 0.02f) {
            LookY = WantY;
        }
        blink(Random, GlanceRate);
    }

    private void center(RandomSource Random, float GlanceRate) {
        Focus = null;
        Hold = 0;
        WantX = 0.0f;
        WantY = 0.0f;
        Rest = Math.round((40 + Random.nextInt(80)) / Math.max(0.3f, GlanceRate));
    }

    private void blink(RandomSource Random, float GlanceRate) {
        if (Blinking > 0) {
            Blinking--;
            int Step = BlinkTicks - Blinking;
            float Half = BlinkTicks / 2.0f;
            Closed = Step <= Half ? Math.min(1.0f, Step / Half) : Math.max(0.0f, (BlinkTicks - Step) / Half);
            if (Blinking == 0) {
                Closed = 0.0f;
                if (Again) {
                    Again = false;
                    BlinkClock = 3;
                }
            }
            return;
        }
        Closed = Math.max(0.0f, Closed - 0.34f);
        if (!Driven && --BlinkClock <= 0) {
            Blinking = BlinkTicks;
            Again = Random.nextInt(6) == 0;
            BlinkClock = Math.round((50 + Random.nextInt(90)) / Math.max(0.6f, GlanceRate * 0.8f));
        }
    }

    private void glance(DClassLike Owner, RandomSource Random, float GlanceRate) {
        Hold = Math.round((10 + Random.nextInt(20)) / Math.max(0.3f, GlanceRate));
        AABB Around = Owner.getBoundingBox().inflate(NoticeRange);
        Player Nearest = Owner.level().getNearestPlayer(Owner, NoticeRange);
        if (Nearest != null && !Nearest.isSpectator() && Random.nextInt(100) < 70 && aim(Owner, Nearest.getEyePosition())) {
            Focus = Nearest;
            Hold = Math.round((30 + Random.nextInt(50)) / Math.max(0.3f, GlanceRate));
            return;
        }
        if (Random.nextInt(100) < 40) {
            List<LivingEntity> Others = Owner.level().getEntitiesOfClass(LivingEntity.class, Around, Found -> Found != Owner && Found.isAlive() && !Found.isInvisible());
            for (LivingEntity Other : Others) {
                if (aim(Owner, Other.getEyePosition())) {
                    Focus = Other;
                    return;
                }
            }
        }
        WantX = (Random.nextFloat() * 2.0f - 1.0f) * GlanceReach;
        WantY = (Random.nextFloat() * 2.0f - 1.0f) * GlanceReach * 0.6f;
    }

    private boolean aim(DClassLike Owner, Vec3 Target) {
        Vec3 Eye = Owner.getEyePosition();
        double Dx = Target.x - Eye.x;
        double Dy = Target.y - Eye.y;
        double Dz = Target.z - Eye.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        float Pitch = (float) -(Mth.atan2(Dy, Flat) * Mth.RAD_TO_DEG);
        float RelativeYaw = Mth.wrapDegrees(Yaw - Owner.getYHeadRot());
        float RelativePitch = Pitch - Owner.getXRot();
        if (Math.abs(RelativeYaw) > FocusCone || Math.abs(RelativePitch) > FocusCone) {
            return false;
        }
        WantX = Mth.clamp(RelativeYaw / YawRange, -1.0f, 1.0f);
        WantY = Mth.clamp(RelativePitch / PitchRange, -1.0f, 1.0f);
        return true;
    }
}
