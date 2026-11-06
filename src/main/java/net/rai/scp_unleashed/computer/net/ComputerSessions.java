package net.rai.scp_unleashed.computer.net;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.network.ComputerNetworking;
import net.rai.scp_unleashed.scp079.Scp079Exposure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ComputerSessions {
    public static final double Reach = 8.0;
    public static final int Refresh = 40;
    public static final int MemeticCycle = 10;
    public static final float MemeticGain = 5.0f;
    public static final int MaxVisible = 16;
    private static final Map<UUID, Session> Sessions = new HashMap<>();

    private ComputerSessions() {
    }

    public static void open(ServerPlayer User, ComputerBlockEntity Computer) {
        Session Opened = new Session(User.getUUID(), User.level().dimension(), Computer.getBlockPos().immutable());
        Opened.LastRefresh = User.level().getGameTime();
        Sessions.put(User.getUUID(), Opened);
    }

    @Nullable
    public static Session of(ServerPlayer User, BlockPos Pos) {
        Session Found = Sessions.get(User.getUUID());
        if (Found == null || !Found.Pos.equals(Pos) || Found.Dimension != User.level().dimension()) {
            return null;
        }
        return Found;
    }

    public static void close(ServerPlayer User) {
        Sessions.remove(User.getUUID());
    }

    public static void closeAll(ServerLevel World, BlockPos Pos, CloseReason Reason) {
        Iterator<Session> Each = Sessions.values().iterator();
        while (Each.hasNext()) {
            Session Open = Each.next();
            if (Open.Dimension != World.dimension() || !Open.Pos.equals(Pos)) {
                continue;
            }
            Each.remove();
            ServerPlayer User = World.getServer().getPlayerList().getPlayer(Open.Player);
            if (User != null) {
                ComputerNetworking.close(User, Pos, Reason);
            }
        }
    }

    public static void touch(ServerLevel World, BlockPos Pos, int Bits) {
        for (Session Open : Sessions.values()) {
            if (Open.Dimension == World.dimension() && Open.Pos.equals(Pos)) {
                Open.Pending |= Bits;
            }
        }
    }

    public static void touchReach(ServerLevel World, ComputerGrid.Reach Network, int Bits) {
        for (Session Open : Sessions.values()) {
            if (Network.reaches(Open.Dimension, Open.Pos)) {
                Open.Pending |= Bits;
            }
        }
    }

    public static void chatPosted(ServerLevel World, ComputerGrid.Reach Network, String Key) {
        for (Session Open : Sessions.values()) {
            if (Network.reaches(Open.Dimension, Open.Pos)) {
                Open.Pending |= ComputerSection.System.bit() | ComputerSection.Chat.bit();
                if (!Key.equals(Open.Channel) || Open.App != ComputerApp.Chat) {
                    Open.Unheard = true;
                }
            }
        }
    }

    public static int openCount(ServerLevel World, BlockPos Pos) {
        int Count = 0;
        for (Session Open : Sessions.values()) {
            if (Open.Dimension == World.dimension() && Open.Pos.equals(Pos)) {
                Count++;
            }
        }
        return Count;
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || Sessions.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        List<UUID> Dropped = new ArrayList<>();
        for (Session Open : List.copyOf(Sessions.values())) {
            if (Open.Dimension != World.dimension()) {
                continue;
            }
            ServerPlayer User = World.getServer().getPlayerList().getPlayer(Open.Player);
            ComputerBlockEntity Computer = World.isLoaded(Open.Pos) && World.getBlockEntity(Open.Pos) instanceof ComputerBlockEntity Found ? Found : null;
            if (User == null || User.level() != World || Computer == null || !Computer.isRunning() || User.isSpectator()
                    || User.distanceToSqr(Vec3.atCenterOf(Open.Pos)) > Reach * Reach) {
                Dropped.add(Open.Player);
                if (User != null) {
                    ComputerNetworking.close(User, Open.Pos, Computer == null ? CloseReason.Removed : !Computer.isRunning() ? CloseReason.Power : CloseReason.Distance);
                }
                continue;
            }
            if (Now - Open.LastRefresh >= Refresh) {
                Open.LastRefresh = Now;
                Open.Pending |= ComputerSection.System.bit() | ComputerSection.ofApp(Open.App).bit();
            }
            if (Open.Pending != 0) {
                int Bits = Open.Pending;
                Open.Pending = 0;
                ComputerNetworking.push(User, Computer, Open, Bits);
            }
            if (Open.Unheard) {
                Open.Unheard = false;
                ComputerNetworking.chime(User, Open.Pos);
            }
            if (Open.App == ComputerApp.Chat && Math.floorMod(Now, MemeticCycle) == 0) {
                expose(World, User, Computer, Open);
            }
        }
        Dropped.forEach(Sessions::remove);
    }

    private static void expose(ServerLevel World, ServerPlayer User, ComputerBlockEntity Computer, Session Open) {
        if (AntiMemetic.isProtected(User) || !Scp079Exposure.isSubject(User)) {
            return;
        }
        List<ChatMessage> Messages = ChatService.history(World, ComputerGrid.reach(World, Computer.getBlockPos()), Open.Channel);
        int End = Messages.size() - Math.max(0, Open.Scroll);
        int Start = Math.max(0, End - Math.max(1, Math.min(MaxVisible, Open.Visible)));
        for (int Index = Math.max(0, Start); Index < End && Index < Messages.size(); Index++) {
            if (Messages.get(Index).Memetic()) {
                Scp079Exposure.set(User, Scp079Exposure.exposure(User) + MemeticGain * MemeticCycle / 20.0f);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Sessions.remove(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Sessions.values().removeIf(Open -> Open.Dimension == World.dimension());
            ComputerGrid.forget(World);
        }
    }

    public enum CloseReason {
        Closed,
        Power,
        Removed,
        Distance
    }

    public static final class Session {
        private final UUID Player;
        private final ResourceKey<Level> Dimension;
        private final BlockPos Pos;
        private ComputerApp App = ComputerApp.Desktop;
        private String Channel = ChatChannels.Network;
        private int Scroll;
        private int Visible = 8;
        private int Pending = ComputerSection.All;
        private long LastRefresh;
        private boolean Unheard;

        private Session(UUID Player, ResourceKey<Level> Dimension, BlockPos Pos) {
            this.Player = Player;
            this.Dimension = Dimension;
            this.Pos = Pos;
        }

        public BlockPos getPos() {
            return Pos;
        }

        public ComputerApp getApp() {
            return App;
        }

        public void setApp(ComputerApp Opened) {
            if (App != Opened) {
                App = Opened;
                Pending |= ComputerSection.ofApp(Opened).bit() | ComputerSection.System.bit();
            }
        }

        public String getChannel() {
            return Channel;
        }

        public void setChannel(String Key) {
            if (!Channel.equals(Key)) {
                Channel = Key;
                Scroll = 0;
                Pending |= ComputerSection.Chat.bit();
            }
        }

        public void setView(int NewScroll, int NewVisible) {
            Scroll = Math.max(0, NewScroll);
            Visible = Math.max(1, Math.min(MaxVisible, NewVisible));
        }

        public void request(int Bits) {
            Pending |= Bits;
        }
    }
}
