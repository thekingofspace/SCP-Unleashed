package net.rai.scp_unleashed.scp076;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp076SealedBlock extends Block implements LiquidBlockContainer {
    public static final MapCodec<Scp076SealedBlock> BlockCodec = simpleCodec(Scp076SealedBlock::new);
    public static final float Resistance = 3600000.0f;

    public Scp076SealedBlock(Properties Settings) {
        super(Settings);
    }

    public static BlockBehaviour.Properties sealed(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(-1.0f, Resistance).pushReaction(PushReaction.BLOCK)
                .isValidSpawn((State, World, Pos, Kind) -> false);
    }

    @Override
    protected MapCodec<? extends Scp076SealedBlock> codec() {
        return BlockCodec;
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState State) {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean canDropFromExplosion(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState State, Level World, BlockPos Pos, Explosion Blast) {
    }

    @Override
    public float getExplosionResistance(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return Resistance;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState State, Level World, BlockPos Pos, Player User, boolean WillHarvest, FluidState Liquid) {
        if (User.getAbilities().instabuild) {
            return super.onDestroyedByPlayer(State, World, Pos, User, WillHarvest, Liquid);
        }
        return false;
    }

    @Override
    public boolean isFlammable(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return false;
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player User, BlockGetter World, BlockPos Pos, BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor World, BlockPos Pos, BlockState State, FluidState Liquid) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, Fluid Liquid) {
        return false;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = getDescriptionId();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".details").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    public static boolean isSealed(BlockState State) {
        return State.getBlock() instanceof Scp076SealedBlock;
    }
}
