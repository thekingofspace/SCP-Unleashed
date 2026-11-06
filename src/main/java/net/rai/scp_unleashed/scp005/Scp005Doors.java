package net.rai.scp_unleashed.scp005;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class Scp005Doors {
    public static final int CooldownTicks = 10;

    private Scp005Doors() {
    }

    @Nullable
    public static KeyHoldable find(BlockState State) {
        if (State.getBlock() instanceof KeyHoldable Door) {
            return Door;
        }
        return VanillaDoor.of(State);
    }

    public static InteractionResult use(ServerLevel World, BlockPos Pos, BlockState State, KeyHoldable Door, HoldTime Time, @Nullable Player User, Vec3 Lock) {
        BlockPos Anchor = Door.keyAnchor(Pos, State);
        Scp005Holds Holds = Scp005Holds.get(World);
        if (User != null && User.isSecondaryUseActive() && Holds.isHolding(Anchor)) {
            Holds.release(World, Anchor, Door);
            relockEffects(World, Lock);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp005_key.released").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }
        if (!Door.holdOpen(World, Anchor, User)) {
            return InteractionResult.CONSUME;
        }
        Holds.hold(World, Anchor, Time);
        unlockEffects(World, Lock);
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp005_key.opened", Time.describe()).withStyle(ChatFormatting.DARK_AQUA), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static float wobble(RandomSource Random, float Centre, float Spread) {
        return Centre + (Random.nextFloat() - 0.5f) * Spread;
    }

    public static void unlockEffects(ServerLevel World, Vec3 Lock) {
        RandomSource Random = World.getRandom();
        World.playSound(null, Lock.x, Lock.y, Lock.z, Scp005Registry.KeyClick.get(), SoundSource.BLOCKS, 0.6f, wobble(Random, 1.0f, 0.06f));
        World.playSound(null, Lock.x, Lock.y, Lock.z, Scp005Registry.KeyTurn.get(), SoundSource.BLOCKS, 0.8f, wobble(Random, 1.0f, 0.06f));
        World.playSound(null, Lock.x, Lock.y, Lock.z, Scp005Registry.KeyUnlatch.get(), SoundSource.BLOCKS, 0.7f, wobble(Random, 1.0f, 0.08f));
        World.playSound(null, Lock.x, Lock.y, Lock.z, Scp005Registry.KeyShimmer.get(), SoundSource.BLOCKS, 0.5f, wobble(Random, 1.0f, 0.04f));
        World.sendParticles(Scp005Registry.Shimmer.get(), Lock.x, Lock.y, Lock.z, 9, 0.09, 0.12, 0.09, 0.006);
    }

    public static void relockEffects(ServerLevel World, Vec3 Lock) {
        RandomSource Random = World.getRandom();
        World.playSound(null, Lock.x, Lock.y, Lock.z, Scp005Registry.KeyRelock.get(), SoundSource.BLOCKS, 0.75f, wobble(Random, 1.0f, 0.06f));
        World.playSound(null, Lock.x, Lock.y, Lock.z, Scp005Registry.KeyClick.get(), SoundSource.BLOCKS, 0.6f, wobble(Random, 0.9f, 0.06f));
        World.sendParticles(Scp005Registry.Shimmer.get(), Lock.x, Lock.y, Lock.z, 4, 0.06, 0.08, 0.06, 0.003);
    }
}
