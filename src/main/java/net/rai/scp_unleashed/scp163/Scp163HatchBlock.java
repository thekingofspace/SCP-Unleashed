package net.rai.scp_unleashed.scp163;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp163HatchBlock extends DoorBlock {
    public static final MapCodec<Scp163HatchBlock> BlockCodec = simpleCodec(Properties0 -> new Scp163HatchBlock(Scp163Registry.HullSet, Properties0));

    public Scp163HatchBlock(BlockSetType Type, Properties Properties) {
        super(Type, Properties);
    }

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return BlockCodec;
    }

    private static void hiss(Level World, BlockPos Pos, boolean Opening) {
        World.playSound(null, Pos, Scp163Registry.HatchHiss.get(), SoundSource.BLOCKS, 0.6f, Opening ? 1.1f + World.random.nextFloat() * 0.1f : 0.9f + World.random.nextFloat() * 0.1f);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        boolean Opening = !State.getValue(OPEN);
        InteractionResult Result = super.useWithoutItem(State, World, Pos, User, Hit);
        if (!World.isClientSide && Result.consumesAction()) {
            hiss(World, Pos, Opening);
        }
        return Result;
    }

    @Override
    public void setOpen(@Nullable Entity Opener, Level World, BlockState State, BlockPos Pos, boolean Open) {
        boolean Changing = State.is(this) && State.getValue(OPEN) != Open;
        super.setOpen(Opener, World, State, Pos, Open);
        if (Changing && !World.isClientSide) {
            hiss(World, Pos, Open);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp163_hatch.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp163_hull.details").withStyle(ChatFormatting.GOLD));
        }
    }
}
