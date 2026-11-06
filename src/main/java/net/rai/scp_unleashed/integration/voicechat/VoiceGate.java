package net.rai.scp_unleashed.integration.voicechat;

import net.neoforged.fml.ModList;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoiceGate {
    public static final String VoicechatId = "voicechat";
    private static final Map<UUID, Set<String>> Reasons = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> Deaf = new ConcurrentHashMap<>();
    private static Boolean Loaded;

    private VoiceGate() {
    }

    public static boolean isLoaded() {
        if (Loaded == null) {
            ModList Mods = ModList.get();
            if (Mods == null) {
                return false;
            }
            Loaded = Mods.isLoaded(VoicechatId);
        }
        return Loaded;
    }

    public static void mark(String Reason, UUID Speaker, boolean Silenced) {
        if (Silenced) {
            Reasons.computeIfAbsent(Speaker, Key -> ConcurrentHashMap.newKeySet()).add(Reason);
            return;
        }
        Set<String> Current = Reasons.get(Speaker);
        if (Current != null) {
            Current.remove(Reason);
            if (Current.isEmpty()) {
                Reasons.remove(Speaker, Current);
            }
        }
    }

    public static void deafen(String Reason, UUID Listener, boolean Deafened) {
        if (Deafened) {
            Deaf.computeIfAbsent(Listener, Key -> ConcurrentHashMap.newKeySet()).add(Reason);
            return;
        }
        Set<String> Current = Deaf.get(Listener);
        if (Current != null) {
            Current.remove(Reason);
            if (Current.isEmpty()) {
                Deaf.remove(Listener, Current);
            }
        }
    }

    public static boolean isDeafened(UUID Listener) {
        Set<String> Current = Deaf.get(Listener);
        return Current != null && !Current.isEmpty();
    }

    public static boolean isSilenced(UUID Speaker) {
        Set<String> Current = Reasons.get(Speaker);
        return Current != null && !Current.isEmpty();
    }

    public static void forget(UUID Speaker) {
        Reasons.remove(Speaker);
        Deaf.remove(Speaker);
    }

    public static void clear() {
        Reasons.clear();
        Deaf.clear();
    }
}
