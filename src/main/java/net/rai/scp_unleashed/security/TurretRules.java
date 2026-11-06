package net.rai.scp_unleashed.security;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TurretRules {
    public static final int MaxEntries = 32;
    public static final int MaxLength = 64;
    public static final Pattern NamePattern = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    public static final String CategoriesKey = "Categories";
    public static final String EntriesKey = "Entries";

    private int Categories = TurretCategory.DefaultMask;
    private final List<Entry> Entries = new ArrayList<>();

    public boolean has(TurretCategory Category) {
        return (Categories & Category.bit()) != 0;
    }

    public void toggle(TurretCategory Category) {
        Categories ^= Category.bit();
    }

    public void set(TurretCategory Category, boolean Enabled) {
        Categories = Enabled ? Categories | Category.bit() : Categories & ~Category.bit();
    }

    public int getMask() {
        return Categories;
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(Entries);
    }

    @Nullable
    public static String normalise(String Text) {
        String Trimmed = Text.trim();
        if (Trimmed.isEmpty() || Trimmed.length() > MaxLength) {
            return null;
        }
        if (Trimmed.indexOf(':') >= 0) {
            ResourceLocation Id = ResourceLocation.tryParse(Trimmed.toLowerCase(Locale.ROOT));
            return Id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(Id) ? Id.toString() : null;
        }
        return NamePattern.matcher(Trimmed).matches() ? Trimmed : null;
    }

    public static boolean isType(String Key) {
        return Key.indexOf(':') >= 0;
    }

    public boolean add(String Text, boolean Deny) {
        String Key = normalise(Text);
        if (Key == null) {
            return false;
        }
        int Existing = indexOf(Key);
        if (Existing >= 0) {
            Entries.set(Existing, new Entry(Entries.get(Existing).Key(), Deny));
            return true;
        }
        if (Entries.size() >= MaxEntries) {
            return false;
        }
        Entries.add(new Entry(Key, Deny));
        return true;
    }

    public boolean remove(int Index) {
        if (Index < 0 || Index >= Entries.size()) {
            return false;
        }
        Entries.remove(Index);
        return true;
    }

    public boolean flip(int Index) {
        if (Index < 0 || Index >= Entries.size()) {
            return false;
        }
        Entry Old = Entries.get(Index);
        Entries.set(Index, new Entry(Old.Key(), !Old.Deny()));
        return true;
    }

    public int indexOf(String Key) {
        for (int Index = 0; Index < Entries.size(); Index++) {
            if (matchesKey(Entries.get(Index).Key(), Key)) {
                return Index;
            }
        }
        return -1;
    }

    private static boolean matchesKey(String First, String Second) {
        return isType(First) ? First.equals(Second) : First.equalsIgnoreCase(Second);
    }

    @Nullable
    public Boolean nameVerdict(String Name) {
        for (Entry Item : Entries) {
            if (!isType(Item.Key()) && Item.Key().equalsIgnoreCase(Name)) {
                return Item.Deny();
            }
        }
        return null;
    }

    @Nullable
    public Boolean typeVerdict(Entity Target) {
        String Type = BuiltInRegistries.ENTITY_TYPE.getKey(Target.getType()).toString();
        for (Entry Item : Entries) {
            if (isType(Item.Key()) && Item.Key().equals(Type)) {
                return Item.Deny();
            }
        }
        return null;
    }

    @Nullable
    public Boolean verdict(Entity Target) {
        if (Target instanceof Player User) {
            Boolean ByName = nameVerdict(User.getGameProfile().getName());
            if (ByName != null) {
                return ByName;
            }
        }
        return typeVerdict(Target);
    }

    public boolean allowsName(String Name) {
        return Boolean.FALSE.equals(nameVerdict(Name));
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt(CategoriesKey, Categories);
        ListTag Rows = new ListTag();
        for (Entry Item : Entries) {
            CompoundTag Saved = new CompoundTag();
            Saved.putString("Key", Item.Key());
            Saved.putBoolean("Deny", Item.Deny());
            Rows.add(Saved);
        }
        Tag.put(EntriesKey, Rows);
        return Tag;
    }

    public void load(CompoundTag Tag) {
        Categories = Tag.contains(CategoriesKey) ? Tag.getInt(CategoriesKey) : TurretCategory.DefaultMask;
        Entries.clear();
        ListTag Rows = Tag.getList(EntriesKey, CompoundTag.TAG_COMPOUND);
        for (int Index = 0; Index < Rows.size() && Entries.size() < MaxEntries; Index++) {
            CompoundTag Saved = Rows.getCompound(Index);
            String Key = Saved.getString("Key");
            if (!Key.isEmpty() && Key.length() <= MaxLength && indexOf(Key) < 0) {
                Entries.add(new Entry(Key, Saved.getBoolean("Deny")));
            }
        }
    }

    public void copyFrom(TurretRules Other) {
        Categories = Other.Categories;
        Entries.clear();
        Entries.addAll(Other.Entries);
    }

    public record Entry(String Key, boolean Deny) {
        public boolean isType() {
            return TurretRules.isType(Key);
        }
    }
}
