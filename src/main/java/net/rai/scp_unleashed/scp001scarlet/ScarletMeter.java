package net.rai.scp_unleashed.scp001scarlet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.network.ScarletSyncPayload;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ScarletMeter {
    public static final int SyncEvery = 40;
    public static final String Prefix = "message.scp_unleashed.scp001_scarlet.";
    public static final ResourceKey<Level> Desert = ResourceKey.create(Registries.DIMENSION, Scp001ScarletRegistry.location("scp2317_desert"));
    private static final List<ChainListener> Listeners = new CopyOnWriteArrayList<>();
    private static float LastSent = -1.0f;
    private static int LastBroken = -1;
    private static ScarletState.Phase LastPhase;

    @FunctionalInterface
    public interface ChainListener {
        void snapped(MinecraftServer Server, int Broken);
    }

    private ScarletMeter() {
    }

    public static void listen(ChainListener Listener) {
        Listeners.add(Listener);
    }

    public static boolean climbing(ScarletState.Phase Phase) {
        return Phase == ScarletState.Phase.Stirring;
    }

    public static boolean shown(ScarletState.Phase Phase) {
        return Phase == ScarletState.Phase.Stirring || Phase == ScarletState.Phase.Xk2 || Phase == ScarletState.Phase.Rite || Phase == ScarletState.Phase.Fight;
    }

    public static boolean seen(Player User) {
        return User.hasData(Scp001ScarletRegistry.Seen) && User.getData(Scp001ScarletRegistry.Seen);
    }

    public static void see(ServerPlayer User) {
        if (!seen(User)) {
            User.setData(Scp001ScarletRegistry.Seen, true);
            sync(User);
        }
    }

    public static void read(ServerPlayer Reader) {
        MinecraftServer Server = Reader.server;
        ScarletState State = ScarletState.get(Server);
        State.markRead();
        if (State.phase() == ScarletState.Phase.Asleep) {
            State.phase(ScarletState.Phase.Stirring, Server.overworld().getGameTime());
            ScarletWorld.get(Server).forgetDayTime();
        }
        see(Reader);
        syncAll(Server);
    }

    public static boolean add(MinecraftServer Server, float Amount) {
        ScarletState State = ScarletState.get(Server);
        if (!climbing(State.phase())) {
            return false;
        }
        State.manifestation(State.manifestation() + Amount);
        return true;
    }

    public static boolean riteReady(MinecraftServer Server) {
        long Last = ScarletWorld.get(Server).lastRite();
        return Last == Long.MIN_VALUE || Server.overworld().getGameTime() - Last >= ScarletConfig.rite2317CooldownTicks();
    }

    public static boolean rite(MinecraftServer Server) {
        ScarletState State = ScarletState.get(Server);
        if (!climbing(State.phase()) || !riteReady(Server)) {
            return false;
        }
        State.manifestation(State.manifestation() - ScarletConfig.rite2317Drop());
        ScarletWorld.get(Server).lastRite(Server.overworld().getGameTime());
        syncAll(Server);
        return true;
    }

    public static void tick(MinecraftServer Server) {
        ScarletState State = ScarletState.get(Server);
        ScarletWorld Data = ScarletWorld.get(Server);
        ScarletState.Phase Phase = State.phase();
        if (climbing(Phase)) {
            long Delta = Data.advance(Server.overworld().getDayTime());
            if (Delta > 0L) {
                State.manifestation(State.manifestation() + (float) (ScarletConfig.baselinePerDay() * Delta / 24000.0));
            }
            if (State.manifestation() >= 1.0f) {
                ScarletXk.begin(Server);
            }
        }
        if (State.broken() > Data.told()) {
            for (int Index = Data.told() + 1; Index <= State.broken(); Index++) {
                snap(Server, Index);
            }
            Data.told(State.broken());
        } else if (State.broken() < Data.told()) {
            Data.told(State.broken());
        }
        boolean Changed = Math.abs(State.manifestation() - LastSent) > 0.0005f || State.broken() != LastBroken || State.phase() != LastPhase;
        if (Changed || Server.getTickCount() % SyncEvery == 0) {
            syncAll(Server);
        }
    }

    private static void snap(MinecraftServer Server, int Broken) {
        Component Line = Component.translatable(Prefix + "chain." + Math.min(Broken, ScarletState.Chains)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            User.sendSystemMessage(Line);
            boolean Near = User.level().dimension() == Desert;
            User.playNotifySound(Scp001ScarletRegistry.ChainRumble.get(), SoundSource.AMBIENT, Near ? 0.9f : 0.35f, 0.6f);
            if (Near) {
                User.playNotifySound(Scp001ScarletRegistry.ChainSnap.get(), SoundSource.AMBIENT, 1.0f, 0.7f);
                User.playNotifySound(Scp001ScarletRegistry.ChainRing.get(), SoundSource.AMBIENT, 0.8f, 0.6f);
            }
        }
        for (ChainListener Listener : Listeners) {
            Listener.snapped(Server, Broken);
        }
    }

    public static ScarletSyncPayload payload(MinecraftServer Server, ServerPlayer User) {
        ScarletState State = ScarletState.get(Server);
        ScarletWorld Data = ScarletWorld.get(Server);
        return new ScarletSyncPayload(State.manifestation(), State.broken(), (byte) State.phase().ordinal(), seen(User), Data.sky(), State.thronePlaced(), State.throne());
    }

    public static void sync(ServerPlayer User) {
        MinecraftServer Server = User.getServer();
        if (Server != null) {
            PacketDistributor.sendToPlayer(User, payload(Server, User));
        }
    }

    public static void syncAll(MinecraftServer Server) {
        ScarletState State = ScarletState.get(Server);
        LastSent = State.manifestation();
        LastBroken = State.broken();
        LastPhase = State.phase();
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(User, payload(Server, User));
        }
    }

    public static void forget() {
        LastSent = -1.0f;
        LastBroken = -1;
        LastPhase = null;
    }
}
