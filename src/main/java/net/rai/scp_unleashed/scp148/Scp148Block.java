package net.rai.scp_unleashed.scp148;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp148Block extends Block implements EntityBlock {
    public static final MapCodec<Scp148Block> BlockCodec = simpleCodec(Scp148Block::new);
    public static final String Key = "block.scp_unleashed.scp148_block.";
    public static final int GlintChance = 9;

    public Scp148Block(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp148BlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (World.isClientSide || Type != Scp148Registry.WardEntity.get()) {
            return null;
        }
        return (Ticked, Pos, Current, Machine) -> Scp148BlockEntity.serverTick(Ticked, Pos, Current, (Scp148BlockEntity) Machine);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(GlintChance) != 0) {
            return;
        }
        Direction Side = Direction.getRandom(Random);
        BlockPos Beside = Pos.relative(Side);
        if (World.getBlockState(Beside).isSolidRender(World, Beside)) {
            return;
        }
        double X = Pos.getX() + 0.5 + Side.getStepX() * 0.52 + (Side.getStepX() == 0 ? Random.nextDouble() - 0.5 : 0.0) * 0.8;
        double Y = Pos.getY() + 0.5 + Side.getStepY() * 0.52 + (Side.getStepY() == 0 ? Random.nextDouble() - 0.5 : 0.0) * 0.8;
        double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.52 + (Side.getStepZ() == 0 ? Random.nextDouble() - 0.5 : 0.0) * 0.8;
        World.addParticle(Scp148Cue.ShimmerDeep, X, Y, Z, 0.0, 0.0, 0.0);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GREEN));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "details").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Key + "containment").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
