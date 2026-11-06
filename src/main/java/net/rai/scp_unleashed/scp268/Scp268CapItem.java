package net.rai.scp_unleashed.scp268;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp268.integration.Scp268Curios;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp268CapItem extends ArmorItem {
    public static final ResourceLocation ModelTexture = Scp268Registry.location("textures/models/armor/scp268_cap.png");
    public static final String Prefix = "item.scp_unleashed.scp268_cap.";
    public static final String WornHead = "tooltip.scp_unleashed.senses.worn_head";
    public static final String WornCurios = "tooltip.scp_unleashed.senses.worn_curios.head";

    public Scp268CapItem(Holder<ArmorMaterial> Material, Properties Settings) {
        super(Material, Type.HELMET, Settings);
    }

    @Nullable
    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer ArmorLayer, boolean InnerModel) {
        return ModelTexture;
    }

    @Override
    public boolean canEquip(ItemStack Stack, EquipmentSlot Slot, LivingEntity Wearer) {
        return !Scp268.settling(Wearer) && super.canEquip(Stack, Slot, Wearer);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        return wear(World, User, Hand, Senses.curiosLoaded());
    }

    public InteractionResultHolder<ItemStack> wear(Level World, Player User, InteractionHand Hand, boolean Accessory) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Scp268.settling(User) || Scp268.isWorn(User)) {
            return InteractionResultHolder.fail(Held);
        }
        if (Accessory && Scp268Curios.wear(User, Held)) {
            Scp268.forget(User);
            if (!World.isClientSide()) {
                User.awardStat(Stats.ITEM_USED.get(this));
            }
            return InteractionResultHolder.sidedSuccess(Held, World.isClientSide());
        }
        InteractionResultHolder<ItemStack> Result = super.use(World, User, Hand);
        Scp268.forget(User);
        return Result;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack Stack) {
        return ItemAttributeModifiers.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "tooltip.details").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Senses.curiosLoaded() ? WornCurios : WornHead).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
