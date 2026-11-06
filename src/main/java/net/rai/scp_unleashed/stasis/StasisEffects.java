package net.rai.scp_unleashed.stasis;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.stasis.network.StasisEffectPayload;
import org.jetbrains.annotations.Nullable;

public final class StasisEffects {
    public static final double EffectRange = 48.0;
    public static final float PitchTop = 1.12f;
    public static final float PitchStep = 0.07f;

    private StasisEffects() {
    }

    public static float pitch(FragmentRarity Grade, ServerLevel World) {
        return PitchTop - PitchStep * (Grade.getTier() - 1) + (World.random.nextFloat() - 0.5f) * 0.05f;
    }

    public static void capture(ServerLevel World, Entity Target, @Nullable Entity Holder, FragmentRarity Grade) {
        AABB Box = Target.getBoundingBox();
        Vec3 Centre = Box.getCenter();
        float Pitch = pitch(Grade, World);
        play(World, Centre, StasisRegistry.Capture.get(), 1.0f, Pitch);
        play(World, Centre, StasisRegistry.CaptureChime.get(), 0.8f, Pitch * 1.06f);
        play(World, Centre, StasisRegistry.CaptureWarp.get(), 0.55f, Pitch);
        send(World, StasisEffectPayload.Captured, Target.getX(), Target.getY(), Target.getZ(), (float) Box.getXsize(), (float) Box.getYsize(), Grade,
                Holder == null ? -1 : Holder.getId());
    }

    public static void release(ServerLevel World, Entity Spawned, FragmentRarity Grade) {
        AABB Box = Spawned.getBoundingBox();
        Vec3 Centre = Box.getCenter();
        float Pitch = pitch(Grade, World);
        play(World, Centre, StasisRegistry.Release.get(), 1.0f, Pitch);
        play(World, Centre, StasisRegistry.ReleaseHiss.get(), 0.7f, Pitch);
        send(World, StasisEffectPayload.Released, Spawned.getX(), Spawned.getY(), Spawned.getZ(), (float) Box.getXsize(), (float) Box.getYsize(), Grade, -1);
    }

    public static void breakout(ServerLevel World, Vec3 Base, float Width, float Height, FragmentRarity Grade) {
        Vec3 Centre = Base.add(0.0, Math.min(Height, 2.0f) * 0.5, 0.0);
        float Pitch = pitch(Grade, World);
        play(World, Centre, StasisRegistry.Breakout.get(), 1.2f, Pitch);
        play(World, Centre, StasisRegistry.BreakoutShards.get(), 1.0f, Pitch * 0.94f);
        play(World, Centre, StasisRegistry.BreakoutBurst.get(), 0.9f, Pitch);
        send(World, StasisEffectPayload.Burst, Base.x, Base.y, Base.z, Width, Height, Grade, -1);
    }

    public static void deny(ServerLevel World, Vec3 At, FragmentRarity Grade) {
        play(World, At, StasisRegistry.Deny.get(), 0.7f, pitch(Grade, World));
        send(World, StasisEffectPayload.Denied, At.x, At.y, At.z, 0.4f, 0.4f, Grade, -1);
    }

    public static boolean warn(ServerLevel World, double X, double Y, double Z, FragmentRarity Grade, long Remaining, long Now) {
        if (Remaining > StasisPodItem.WarningTicks) {
            return false;
        }
        int Interval = Remaining > StasisPodItem.UrgentTicks ? StasisPodItem.RattleInterval : StasisPodItem.UrgentRattleInterval;
        if (Now % Interval != 0L) {
            return false;
        }
        rattle(World, X, Y, Z, Grade, Remaining);
        return true;
    }

    public static void rattle(ServerLevel World, double X, double Y, double Z, FragmentRarity Grade, long Remaining) {
        float Urgency = Mth.clamp(1.0f - (float) Remaining / StasisPodItem.WarningTicks, 0.0f, 1.0f);
        Vec3 At = new Vec3(X, Y, Z);
        float Pitch = pitch(Grade, World) + 0.15f * Urgency;
        play(World, At, StasisRegistry.Rattle.get(), 0.55f + 0.35f * Urgency, Pitch);
        play(World, At, StasisRegistry.RattleClink.get(), 0.4f + 0.3f * Urgency, Pitch + 0.1f);
        send(World, StasisEffectPayload.Rattled, X, Y, Z, 0.3f, 0.3f, Grade, -1);
    }

    private static void play(ServerLevel World, Vec3 At, SoundEvent Event, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Event, SoundSource.PLAYERS, Volume, Pitch);
    }

    private static void send(ServerLevel World, int Kind, double X, double Y, double Z, float Width, float Height, FragmentRarity Grade, int Holder) {
        PacketDistributor.sendToPlayersNear(World, null, X, Y, Z, EffectRange, new StasisEffectPayload(Kind, X, Y, Z, Width, Height, Grade.getTier(), Holder));
    }
}
