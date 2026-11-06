package net.rai.scp_unleashed.playerclone;

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
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class SamplingNeedleItem extends Item {
    private static final String Key = "item.scp_unleashed.sampling_needle.";

    public SamplingNeedleItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public boolean hurtEnemy(ItemStack Stack, LivingEntity Target, LivingEntity Attacker) {
        if (Attacker instanceof ServerPlayer User) {
            PlayerSamples.draw(User, Target, Stack);
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!User.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(Stack);
        }
        if (User instanceof ServerPlayer Server) {
            PlayerSamples.drawSelf(Server, Stack);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "vial").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "self").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "players").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "clone").withStyle(ChatFormatting.GOLD));
        }
    }
}
