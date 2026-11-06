package net.rai.scp_unleashed.z0002;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Z0002FingerItem extends Item {
    public Z0002FingerItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Z0002FingerItem;
    }

    public static ItemStack create() {
        return new ItemStack(Z0002Registry.Finger.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!World.isClientSide && User instanceof ServerPlayer Holder) {
            Z0002Domains.use(Holder, Stack, Hand);
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return !Source.is(DamageTypeTags.IS_FIRE) && !Source.is(DamageTypeTags.IS_EXPLOSION);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Prefix = getDescriptionId();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        Level World = Context.level();
        if (World != null && Z0002Domains.itemDormant(Stack, World.getGameTime())) {
            Lines.add(Component.translatable(Prefix + ".tooltip.dormant").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable(Prefix + ".detail.point", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + ".detail.warm").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Prefix + ".detail.rest").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
