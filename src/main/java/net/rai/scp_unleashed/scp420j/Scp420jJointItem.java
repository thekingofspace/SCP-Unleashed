package net.rai.scp_unleashed.scp420j;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp420jJointItem extends Item {
    public Scp420jJointItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(Stack);
        }
        if (World.isClientSide) {
            return InteractionResultHolder.success(Stack);
        }
        if (!(User instanceof ServerPlayer Smoker) || !Smoker.isAlive()) {
            return InteractionResultHolder.pass(Stack);
        }
        return InteractionResultHolder.consume(smoke(Smoker, Stack));
    }

    public ItemStack smoke(ServerPlayer Smoker, ItemStack Stack) {
        Scp420jFloat.lightUp(Smoker);
        Scp420jFloat.start(Smoker);
        Smoker.awardStat(Stats.ITEM_USED.get(this));
        Smoker.getCooldowns().addCooldown(this, Scp420jFloat.CooldownTicks);
        Stack.consume(1, Smoker);
        return Stack;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp420j_joint.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp420j_joint.use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp420j_joint.harmless").withStyle(ChatFormatting.GOLD));
        }
    }
}
