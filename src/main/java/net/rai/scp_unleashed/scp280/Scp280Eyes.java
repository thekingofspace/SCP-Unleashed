package net.rai.scp_unleashed.scp280;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class Scp280Eyes extends Entity {
    public static final float Width = 0.5f;
    public static final float Height = 0.3f;
    public static final int FrameOpen = 0;
    public static final int FrameLeft = 1;
    public static final int FrameRight = 2;
    public static final int FrameHalf = 3;
    public static final int FrameSlit = 4;
    public static final int FrameShut = 5;
    public static final int Frames = 6;
    public static final int OpenTicks = 6;
    public static final int CloseTicks = 6;
    public static final int QuickCloseTicks = 3;
    public static final int BlinkTicks = 6;
    public static final int BlinkMin = 22;
    public static final int BlinkMax = 60;
    public static final int GlanceMin = 18;
    public static final int GlanceMax = 46;
    public static final double VanishRange = 6.0;
    public static final int VanishLight = 7;
    public static final float GlowSteps = 6.0f;
    public static final float SoundVolume = 0.7f;
    public static final float BlinkVolume = 0.22f;
    public static final double HearDistance = 4.0;
    public static final double HearReach = 64.0;

    private int Variant;
    private int Delay;
    private int Life;
    private int Age;
    private int Closing = -1;
    private int CloseLength = CloseTicks;
    private int NextBlink;
    private int BlinkStart = -100;
    private int NextGlance;
    private int Glance = FrameOpen;
    private boolean Spoken;
    private boolean Quiet;

    public Scp280Eyes(EntityType<? extends Scp280Eyes> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
        setSilent(true);
    }

    public void setup(int Kind, int Wait, int Duration, boolean Hushed) {
        Variant = Math.floorMod(Kind, 4);
        Delay = Math.max(0, Wait);
        Life = Math.max(OpenTicks + CloseTicks + 2, Duration);
        Quiet = Hushed;
        NextBlink = Delay + OpenTicks + BlinkMin + random.nextInt(BlinkMax - BlinkMin + 1);
        NextGlance = Delay + OpenTicks + GlanceMin + random.nextInt(GlanceMax - GlanceMin + 1);
    }

    public int variant() {
        return Variant;
    }

    public boolean finished() {
        return Closing >= 0 && Age - Closing >= CloseLength;
    }

    public boolean closing() {
        return Closing >= 0;
    }

    public void close(boolean Quick) {
        if (Closing < 0) {
            Closing = Age;
            CloseLength = Quick ? QuickCloseTicks : CloseTicks;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    public void tick() {
        xo = getX();
        yo = getY();
        zo = getZ();
        Age++;
        if (!level().isClientSide) {
            discard();
            return;
        }
        if (Age < Delay) {
            return;
        }
        if (!Spoken && !Quiet) {
            Spoken = true;
            play(Scp280Registry.SightingLayers, SoundVolume, 0.9f + random.nextFloat() * 0.15f);
        }
        if (Closing < 0) {
            Player Near = level().getNearestPlayer(this, VanishRange);
            if (Near != null && !Near.isSpectator()) {
                close(true);
            } else if (level().getMaxLocalRawBrightness(blockPosition()) > VanishLight) {
                close(true);
            } else if (Age - Delay >= Life) {
                close(false);
            }
        }
        if (Closing < 0 && Age >= NextBlink) {
            BlinkStart = Age;
            NextBlink = Age + BlinkTicks + BlinkMin + random.nextInt(BlinkMax - BlinkMin + 1);
            if (!Quiet) {
                play(Scp280Registry.BlinkLayers, BlinkVolume, 0.95f + random.nextFloat() * 0.2f);
            }
        }
        if (Age >= NextGlance) {
            int Roll = random.nextInt(5);
            Glance = Roll == 0 ? FrameLeft : Roll == 1 ? FrameRight : FrameOpen;
            NextGlance = Age + GlanceMin + random.nextInt(GlanceMax - GlanceMin + 1);
        }
        if (finished()) {
            discard();
        }
    }

    private void play(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        Vec3 At = position();
        Player Near = level().getNearestPlayer(this, HearReach);
        if (Near != null) {
            Vec3 Ear = Near.getEyePosition();
            Vec3 Toward = At.subtract(Ear);
            double Distance = Toward.length();
            if (Distance > HearDistance) {
                At = Ear.add(Toward.scale(HearDistance / Distance));
            }
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playLocalSound(At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch, false);
        }
    }

    public boolean visible() {
        return Age >= Delay && !finished();
    }

    public float openness(float Partial) {
        float Time = Age + Partial;
        if (Time < Delay) {
            return 0.0f;
        }
        float Opening = Mth.clamp((Time - Delay) / OpenTicks, 0.0f, 1.0f);
        float Shut = Closing >= 0 ? 1.0f - Mth.clamp((Time - Closing) / CloseLength, 0.0f, 1.0f) : 1.0f;
        float Blink = 1.0f;
        float Since = Time - BlinkStart;
        if (Since >= 0.0f && Since < BlinkTicks) {
            Blink = Math.abs(Since / BlinkTicks * 2.0f - 1.0f);
        }
        return Math.min(Opening, Math.min(Shut, Blink));
    }

    public int frame(float Partial) {
        float Open = openness(Partial);
        if (Open <= 0.12f) {
            return FrameShut;
        }
        if (Open <= 0.42f) {
            return FrameSlit;
        }
        if (Open <= 0.75f) {
            return FrameHalf;
        }
        return Glance;
    }

    public float glow(float Partial) {
        float Time = Age + Partial - Delay;
        float In = Mth.clamp(Time / OpenTicks, 0.0f, 1.0f);
        float Out = Closing >= 0 ? 1.0f - Mth.clamp((Age + Partial - Closing) / CloseLength, 0.0f, 1.0f) : 1.0f;
        float Pulse = 0.92f + 0.08f * Mth.sin((Age + Partial) * 0.21f + Variant);
        return Math.round(Math.min(In, Out) * Pulse * GlowSteps) / GlowSteps;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 64.0 * 64.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    public void place(Vec3 At) {
        moveTo(At.x, At.y - Height * 0.5, At.z, 0.0f, 0.0f);
        xo = getX();
        yo = getY();
        zo = getZ();
    }
}
