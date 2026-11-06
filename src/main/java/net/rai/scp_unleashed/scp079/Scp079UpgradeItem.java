package net.rai.scp_unleashed.scp079;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp079UpgradeItem extends Item {
    public Scp079UpgradeItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = World.getBlockState(Pos);
        Scp079BlockEntity Computer = Scp079Block.masterEntity(World, Pos, State);
        if (Computer == null) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player User = Context.getPlayer();
        if (Computer.isUpgraded()) {
            if (User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.already"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!Computer.installUpgrade()) {
            return InteractionResult.FAIL;
        }
        if (User == null || !User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.installed", Stack.getHoverName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp079_anomalous_upgrade.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp079_anomalous_upgrade.fits").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp079_anomalous_upgrade.install").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp079_anomalous_upgrade.remove").withStyle(ChatFormatting.GOLD));
        }
    }
}
