package net.rai.scp_unleashed.control;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BodyState {
    public static final String InventoryKey = "Inventory";
    public static final String SelectedKey = "Selected";
    public static final String HealthKey = "Health";
    public static final String AbsorptionKey = "Absorption";
    public static final String FoodKey = "Food";
    public static final String LevelKey = "XpLevel";
    public static final String ProgressKey = "XpProgress";
    public static final String TotalKey = "XpTotal";
    public static final String EffectsKey = "Effects";
    public static final String AirKey = "Air";
    public static final String FireKey = "Fire";
    public static final String FrozenKey = "Frozen";
    public static final String ArrowsKey = "Arrows";
    public static final String AttachmentsKey = "Attachments";
    public static final int MainSize = 36;
    public static final int ArmorSize = 4;
    public static final int ArmorBase = 100;
    public static final int OffhandSlot = 150;

    private BodyState() {
    }

    public static CompoundTag capture(ServerPlayer User) {
        CompoundTag Body = new CompoundTag();
        Body.put(InventoryKey, User.getInventory().save(new ListTag()));
        Body.putInt(SelectedKey, User.getInventory().selected);
        Body.putFloat(HealthKey, User.getHealth());
        Body.putFloat(AbsorptionKey, User.getAbsorptionAmount());
        CompoundTag Food = new CompoundTag();
        User.getFoodData().addAdditionalSaveData(Food);
        Body.put(FoodKey, Food);
        Body.putInt(LevelKey, User.experienceLevel);
        Body.putFloat(ProgressKey, User.experienceProgress);
        Body.putInt(TotalKey, User.totalExperience);
        Body.put(EffectsKey, saveEffects(User));
        Body.putInt(AirKey, User.getAirSupply());
        Body.putInt(FireKey, User.getRemainingFireTicks());
        Body.putInt(FrozenKey, User.getTicksFrozen());
        Body.putInt(ArrowsKey, User.getArrowCount());
        Body.put(AttachmentsKey, BodyAttachments.capture(User));
        return Body;
    }

    public static void apply(ServerPlayer User, CompoundTag Body) {
        if (User.isUsingItem()) {
            User.stopUsingItem();
        }
        Inventory Pack = User.getInventory();
        Pack.load(Body.getList(InventoryKey, Tag.TAG_COMPOUND));
        Pack.selected = Mth.clamp(Body.getInt(SelectedKey), 0, Inventory.getSelectionSize() - 1);
        loadEffects(User, Body.getList(EffectsKey, Tag.TAG_COMPOUND));
        BodyAttachments.apply(User, Body.getCompound(AttachmentsKey));
        User.getFoodData().readAdditionalSaveData(Body.getCompound(FoodKey));
        User.experienceLevel = Math.max(0, Body.getInt(LevelKey));
        User.experienceProgress = Mth.clamp(Body.getFloat(ProgressKey), 0.0f, 1.0f);
        User.totalExperience = Math.max(0, Body.getInt(TotalKey));
        User.setExperienceLevels(User.experienceLevel);
        User.setAirSupply(Body.contains(AirKey) ? Body.getInt(AirKey) : User.getMaxAirSupply());
        User.setRemainingFireTicks(Body.getInt(FireKey));
        User.setTicksFrozen(Body.getInt(FrozenKey));
        User.setArrowCount(Body.getInt(ArrowsKey));
        float Health = Body.contains(HealthKey) ? Body.getFloat(HealthKey) : User.getMaxHealth();
        User.setHealth(Math.max(0.5f, Health));
        User.setAbsorptionAmount(Body.getFloat(AbsorptionKey));
        User.fallDistance = 0.0f;
        User.inventoryMenu.sendAllDataToRemote();
        if (User.connection != null) {
            User.connection.send(new ClientboundSetCarriedItemPacket(Pack.selected));
        }
        User.resetSentInfo();
    }

    public static CompoundTag fresh(float Health) {
        CompoundTag Body = new CompoundTag();
        Body.put(InventoryKey, new ListTag());
        Body.putInt(SelectedKey, 0);
        Body.putFloat(HealthKey, Health);
        Body.putFloat(AbsorptionKey, 0.0f);
        CompoundTag Food = new CompoundTag();
        Food.putInt("foodLevel", 20);
        Food.putInt("foodTickTimer", 0);
        Food.putFloat("foodSaturationLevel", 5.0f);
        Food.putFloat("foodExhaustionLevel", 0.0f);
        Body.put(FoodKey, Food);
        Body.put(EffectsKey, new ListTag());
        Body.put(AttachmentsKey, new CompoundTag());
        return Body;
    }

    public static ListTag saveEffects(LivingEntity Holder) {
        ListTag Effects = new ListTag();
        for (MobEffectInstance Instance : Holder.getActiveEffects()) {
            Effects.add(Instance.save());
        }
        return Effects;
    }

    public static void loadEffects(LivingEntity Holder, ListTag Effects) {
        Holder.removeAllEffects();
        for (int Index = 0; Index < Effects.size(); Index++) {
            MobEffectInstance Instance = MobEffectInstance.load(Effects.getCompound(Index));
            if (Instance != null) {
                Holder.addEffect(Instance);
            }
        }
    }

    public static ListTag saveItems(HolderLookup.Provider Registries, NonNullList<ItemStack> Main, NonNullList<ItemStack> Armor, ItemStack Offhand) {
        ListTag Items = new ListTag();
        for (int Slot = 0; Slot < Main.size(); Slot++) {
            put(Registries, Items, Main.get(Slot), Slot);
        }
        for (int Slot = 0; Slot < Armor.size(); Slot++) {
            put(Registries, Items, Armor.get(Slot), ArmorBase + Slot);
        }
        put(Registries, Items, Offhand, OffhandSlot);
        return Items;
    }

    private static void put(HolderLookup.Provider Registries, ListTag Items, ItemStack Stack, int Slot) {
        if (Stack.isEmpty()) {
            return;
        }
        CompoundTag Entry = new CompoundTag();
        Entry.putByte("Slot", (byte) Slot);
        Items.add(Stack.save(Registries, Entry));
    }

    public static ItemStack loadItems(HolderLookup.Provider Registries, ListTag Items, NonNullList<ItemStack> Main, NonNullList<ItemStack> Armor) {
        for (int Slot = 0; Slot < Main.size(); Slot++) {
            Main.set(Slot, ItemStack.EMPTY);
        }
        for (int Slot = 0; Slot < Armor.size(); Slot++) {
            Armor.set(Slot, ItemStack.EMPTY);
        }
        ItemStack Offhand = ItemStack.EMPTY;
        for (int Index = 0; Index < Items.size(); Index++) {
            CompoundTag Entry = Items.getCompound(Index);
            int Slot = Entry.getByte("Slot") & 255;
            ItemStack Stack = ItemStack.parse(Registries, Entry).orElse(ItemStack.EMPTY);
            if (Slot < Main.size()) {
                Main.set(Slot, Stack);
            } else if (Slot >= ArmorBase && Slot < ArmorBase + Armor.size()) {
                Armor.set(Slot - ArmorBase, Stack);
            } else if (Slot == OffhandSlot) {
                Offhand = Stack;
            }
        }
        return Offhand;
    }

    public static List<ItemStack> itemsOf(HolderLookup.Provider Registries, CompoundTag Body) {
        List<ItemStack> Found = new ArrayList<>();
        ListTag Items = Body.getList(InventoryKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Items.size(); Index++) {
            ItemStack Stack = ItemStack.parse(Registries, Items.getCompound(Index)).orElse(ItemStack.EMPTY);
            if (!Stack.isEmpty()) {
                Found.add(Stack);
            }
        }
        return Found;
    }

    public static void dropItems(ServerLevel World, Vec3 At, CompoundTag Body) {
        for (ItemStack Stack : itemsOf(World.registryAccess(), Body)) {
            Containers.dropItemStack(World, At.x, At.y + 0.5, At.z, Stack);
        }
    }
}
