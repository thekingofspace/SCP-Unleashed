package net.rai.scp_unleashed.scp428;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.DClassNumbers;
import net.rai.scp_unleashed.mtf.MtfOperative;
import net.rai.scp_unleashed.playerclone.PlayerClone;
import net.rai.scp_unleashed.playerclone.PlayerSamples;

import java.util.EnumMap;
import java.util.Map;

public final class Scp428Looks {
    public static final int NumberLow = 1000;
    public static final int NumberSpread = 9000;
    public static final String GearKey = "Gear";
    public static final String SlotKey = "Slot";
    public static final String StackKey = "Stack";
    public static final EquipmentSlot[] Worn = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private Scp428Looks() {
    }

    public record Look(int Appearance, CompoundTag Sample, String Name) {
        public boolean isPlayer() {
            return Sample.contains(PlayerSamples.ProfileKey);
        }
    }

    public static Look of(LivingEntity Victim, RandomSource Dice) {
        if (Victim instanceof Player User) {
            GameProfile Source = User.getGameProfile();
            GameProfile Copy = new GameProfile(Source.getId(), Source.getName());
            Copy.getProperties().putAll(Source.getProperties());
            return new Look(0, PlayerSamples.write(new ResolvableProfile(Copy)), Source.getName());
        }
        if (Victim instanceof PlayerClone Clone) {
            ResolvableProfile Profile = Clone.profile();
            return new Look(Clone.appearance(), PlayerSamples.write(Profile), Clone.getName().getString());
        }
        if (Victim instanceof MtfOperative Operative) {
            return new Look(Operative.appearance(), wornBy(Operative), Operative.getName().getString());
        }
        if (Victim instanceof DClassLike Body) {
            return new Look(Body.appearance(), new CompoundTag(), Body.getName().getString());
        }
        return new Look(DClassAppearance.random(Dice), new CompoundTag(), Victim.getName().getString());
    }

    public static CompoundTag wornBy(LivingEntity Wearer) {
        CompoundTag Sample = new CompoundTag();
        ListTag Listed = new ListTag();
        for (EquipmentSlot Slot : Worn) {
            ItemStack Stack = Wearer.getItemBySlot(Slot);
            if (!Stack.isEmpty()) {
                CompoundTag Entry = new CompoundTag();
                Entry.putString(SlotKey, Slot.getName());
                Entry.put(StackKey, Stack.copyWithCount(1).save(Wearer.registryAccess()));
                Listed.add(Entry);
            }
        }
        if (!Listed.isEmpty()) {
            Sample.put(GearKey, Listed);
        }
        return Sample;
    }

    public static Map<EquipmentSlot, ItemStack> gear(CompoundTag Sample, HolderLookup.Provider Registries) {
        Map<EquipmentSlot, ItemStack> Found = new EnumMap<>(EquipmentSlot.class);
        ListTag Listed = Sample.getList(GearKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Listed.size(); Index++) {
            CompoundTag Entry = Listed.getCompound(Index);
            EquipmentSlot Slot;
            try {
                Slot = EquipmentSlot.byName(Entry.getString(SlotKey));
            } catch (IllegalArgumentException Unknown) {
                continue;
            }
            ItemStack Stack = ItemStack.parseOptional(Registries, Entry.getCompound(StackKey));
            if (!Stack.isEmpty() && Slot.isArmor()) {
                Found.put(Slot, Stack);
            }
        }
        return Found;
    }

    public static Look stray(RandomSource Dice) {
        return new Look(DClassAppearance.random(Dice), new CompoundTag(), DClassNumbers.format(NumberLow + Dice.nextInt(NumberSpread)));
    }
}
