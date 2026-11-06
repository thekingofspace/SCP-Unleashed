package net.rai.scp_unleashed.scp063;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;

public final class Scp063Brushing {
    public static final int TicksPerSecond = 20;
    public static final int FreshTicks = 100;
    public static final int StingTicks = 200;
    public static final int SoreTicks = 300;
    public static final int LooseTicks = 800;
    public static final int ToothlessTicks = 1200;
    public static final int MaxTicks = 1500;
    public static final int DamageInterval = 50;
    public static final float DamageAmount = 1.0f;
    public static final int RestPerTick = 3;
    public static final int StrokeInterval = 8;
    public static final int FoamInterval = 4;
    public static final int FoamCount = 2;
    public static final double MouthForward = 0.3;
    public static final double MouthDrop = 0.17;
    public static final String Prefix = "message.scp_unleashed.scp063.";

    private Scp063Brushing() {
    }

    public static int brushTime(Player User) {
        return User.getExistingData(Scp063Registry.BrushTime).orElse(0);
    }

    public static void setBrushTime(Player User, int Ticks) {
        if (Ticks <= 0) {
            User.removeData(Scp063Registry.BrushTime);
        } else {
            User.setData(Scp063Registry.BrushTime, Math.min(MaxTicks, Ticks));
        }
    }

    public static void begin(ServerPlayer User) {
        if (Scp063Teeth.isToothless(User)) {
            tell(User, "gums", ChatFormatting.GRAY);
        }
    }

    public static void tick(ServerPlayer User, int TicksUsing) {
        if (Targeting.isStatue(User)) {
            return;
        }
        int Before = brushTime(User);
        int After = Math.min(MaxTicks, Before + 1);
        setBrushTime(User, After);
        if (TicksUsing % StrokeInterval == 0) {
            stroke(User);
        }
        if (TicksUsing % FoamInterval == 0) {
            foam(User);
        }
        boolean Toothless = Scp063Teeth.isToothless(User);
        if (crossed(Before, After, FreshTicks) && !Toothless) {
            tell(User, "fresh", ChatFormatting.AQUA);
            play(User, Scp063Registry.Fresh.get(), 1.0f, 1.0f);
        }
        if (crossed(Before, After, StingTicks)) {
            tell(User, "sting", ChatFormatting.YELLOW);
        }
        if (crossed(Before, After, SoreTicks)) {
            tell(User, "sore", ChatFormatting.RED);
        }
        int Clock = After > Before ? After - SoreTicks : User.tickCount;
        if (After >= SoreTicks && Clock % DamageInterval == 0) {
            User.hurt(User.damageSources().source(Scp063Registry.BrushDamage), DamageAmount);
        }
        if (crossed(Before, After, LooseTicks) && !Toothless) {
            tell(User, "loose", ChatFormatting.RED);
            play(User, Scp063Registry.Rattle.get(), 0.8f, 1.0f);
        }
        if (After >= ToothlessTicks && !Toothless) {
            Scp063Teeth.lose(User);
        }
    }

    public static void rest(ServerPlayer User) {
        int Current = brushTime(User);
        if (Current > 0 && User.tickCount % RestPerTick == 0) {
            setBrushTime(User, Current - 1);
        }
    }

    private static boolean crossed(int Before, int After, int Mark) {
        return Before < Mark && After >= Mark;
    }

    public static void tell(ServerPlayer User, String Name, ChatFormatting Colour) {
        User.displayClientMessage(Component.translatable(Prefix + Name).withStyle(Colour), true);
    }

    public static Vec3 mouth(Player User) {
        Vec3 Look = User.getViewVector(1.0f);
        Vec3 Up = User.getUpVector(1.0f);
        return User.getEyePosition().add(Look.scale(MouthForward)).subtract(Up.scale(MouthDrop));
    }

    public static void play(ServerPlayer User, SoundEvent Sound, float Volume, float Pitch) {
        Vec3 Mouth = mouth(User);
        User.level().playSound(null, Mouth.x, Mouth.y, Mouth.z, Sound, SoundSource.PLAYERS, Volume, Pitch);
    }

    private static void stroke(ServerPlayer User) {
        float Pitch = 0.92f + User.getRandom().nextFloat() * 0.16f;
        play(User, Scp063Registry.Brush.get(), 0.7f, Pitch);
        play(User, Scp063Registry.Foam.get(), 0.35f, Pitch);
    }

    private static void foam(ServerPlayer User) {
        ServerLevel World = User.serverLevel();
        Vec3 Mouth = mouth(User);
        World.sendParticles(Scp063Registry.FoamSpeck.get(), Mouth.x, Mouth.y, Mouth.z, FoamCount, 0.06, 0.03, 0.06, 0.02);
    }
}
