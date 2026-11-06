package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.Map;

public class Scp4666ExitDoorBlock extends Scp4666TallBlock {
    public static final MapCodec<Scp4666ExitDoorBlock> BlockCodec = simpleCodec(Scp4666ExitDoorBlock::new);
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{0, 0, 6, 16, 16, 10}});

    public Scp4666ExitDoorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected Map<Direction, VoxelShape> lowerShapes() {
        return Shapes;
    }

    @Override
    protected Map<Direction, VoxelShape> upperShapes() {
        return Shapes;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(User instanceof ServerPlayer Leaver) || !Scp4666World.inside(World)) {
            return InteractionResult.PASS;
        }
        int Phase = Scp4666State.get(Leaver.server).phase();
        if (Phase != Scp4666State.PhaseOver) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.door_locked").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        World.playSound(null, Pos, Scp4666Registry.ExitOpen.get(), SoundSource.BLOCKS, 1.0f, 0.9f);
        GatewayTravel.leave(Leaver);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Half) == DoubleBlockHalf.LOWER && Random.nextInt(3) == 0) {
            World.addParticle(ParticleTypes.END_ROD, Pos.getX() + Random.nextDouble(), Pos.getY() + Random.nextDouble() * 2.0, Pos.getZ() + 0.5, 0.0, 0.01, 0.0);
        }
    }
}
