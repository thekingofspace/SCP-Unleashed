package net.rai.scp_unleashed.scp4666.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp4666.Scp4666Director;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.List;

public class Scp4666CocoaItem extends Item {
    public static final int WarmthTicks = 200;
    public static final String Prefix = "item.scp_unleashed.scp4666_hot_cocoa.";

    public Scp4666CocoaItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity Drinker) {
        return 40;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return Scp4666Registry.Drink.get();
    }

    @Override
    public SoundEvent getEatingSound() {
        return Scp4666Registry.Drink.get();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Drinker) {
        if (!World.isClientSide) {
            Drinker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, WarmthTicks, 0, false, true));
            Drinker.setTicksFrozen(0);
            if (Drinker instanceof ServerPlayer User) {
                User.displayClientMessage(Component.translatable(Prefix + "warm").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
                if (Scp4666World.inside(User)) {
                    Scp4666Director.drank(User);
                }
            }
        }
        return super.finishUsingItem(Stack, World, Drinker);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "detail").withStyle(ChatFormatting.GOLD));
        }
    }
}
