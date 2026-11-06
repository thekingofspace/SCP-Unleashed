package net.rai.scp_unleashed.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MachineUpgrades {
    public static final String SaveKey = "Upgrades";
    public static final float SpeedPerUpgrade = 0.5f;
    public static final float PowerPerUpgrade = 1.0f;

    private final Map<UpgradeType, Integer> Counts = new EnumMap<>(UpgradeType.class);
    private final Set<UpgradeType> Accepted;
    private final Runnable Changed;

    public MachineUpgrades(Runnable Changed, UpgradeType First, UpgradeType... Rest) {
        this.Changed = Changed;
        this.Accepted = EnumSet.of(First, Rest);
    }

    public boolean accepts(UpgradeType Type) {
        return Accepted.contains(Type);
    }

    public int count(UpgradeType Type) {
        return Counts.getOrDefault(Type, 0);
    }

    public boolean install(UpgradeType Type) {
        if (!accepts(Type) || count(Type) >= Type.getLimit()) {
            return false;
        }
        Counts.merge(Type, 1, Integer::sum);
        Changed.run();
        return true;
    }

    public Collection<UpgradeType> list() {
        List<UpgradeType> Installed = new ArrayList<>();
        for (Map.Entry<UpgradeType, Integer> Entry : Counts.entrySet()) {
            for (int Index = 0; Index < Entry.getValue(); Index++) {
                Installed.add(Entry.getKey());
            }
        }
        return Collections.unmodifiableList(Installed);
    }

    public float speedFactor() {
        return 1.0f + SpeedPerUpgrade * count(UpgradeType.Speed);
    }

    public float powerFactor() {
        return 1.0f + PowerPerUpgrade * count(UpgradeType.Speed);
    }

    public int draw(int Base) {
        return Math.max(1, Math.round(Base * powerFactor()));
    }

    public void save(CompoundTag Data) {
        if (Counts.isEmpty()) {
            return;
        }
        ListTag Installed = new ListTag();
        for (UpgradeType Type : list()) {
            Installed.add(StringTag.valueOf(Type.getSerializedName()));
        }
        Data.put(SaveKey, Installed);
    }

    public void load(CompoundTag Data) {
        if (!Data.contains(SaveKey)) {
            return;
        }
        Counts.clear();
        ListTag Installed = Data.getList(SaveKey, Tag.TAG_STRING);
        for (int Index = 0; Index < Installed.size(); Index++) {
            UpgradeType Type = UpgradeType.byName(Installed.getString(Index));
            if (Type != null && accepts(Type) && count(Type) < Type.getLimit()) {
                Counts.merge(Type, 1, Integer::sum);
            }
        }
    }

    public void drop(Level World, BlockPos Pos) {
        for (UpgradeType Type : list()) {
            Containers.dropItemStack(World, Pos.getX(), Pos.getY(), Pos.getZ(), new ItemStack(Type.getItem()));
        }
        Counts.clear();
    }
}
