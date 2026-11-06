package net.rai.scp_unleashed.terminal;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.arrival.ArrivalCards;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TerminalDimensions {
    public static final String Prefix = "terminal.scp_unleashed.dimension.";

    private TerminalDimensions() {
    }

    public static List<String> candidates(ResourceLocation Id) {
        List<String> Keys = new ArrayList<>();
        if (Id == null) {
            return Keys;
        }
        if (Scp_unleashed.ModId.equals(Id.getNamespace())) {
            Keys.add(ArrivalCards.Prefix + Id.getPath() + ".title");
        }
        Keys.add(Prefix + Id.getNamespace() + "." + Id.getPath());
        return Keys;
    }

    public static String name(ResourceLocation Id, Predicate<String> Exists, Function<String, String> Translate) {
        if (Id == null) {
            return "?";
        }
        for (String Key : candidates(Id)) {
            if (Exists.test(Key)) {
                return Translate.apply(Key);
            }
        }
        return Id.toString();
    }
}
