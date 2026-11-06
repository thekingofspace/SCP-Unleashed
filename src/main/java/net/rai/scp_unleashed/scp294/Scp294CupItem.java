package net.rai.scp_unleashed.scp294;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp294CupItem extends Item {
    public static final int DrinkTicks = 32;
    public static final String Prefix = "item.scp_unleashed.scp294_cup.";

    public Scp294CupItem(Properties Properties) {
        super(Properties);
    }

    @Nullable
    public static Scp294Drink drinkOf(ItemStack Stack) {
        return Stack.getItem() instanceof Scp294CupItem ? Stack.get(Scp294Registry.DrinkData.get()) : null;
    }

    public static boolean isFilled(ItemStack Stack) {
        return drinkOf(Stack) != null;
    }

    public static boolean isArsenic(ItemStack Stack) {
        Scp294Drink Drink = drinkOf(Stack);
        return Drink != null && Drink.isArsenic();
    }

    public static ItemStack filled(Scp294Drink Drink) {
        ItemStack Stack = new ItemStack(Scp294Registry.Cup.get());
        Stack.set(Scp294Registry.DrinkData.get(), Drink);
        return Stack;
    }

    public static int colourOf(ItemStack Stack) {
        Scp294Drink Drink = drinkOf(Stack);
        return Drink == null ? 0xFFFFFF : Drink.Colour();
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity Holder) {
        return isFilled(Stack) ? DrinkTicks : 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return isFilled(Stack) ? UseAnim.DRINK : UseAnim.NONE;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return Scp294Registry.Drink.get();
    }

    @Override
    public SoundEvent getEatingSound() {
        return Scp294Registry.Drink.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!isFilled(Stack)) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp294.empty").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(Stack);
        }
        return ItemUtils.startUsingInstantly(World, User, Hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Drinker) {
        Scp294Drink Drink = drinkOf(Stack);
        if (Drink == null) {
            return Stack;
        }
        if (Drinker instanceof ServerPlayer Server) {
            CriteriaTriggers.CONSUME_ITEM.trigger(Server, Stack);
            Server.awardStat(Stats.ITEM_USED.get(this));
        }
        Stack.consume(1, Drinker);
        if (!World.isClientSide) {
            World.gameEvent(Drinker, GameEvent.DRINK, Drinker.position());
            Scp294Drinks.apply(Drinker, Drink);
        }
        return Stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        if (Target instanceof Theodore Subject && isArsenic(Stack)) {
            return Subject.offerArsenic(User, Hand);
        }
        return InteractionResult.PASS;
    }

    @Override
    public Component getName(ItemStack Stack) {
        Scp294Drink Drink = drinkOf(Stack);
        return Drink == null ? super.getName(Stack) : Drink.Title();
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        boolean Full = isFilled(Stack);
        Lines.add(Component.translatable(Prefix + (Full ? "full" : "empty")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            if (Full) {
                Lines.add(Component.translatable(Prefix + "drink").withStyle(ChatFormatting.GOLD));
            }
            Lines.add(Component.translatable(Prefix + "origin").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
