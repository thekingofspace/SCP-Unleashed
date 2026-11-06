package net.rai.scp_unleashed.dclass.theodore;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AmalgamationEyes {
    public static final double NoticeRange = 20.0;
    public static final float YawRange = 55.0f;
    public static final float PitchRange = 35.0f;
    public static final float Saccade = 0.45f;
    public static final int Stubborn = 70;

    private final float[] LookX;
    private final float[] LookY;
    private final float[] LookXO;
    private final float[] LookYO;
    private final float[] WantX;
    private final float[] WantY;
    private final int[] Hold;
    private final boolean[] Watching;

    public AmalgamationEyes(int Count) {
        LookX = new float[Count];
        LookY = new float[Count];
        LookXO = new float[Count];
        LookYO = new float[Count];
        WantX = new float[Count];
        WantY = new float[Count];
        Hold = new int[Count];
        Watching = new boolean[Count];
    }

    public int count() {
        return LookX.length;
    }

    public float lookX(int Head, float Partial) {
        return Mth.lerp(Partial, LookXO[Head], LookX[Head]);
    }

    public float lookY(int Head, float Partial) {
        return Mth.lerp(Partial, LookYO[Head], LookY[Head]);
    }

    public void tick(LivingEntity Owner) {
        RandomSource Random = Owner.getRandom();
        Player Nearest = Owner.level().getNearestPlayer(Owner, NoticeRange);
        if (Nearest != null && Nearest.isSpectator()) {
            Nearest = null;
        }
        for (int Head = 0; Head < LookX.length; Head++) {
            LookXO[Head] = LookX[Head];
            LookYO[Head] = LookY[Head];
            if (--Hold[Head] <= 0) {
                Hold[Head] = 8 + Random.nextInt(36);
                Watching[Head] = Nearest != null && Random.nextInt(100) < Stubborn;
                if (!Watching[Head]) {
                    WantX[Head] = Random.nextFloat() * 2.0f - 1.0f;
                    WantY[Head] = (Random.nextFloat() * 2.0f - 1.0f) * 0.8f;
                }
            }
            if (Watching[Head]) {
                aim(Owner, Nearest, Head, Random);
            }
            LookX[Head] += (WantX[Head] - LookX[Head]) * Saccade;
            LookY[Head] += (WantY[Head] - LookY[Head]) * Saccade;
        }
    }

    private void aim(LivingEntity Owner, @Nullable Player Target, int Head, RandomSource Random) {
        if (Target == null) {
            Watching[Head] = false;
            return;
        }
        Vec3 Eye = Owner.getEyePosition();
        Vec3 Toward = Target.getEyePosition().subtract(Eye);
        double Flat = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        float Pitch = (float) -(Mth.atan2(Toward.y, Flat) * Mth.RAD_TO_DEG);
        float RelativeYaw = Mth.wrapDegrees(Yaw - Owner.yBodyRot);
        if (Math.abs(RelativeYaw) > 120.0f) {
            WantX[Head] = Math.signum(RelativeYaw);
            WantY[Head] = 0.0f;
            return;
        }
        float Jitter = Random.nextInt(12) == 0 ? (Random.nextFloat() - 0.5f) * 0.3f : 0.0f;
        WantX[Head] = Mth.clamp(RelativeYaw / YawRange + Jitter, -1.0f, 1.0f);
        WantY[Head] = Mth.clamp(Pitch / PitchRange, -1.0f, 1.0f);
    }
}
