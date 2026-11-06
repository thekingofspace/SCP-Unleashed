package net.rai.scp_unleashed.scp323;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.control.BodyState;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSession;
import net.rai.scp_unleashed.control.ControlSessions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp323Cycle {
    public static final int CheckInterval = 10;
    public static final int JoinGrace = 60;
    public static final int MissingGrace = 60;
    public static final int AbsorbGrace = 100;
    private static final Map<UUID, Long> NotBefore = new HashMap<>();
    private static final Map<UUID, Integer> Missing = new HashMap<>();
    private static final Map<UUID, Long> Recalled = new HashMap<>();
    private static final List<Pending> Queue = new ArrayList<>();

    private Scp323Cycle() {
    }

    public static void grace(Entity Target) {
        NotBefore.put(Target.getUUID(), Target.level().getGameTime() + JoinGrace);
    }

    public static boolean due(Scp323Bond Bond, long Time) {
        return Bond.horned() && Bond.newNight(Time);
    }

    public static void check(LivingEntity Host) {
        if (!(Host.level() instanceof ServerLevel World)) {
            return;
        }
        Scp323Bond Bond = Scp323Bond.of(Host);
        if (Bond.Stage() <= 0) {
            Missing.remove(Host.getUUID());
            return;
        }
        if (Bond.transformed()) {
            verify(World, Host, Bond);
            return;
        }
        Missing.remove(Host.getUUID());
        Long Wait = NotBefore.get(Host.getUUID());
        if (Wait != null) {
            if (World.getGameTime() < Wait) {
                return;
            }
            NotBefore.remove(Host.getUUID());
        }
        if (Scp323Hosts.canChange(Host) && due(Bond, Scp323Night.time(World))) {
            Scp323Hosts.transform(Host, false);
        }
    }

    private static void verify(ServerLevel World, LivingEntity Host, Scp323Bond Bond) {
        Scp323Beast Beast = Scp323Hosts.beast(World.getServer(), Bond.Creature());
        if (Beast != null && Beast.isAlive()) {
            Missing.remove(Host.getUUID());
            if (!(Host instanceof Player) && Host.isAlive() && (Beast.awaitsBody() || Host.getUUID().equals(Beast.hostBody()) && Beast.hostPlayer() == null)) {
                absorb(Beast, Host);
            }
            return;
        }
        int Gone = Missing.merge(Host.getUUID(), CheckInterval, Integer::sum);
        if (Gone < MissingGrace) {
            return;
        }
        Missing.remove(Host.getUUID());
        Scp323Bond.set(Host, Bond.interrupt(Bond.Kills()));
        if (Host instanceof ServerPlayer User) {
            Scp323Watch.stop(User);
        }
    }

    public static void absorb(Scp323Beast Beast, LivingEntity Body) {
        Beast.adoptBody(Body);
        if (Beast.storedBody() != null) {
            Body.discard();
        }
    }

    public static void joined(ServerLevel World, LivingEntity Body) {
        if (Body instanceof Player || Body instanceof Scp323Beast) {
            return;
        }
        Scp323Bond Bond = Scp323Bond.of(Body);
        if (!Bond.transformed()) {
            return;
        }
        Scp323Beast Beast = Scp323Hosts.beast(World.getServer(), Bond.Creature());
        if (Beast != null && Beast.hostPlayer() != null && Body.getUUID().equals(Beast.hostBody())) {
            ServerPlayer User = Scp323Hosts.player(World.getServer(), Beast.hostPlayer());
            Beast.detachPlayer();
            if (User != null) {
                Scp323Watch.stop(User);
            }
        }
        Queue.add(new Pending(World.dimension(), Body.getUUID(), World.getGameTime()));
    }

    public static void absorbQueued(MinecraftServer Server) {
        if (Queue.isEmpty()) {
            return;
        }
        Iterator<Pending> Each = Queue.iterator();
        List<Pending> Kept = new ArrayList<>();
        while (Each.hasNext()) {
            Pending Entry = Each.next();
            Each.remove();
            ServerLevel World = Server.getLevel(Entry.Dimension());
            if (World == null || !(World.getEntity(Entry.Id()) instanceof LivingEntity Body) || !Body.isAlive()) {
                continue;
            }
            Scp323Bond Bond = Scp323Bond.of(Body);
            if (!Bond.transformed()) {
                continue;
            }
            Scp323Beast Beast = Scp323Hosts.beast(Server, Bond.Creature());
            if (Beast != null && Beast.isAlive() && Beast.hostPlayer() == null && (Beast.awaitsBody() || Body.getUUID().equals(Beast.hostBody()))) {
                absorb(Beast, Body);
                continue;
            }
            if (Beast == null && World.getGameTime() - Entry.Since() >= AbsorbGrace) {
                Scp323Bond.set(Body, Bond.interrupt(Bond.Kills()));
                continue;
            }
            Kept.add(Entry);
        }
        Queue.addAll(Kept);
    }

    public static void observe(LivingEntity Host) {
        if (!(Host.level() instanceof ServerLevel World)) {
            return;
        }
        if (Scp323Night.isNight(World)) {
            return;
        }
        Scp323Bond Bond = Scp323Bond.of(Host);
        if (Bond.Stage() > 0 && !Bond.Armed()) {
            Host.setData(Scp323Registry.Bond.get(), Bond.arm());
        }
        if (Host instanceof ServerPlayer User) {
            Recalled.remove(User.getUUID());
            armHome(User);
        }
    }

    private static void armHome(ServerPlayer User) {
        if (!ControlSessions.isControlling(User)) {
            return;
        }
        CompoundTag Attachments = ControlSessions.session(User).Home().getCompound(BodyState.AttachmentsKey);
        Scp323Bond Home = Scp323Bond.fromAttachments(Attachments);
        if (Home.Stage() > 0 && !Home.Armed()) {
            store(Attachments, Home.arm());
        }
    }

    private static void store(CompoundTag Attachments, Scp323Bond Value) {
        Scp323Bond.BondCodec.encodeStart(NbtOps.INSTANCE, Value).result().ifPresent(Saved -> Attachments.put(Scp323Registry.Bond.getId().toString(), Saved));
    }

    public static void recall(ServerPlayer User) {
        if (!ControlSessions.isControlling(User) || ControlManager.hasPendingDeath(User)) {
            return;
        }
        ControlSession Session = ControlSessions.session(User);
        CompoundTag Attachments = Session.Home().getCompound(BodyState.AttachmentsKey);
        Scp323Bond Home = Scp323Bond.fromAttachments(Attachments);
        long Time = Scp323Night.time(User.serverLevel());
        if (!Home.horned() || !Scp323Night.isNight(Time)) {
            return;
        }
        long Night = Scp323Night.nightId(Time);
        Long Last = Recalled.get(User.getUUID());
        if (Last != null && Last == Night || !due(Home, Time)) {
            return;
        }
        Recalled.put(User.getUUID(), Night);
        store(Attachments, Home.arm());
        ControlManager.requestRelease(User);
        User.displayClientMessage(Component.translatable(Scp323Possession.Prefix + "recalled").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
    }

    public static void sleep(MinecraftServer Server) {
        ServerLevel Over = Server.overworld();
        long Time = Over.getDayTime();
        if (!Scp323Night.isNight(Time) || !Over.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            return;
        }
        int Others = 0;
        int Sleeping = 0;
        boolean Watcher = false;
        for (ServerPlayer User : Over.players()) {
            if (User.isSpectator()) {
                continue;
            }
            if (Scp323Watch.isWatching(User)) {
                Watcher = true;
                continue;
            }
            Others++;
            if (User.isSleepingLongEnough()) {
                Sleeping++;
            }
        }
        if (!Watcher || Others == 0) {
            return;
        }
        int Percent = Over.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        int Needed = Math.max(1, Mth.ceil(Others * Percent / 100.0f));
        if (Sleeping < Needed) {
            return;
        }
        long Next = Time + Scp323Night.DayLength;
        Over.setDayTime(EventHooks.onSleepFinished(Over, Next - Next % Scp323Night.DayLength, Time));
        for (ServerPlayer User : List.copyOf(Over.players())) {
            if (User.isSleeping()) {
                User.stopSleepInBed(false, false);
            }
        }
        if (Over.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && Over.isRaining()) {
            Over.setWeatherParameters(0, 0, false, false);
        }
    }

    public static void forget(UUID Id) {
        NotBefore.remove(Id);
        Missing.remove(Id);
        Recalled.remove(Id);
    }

    public static void clear() {
        NotBefore.clear();
        Missing.clear();
        Recalled.clear();
        Queue.clear();
    }

    private record Pending(ResourceKey<Level> Dimension, UUID Id, long Since) {
    }
}
