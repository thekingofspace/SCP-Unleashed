package net.rai.scp_unleashed.orbital;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public enum OrbitalPart implements StringRepresentable {
    FocusingArray("focusing_array"),
    CapacitorRing("capacitor_ring"),
    HeatSink("heat_sink"),
    TargetingCore("targeting_core"),
    PowerCore("power_core"),
    EmitterBarrel("emitter_barrel");

    public static final int Count = values().length;
    public static final int FullMask = (1 << Count) - 1;

    private final String Name;

    OrbitalPart(String Name) {
        this.Name = Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public String getItemName() {
        return "orbital_" + Name;
    }

    public String getTranslationKey() {
        return "item.scp_unleashed." + getItemName();
    }

    public int bit() {
        return 1 << ordinal();
    }

    public static Optional<OrbitalPart> byName(String Wanted) {
        for (OrbitalPart Part : values()) {
            if (Part.Name.equals(Wanted) || Part.getItemName().equals(Wanted)) {
                return Optional.of(Part);
            }
        }
        return Optional.empty();
    }

    public static Optional<OrbitalPart> of(ItemStack Stack) {
        return Stack.getItem() instanceof OrbitalPartItem Part ? Optional.of(Part.getPart()) : Optional.empty();
    }
}
