package net.rai.scp_unleashed.scp428;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.rai.scp_unleashed.playerclone.PlayerSamples;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Scp428Record {
    public static final String IdKey = "Id";
    public static final String BodyKey = "Body";
    public static final String AppearanceKey = "Appearance";
    public static final String SampleKey = "Sample";
    public static final String NameKey = "Name";
    public static final String HealthKey = "Health";
    public static final String CarriedKey = "Carried";
    public static final String OwnedKey = "Owned";
    public static final String FromPlayerKey = "FromPlayer";

    private final UUID Id;
    private final int Appearance;
    private final CompoundTag Sample;
    private final String Name;
    @Nullable
    private UUID Body;
    private float Health;
    private int Missing;
    private final List<ItemStack> Carried = new ArrayList<>();
    private boolean Owned;
    private boolean FromPlayer;

    public Scp428Record(UUID Id, int Appearance, CompoundTag Sample, String Name, float Health) {
        this.Id = Id;
        this.Appearance = Appearance;
        this.Sample = Sample.copy();
        this.Name = Name;
        this.Health = Health;
    }

    public static Scp428Record of(Scp428Looks.Look Taken) {
        return new Scp428Record(UUID.randomUUID(), Taken.Appearance(), Taken.Sample(), Taken.Name(), 0.0f);
    }

    public static Scp428Record of(Scp428Looks.Look Taken, Scp428Carry.Haul Load) {
        Scp428Record Made = of(Taken);
        Made.carry(Load);
        return Made;
    }

    public void carry(Scp428Carry.Haul Load) {
        Carried.clear();
        for (ItemStack Stack : Load.Stacks()) {
            if (!Stack.isEmpty()) {
                Carried.add(Stack);
            }
        }
        Owned = Load.Owned();
        FromPlayer = Load.FromPlayer();
    }

    public List<ItemStack> carried() {
        return Carried;
    }

    public boolean owned() {
        return Owned;
    }

    public boolean fromPlayer() {
        return FromPlayer;
    }

    public List<ItemStack> release() {
        List<ItemStack> Gone = new ArrayList<>();
        for (ItemStack Stack : Carried) {
            if (!Stack.isEmpty()) {
                Gone.add(Stack);
            }
        }
        Carried.clear();
        return Gone;
    }

    public UUID id() {
        return Id;
    }

    public int appearance() {
        return Appearance;
    }

    public CompoundTag sample() {
        return Sample.copy();
    }

    public boolean isPlayer() {
        return Sample.contains(PlayerSamples.ProfileKey);
    }

    @Nullable
    public ResolvableProfile profile() {
        return PlayerSamples.read(Sample);
    }

    public String name() {
        return Name;
    }

    @Nullable
    public UUID body() {
        return Body;
    }

    public void setBody(@Nullable UUID Next) {
        Body = Next;
        Missing = 0;
    }

    public float health() {
        return Health;
    }

    public void setHealth(float Value) {
        Health = Value;
    }

    public int missing() {
        return Missing;
    }

    public void addMissing(int Ticks) {
        Missing += Ticks;
    }

    public void found() {
        Missing = 0;
    }

    public CompoundTag save(HolderLookup.Provider Registries) {
        CompoundTag Saved = save();
        ListTag Listed = new ListTag();
        for (ItemStack Stack : Carried) {
            if (!Stack.isEmpty()) {
                Listed.add(Stack.save(Registries));
            }
        }
        Saved.put(CarriedKey, Listed);
        Saved.putBoolean(OwnedKey, Owned);
        Saved.putBoolean(FromPlayerKey, FromPlayer);
        return Saved;
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putUUID(IdKey, Id);
        if (Body != null) {
            Saved.putUUID(BodyKey, Body);
        }
        Saved.putInt(AppearanceKey, Appearance);
        Saved.put(SampleKey, Sample.copy());
        Saved.putString(NameKey, Name);
        Saved.putFloat(HealthKey, Health);
        return Saved;
    }

    public static Scp428Record load(CompoundTag Saved) {
        UUID Id = Saved.hasUUID(IdKey) ? Saved.getUUID(IdKey) : UUID.randomUUID();
        Scp428Record Loaded = new Scp428Record(Id, Saved.getInt(AppearanceKey), Saved.getCompound(SampleKey), Saved.getString(NameKey), Saved.getFloat(HealthKey));
        if (Saved.hasUUID(BodyKey)) {
            Loaded.Body = Saved.getUUID(BodyKey);
        }
        return Loaded;
    }

    public static Scp428Record load(CompoundTag Saved, HolderLookup.Provider Registries) {
        Scp428Record Loaded = load(Saved);
        ListTag Listed = Saved.getList(CarriedKey, Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Listed.size(); Index++) {
            ItemStack Stack = ItemStack.parseOptional(Registries, Listed.getCompound(Index));
            if (!Stack.isEmpty()) {
                Loaded.Carried.add(Stack);
            }
        }
        Loaded.Owned = Saved.getBoolean(OwnedKey);
        Loaded.FromPlayer = Saved.getBoolean(FromPlayerKey);
        return Loaded;
    }
}
