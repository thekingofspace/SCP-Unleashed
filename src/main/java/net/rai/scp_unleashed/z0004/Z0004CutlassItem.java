package net.rai.scp_unleashed.z0004;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Z0004CutlassItem extends SwordItem {
    public static final float WaterBonus = 3.0f;

    public Z0004CutlassItem(Tier Material, Properties Settings) {
        super(Material, Settings);
    }

    @Override
    public float getAttackDamageBonus(Entity Target, float Damage, DamageSource Source) {
        Entity Wielder = Source.getEntity();
        return Wielder != null && Wielder.isInWater() ? WaterBonus : 0.0f;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.z0004_cutlass.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.z0004_cutlass.detail").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
