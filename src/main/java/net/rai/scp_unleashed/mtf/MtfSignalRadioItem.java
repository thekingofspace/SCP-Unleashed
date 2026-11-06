package net.rai.scp_unleashed.mtf;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.rifttech.ApocalypticFragmentItem;

import java.util.List;

public class MtfSignalRadioItem extends Item {
    public static final String Key = "item.scp_unleashed.mtf_signal_radio.";
    public static final int CallCooldown = 40;
    public static final float CallPitch = 1.0f;
    public static final float DenyPitch = 0.8f;

    public MtfSignalRadioItem(Properties Settings) {
        super(Settings);
    }

    public static InteractionHand otherHand(InteractionHand Hand) {
        return Hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Radio = User.getItemInHand(Hand);
        if (User instanceof ServerPlayer Server) {
            signal(Server, Hand);
        }
        return InteractionResultHolder.sidedSuccess(Radio, World.isClientSide());
    }

    public static void signal(ServerPlayer User, InteractionHand Hand) {
        if (MtfMissions.busy(User.getUUID())) {
            if (User.isShiftKeyDown()) {
                if (MtfMissions.requestExtraction(User)) {
                    MtfMissions.playLayers(User, MtfRegistry.CallLayers, CallPitch);
                    User.displayClientMessage(Component.translatable(MtfMissions.Prefix + "dismissed").withStyle(ChatFormatting.GRAY), true);
                } else {
                    MtfMissions.report(User);
                }
            } else {
                MtfMissions.report(User);
            }
            return;
        }
        long Left = MtfMissions.cooldownLeft(User);
        if (Left > 0L) {
            int Ticks = (int) Math.min(Left, Integer.MAX_VALUE);
            User.getCooldowns().addCooldown(MtfRegistry.SignalRadio.get(), Ticks);
            User.displayClientMessage(Component.translatable(MtfMissions.Prefix + "deny.cooldown", StringUtil.formatTickDuration(Ticks, User.level().tickRateManager().tickrate()))
                    .withStyle(ChatFormatting.RED), true);
            MtfMissions.playLayers(User, MtfRegistry.DenyLayers, DenyPitch);
            return;
        }
        ItemStack Fragment = User.getItemInHand(otherHand(Hand));
        if (Fragment.getItem() instanceof ApocalypticFragmentItem) {
            deny(User, "apocalyptic");
            return;
        }
        MtfTier Tier = MtfTier.fromFragment(Fragment);
        if (Tier == null) {
            deny(User, "no_fragment");
            return;
        }
        MtfMissions.Outcome Result = MtfMissions.call(User, Tier);
        switch (Result) {
            case Called -> {
                if (!User.getAbilities().instabuild) {
                    Fragment.shrink(1);
                }
                User.getCooldowns().addCooldown(MtfRegistry.SignalRadio.get(), CallCooldown);
                MtfMissions.playLayers(User, MtfRegistry.CallLayers, CallPitch);
            }
            case Busy -> deny(User, "busy");
            case NoSky -> deny(User, "no_sky");
            default -> deny(User, "failed");
        }
    }

    private static void deny(ServerPlayer User, String Reason) {
        User.displayClientMessage(Component.translatable(MtfMissions.Prefix + "deny." + Reason).withStyle(ChatFormatting.RED), true);
        MtfMissions.playLayers(User, MtfRegistry.DenyLayers, DenyPitch);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "fragment").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "sky").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "status").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "dismiss").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "grades").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
