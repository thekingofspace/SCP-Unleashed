package net.rai.scp_unleashed.computer.net;

import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.computer.ChatServerBlockEntity;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.data.ActionStep;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.discord.DiscordNotices;

import java.util.ArrayList;
import java.util.List;

public final class ChatService {
    public static final String NetworkName = "NETWORK";
    private static long NetworkIds = (System.currentTimeMillis() & 0xFFFFFFFFL) << 8;

    private ChatService() {
    }

    public static List<ChatChannels.Info> channels(ServerLevel World, ComputerGrid.Reach Reach) {
        List<ChatChannels.Info> Found = new ArrayList<>();
        long Now = World.getGameTime();
        ChatServerBlockEntity Server = Reach.chatServer();
        List<ChatMessage> Recent = Server == null ? List.of() : Server.recent(Now);
        Found.add(new ChatChannels.Info(ChatChannels.Network, NetworkName, Server != null, Recent.size(), Recent.isEmpty() ? 0L : Recent.getLast().Id()));
        for (MessageDatabaseBlockEntity Database : Reach.runningDatabases()) {
            Found.add(new ChatChannels.Info(Database.getKey(), Database.getChannelName(), Server != null, Database.count(), Database.lastId()));
        }
        return Found;
    }

    public static List<ChatMessage> history(ServerLevel World, ComputerGrid.Reach Reach, String Key) {
        if (ChatChannels.Network.equals(Key)) {
            ChatServerBlockEntity Server = Reach.chatServer();
            return Server == null ? List.of() : Server.recent(World.getGameTime());
        }
        MessageDatabaseBlockEntity Database = Reach.database(Key);
        return Database == null ? List.of() : Database.messages();
    }

    public static Result post(ServerLevel World, ComputerGrid.Reach Reach, String Key, ChatMessage Draft) {
        if (Reach.chatServer() == null) {
            return Result.NoServer;
        }
        if (ChatChannels.Network.equals(Key)) {
            ChatMessage Stored = Draft.withId(++NetworkIds);
            for (ChatServerBlockEntity Server : Reach.runningChatServers()) {
                Server.buffer(Stored, World.getGameTime());
            }
        } else {
            MessageDatabaseBlockEntity Database = Reach.database(Key);
            if (Database == null) {
                return Result.NoChannel;
            }
            Database.append(Draft);
        }
        ComputerSessions.chatPosted(World, Reach, Key);
        DiscordNotices.computerPosted(World, Reach, Key, Draft);
        return Result.Posted;
    }

    public static int broadcast(ServerLevel World, ComputerGrid.Reach Reach, String Target, ChatMessage Draft) {
        if (ActionStep.AllChannels.equals(Target)) {
            int Posted = post(World, Reach, ChatChannels.Network, Draft) == Result.Posted ? 1 : 0;
            for (MessageDatabaseBlockEntity Database : Reach.runningDatabases()) {
                Posted += post(World, Reach, Database.getKey(), Draft) == Result.Posted ? 1 : 0;
            }
            return Posted;
        }
        return post(World, Reach, Target, Draft) == Result.Posted ? 1 : 0;
    }

    public enum Result {
        Posted,
        NoServer,
        NoChannel
    }
}
