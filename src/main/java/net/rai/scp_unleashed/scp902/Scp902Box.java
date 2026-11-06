package net.rai.scp_unleashed.scp902;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public final class Scp902Box {
    public static final int Beat = 20;
    public static final float TickVolume = 0.42f;
    public static final float TickBodyVolume = 0.3f;
    public static final float TickPitch = 1.0f;
    public static final float TockPitch = 0.84f;
    public static final float OpenVolume = 0.7f;
    public static final float ShutVolume = 0.8f;
    public static final int EmptyLines = 4;
    public static final String Prefix = "message.scp_unleashed.scp902.";

    private Scp902Box() {
    }

    public static boolean canUse(LivingEntity User) {
        return !(User instanceof FakePlayer) && !User.isSpectator() && User.isAlive();
    }

    public static boolean onBeat(long GameTime) {
        return GameTime % Beat == 0L;
    }

    public static float beatPitch(long GameTime) {
        return (GameTime / Beat) % 2L == 0L ? TickPitch : TockPitch;
    }

    public static void tick(ServerLevel World, Vec3 At) {
        long Now = World.getGameTime();
        float Pitch = beatPitch(Now);
        World.playSound(null, At.x, At.y, At.z, Scp902Registry.Tick.get(), SoundSource.BLOCKS, TickVolume, Pitch);
        World.playSound(null, At.x, At.y, At.z, Scp902Registry.TickBody.get(), SoundSource.BLOCKS, TickBodyVolume, Pitch);
    }

    public static boolean heldBeat(ServerPlayer User, long GameTime) {
        if (User.isSpectator() || !onBeat(GameTime) || !holds(User)) {
            return false;
        }
        tick(User.serverLevel(), new Vec3(User.getX(), User.getY() + 0.9, User.getZ()));
        return true;
    }

    public static boolean holds(Player User) {
        return Scp902BoxItem.is(User.getMainHandItem()) || Scp902BoxItem.is(User.getOffhandItem());
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        float Pitch = 0.94f + World.getRandom().nextFloat() * 0.12f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    public static void lid(ServerLevel World, Vec3 At, boolean Opened) {
        play(World, At, Opened ? Scp902Registry.OpenLayers : Scp902Registry.ShutLayers, Opened ? OpenVolume : ShutVolume);
    }

    public static void empty(LivingEntity User, ServerLevel World) {
        if (User instanceof Player Viewer) {
            int Line = World.getRandom().nextInt(EmptyLines);
            Viewer.displayClientMessage(Component.translatable(Prefix + "empty." + Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    public static ItemStack fresh() {
        return new ItemStack(Scp902Registry.BoxItem.get());
    }
}
