package net.rai.scp_unleashed.dclass.theodore;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class ArsenicItem extends Item {
    public ArsenicItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        if (Target instanceof Theodore Subject) {
            return Subject.offerArsenic(User, Hand);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.arsenic.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.arsenic.feed").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.arsenic.drop", Theodore.SeekRange).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.arsenic.warning", Theodore.ClusterSize).withStyle(ChatFormatting.RED));
        }
    }
}
