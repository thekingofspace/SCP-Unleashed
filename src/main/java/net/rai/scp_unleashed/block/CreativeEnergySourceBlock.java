package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.block.entity.CreativeEnergySourceBlockEntity;
import net.rai.scp_unleashed.door.Screwdrivable;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreativeEnergySourceBlock extends BaseEntityBlock implements Screwdrivable {
    public static final MapCodec<CreativeEnergySourceBlock> BlockCodec = simpleCodec(CreativeEnergySourceBlock::new);

    public CreativeEnergySourceBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        if (!(World.getBlockEntity(Pos) instanceof CreativeEnergySourceBlockEntity Source)) {
            return InteractionResult.PASS;
        }
        Source.toggleMode();
        if (User != null) {
            User.displayClientMessage(Source.isSinkMode()
                    ? Component.translatable("message.scp_unleashed.creative_energy_source.sink").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("message.scp_unleashed.creative_energy_source.source").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.creative_energy_source.summary").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.creative_energy_source.detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.creative_energy_source.mode").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.creative_energy_source.sink_detail").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new CreativeEnergySourceBlockEntity(Pos, State);
    }
}
