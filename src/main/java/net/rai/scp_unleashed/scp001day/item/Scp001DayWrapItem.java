package net.rai.scp_unleashed.scp001day.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;

public class Scp001DayWrapItem extends ArmorItem {
    public static final double Slowdown = -0.0625;
    public static final float Thick = 0.66f;
    public static final float Worn = 0.33f;
    private final String Key;
    private ItemAttributeModifiers Modifiers;

    public Scp001DayWrapItem(Holder<ArmorMaterial> Material, Type Kind, Properties Properties, String Key) {
        super(Material, Kind, Properties);
        this.Key = Key;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        if (Modifiers == null) {
            EquipmentSlotGroup Group = EquipmentSlotGroup.bySlot(getEquipmentSlot());
            ItemAttributeModifiers.Builder Builder = ItemAttributeModifiers.builder();
            for (ItemAttributeModifiers.Entry Entry : super.getDefaultAttributeModifiers().modifiers()) {
                Builder.add(Entry.attribute(), Entry.modifier(), Entry.slot());
            }
            Builder.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(Scp001DayRegistry.location("scp001_day_wrap_speed_" + getEquipmentSlot().getName()), Slowdown,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE), Group);
            Modifiers = Builder.build();
        }
        return Modifiers;
    }

    public static boolean shields(LivingEntity Wearer, EquipmentSlot Slot) {
        ItemStack Stack = Wearer.getItemBySlot(Slot);
        return Stack.getItem() instanceof Scp001DayWrapItem Wrap && Wrap.getEquipmentSlot() == Slot && Stack.getDamageValue() < Stack.getMaxDamage();
    }

    public static int worn(LivingEntity Wearer) {
        int Count = 0;
        for (EquipmentSlot Slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (shields(Wearer, Slot)) {
                Count++;
            }
        }
        return Count;
    }

    public static void wear(ServerLevel World, ServerPlayer Wearer, EquipmentSlot Slot, int Amount) {
        ItemStack Stack = Wearer.getItemBySlot(Slot);
        if (!(Stack.getItem() instanceof Scp001DayWrapItem) || Wearer.getAbilities().instabuild) {
            return;
        }
        int Next = Stack.getDamageValue() + Amount;
        if (Next < Stack.getMaxDamage()) {
            Stack.setDamageValue(Next);
            return;
        }
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Stack.copy()), Wearer.getX(), Wearer.getY() + (Slot == EquipmentSlot.HEAD ? 1.8 : Slot == EquipmentSlot.CHEST ? 1.2 : Slot == EquipmentSlot.LEGS ? 0.7 : 0.2),
                Wearer.getZ(), 10, 0.25, 0.15, 0.25, 0.05);
        World.playSound(null, Wearer.getX(), Wearer.getY() + 1.0, Wearer.getZ(), Scp001DayRegistry.WrapTear.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
        Wearer.setItemSlot(Slot, ItemStack.EMPTY);
        Wearer.displayClientMessage(Component.translatable("message.scp_unleashed.scp001_day.wrap_torn", Stack.getHoverName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer Layer, boolean Inner) {
        return Scp001DayRegistry.location("textures/models/armor/" + Key + ".png");
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed." + Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        float Left = Stack.getMaxDamage() <= 0 ? 1.0f : 1.0f - (float) Stack.getDamageValue() / Stack.getMaxDamage();
        String State = Left > Thick ? "thick" : Left > Worn ? "thin" : "frayed";
        Lines.add(Component.translatable("item.scp_unleashed.scp001_day_sun_wrap." + State).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
