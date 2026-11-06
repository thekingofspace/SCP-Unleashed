package net.rai.scp_unleashed.scp4666.item;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class Scp4666WinterGearItem extends ArmorItem {
    public static final float IndoorShare = 1.0f / 3.0f;
    public static final float Thick = 0.66f;
    public static final float Worn = 0.33f;
    private final float Share;
    private final String Key;

    public Scp4666WinterGearItem(Holder<ArmorMaterial> Material, Type Kind, Properties Properties, float Share, String Key) {
        super(Material, Kind, Properties);
        this.Share = Share;
        this.Key = Key;
    }

    public float share() {
        return Share;
    }

    public static float warmth(LivingEntity Wearer) {
        if (!Scp4666World.inside(Wearer)) {
            return 0.0f;
        }
        float Total = 0.0f;
        for (EquipmentSlot Slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET)) {
            if (Wearer.getItemBySlot(Slot).getItem() instanceof Scp4666WinterGearItem Gear) {
                Total += Gear.share();
            }
        }
        return Math.min(1.0f, Total);
    }

    public static void wear(ServerLevel World, ServerPlayer Wearer, int Amount) {
        for (EquipmentSlot Slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET)) {
            ItemStack Stack = Wearer.getItemBySlot(Slot);
            if (!(Stack.getItem() instanceof Scp4666WinterGearItem)) {
                continue;
            }
            int Next = Stack.getDamageValue() + Amount;
            if (Next < Stack.getMaxDamage()) {
                Stack.setDamageValue(Next);
                continue;
            }
            World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Stack.copy()), Wearer.getX(), Wearer.getY() + (Slot == EquipmentSlot.HEAD ? 1.8 : Slot == EquipmentSlot.CHEST ? 1.2 : 0.2),
                    Wearer.getZ(), 10, 0.25, 0.15, 0.25, 0.05);
            World.playSound(null, Wearer.getX(), Wearer.getY() + 1.0, Wearer.getZ(), Scp4666Registry.GearTear.get(), SoundSource.PLAYERS, 0.9f, 0.9f);
            Wearer.setItemSlot(Slot, ItemStack.EMPTY);
            Wearer.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.gear_torn", Stack.getHoverName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack Stack, int Amount, @Nullable T Wearer, Consumer<Item> Broken) {
        return 0;
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack Stack, Entity Wearer, EquipmentSlot Slot, ArmorMaterial.Layer Layer, boolean Inner) {
        return Scp4666Registry.location("textures/models/armor/" + Key + ".png");
    }

    @Override
    public boolean isEnchantable(ItemStack Stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide || !(Holder instanceof Player User) || User.getAbilities().instabuild || Scp4666World.inside(World)) {
            return;
        }
        Stack.setCount(0);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed." + Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        float Left = Stack.getMaxDamage() <= 0 ? 1.0f : 1.0f - (float) Stack.getDamageValue() / Stack.getMaxDamage();
        String State = Left > Thick ? "thick" : Left > Worn ? "thin" : "frayed";
        Lines.add(Component.translatable("item.scp_unleashed.scp4666_winter_gear." + State).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
