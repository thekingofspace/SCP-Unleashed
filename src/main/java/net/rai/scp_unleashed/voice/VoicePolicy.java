package net.rai.scp_unleashed.voice;

import net.rai.scp_unleashed.radio.VoiceConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public final class VoicePolicy {
    public static final String Scp939 = "scp939";
    public static final String Scp001Day = "scp001_day";
    public static final Pattern ValidId = Pattern.compile("[a-z0-9_]{1,40}");
    private static final Map<String, BooleanSupplier> Switches = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> ForcedSwitches = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> Cached = new ConcurrentHashMap<>();
    @Nullable
    private static volatile Boolean ForcedMaster;
    @Nullable
    private static volatile int[] ForcedCaps;
    private static volatile boolean CachedMaster;

    static {
        register(Scp939, VoiceConfig::lures);
        register(Scp001Day, VoiceConfig::dayVoices);
    }

    private VoicePolicy() {
    }

    public static boolean validId(@Nullable String Anomaly) {
        return Anomaly != null && ValidId.matcher(Anomaly).matches();
    }

    public static void register(String Anomaly, BooleanSupplier Switch) {
        if (!validId(Anomaly)) {
            throw new IllegalArgumentException("Bad anomaly voice id: " + Anomaly);
        }
        Switches.put(Anomaly, Switch);
    }

    public static Set<String> registered() {
        return Set.copyOf(Switches.keySet());
    }

    public static boolean master() {
        Boolean Override = ForcedMaster;
        return Override != null ? Override : VoiceConfig.recording();
    }

    public static boolean anomalySwitch(String Anomaly) {
        Boolean Override = ForcedSwitches.get(Anomaly);
        if (Override != null) {
            return Override;
        }
        BooleanSupplier Switch = Switches.get(Anomaly);
        return Switch == null || Switch.getAsBoolean();
    }

    public static boolean allows(String Anomaly) {
        return validId(Anomaly) && master() && anomalySwitch(Anomaly);
    }

    public static void refresh() {
        boolean Master = master();
        CachedMaster = Master;
        for (String Anomaly : Switches.keySet()) {
            Cached.put(Anomaly, Master && anomalySwitch(Anomaly));
        }
    }

    public static boolean masterCached() {
        return CachedMaster;
    }

    public static boolean allowsCached(String Anomaly) {
        Boolean Known = Cached.get(Anomaly);
        return Known != null ? Known : CachedMaster && validId(Anomaly);
    }

    public static int clipsPerAnomaly() {
        int[] Caps = ForcedCaps;
        return Caps != null ? Caps[0] : VoiceConfig.clipsPerAnomaly();
    }

    public static int clipsPerSpeaker() {
        int[] Caps = ForcedCaps;
        return Caps != null ? Caps[1] : VoiceConfig.clipsPerSpeaker();
    }

    public static int maxAgeDays() {
        int[] Caps = ForcedCaps;
        return Caps != null ? Caps[2] : VoiceConfig.maxAgeDays();
    }

    public static long maxAgeMillis() {
        return maxAgeDays() * 86_400_000L;
    }

    public static void forceMaster(@Nullable Boolean Value) {
        ForcedMaster = Value;
        refresh();
    }

    public static void forceSwitch(String Anomaly, @Nullable Boolean Value) {
        if (Value == null) {
            ForcedSwitches.remove(Anomaly);
        } else {
            ForcedSwitches.put(Anomaly, Value);
        }
        refresh();
    }

    public static void forceCaps(@Nullable int[] Caps) {
        ForcedCaps = Caps == null ? null : Caps.clone();
    }

    public static void resetForces() {
        ForcedMaster = null;
        ForcedSwitches.clear();
        ForcedCaps = null;
        refresh();
    }
}
