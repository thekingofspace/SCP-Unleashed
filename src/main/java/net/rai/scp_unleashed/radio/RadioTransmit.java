package net.rai.scp_unleashed.radio;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RadioTransmit {
    public static final int RouteEvery = 5;
    public static final int CrackleEvery = 40;
    public static final float CrackleVolume = 0.18f;
    public static final int StaticEvery = 12;
    public static final float StaticVolume = 0.5f;
    private static final Map<UUID, Session> Sessions = new HashMap<>();

    private RadioTransmit() {
    }

    private static final class Session {
        private final int Channel;
        private int Age;
        private List<RadioRelay.Hearer> Last = List.of();

        private Session(int Channel) {
            this.Channel = Channel;
        }
    }

    public static void tick(MinecraftServer Server) {
        Set<UUID> Seen = new HashSet<>();
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            Seen.add(User.getUUID());
            ItemStack Keyed = RadioCarry.keyed(User);
            Session Current = Sessions.get(User.getUUID());
            if (Keyed.isEmpty()) {
                if (Current != null) {
                    end(User, Current);
                }
                continue;
            }
            int Channel = RadioCarry.channel(Keyed);
            if (Current != null && Current.Channel != Channel) {
                end(User, Current);
                Current = null;
            }
            if (Current == null) {
                Current = begin(User, Channel);
            }
            Current.Age++;
            if (Current.Age % RouteEvery == 0) {
                refresh(User, Current);
            }
            if (Current.Age % CrackleEvery == 0 && VoiceGate.isLoaded()) {
                RadioSignal.playTo(Current.Last, List.of(RadioRegistry.Crackle), CrackleVolume, 1.0f);
            }
            if (Current.Age % StaticEvery == 0) {
                long Now = User.serverLevel().getGameTime();
                for (RadioRelay.Hearer Each : Current.Last) {
                    if (RadioBleed.isCorrupt(Each.Listener().getUUID(), Now)) {
                        RadioSignal.playTo(Each.Listener(), RadioRegistry.StaticLayers, StaticVolume, 0.8f + Each.Listener().getRandom().nextFloat() * 0.4f);
                    }
                }
            }
        }
        Sessions.keySet().removeIf(Id -> {
            if (!Seen.contains(Id)) {
                RadioVoice.clear(Id);
                return true;
            }
            return false;
        });
    }

    private static Session begin(ServerPlayer User, int Channel) {
        Session Made = new Session(Channel);
        Sessions.put(User.getUUID(), Made);
        refresh(User, Made);
        RadioSignal.playTo(Made.Last, RadioRegistry.KeyLayers, 0.5f, 1.25f);
        RadioSignal.playTo(User, List.of(RadioRegistry.Click), 0.35f, 1.4f);
        if (!VoiceGate.isLoaded()) {
            User.displayClientMessage(Component.translatable("radio.scp_unleashed.no_voice", VoiceConfig.chatPrefix()).withStyle(ChatFormatting.GRAY), true);
        } else {
            User.displayClientMessage(Component.translatable("radio.scp_unleashed.transmitting", RadioCarry.label(Channel), Made.Last.size()).withStyle(ChatFormatting.GREEN), true);
        }
        return Made;
    }

    private static void refresh(ServerPlayer User, Session Current) {
        Current.Last = RadioRelay.hearers(User, Current.Channel);
        long Now = User.serverLevel().getGameTime();
        List<RadioVoice.Target> Targets = new ArrayList<>(Current.Last.size());
        for (RadioRelay.Hearer Each : Current.Last) {
            UUID Id = Each.Listener().getUUID();
            Targets.add(new RadioVoice.Target(Id, (float) Each.Strength(), RadioBleed.isHaunted(Id, Now), RadioBleed.isCorrupt(Id, Now)));
        }
        RadioVoice.publish(User.getUUID(), new RadioVoice.Route(Current.Channel, Targets));
    }

    private static void end(ServerPlayer User, Session Current) {
        Sessions.remove(User.getUUID());
        RadioVoice.clear(User.getUUID());
        RadioSignal.playTo(RadioRelay.hearers(User, Current.Channel), RadioRegistry.UnkeyLayers, 0.45f, 1.0f);
        RadioSignal.playTo(User, List.of(RadioRegistry.Click), 0.3f, 1.1f);
    }

    public static boolean isKeyed(UUID Speaker) {
        return Sessions.containsKey(Speaker);
    }

    public static void forget(UUID Speaker) {
        Sessions.remove(Speaker);
        RadioVoice.clear(Speaker);
    }

    public static void clear() {
        Sessions.clear();
        RadioVoice.clearAll();
    }
}
