package net.rai.scp_unleashed.scp217;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp217Ambience {
    public static final int TickGap = 20;
    public static final int QuietPeriod = 600;
    public static final int QuietBurst = 4;
    public static final int StirPeriod = 300;
    public static final int StirBurst = 6;
    public static final int StirClicks = 220;
    public static final int StiffClicks = 300;
    public static final int StiffGrind = 400;
    public static final int HeavyTickGap = 30;
    public static final int HeavyGrind = 240;
    public static final int WindingGrind = 160;
    public static final float WindingStartRate = 1.0f / 30.0f;
    public static final float WindingEndRate = 1.0f / 90.0f;
    public static final float[] TickVolumes = {0.16f, 0.26f, 0.32f, 0.38f};
    public static final float[] Strides = {0.0f, 0.0f, 1.6f, 1.35f};
    public static final double MaxStep = 1.0;
    public static final int ShiftBits = 4;
    public static final int WoundBits = 14;
    public static final ItemParticleOption Brass = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET));
    public static final ItemParticleOption Steel = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_NUGGET));

    private Scp217Ambience() {
    }

    public static void tick(ServerLevel World, LivingEntity Subject, int Now, int Stage, boolean Winding) {
        tick(World, Subject, Now, Stage, Winding, false);
    }

    public static void tick(ServerLevel World, LivingEntity Subject, int Now, int Stage, boolean Winding, boolean Perfect) {
        int Salt = Subject.getId() * 37;
        if (Winding) {
            winding(World, Subject, Now, Salt);
        } else if (Perfect && Stage >= 3) {
            if (Math.floorMod(Now + Salt, TickGap) == 0) {
                play(World, Subject, Scp217Registry.TickLayers, TickVolumes[2], tock((Now + Salt) / TickGap));
            }
            if (Math.floorMod(Now + Salt * 3, StiffClicks) == 0) {
                play(World, Subject, Scp217Registry.ClickLayers, 0.4f, 1.05f + World.getRandom().nextFloat() * 0.1f);
            }
        } else if (Stage <= 1) {
            int Phase = Math.floorMod(Now + Salt, QuietPeriod);
            if (Phase < QuietBurst * TickGap && Phase % TickGap == 0 && Subject instanceof ServerPlayer User) {
                for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp217Registry.TickLayers) {
                    User.playNotifySound(Layer.get(), SoundSource.PLAYERS, TickVolumes[0], tock(Phase / TickGap));
                }
            }
        } else if (Stage == 2) {
            int Phase = Math.floorMod(Now + Salt, StirPeriod);
            if (Phase < StirBurst * TickGap && Phase % TickGap == 0) {
                play(World, Subject, Scp217Registry.TickLayers, TickVolumes[1], tock(Phase / TickGap));
            }
            if (Math.floorMod(Now + Salt * 3, StirClicks) == 0) {
                play(World, Subject, Scp217Registry.ClickLayers, 0.4f, 1.0f + World.getRandom().nextFloat() * 0.1f);
            }
        } else if (Stage == 3) {
            if (Math.floorMod(Now + Salt, TickGap) == 0) {
                play(World, Subject, Scp217Registry.TickLayers, TickVolumes[2], tock((Now + Salt) / TickGap));
            }
            if (Math.floorMod(Now + Salt * 3, StiffClicks) == 0) {
                play(World, Subject, Scp217Registry.ClickLayers, 0.45f, 0.92f + World.getRandom().nextFloat() * 0.1f);
            }
            if (Math.floorMod(Now + Salt * 5, StiffGrind) == 0) {
                play(World, Subject, Scp217Registry.GrindLayers, 0.45f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            }
        } else {
            if (Math.floorMod(Now + Salt, HeavyTickGap) == 0) {
                play(World, Subject, Scp217Registry.TickLayers, TickVolumes[3], tock((Now + Salt) / HeavyTickGap) * 0.9f);
            }
            if (Math.floorMod(Now + Salt * 5, HeavyGrind) == 0) {
                play(World, Subject, Scp217Registry.GrindLayers, 0.55f, 0.85f + World.getRandom().nextFloat() * 0.1f);
            }
        }
        steps(World, Subject, Stage);
    }

    private static void winding(ServerLevel World, LivingEntity Subject, int Now, int Salt) {
        int Since = Now - (Scp217Clockwork.Duration - Scp217Clockwork.WindingTicks);
        if (Since <= 0) {
            return;
        }
        if (Math.floor(count(Since)) > Math.floor(count(Since - 1))) {
            float Progress = Scp217Clockwork.windingProgress(Now);
            play(World, Subject, Scp217Registry.TickLayers, 0.45f, Mth.lerp(Progress, 0.85f, 0.55f));
        }
        if (Math.floorMod(Now + Salt * 5, WindingGrind) == 0) {
            play(World, Subject, Scp217Registry.GrindLayers, 0.6f, 0.75f);
        }
    }

    private static double count(int Since) {
        double Span = Scp217Clockwork.WindingTicks;
        return WindingStartRate * Since - (WindingStartRate - WindingEndRate) * Since * (double) Since / (2.0 * Span);
    }

    private static float tock(int Beat) {
        return Math.floorMod(Beat, 2) == 0 ? 1.0f : 0.86f;
    }

    private static void steps(ServerLevel World, LivingEntity Subject, int Stage) {
        Scp217Trace Trace = Subject.getData(Scp217Registry.Trace.get());
        double X = Subject.getX();
        double Z = Subject.getZ();
        if (!Trace.Placed) {
            Trace.Placed = true;
            Trace.LastX = X;
            Trace.LastZ = Z;
            return;
        }
        double Moved = Math.hypot(X - Trace.LastX, Z - Trace.LastZ);
        Trace.LastX = X;
        Trace.LastZ = Z;
        if (Stage < 3 || !Subject.onGround() || Moved > MaxStep || Subject.isPassenger() || Subject.isSpectator()) {
            return;
        }
        Trace.Walked += (float) Moved;
        if (Trace.Walked < Strides[Mth.clamp(Stage, 1, Strides.length) - 1]) {
            return;
        }
        Trace.Walked = 0.0f;
        float Volume = Subject.isSteppingCarefully() ? 0.25f : 0.55f;
        float Pitch = (Stage >= 4 ? 0.82f : 1.0f) + (World.getRandom().nextFloat() - 0.5f) * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp217Registry.StepLayers) {
            World.playSound(null, X, Subject.getY(), Z, Layer.get(), source(Subject), Volume, Pitch);
        }
    }

    public static void shift(ServerLevel World, LivingEntity Subject, int Stage) {
        shift(World, Subject, Stage, false);
    }

    public static void shift(ServerLevel World, LivingEntity Subject, int Stage, boolean Perfect) {
        if (Stage <= 1) {
            return;
        }
        play(World, Subject, Scp217Registry.ShiftLayers, 0.6f, Perfect ? 1.2f : 1.15f - Stage * 0.08f);
        AABB Box = Subject.getBoundingBox();
        World.sendParticles(Perfect ? Brass : Steel, Subject.getX(), Box.minY + Box.getYsize() * 0.6, Subject.getZ(), ShiftBits, Box.getXsize() * 0.3, Box.getYsize() * 0.2,
                Box.getZsize() * 0.3, 0.02);
    }

    public static void windDown(ServerLevel World, LivingEntity Subject) {
        play(World, Subject, Scp217Registry.WindDownLayers, 1.0f, 1.0f);
        AABB Box = Subject.getBoundingBox();
        double Middle = Box.minY + Box.getYsize() * 0.55;
        World.sendParticles(Brass, Subject.getX(), Middle, Subject.getZ(), WoundBits, Box.getXsize() * 0.35, Box.getYsize() * 0.3, Box.getZsize() * 0.35, 0.08);
        World.sendParticles(Steel, Subject.getX(), Middle, Subject.getZ(), WoundBits, Box.getXsize() * 0.35, Box.getYsize() * 0.3, Box.getZsize() * 0.35, 0.08);
    }

    private static void play(ServerLevel World, LivingEntity Subject, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        double Y = Subject.getY() + Subject.getBbHeight() * 0.6;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Subject.getX(), Y, Subject.getZ(), Layer.get(), source(Subject), Volume, Pitch);
        }
    }

    private static SoundSource source(LivingEntity Subject) {
        return Subject instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
    }
}
