package net.rai.scp_unleashed.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GearArmorItem extends ArmorItem {
    private final GearPiece Piece;

    public GearArmorItem(GearPiece Piece, Properties Properties) {
        super(Piece.getOutfit().getArmor(), Piece.getArmorType(), Properties);
        this.Piece = Piece;
    }

    public GearPiece getPiece() {
        return Piece;
    }

    public static boolean coversLegs(ItemStack Stack) {
        return Stack.getItem() instanceof GearArmorItem Gear && Gear.getPiece().coversLegs();
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return Piece.hasCustomModel() ? Piece.getModelTexture() : null;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.gear.outfit." + Piece.getOutfit().getKey()).withStyle(ChatFormatting.GOLD));
            if (Stack.is(GearTags.ToxicGasProtection)) {
                Lines.add(Component.translatable("tooltip.scp_unleashed.gear.toxic_gas").withStyle(ChatFormatting.GOLD));
            }
            if (Stack.is(GearTags.HazmatSuit)) {
                Lines.add(Component.translatable("tooltip.scp_unleashed.gear.hazmat_suit").withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
