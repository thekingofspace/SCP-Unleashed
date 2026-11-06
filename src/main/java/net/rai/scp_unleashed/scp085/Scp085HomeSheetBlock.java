package net.rai.scp_unleashed.scp085;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class Scp085HomeSheetBlock extends Scp085SheetBlock {
    public static final MapCodec<Scp085HomeSheetBlock> BlockCodec = simpleCodec(Scp085HomeSheetBlock::new);

    public Scp085HomeSheetBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    public Scp085Scene sceneOf(BlockState State) {
        return Scp085Scene.Home;
    }

    @Override
    public boolean isHomeSheet() {
        return true;
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        super.setPlacedBy(World, Pos, State, Placer, Stack);
        if (World instanceof ServerLevel Server) {
            Scp085Brain.homePlaced(Server, Pos);
        }
    }
}
