package net.rai.scp_unleashed.scp005;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public enum VanillaDoor implements KeyHoldable {
    Hinged {
        @Override
        public BlockPos keyAnchor(BlockPos Pos, BlockState State) {
            return State.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
        }

        @Override
        public boolean holdOpen(ServerLevel World, BlockPos Anchor, @Nullable Player User) {
            BlockState State = World.getBlockState(Anchor);
            if (!(State.getBlock() instanceof DoorBlock Door)) {
                return false;
            }
            Door.setOpen(null, World, State, Anchor, true);
            return true;
        }

        @Override
        public void releaseHold(ServerLevel World, BlockPos Anchor) {
            BlockState State = World.getBlockState(Anchor);
            if (State.getBlock() instanceof DoorBlock Door && !State.getValue(DoorBlock.POWERED)) {
                Door.setOpen(null, World, State, Anchor, false);
            }
        }

        @Override
        public boolean isHeldOpen(BlockGetter World, BlockPos Anchor) {
            BlockState State = World.getBlockState(Anchor);
            return State.getBlock() instanceof DoorBlock && State.getValue(DoorBlock.OPEN);
        }
    },
    Trapdoor {
        @Override
        public BlockPos keyAnchor(BlockPos Pos, BlockState State) {
            return Pos;
        }

        @Override
        public boolean holdOpen(ServerLevel World, BlockPos Anchor, @Nullable Player User) {
            BlockState State = World.getBlockState(Anchor);
            if (!(State.getBlock() instanceof TrapDoorBlock)) {
                return false;
            }
            if (!State.getValue(TrapDoorBlock.OPEN)) {
                swingTrapdoor(World, Anchor, State, true);
            }
            return true;
        }

        @Override
        public void releaseHold(ServerLevel World, BlockPos Anchor) {
            BlockState State = World.getBlockState(Anchor);
            if (State.getBlock() instanceof TrapDoorBlock && State.getValue(TrapDoorBlock.OPEN) && !State.getValue(TrapDoorBlock.POWERED)) {
                swingTrapdoor(World, Anchor, State, false);
            }
        }

        @Override
        public boolean isHeldOpen(BlockGetter World, BlockPos Anchor) {
            BlockState State = World.getBlockState(Anchor);
            return State.getBlock() instanceof TrapDoorBlock && State.getValue(TrapDoorBlock.OPEN);
        }
    },
    Gate {
        @Override
        public BlockPos keyAnchor(BlockPos Pos, BlockState State) {
            return Pos;
        }

        @Override
        public boolean holdOpen(ServerLevel World, BlockPos Anchor, @Nullable Player User) {
            BlockState State = World.getBlockState(Anchor);
            if (!(State.getBlock() instanceof FenceGateBlock Fence)) {
                return false;
            }
            if (!State.getValue(FenceGateBlock.OPEN)) {
                BlockState Swung = State;
                if (User != null) {
                    Direction Side = User.getDirection();
                    if (State.getValue(FenceGateBlock.FACING) == Side.getOpposite()) {
                        Swung = Swung.setValue(FenceGateBlock.FACING, Side);
                    }
                }
                swingGate(World, Anchor, Swung.setValue(FenceGateBlock.OPEN, true), Fence.openSound, true);
            }
            return true;
        }

        @Override
        public void releaseHold(ServerLevel World, BlockPos Anchor) {
            BlockState State = World.getBlockState(Anchor);
            if (State.getBlock() instanceof FenceGateBlock Fence && State.getValue(FenceGateBlock.OPEN) && !State.getValue(FenceGateBlock.POWERED)) {
                swingGate(World, Anchor, State.setValue(FenceGateBlock.OPEN, false), Fence.closeSound, false);
            }
        }

        @Override
        public boolean isHeldOpen(BlockGetter World, BlockPos Anchor) {
            BlockState State = World.getBlockState(Anchor);
            return State.getBlock() instanceof FenceGateBlock && State.getValue(FenceGateBlock.OPEN);
        }
    };

    @Nullable
    public static VanillaDoor of(BlockState State) {
        Block Found = State.getBlock();
        if (Found instanceof DoorBlock) {
            return Hinged;
        }
        if (Found instanceof TrapDoorBlock) {
            return Trapdoor;
        }
        if (Found instanceof FenceGateBlock) {
            return Gate;
        }
        return null;
    }

    private static float swingPitch(ServerLevel World) {
        return World.getRandom().nextFloat() * 0.1f + 0.9f;
    }

    private static void swingTrapdoor(ServerLevel World, BlockPos Pos, BlockState State, boolean Opening) {
        BlockState Swung = State.setValue(TrapDoorBlock.OPEN, Opening);
        World.setBlock(Pos, Swung, Block.UPDATE_CLIENTS);
        if (Swung.getValue(TrapDoorBlock.WATERLOGGED)) {
            World.scheduleTick(Pos, Fluids.WATER, Fluids.WATER.getTickDelay(World));
        }
        BlockSetType Type = setTypeOf(World, Pos, State);
        World.playSound(null, Pos, Opening ? Type.trapdoorOpen() : Type.trapdoorClose(), SoundSource.BLOCKS, 1.0f, swingPitch(World));
        World.gameEvent(null, Opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, Pos);
    }

    private static void swingGate(ServerLevel World, BlockPos Pos, BlockState Swung, SoundEvent Sound, boolean Opening) {
        World.setBlock(Pos, Swung, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        World.playSound(null, Pos, Sound, SoundSource.BLOCKS, 1.0f, swingPitch(World));
        World.gameEvent(null, Opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, Pos);
    }

    private static BlockSetType setTypeOf(ServerLevel World, BlockPos Pos, BlockState State) {
        SoundType Sound = State.getSoundType(World, Pos, null);
        return BlockSetType.values().filter(Candidate -> Candidate.soundType() == Sound).findFirst().orElse(BlockSetType.OAK);
    }
}
