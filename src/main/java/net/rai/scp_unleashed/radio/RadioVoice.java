package net.rai.scp_unleashed.radio;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RadioVoice {
    public static final String Category = "scp_radio";
    public static final float WeakBelow = 0.25f;
    public static final float WeakestDrop = 0.35f;
    public static final float HauntedDrop = 0.2f;
    private static final Map<UUID, Route> Routes = new ConcurrentHashMap<>();

    private RadioVoice() {
    }

    public record Target(UUID Id, float Strength, boolean Haunted, boolean Corrupt) {
    }

    public record Route(int Channel, List<Target> Targets) {
        public Route {
            Targets = List.copyOf(Targets);
        }
    }

    public static void publish(UUID Speaker, Route Current) {
        Routes.put(Speaker, Current);
    }

    public static void clear(UUID Speaker) {
        Routes.remove(Speaker);
    }

    public static void clearAll() {
        Routes.clear();
    }

    @Nullable
    public static Route route(UUID Speaker) {
        return Routes.get(Speaker);
    }

    public static boolean isKeyed(UUID Speaker) {
        return Routes.containsKey(Speaker);
    }

    public static float dropChance(float Strength, boolean Haunted) {
        float Chance = Strength < WeakBelow ? (WeakBelow - Math.max(0.0f, Strength)) / WeakBelow * WeakestDrop : 0.0f;
        return Haunted ? Chance + HauntedDrop : Chance;
    }

    public static boolean drops(Target Listener, float Roll) {
        return Listener.Corrupt() || Roll < dropChance(Listener.Strength(), Listener.Haunted());
    }
}
