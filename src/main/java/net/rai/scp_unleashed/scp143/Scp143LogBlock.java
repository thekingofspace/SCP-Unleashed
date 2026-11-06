package net.rai.scp_unleashed.scp143;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

public class Scp143LogBlock extends RotatedPillarBlock {
    public static final MapCodec<Scp143LogBlock> BlockCodec = simpleCodec(Scp143LogBlock::new);

    public Scp143LogBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return BlockCodec;
    }

    @Override
    @Nullable
    public BlockState getToolModifiedState(BlockState State, UseOnContext Context, ItemAbility Ability, boolean Simulate) {
        if (Ability == ItemAbilities.AXE_STRIP && Context.getItemInHand().canPerformAction(Ability)) {
            Block Stripped = Scp143Registry.strippedOf(State.getBlock());
            if (Stripped != null) {
                if (!Simulate && Context.getLevel() instanceof ServerLevel World) {
                    Scp143Petals.shake(World, Context.getClickedPos(), 2, World.getRandom());
                }
                return Stripped.defaultBlockState().setValue(AXIS, State.getValue(AXIS));
            }
        }
        return super.getToolModifiedState(State, Context, Ability, Simulate);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player Breaker) {
        if (World instanceof ServerLevel ServerWorld) {
            Scp143Petals.chopped(ServerWorld, Pos, ServerWorld.getRandom());
        }
        return super.playerWillDestroy(World, Pos, State, Breaker);
    }
}
