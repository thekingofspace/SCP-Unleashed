package net.rai.scp_unleashed.scp024;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp024.network.Scp024QuipPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class Scp024Quips {
    public static final int Ticks = 70;
    public static final Map<String, Integer> Variants = Map.ofEntries(
            Map.entry("near", 6),
            Map.entry("hurt", 4),
            Map.entry("time", 3),
            Map.entry("wrong", 4),
            Map.entry("zonk", 3),
            Map.entry("mine", 3),
            Map.entry("blast", 3),
            Map.entry("eliminated", 4),
            Map.entry("resume", 2),
            Map.entry("go", 4),
            Map.entry("lucky", 3),
            Map.entry("one_way", 1),
            Map.entry("no_room", 1),
            Map.entry("rules", 3),
            Map.entry("back_to_start", 2),
            Map.entry("catch", 2),
            Map.entry("correct", 3),
            Map.entry("timeout", 2));

    private Scp024Quips() {
    }

    public static Component line(String Key, RandomSource Random) {
        int Count = Variants.getOrDefault(Key, 1);
        return Component.translatable("quip.scp_unleashed.scp024." + Key + "." + Random.nextInt(Count));
    }

    public static void send(@Nullable ServerPlayer Listener, Component Text) {
        if (Listener != null) {
            PacketDistributor.sendToPlayer(Listener, new Scp024QuipPayload(Text, Ticks));
        }
    }

    public static void say(@Nullable ServerPlayer Listener, String Key) {
        if (Listener != null) {
            send(Listener, line(Key, Listener.getRandom()));
        }
    }
}
