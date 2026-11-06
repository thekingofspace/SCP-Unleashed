package net.rai.scp_unleashed.scp4845.staff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp4845Lines {
    public static final String Greet = "greet";
    public static final String Counter = "counter";
    public static final String Kitchen = "kitchen";
    public static final String Manager = "manager";
    public static final String Hurt = "hurt";
    public static final String Drifter = "drifter";
    public static final String Closed = "closed";
    public static final Map<String, Integer> Counts = Map.of(Greet, 5, Counter, 8, Kitchen, 10, Manager, 8, Hurt, 5, Drifter, 6, Closed, 3);
    public static final long Cooldown = 30L;
    private static final Map<UUID, Long> Spoken = new ConcurrentHashMap<>();

    private Scp4845Lines() {
    }

    public static String categoryOf(Scp4845Employee Speaker) {
        if (!Speaker.isBound()) {
            return Drifter;
        }
        return switch (Speaker.role()) {
            case Scp4845Employee.RoleCounter -> Counter;
            case Scp4845Employee.RoleManager -> Manager;
            default -> Kitchen;
        };
    }

    public static Component line(String Category, int Index) {
        return Component.translatable("message.scp_unleashed.scp4845.line." + Category + "." + Index);
    }

    public static boolean say(Scp4845Employee Speaker, ServerPlayer Listener, String Category) {
        long Now = Speaker.level().getGameTime();
        Long Last = Spoken.get(Speaker.getUUID());
        if (Last != null && Now >= Last && Now - Last < Cooldown) {
            return false;
        }
        Spoken.put(Speaker.getUUID(), Now);
        int Count = Counts.getOrDefault(Category, 1);
        Component Words = line(Category, Speaker.getRandom().nextInt(Count));
        Component Name = Speaker.speakerName().copy().withStyle(ChatFormatting.LIGHT_PURPLE);
        Listener.sendSystemMessage(Component.translatable("message.scp_unleashed.scp4845.speech", Name, Words).withStyle(ChatFormatting.WHITE));
        return true;
    }

    public static void forget() {
        Spoken.clear();
    }
}
