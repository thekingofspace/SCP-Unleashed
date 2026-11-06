package net.rai.scp_unleashed.radio;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class HandheldRadioItem extends Item {
    public static final String Key = "item.scp_unleashed.handheld_radio.";
    public static final int UseTicks = 72000;

    public HandheldRadioItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User.isSecondaryUseActive()) {
            if (User instanceof ServerPlayer Server) {
                toggle(Server, Stack);
            }
            return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide());
        }
        if (!RadioCarry.isOn(Stack)) {
            if (User instanceof ServerPlayer Server) {
                Server.displayClientMessage(Component.translatable(Key + "off_hint").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(Stack);
        }
        User.startUsingItem(Hand);
        return InteractionResultHolder.consume(Stack);
    }

    public static void toggle(ServerPlayer User, ItemStack Stack) {
        boolean On = !RadioCarry.isOn(Stack);
        RadioCarry.setOn(Stack, On);
        if (!Stack.has(RadioRegistry.Channel.get())) {
            RadioCarry.setChannel(Stack, RadioCarry.DefaultChannel);
        }
        RadioSignal.playTo(User, On ? RadioRegistry.SwitchOnLayers : RadioRegistry.SwitchOffLayers, 0.6f, On ? 1.15f : 0.9f);
        Component Shown = On ? Component.translatable(Key + "switched_on", RadioCarry.label(RadioCarry.channel(Stack))).withStyle(ChatFormatting.GREEN)
                : Component.translatable(Key + "switched_off").withStyle(ChatFormatting.GRAY);
        User.displayClientMessage(Shown, true);
    }

    public static void tune(ServerPlayer User, ItemStack Stack, int Step) {
        int Next = RadioCarry.wrapChannel(RadioCarry.channel(Stack) + Integer.signum(Step));
        RadioCarry.setChannel(Stack, Next);
        RadioSignal.playTo(User, List.of(RadioRegistry.Dial), 0.45f, 0.9f + (Next % 5) * 0.06f);
        User.displayClientMessage(Component.translatable(Key + "channel", RadioCarry.label(Next)).withStyle(RadioCarry.isOn(Stack) ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack Old, ItemStack Fresh, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(Old, Fresh);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Channel = RadioCarry.label(RadioCarry.channel(Stack));
        Lines.add(RadioCarry.isOn(Stack) ? Component.translatable(Key + "tooltip.on", Channel).withStyle(ChatFormatting.GREEN)
                : Component.translatable(Key + "tooltip.off", Channel).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "tooltip.toggle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "tooltip.tune").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "tooltip.talk").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "tooltip.text", VoiceConfig.chatPrefix()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "tooltip.listen").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "tooltip.range", VoiceConfig.ranges().Direct()).withStyle(ChatFormatting.GOLD));
        }
    }
}
