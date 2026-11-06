package net.rai.scp_unleashed.scp001scarlet.king.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class ScarletArmorItem extends ArmorItem {
    public static final ResourceLocation OuterTexture = ScarletKingRegistry.location("textures/models/armor/scarlet_model_outer.png");
    public static final ResourceLocation InnerTexture = ScarletKingRegistry.location("textures/models/armor/scarlet_model_inner.png");
    public static final int AuraEvery = 5;

    public ScarletArmorItem(Type Slot, Properties Settings) {
        super(ScarletKingRegistry.Scarlet, Slot, Settings);
    }

    public static boolean fullSet(LivingEntity Wearer) {
        for (EquipmentSlot Slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (!(Wearer.getItemBySlot(Slot).getItem() instanceof ScarletArmorItem)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return InnerModel ? InnerTexture : OuterTexture;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        super.inventoryTick(Stack, World, Holder, Slot, Selected);
        if (!World.isClientSide || getType() != Type.CHESTPLATE || !(Holder instanceof LivingEntity Wearer) || Wearer.getItemBySlot(EquipmentSlot.CHEST) != Stack
                || Wearer.tickCount % AuraEvery != 0 || !fullSet(Wearer) || Wearer.isInvisible()) {
            return;
        }
        double Angle = World.random.nextDouble() * Math.PI * 2.0;
        double X = Wearer.getX() + Math.cos(Angle) * 0.45;
        double Z = Wearer.getZ() + Math.sin(Angle) * 0.45;
        double Y = Wearer.getY() + World.random.nextDouble() * Wearer.getBbHeight() * 0.8;
        if (World.random.nextInt(3) == 0) {
            World.addParticle(ParticleTypes.SMALL_FLAME, X, Y, Z, 0.0, 0.015, 0.0);
        } else {
            World.addParticle(new DustParticleOptions(new Vector3f(0.85f, 0.18f, 0.12f), 0.7f), X, Y, Z, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scarlet_armor.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.scarlet_armor.set").withStyle(ChatFormatting.GOLD));
        }
    }
}
