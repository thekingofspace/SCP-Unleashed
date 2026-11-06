package net.rai.scp_unleashed.scp330;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlSessions;

import java.util.List;
import java.util.UUID;

public final class Scp330Taking {
    public static final int Allowed = 2;
    public static final int GreedLines = 3;
    public static final String GreedPrefix = "message.scp_unleashed.scp330.greed.";
    public static final float TakeVolume = 0.7f;
    public static final float GreedVolume = 1.0f;

    public enum Outcome {
        Free,
        Given,
        Greedy
    }

    private Scp330Taking() {
    }

    public static boolean exempt(Player User) {
        return User.isCreative() || User.isSpectator();
    }

    public static UUID takerId(ServerPlayer User) {
        if (ControlSessions.isControlling(User)) {
            return ControlSessions.session(User).BodyId();
        }
        return User.getUUID();
    }

    public static Outcome take(ServerPlayer User, Scp330BowlBlockEntity Bowl) {
        ServerLevel World = User.serverLevel();
        BlockPos Pos = Bowl.getBlockPos();
        if (exempt(User)) {
            give(User, World, Pos);
            return Outcome.Free;
        }
        int Count = Bowl.record(takerId(User));
        if (Count <= Allowed) {
            give(User, World, Pos);
            return Outcome.Given;
        }
        Scp330Greed.afflict(User);
        play(World, Vec3.atCenterOf(Pos), Scp330Registry.GreedLayers, GreedVolume, 1.0f);
        Component Line = Component.translatable(GreedPrefix + World.getRandom().nextInt(GreedLines)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        User.displayClientMessage(Line, true);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return Outcome.Greedy;
    }

    private static void give(ServerPlayer User, ServerLevel World, BlockPos Pos) {
        ItemStack Candy = new ItemStack(Scp330Registry.candy(Scp330Colour.random(World.getRandom())));
        if (!User.getInventory().add(Candy) && !Candy.isEmpty()) {
            User.drop(Candy, false);
        }
        User.inventoryMenu.broadcastChanges();
        play(World, Vec3.atCenterOf(Pos), Scp330Registry.TakeLayers, TakeVolume, 1.0f);
        World.gameEvent(User, GameEvent.ITEM_INTERACT_FINISH, Pos);
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, Volume, Varied);
        }
    }
}
