package net.rai.scp_unleashed.computer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.net.ChatService;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ComputerScp079 {
    public static final String Prefix = "chat.scp_unleashed.computer.scp079.";
    public static final int MemeticChance = 3;
    public static final int BaseLines = 10;
    public static final int UpgradedLines = 8;
    public static final int MemeticLines = 4;
    public static final int Names = 4;
    public static final double NameRange = 48.0;

    private ComputerScp079() {
    }

    public static boolean canChat(Collection<WireDevice> Devices) {
        return chatServer(Devices) != null;
    }

    public static int post(ServerLevel World, BlockPos Self, Collection<WireDevice> Devices, boolean Upgraded, RandomSource Random) {
        ChatServerBlockEntity Server = chatServer(Devices);
        if (Server == null) {
            return 0;
        }
        List<String> Keys = new ArrayList<>();
        Keys.add(ChatChannels.Network);
        for (WireDevice Device : Devices) {
            if (Device instanceof MessageDatabaseBlockEntity Database && !Database.isRemoved() && Database.isRunning() && !Keys.contains(Database.getKey())) {
                Keys.add(Database.getKey());
            }
        }
        String Key = Keys.get(Random.nextInt(Keys.size()));
        boolean Memetic = Upgraded && Random.nextInt(MemeticChance) == 0;
        String Line;
        String Argument = "";
        if (Memetic) {
            Line = Prefix + "memetic." + Random.nextInt(MemeticLines);
        } else if (Upgraded) {
            Line = Prefix + "upgraded." + Random.nextInt(UpgradedLines);
            Argument = victimName(World, Self, Random);
        } else {
            Line = Prefix + Random.nextInt(BaseLines);
        }
        ChatMessage Draft = new ChatMessage(0L, ChatMessage.Sender.Anomaly, Prefix + "name." + Random.nextInt(Names), Line, Argument, Optional.empty(),
                World.getDayTime(), Memetic);
        return ChatService.post(World, ComputerGrid.reach(World, Server.getBlockPos()), Key, Draft) == ChatService.Result.Posted ? 1 : 0;
    }

    @Nullable
    private static ChatServerBlockEntity chatServer(Collection<WireDevice> Devices) {
        for (WireDevice Device : Devices) {
            if (Device instanceof ChatServerBlockEntity Server && !Server.isRemoved() && Server.isRunning()) {
                return Server;
            }
        }
        return null;
    }

    private static String victimName(ServerLevel World, BlockPos Self, RandomSource Random) {
        List<ServerPlayer> Near = new ArrayList<>();
        Vec3 Centre = Vec3.atCenterOf(Self);
        for (ServerPlayer Candidate : World.players()) {
            if (!Candidate.isSpectator() && Candidate.distanceToSqr(Centre) <= NameRange * NameRange) {
                Near.add(Candidate);
            }
        }
        if (Near.isEmpty()) {
            return "OPERATOR";
        }
        return Near.get(Random.nextInt(Near.size())).getGameProfile().getName().toUpperCase(Locale.ROOT);
    }
}
