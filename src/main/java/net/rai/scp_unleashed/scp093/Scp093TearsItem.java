package net.rai.scp_unleashed.scp093;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp093TearsItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp093_tears.";
    public static final int DrinkTicks = 36;

    public Scp093TearsItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp093TearsItem;
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return DrinkTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return Scp093Registry.Drink.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        return ItemUtils.startUsingInstantly(World, User, Hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity User) {
        if (User instanceof ServerPlayer Drinker) {
            CriteriaTriggers.CONSUME_ITEM.trigger(Drinker, Stack);
            Drinker.awardStat(Stats.ITEM_USED.get(this));
        }
        if (World instanceof ServerLevel Server) {
            Scp093Tears.play(Server, User, Scp093Registry.DrinkLayers, 0.8f, 0.9f + User.getRandom().nextFloat() * 0.15f);
            Scp093Tears.drink(User);
        }
        Stack.consume(1, User);
        return Stack;
    }

    @Override
    public boolean isFoil(ItemStack Stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "drink").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "feed").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "warning").withStyle(ChatFormatting.RED));
        }
    }
}
