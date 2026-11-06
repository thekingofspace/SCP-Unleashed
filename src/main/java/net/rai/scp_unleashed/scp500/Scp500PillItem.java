package net.rai.scp_unleashed.scp500;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp500PillItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp500_pill";

    public Scp500PillItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp500PillItem;
    }

    public static boolean swallow(LivingEntity Taker, @Nullable Player Giver, ItemStack Stack) {
        if (!is(Stack) || !Scp500Panacea.dose(Taker, Giver)) {
            return false;
        }
        if (Giver instanceof ServerPlayer Server) {
            if (Taker == Giver) {
                CriteriaTriggers.CONSUME_ITEM.trigger(Server, Stack);
            }
            Server.awardStat(Stats.ITEM_USED.get(Stack.getItem()));
        }
        Taker.gameEvent(GameEvent.EAT);
        Stack.consume(1, Giver);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!Scp500Panacea.canDose(User)) {
            return InteractionResultHolder.pass(Stack);
        }
        if (!World.isClientSide) {
            swallow(User, User, Stack);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        if (Target == User || !Scp500Panacea.canDose(Target)) {
            return InteractionResult.PASS;
        }
        if (!User.level().isClientSide) {
            swallow(Target, User, Stack);
        }
        return InteractionResult.sidedSuccess(User.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + ".detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".detail.1").withStyle(ChatFormatting.GOLD));
        }
    }
}
