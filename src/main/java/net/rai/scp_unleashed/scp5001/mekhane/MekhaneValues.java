package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MekhaneValues {
    public static final int Scale = 36;
    @Nullable
    private static Map<Item, Integer> IronCache;
    @Nullable
    private static Map<Item, Integer> ElectronicsCache;

    public enum Kind {
        Iron,
        Electronics
    }

    public record Worth(Kind Sort, int Units) {
    }

    private MekhaneValues() {
    }

    public static void invalidate() {
        IronCache = null;
        ElectronicsCache = null;
    }

    @Nullable
    public static Map.Entry<ResourceLocation, Integer> parseEntry(String Text) {
        int Split = Text.indexOf('=');
        if (Split <= 0 || Split >= Text.length() - 1) {
            return null;
        }
        ResourceLocation Id = ResourceLocation.tryParse(Text.substring(0, Split).trim());
        if (Id == null) {
            return null;
        }
        String Amount = Text.substring(Split + 1).trim();
        double Value;
        try {
            int Slash = Amount.indexOf('/');
            if (Slash > 0) {
                double Top = Double.parseDouble(Amount.substring(0, Slash).trim());
                double Bottom = Double.parseDouble(Amount.substring(Slash + 1).trim());
                if (Bottom <= 0.0) {
                    return null;
                }
                Value = Top / Bottom;
            } else {
                Value = Double.parseDouble(Amount);
            }
        } catch (NumberFormatException Problem) {
            return null;
        }
        int Units = (int) Math.round(Value * Scale);
        if (Units <= 0) {
            return null;
        }
        return new AbstractMap.SimpleImmutableEntry<>(Id, Units);
    }

    private static Map<Item, Integer> build(List<String> Lines) {
        Map<Item, Integer> Made = new HashMap<>();
        for (String Line : Lines) {
            Map.Entry<ResourceLocation, Integer> Entry = parseEntry(Line);
            if (Entry == null) {
                continue;
            }
            BuiltInRegistries.ITEM.getOptional(Entry.getKey()).ifPresent(Found -> Made.put(Found, Entry.getValue()));
        }
        return Made;
    }

    private static Map<Item, Integer> iron() {
        if (IronCache == null) {
            IronCache = build(MekhaneConfig.ironValues());
        }
        return IronCache;
    }

    private static Map<Item, Integer> electronics() {
        if (ElectronicsCache == null) {
            ElectronicsCache = build(MekhaneConfig.electronicsValues());
        }
        return ElectronicsCache;
    }

    @Nullable
    public static Worth worth(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return null;
        }
        Integer Metal = iron().get(Stack.getItem());
        if (Metal != null) {
            return new Worth(Kind.Iron, Metal);
        }
        if (Stack.is(Scp5001MekhaneRegistry.Electronics)) {
            return new Worth(Kind.Electronics, electronics().getOrDefault(Stack.getItem(), Scale));
        }
        return null;
    }

    public static long ironNeed() {
        return (long) MekhaneConfig.ironDemand() * Scale;
    }

    public static long electronicsNeed() {
        return (long) MekhaneConfig.electronicsDemand() * Scale;
    }
}
