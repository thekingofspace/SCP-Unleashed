package net.rai.scp_unleashed.door;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public final class DevicePulse {
    private static final int[] Ticks = {10, 20, 40, 60, 100, 200, 400};
    public static final IntegerProperty Pulse = IntegerProperty.create("pulse", 0, Ticks.length - 1);
    public static final int ButtonDefault = 1;
    public static final int ScannerDefault = 2;

    private DevicePulse() {
    }

    public static int ticksOf(BlockState State) {
        return Ticks[State.getValue(Pulse)];
    }

    public static String secondsOf(int Index) {
        int Length = Ticks[Index];
        return Length % 20 == 0 ? Integer.toString(Length / 20) : Float.toString(Length / 20.0f);
    }

    public static Component describe(BlockState State) {
        return Component.translatable("block.scp_unleashed.door_device.seconds", secondsOf(State.getValue(Pulse)));
    }

    public static InteractionResult cycle(Level World, BlockPos Pos, BlockState State, @Nullable Player User) {
        int Next = (State.getValue(Pulse) + 1) % Ticks.length;
        BlockState Changed = State.setValue(Pulse, Next);
        World.setBlock(Pos, Changed, Block.UPDATE_ALL);
        World.playSound(null, Pos, DoorSounds.KeycardSet.get(), SoundSource.BLOCKS, 0.4f, 0.75f + Next * 0.08f);
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.door_device.pulse", describe(Changed)).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.SUCCESS;
    }
}
