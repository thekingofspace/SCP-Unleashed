package net.rai.scp_unleashed.archive.investigation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StartConditions {
    private static final Map<String, MapCodec<? extends StartCondition>> Types = new ConcurrentHashMap<>();

    public static final Codec<StartCondition> ConditionCodec = Codec.STRING.dispatch("type", StartCondition::getTypeName, StartConditions::codecFor);

    static {
        register(SiteCondition.TypeName, SiteCondition.ConditionCodec);
        register(NightfallCondition.TypeName, NightfallCondition.ConditionCodec);
    }

    private StartConditions() {
    }

    public static void register(String Name, MapCodec<? extends StartCondition> ConditionType) {
        if (Types.putIfAbsent(Name, ConditionType) != null) {
            throw new IllegalStateException("Start condition type '" + Name + "' is already registered");
        }
    }

    public static boolean isRegistered(String Name) {
        return Types.containsKey(Name);
    }

    public static Set<String> registeredTypes() {
        return Set.copyOf(Types.keySet());
    }

    private static MapCodec<? extends StartCondition> codecFor(String Name) {
        MapCodec<? extends StartCondition> Known = Types.get(Name);
        return Known != null ? Known : DormantCondition.codecFor(Name);
    }
}
