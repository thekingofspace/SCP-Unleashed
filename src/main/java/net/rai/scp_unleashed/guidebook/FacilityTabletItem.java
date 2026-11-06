package net.rai.scp_unleashed.guidebook;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.guidebook.client.GuidebookClientHooks;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class FacilityTabletItem extends Item {
    public FacilityTabletItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            GuidebookClientHooks.open();
        } else {
            World.playSound(null, User.getX(), User.getY(), User.getZ(), GuidebookRegistry.TabletPower.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
            World.playSound(null, User.getX(), User.getY(), User.getZ(), GuidebookRegistry.TabletBoot.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
        }
        User.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.facility_tablet.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.facility_tablet.detail").withStyle(ChatFormatting.GOLD));
        }
    }
}
