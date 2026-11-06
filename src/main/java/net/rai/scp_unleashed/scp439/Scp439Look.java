package net.rai.scp_unleashed.scp439;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.playerclone.PlayerClone;
import net.rai.scp_unleashed.playerclone.PlayerSamples;
import org.jetbrains.annotations.Nullable;

public record Scp439Look(byte Kind, int Appearance, CompoundTag Profile, String Type, String Profession, int Level, CompoundTag Name, CompoundTag Gear) {
    public static final byte KindDClass = 0;
    public static final byte KindPlayer = 1;
    public static final byte KindVillager = 2;
    public static final byte KindTrader = 3;
    public static final EquipmentSlot[] Worn = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public static final Scp439Look Blank = new Scp439Look(KindDClass, 0, new CompoundTag(), "plains", "none", 1, new CompoundTag(), new CompoundTag());

    public static Scp439Look of(LivingEntity Host) {
        CompoundTag Named = nameTag(Host.getName(), Host.registryAccess());
        if (Host instanceof Player User) {
            GameProfile Known = User.getGameProfile();
            GameProfile Copy = new GameProfile(Known.getId(), Known.getName());
            Copy.getProperties().putAll(Known.getProperties());
            return new Scp439Look(KindPlayer, 0, PlayerSamples.write(new ResolvableProfile(Copy)), "plains", "none", 1, Named, new CompoundTag());
        }
        if (Host instanceof PlayerClone Clone && Clone.profile() != null) {
            return new Scp439Look(KindPlayer, 0, PlayerSamples.write(Clone.profile()), "plains", "none", 1, Named, new CompoundTag());
        }
        if (Host instanceof DClassLike Person) {
            return new Scp439Look(KindDClass, Person.appearance(), new CompoundTag(), "plains", "none", 1, Named, gearOf(Person));
        }
        if (Host instanceof Villager Local) {
            VillagerData Data = Local.getVillagerData();
            return new Scp439Look(KindVillager, 0, new CompoundTag(), BuiltInRegistries.VILLAGER_TYPE.getKey(Data.getType()).getPath(),
                    BuiltInRegistries.VILLAGER_PROFESSION.getKey(Data.getProfession()).getPath(), Data.getLevel(), Named, new CompoundTag());
        }
        if (Host instanceof WanderingTrader) {
            return new Scp439Look(KindTrader, 0, new CompoundTag(), "plains", "none", 1, Named, new CompoundTag());
        }
        return new Scp439Look(KindDClass, DClassAppearance.random(Host.getRandom()), new CompoundTag(), "plains", "none", 1, Named, new CompoundTag());
    }

    public static CompoundTag gearOf(LivingEntity Host) {
        CompoundTag Kept = new CompoundTag();
        for (EquipmentSlot Slot : Worn) {
            ItemStack Stack = Host.getItemBySlot(Slot);
            if (Stack.getItem() instanceof ArmorItem Armour && Armour.getEquipmentSlot() == Slot) {
                Kept.putString(Slot.getName(), BuiltInRegistries.ITEM.getKey(Stack.getItem()).toString());
            }
        }
        return Kept;
    }

    public ItemStack gear(EquipmentSlot Slot) {
        if (!Gear.contains(Slot.getName())) {
            return ItemStack.EMPTY;
        }
        ResourceLocation Id = ResourceLocation.tryParse(Gear.getString(Slot.getName()));
        if (Id == null || !BuiltInRegistries.ITEM.containsKey(Id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(Id));
    }

    public boolean geared() {
        return !Gear.isEmpty();
    }

    public static CompoundTag nameTag(Component Text, HolderLookup.Provider Registries) {
        CompoundTag Holder = new CompoundTag();
        ComponentSerialization.CODEC.encodeStart(Registries.createSerializationContext(NbtOps.INSTANCE), Text).result().ifPresent(Encoded -> Holder.put("Text", Encoded));
        return Holder;
    }

    @Nullable
    public Component name(HolderLookup.Provider Registries) {
        if (!Name.contains("Text")) {
            return null;
        }
        return ComponentSerialization.CODEC.parse(Registries.createSerializationContext(NbtOps.INSTANCE), Name.get("Text")).result().orElse(null);
    }

    @Nullable
    public ResolvableProfile profile() {
        return PlayerSamples.read(Profile);
    }

    public boolean villagerLike() {
        return Kind == KindVillager || Kind == KindTrader;
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putByte("Kind", Kind);
        Saved.putInt("Appearance", Appearance);
        Saved.put("Profile", Profile.copy());
        Saved.putString("Type", Type);
        Saved.putString("Profession", Profession);
        Saved.putInt("Level", Level);
        Saved.put("Name", Name.copy());
        Saved.put("Gear", Gear.copy());
        return Saved;
    }

    public static Scp439Look load(CompoundTag Saved) {
        if (!Saved.contains("Kind")) {
            return Blank;
        }
        return new Scp439Look(Saved.getByte("Kind"), Saved.getInt("Appearance"), Saved.getCompound("Profile").copy(), Saved.contains("Type") ? Saved.getString("Type") : "plains",
                Saved.contains("Profession") ? Saved.getString("Profession") : "none", Math.max(1, Saved.getInt("Level")), Saved.getCompound("Name").copy(),
                Saved.getCompound("Gear").copy());
    }
}
