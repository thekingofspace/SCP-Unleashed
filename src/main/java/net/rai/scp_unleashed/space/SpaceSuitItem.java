package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpaceSuitItem extends ArmorItem {
    private final SpaceSuitPiece Piece;

    public SpaceSuitItem(SpaceSuitPiece Piece, Properties Properties) {
        super(SpaceRegistry.SuitMaterial, Piece.getArmorType(), Properties);
        this.Piece = Piece;
    }

    public SpaceSuitPiece getPiece() {
        return Piece;
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return Piece.getModelTexture();
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack Stack, ItemStack Other, Slot Target, ClickAction Action, Player User, SlotAccess Carried) {
        if (Piece != SpaceSuitPiece.Chestplate || Action != ClickAction.SECONDARY || !(Other.getItem() instanceof AirCanisterItem) || !Target.allowModification(User)) {
            return false;
        }
        String Problem = SpaceAir.problem(Stack, Other);
        if (Problem != null) {
            if (!User.level().isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.space." + Problem).withStyle(ChatFormatting.GRAY), true);
            }
            return true;
        }
        ItemStack Returned = SpaceAir.fit(Stack, Other.copyWithCount(1));
        Other.shrink(1);
        if (Other.isEmpty()) {
            Carried.set(Returned);
        } else if (!Returned.isEmpty() && !User.getInventory().add(Returned)) {
            User.drop(Returned, false);
        }
        Target.setChanged();
        if (!User.level().isClientSide) {
            SpaceAir.refillSound(User, 0.8F);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (Piece == SpaceSuitPiece.Chestplate) {
            SpaceSuitTank Tank = SpaceAir.tank(Stack);
            if (!Tank.Fitted()) {
                Lines.add(Component.translatable("tooltip.scp_unleashed.space.no_canister").withStyle(ChatFormatting.DARK_GRAY));
            } else if (Tank.Air() <= 0) {
                Lines.add(Component.translatable("tooltip.scp_unleashed.space.tank_empty").withStyle(ChatFormatting.RED));
            } else {
                Lines.add(Component.translatable("tooltip.scp_unleashed.space.air", Math.round(Tank.fraction() * 100.0F)).withStyle(ChatFormatting.AQUA));
            }
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.space.suit_part").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(getDescriptionId() + ".detail").withStyle(ChatFormatting.GOLD));
        }
    }
}
