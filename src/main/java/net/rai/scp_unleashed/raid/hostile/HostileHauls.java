package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.rai.scp_unleashed.raid.Raid;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class HostileHauls {
    public static final String HaulKey = "Hauls";
    public static final String IssuedKey = "Issued";

    private HostileHauls() {
    }

    private static ListTag list(Raid Current, String Key) {
        return Current.extra().getList(Key, Tag.TAG_COMPOUND);
    }

    private static void store(Raid Current, String Key, ListTag Saved) {
        Current.extra().put(Key, Saved);
        Current.changed();
    }

    public static void put(Raid Current, UUID Raider, HostileTrophy Trophy) {
        ListTag Saved = list(Current, HaulKey);
        for (int Index = 0; Index < Saved.size(); Index++) {
            if (Raider.toString().equals(Saved.getCompound(Index).getString("Raider"))) {
                Saved.remove(Index);
                break;
            }
        }
        CompoundTag One = new CompoundTag();
        One.putString("Raider", Raider.toString());
        One.put("Trophy", Trophy.save());
        Saved.add(One);
        store(Current, HaulKey, Saved);
    }

    @Nullable
    public static HostileTrophy of(Raid Current, UUID Raider) {
        ListTag Saved = list(Current, HaulKey);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag One = Saved.getCompound(Index);
            if (Raider.toString().equals(One.getString("Raider"))) {
                return HostileTrophy.load(One.getCompound("Trophy"));
            }
        }
        return null;
    }

    public static boolean has(Raid Current, UUID Raider) {
        return of(Current, Raider) != null;
    }

    @Nullable
    public static HostileTrophy take(Raid Current, UUID Raider) {
        ListTag Saved = list(Current, HaulKey);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag One = Saved.getCompound(Index);
            if (Raider.toString().equals(One.getString("Raider"))) {
                Saved.remove(Index);
                store(Current, HaulKey, Saved);
                return HostileTrophy.load(One.getCompound("Trophy"));
            }
        }
        return null;
    }

    public static Map<UUID, HostileTrophy> all(Raid Current) {
        Map<UUID, HostileTrophy> Found = new LinkedHashMap<>();
        ListTag Saved = list(Current, HaulKey);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag One = Saved.getCompound(Index);
            UUID Raider = Raid.uuid(One.getString("Raider"));
            HostileTrophy Trophy = HostileTrophy.load(One.getCompound("Trophy"));
            if (Raider != null && Trophy != null) {
                Found.put(Raider, Trophy);
            }
        }
        return Found;
    }

    public static int count(Raid Current) {
        return list(Current, HaulKey).size();
    }

    public static void clear(Raid Current) {
        Current.extra().remove(HaulKey);
        Current.extra().remove(IssuedKey);
        Current.changed();
    }

    public static void issue(Raid Current, UUID Raider, int EntryId) {
        ListTag Saved = list(Current, IssuedKey);
        CompoundTag One = new CompoundTag();
        One.putString("Raider", Raider.toString());
        One.putInt("Entry", EntryId);
        Saved.add(One);
        store(Current, IssuedKey, Saved);
    }

    public static int issued(Raid Current, UUID Raider) {
        ListTag Saved = list(Current, IssuedKey);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag One = Saved.getCompound(Index);
            if (Raider.toString().equals(One.getString("Raider"))) {
                return One.getInt("Entry");
            }
        }
        return -1;
    }

    public static boolean isIssued(Raid Current, int EntryId) {
        ListTag Saved = list(Current, IssuedKey);
        for (int Index = 0; Index < Saved.size(); Index++) {
            if (Saved.getCompound(Index).getInt("Entry") == EntryId) {
                return true;
            }
        }
        return false;
    }

    public static int issuedCount(Raid Current) {
        return list(Current, IssuedKey).size();
    }

    public static void unissue(Raid Current, UUID Raider) {
        ListTag Saved = list(Current, IssuedKey);
        boolean Changed = false;
        for (int Index = Saved.size() - 1; Index >= 0; Index--) {
            if (Raider.toString().equals(Saved.getCompound(Index).getString("Raider"))) {
                Saved.remove(Index);
                Changed = true;
            }
        }
        if (Changed) {
            store(Current, IssuedKey, Saved);
        }
    }
}
