package net.rai.scp_unleashed.stranger;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public record StrangerPlan(String Variant, String Dialogue, int Pages, List<StrangerGift> Gifts, int MinDelay, int MaxDelay, int MinDistance, int MaxDistance, int Linger) {
    public static final int MinLinger = 400;

    public StrangerPlan {
        Gifts = List.copyOf(Gifts);
        Pages = Math.max(1, Math.min(16, Pages));
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        MinDistance = Math.max(4, Low);
        MaxDistance = Math.max(MinDistance, High);
        int Earliest = Math.min(MinDelay, MaxDelay);
        int Latest = Math.max(MinDelay, MaxDelay);
        MinDelay = Math.max(0, Earliest);
        MaxDelay = Math.max(MinDelay, Latest);
        Linger = Math.max(MinLinger, Linger);
    }

    public StrangerVariant variant() {
        return StrangerVariants.get(Variant);
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putString("Variant", Variant);
        Saved.putString("Dialogue", Dialogue);
        Saved.putInt("Pages", Pages);
        ListTag Entries = new ListTag();
        for (StrangerGift Gift : Gifts) {
            Entries.add(Gift.save());
        }
        Saved.put("Gifts", Entries);
        Saved.putInt("MinDelay", MinDelay);
        Saved.putInt("MaxDelay", MaxDelay);
        Saved.putInt("MinDistance", MinDistance);
        Saved.putInt("MaxDistance", MaxDistance);
        Saved.putInt("Linger", Linger);
        return Saved;
    }

    public static StrangerPlan load(CompoundTag Saved) {
        List<StrangerGift> Gifts = new ArrayList<>();
        ListTag Entries = Saved.getList("Gifts", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            Gifts.add(StrangerGift.load(Entries.getCompound(Index)));
        }
        return new StrangerPlan(Saved.getString("Variant"), Saved.getString("Dialogue"), Saved.getInt("Pages"), Gifts, Saved.getInt("MinDelay"), Saved.getInt("MaxDelay"),
                Saved.getInt("MinDistance"), Saved.getInt("MaxDistance"), Saved.getInt("Linger"));
    }
}
