package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.scp4666.block.Scp4666BoxBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666BoxBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666CalendarBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666GramophoneBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666LampBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666OwnedBlockEntity;
import net.rai.scp_unleashed.scp4666.block.Scp4666StockingBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666TreeBlock;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;
import net.rai.scp_unleashed.scp4666.network.Scp4666SyncPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp4666Director {
    public static final int SyncEvery = 20;
    public static final int MinEventDelay = 160;
    public static final int MaxEventDelay = 5800;
    public static final int RetryDelay = 200;
    public static final int RetryLimit = 8;
    public static final long WokenClock = 19200L;
    public static final float MidnightWakeChance = 0.55f;
    public static final float CellarAjarChance = 0.3f;
    public static final float BaseLuck = 0.15f;
    public static final float CocoaLuck = 0.30f;
    public static final float TreeLuck = 0.25f;
    public static final float StarePenalty = 0.07f;
    public static final float SightingPenalty = 0.03f;
    public static final float OutsidePenalty = 0.06f;
    public static final float LoudPenalty = 0.04f;
    public static final float ColdPenalty = 0.05f;
    public static final float EventBoost = 1.0f;
    public static final float EventCap = 0.95f;
    public static final float DelayCut = 0.35f;
    public static final float WakeBoost = 0.30f;
    public static final float WideWake = 50.0f;
    public static final int WakeFrom = 8;
    public static final int WakeTo = 10;
    public static final float LuckFloor = 0.05f;
    public static final float LuckCeiling = 0.9f;
    public static final String Prefix = "message.scp_unleashed.scp4666.";
    private static final Map<MinecraftServer, Integer> RolledNight = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Integer> RolledMorning = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, Integer> Empty = new ConcurrentHashMap<>();
    private static final Map<MinecraftServer, double[]> Carry = new ConcurrentHashMap<>();
    private static final List<Plan> Plans = new ArrayList<>();

    public record Plan(String Key, boolean Audio, int From, int To, float Chance, int Times, Trigger Action) {
    }

    @FunctionalInterface
    public interface Trigger {
        boolean run(ServerLevel World, Scp4666Map Layout, RandomSource Random);
    }

    static {
        plan("chimes", true, 1, 3, 0.6f, 1, Scp4666Happenings::chimes);
        plan("whistle", true, 2, 6, 0.35f, 1, Scp4666Happenings::whistle);
        plan("whistle_late", true, 7, 14, 0.7f, 1, Scp4666Happenings::whistle);
        plan("watcher", false, 4, 6, 0.65f, 1, Scp4666Happenings::watcher);
        plan("watcher_late", false, 7, 13, 0.9f, 1, Scp4666Happenings::watcher);
        plan("watcher_again", false, 10, 13, 0.4f, 1, Scp4666Happenings::watcher);
        plan("roof", true, 5, 7, 0.65f, 1, Scp4666Happenings::roof);
        plan("roof_late", true, 11, 14, 0.35f, 1, Scp4666Happenings::roof);
        plan("gramophone", true, 7, 14, 0.4f, 1, Scp4666Happenings::gramophone);
        plan("breath_glass", false, 8, 14, 0.4f, 1, Scp4666Happenings::breathOnGlass);
        plan("attic", false, 9, 14, 0.35f, 1, Scp4666Happenings::attic);
        plan("stairs", true, 10, 14, 0.45f, 1, Scp4666Happenings::stairs);
        plan("snow_inside", false, 11, 14, 0.4f, 1, Scp4666Happenings::snowInside);
        plan("hum", true, 12, 14, 0.5f, 1, Scp4666Happenings::hum);
        plan("knock", true, 13, 13, 0.9f, 2, Scp4666Happenings::knock);
        plan("knock_early", true, 11, 12, 0.25f, 1, Scp4666Happenings::knock);
        plan("knock_final", true, 14, 14, 0.5f, 1, Scp4666Happenings::knock);
        plan("basement", true, 13, 13, 0.85f, 1, Scp4666Happenings::basementBreath);
        plan("sniff", true, 13, 14, 0.6f, 1, Scp4666Happenings::sniff);
        plan("tree_out", false, 13, 14, 0.6f, 1, Scp4666Happenings::treeOut);
        plan("tree_out_early", false, 11, 12, 0.2f, 1, Scp4666Happenings::treeOut);
    }

    private Scp4666Director() {
    }

    private static void plan(String Key, boolean Audio, int From, int To, float Chance, int Times, Trigger Action) {
        Plans.add(new Plan(Key, Audio, From, To, Chance, Times, Action));
    }

    public static List<Plan> plans() {
        return List.copyOf(Plans);
    }

    public static void tick(MinecraftServer Server) {
        ServerLevel World = Scp4666World.level(Server);
        if (World == null) {
            return;
        }
        Scp4666Builder.tick(World);
        Scp4666Builder.thawTick(World);
        Scp4666Cues.tick(World);
        Scp4666Pose.tick(Server);
        Scp4666State State = Scp4666State.get(Server);
        List<ServerPlayer> Inside = Scp4666World.visitors(World);
        Scp4666Clock.bind(Scp4666World.standingIn() && Inside.isEmpty() && !watched(World) ? null : World);
        Scp4666Clock.set(State.clock());
        if (Inside.isEmpty()) {
            checkEmpty(Server, World, State);
            if (Server.getTickCount() % SyncEvery == 0) {
                sync(World, State);
            }
            return;
        }
        Empty.remove(Server);
        if (State.built() != Scp4666World.Signature && !Scp4666Builder.pending(Server)) {
            Scp4666World.upgrade(World);
        }
        Scp4666Map Layout = Scp4666Map.get(Server);
        for (ServerPlayer User : Inside) {
            Scp4666Boundary.check(World, Layout, User);
        }
        Scp4666Endings.tick(World, Layout, State);
        Scp4666Cold.tick(World, Layout, State);
        List<ServerPlayer> Living = Scp4666World.living(World);
        if (!Living.isEmpty()) {
            float Rate = Scp4666Clock.rate(Server);
            double[] Owed = Carry.computeIfAbsent(Server, Key -> new double[2]);
            Scp4666Heat.tick(World, Layout, State, whole(Owed, 1, Rate));
            Scp4666Chores.tick(World, Layout, State);
            advanceClock(World, Layout, State, whole(Owed, 0, Rate));
            if (State.phase() == Scp4666State.PhaseMidnight || State.phase() == Scp4666State.PhaseWoken) {
                if (allAsleep(Scp4666Cold.sleepers(Living, State))) {
                    resolveSleep(World, Layout, State, Living);
                }
            }
            if (Scp4666Clock.night(State.clock()) && State.phase() != Scp4666State.PhaseOver && State.phase() != Scp4666State.PhaseEnding) {
                rollNight(World, State);
            }
        }
        if (Server.getTickCount() % SyncEvery == 0) {
            sync(World, State);
        }
    }

    private static void checkEmpty(MinecraftServer Server, ServerLevel World, Scp4666State State) {
        if (!State.started() || watched(World)) {
            Empty.remove(Server);
            return;
        }
        int Count = Empty.merge(Server, 1, Integer::sum);
        if (Count < 20) {
            return;
        }
        if (offlineParticipants(Server)) {
            return;
        }
        Empty.remove(Server);
        reset(World);
    }

    public static boolean watched(ServerLevel World) {
        for (ServerPlayer User : World.players()) {
            if (!Scp4666World.standingIn() || Scp4666World.nearSite(World.getServer(), User.blockPosition())) {
                return true;
            }
        }
        return false;
    }

    public static boolean running(ServerLevel World, Scp4666State State) {
        int Phase = State.phase();
        boolean Moving = Phase == Scp4666State.PhaseDay || Phase == Scp4666State.PhaseOver && State.clock() < 6000L;
        return Moving && !Scp4666World.living(World).isEmpty();
    }

    public static int emptyTicks(MinecraftServer Server) {
        return Empty.getOrDefault(Server, 0);
    }

    public static boolean offlineParticipants(MinecraftServer Server) {
        for (GatewayStash.Entry Entry : GatewayStash.get(Server).entries()) {
            GatewayDestination Where = GatewayDestinations.get(Entry.Destination);
            if (Where == null || Where.Dimension() != Scp4666World.key()) {
                continue;
            }
            if (Server.getPlayerList().getPlayer(Entry.Player) == null) {
                return true;
            }
        }
        return false;
    }

    public static void reset(ServerLevel World) {
        MinecraftServer Server = World.getServer();
        Scp4666State State = Scp4666State.get(Server);
        Scp4666Cues.clear(Server);
        RolledNight.remove(Server);
        RolledMorning.remove(Server);
        Carry.remove(Server);
        Scp4666Endings.clear(Server);
        Scp4666Cold.clear(Server);
        Scp4666Chores.clear(Server);
        State.reset();
        Scp4666Builder.forget(Server);
        Scp4666Builder.schedule(World);
    }

    public static int whole(double[] Owed, int Slot, float Rate) {
        Owed[Slot] += Rate;
        int Whole = (int) Math.floor(Owed[Slot]);
        Owed[Slot] -= Whole;
        return Whole;
    }

    private static void advanceClock(ServerLevel World, Scp4666Map Layout, Scp4666State State, int Step) {
        if (Step <= 0) {
            return;
        }
        int Phase = State.phase();
        long Clock = State.clock();
        if (Phase == Scp4666State.PhaseDay) {
            long Next = Clock + Step;
            if (Clock < Scp4666Clock.Dusk && Next >= Scp4666Clock.Dusk) {
                State.setClock(Next);
                dusk(World, Layout, State);
                return;
            }
            if (Next >= Scp4666Clock.Midnight) {
                State.setClock(Scp4666Clock.Midnight);
                State.setPhase(Scp4666State.PhaseMidnight);
                midnight(World, Layout, State);
                sync(World, State);
                return;
            }
            State.setClock(Next);
        } else if (Phase == Scp4666State.PhaseOver) {
            if (Clock < 6000L) {
                State.setClock(Math.min(6000L, Clock + Step));
            }
        }
    }

    public static long hasten(ServerLevel World, Scp4666Map Layout, Scp4666State State, long Ticks) {
        if (State.phase() != Scp4666State.PhaseDay || Ticks <= 0) {
            return 0L;
        }
        long Clock = State.clock();
        long Next = Math.min(Clock + Ticks, Scp4666Clock.Midnight);
        if (Clock < Scp4666Clock.Dusk && Next >= Scp4666Clock.Dusk) {
            State.setClock(Math.min(Next, Scp4666Clock.Midnight - 1L));
            dusk(World, Layout, State);
        }
        if (Next >= Scp4666Clock.Midnight) {
            State.setClock(Scp4666Clock.Midnight);
            State.setPhase(Scp4666State.PhaseMidnight);
            midnight(World, Layout, State);
        } else {
            State.setClock(Next);
        }
        sync(World, State);
        return Next - Clock;
    }

    public static float wakeChance(Scp4666State State) {
        return Math.min(1.0f, MidnightWakeChance + WakeBoost * State.activity() / 100.0f);
    }

    public static boolean wakeNight(Scp4666State State) {
        int Night = State.night();
        boolean Wide = State.activity() >= WideWake;
        return Night >= WakeFrom - (Wide ? 1 : 0) && Night <= WakeTo + (Wide ? 1 : 0);
    }

    public static float eventChance(float Base, Scp4666State State) {
        return eventChance(Base, State.activity());
    }

    public static float eventChance(float Base, float Activity) {
        return Math.max(Base, Math.min(EventCap, Base * (1.0f + EventBoost * Activity / 100.0f)));
    }

    public static boolean allAsleep(List<ServerPlayer> Living) {
        if (Living.isEmpty()) {
            return false;
        }
        for (ServerPlayer User : Living) {
            if (!User.isSleepingLongEnough()) {
                return false;
            }
        }
        return true;
    }

    public static void resolveSleep(ServerLevel World, Scp4666Map Layout, Scp4666State State, List<ServerPlayer> Living) {
        int Night = State.night();
        if (Night >= Scp4666State.FinalNight) {
            wakeAll(Living);
            Scp4666Endings.begin(World, Layout, State, Living);
            return;
        }
        if (wakeNight(State) && State.phase() == Scp4666State.PhaseMidnight && !State.wokeTonight() && World.getRandom().nextFloat() < wakeChance(State)) {
            midnightWake(World, Layout, State, Living);
            return;
        }
        wakeAll(Living);
        morning(World, Layout, State);
    }

    public static void wakeAll(List<ServerPlayer> Living) {
        for (ServerPlayer User : Living) {
            if (User.isSleeping()) {
                User.stopSleepInBed(false, true);
            }
        }
    }

    public static void midnightWake(ServerLevel World, Scp4666Map Layout, Scp4666State State, List<ServerPlayer> Living) {
        State.setWoke(true);
        wakeAll(Living);
        State.setPhase(Scp4666State.PhaseWoken);
        State.setClock(WokenClock);
        for (ServerPlayer User : Living) {
            PacketDistributor.sendToPlayer(User, new Scp4666FxPayload(Scp4666FxPayload.Wake, User.getX(), User.getY(), User.getZ(), 0));
            World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp4666Registry.Wake.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        Scp4666Cues.after(World, 50, Level -> Scp4666Happenings.windowVisit(Level, Layout, Level.getRandom()));
        sync(World, State);
    }

    public static void dusk(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        State.newNight();
        Scp4666BoxBlock.clear(World, Layout);
        restorePrints(World, State);
        RolledNight.remove(World.getServer());
        rollNight(World, State);
        World.playSound(null, Layout.Centre.getX() + 0.5, Layout.Centre.getY() + 6.0, Layout.Centre.getZ() + 0.5, Scp4666Registry.WindHowl.get(), SoundSource.AMBIENT, 2.0f, 0.7f);
    }

    public static void rollNight(ServerLevel World, Scp4666State State) {
        MinecraftServer Server = World.getServer();
        Integer Done = RolledNight.get(Server);
        if (Done != null && Done == State.night() * 100 + State.epoch() % 100) {
            return;
        }
        RolledNight.put(Server, State.night() * 100 + State.epoch() % 100);
        RandomSource Random = World.getRandom();
        int Night = State.night();
        float Shorter = 1.0f - DelayCut * State.activity() / 100.0f;
        float Rate = Scp4666Clock.rate(Server);
        float Audio = Scp4666Registry.audioShare(Server);
        for (Plan Entry : Plans) {
            if (Night < Entry.From() || Night > Entry.To()) {
                continue;
            }
            for (int Time = 0; Time < Entry.Times(); Time++) {
                if (Random.nextFloat() >= chanceOf(Entry, State.activity(), Audio)) {
                    continue;
                }
                int Delay = (int) ((MinEventDelay + Random.nextInt(MaxEventDelay - MinEventDelay) * Shorter) / Rate);
                schedule(World, Entry, Delay, 0);
            }
        }
    }

    public static float chanceOf(Plan Entry, float Activity, float Audio) {
        return eventChance(Entry.Audio() ? Entry.Chance() * Audio : Entry.Chance(), Activity);
    }

    public static float expectedEvents(int Night, float Activity, float Audio, boolean AudioOnly) {
        float Total = 0.0f;
        for (Plan Entry : Plans) {
            if (Night >= Entry.From() && Night <= Entry.To() && (!AudioOnly || Entry.Audio())) {
                Total += chanceOf(Entry, Activity, Audio) * Entry.Times();
            }
        }
        return Total;
    }

    public static void stirred(ServerLevel World, Plan Entry, int Delay) {
        if (Entry.Audio() && World.getRandom().nextFloat() >= Scp4666Registry.audioShare(World.getServer())) {
            return;
        }
        schedule(World, Entry, (int) (Delay / Scp4666Clock.rate(World.getServer())), 0);
    }

    private static void schedule(ServerLevel World, Plan Entry, int Delay, int Tries) {
        int Epoch = Scp4666State.get(World.getServer()).epoch();
        int Night = Scp4666State.get(World.getServer()).night();
        Scp4666Cues.after(World, Delay, Level -> {
            Scp4666State State = Scp4666State.get(Level.getServer());
            if (State.epoch() != Epoch || State.night() != Night || State.phase() == Scp4666State.PhaseEnding || State.phase() == Scp4666State.PhaseOver) {
                return;
            }
            List<ServerPlayer> Awake = awake(Level);
            boolean Ran = !Awake.isEmpty() && Entry.Action().run(Level, Scp4666Map.get(Level.getServer()), Level.getRandom());
            if (Ran) {
                State.fire(Entry.Key());
            } else if (Tries < RetryLimit) {
                schedule(Level, Entry, RetryDelay, Tries + 1);
            }
        });
    }

    public static List<ServerPlayer> awake(ServerLevel World) {
        List<ServerPlayer> Found = new ArrayList<>();
        for (ServerPlayer User : Scp4666World.living(World)) {
            if (!User.isSleeping() && !Scp4666Endings.held(User)) {
                Found.add(User);
            }
        }
        return Found;
    }

    public static void midnight(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        Scp4666Chores.takeOffering(World, Layout, State);
        boolean Loud = State.activity() >= Scp4666Chores.LoudLevel;
        boolean Frozen = Scp4666Heat.dead(State);
        for (ServerPlayer User : Scp4666World.living(World)) {
            Scp4666State.Visitor Entry = State.visitor(User.getUUID());
            Entry.Nights++;
            if (Loud) {
                Entry.LoudNights++;
            }
            if (Frozen) {
                Entry.ColdNights++;
            }
            if (State.tree() >= Scp4666State.TreeFull) {
                Entry.TreeNights++;
            }
            if (!Layout.insideCabin(User.position())) {
                Entry.OutsideNights++;
            }
        }
        State.touch();
        World.playSound(null, Layout.Centre.getX() + 0.5, Layout.Floor + 2.0, Layout.Centre.getZ() + 0.5, Scp4666Registry.Creak.get(), SoundSource.AMBIENT, 0.9f, 0.6f);
        Scp4666Cold.assign(World, Layout, State);
    }

    public static void morning(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        int Before = State.night();
        State.clearCold();
        State.setNight(Before + 1);
        State.setPhase(Scp4666State.PhaseDay);
        State.setClock(Scp4666Clock.Dawn);
        State.newNight();
        int Day = State.night();
        for (ServerPlayer User : Scp4666World.living(World)) {
            State.visitor(User.getUUID()).Mornings++;
        }
        State.touch();
        Scp4666Heat.overnight(World, Layout, State);
        Scp4666Builder.queueAll(World);
        Scp4666Chores.morning(World, Layout, State, Before, World.getRandom());
        Scp4666BoxBlock.deliver(World, Layout, Day >= Scp4666State.FoodStops ? 0 : 1, World.getRandom().nextLong());
        Scp4666CalendarBlock.mark(World, Layout, Day);
        if (Day >= Scp4666State.LightsDie) {
            refreshLamps(World, Layout, State);
            if (Day == Scp4666State.LightsDie) {
                World.playSound(null, Layout.Centre.getX() + 0.5, Layout.Floor + 2.0, Layout.Centre.getZ() + 0.5, Scp4666Registry.LightsDie.get(), SoundSource.BLOCKS, 1.2f, 0.8f);
            }
        }
        RandomSource Random = World.getRandom();
        if (Before >= 6 && Random.nextFloat() < 0.5f) {
            Scp4666Happenings.prints(World, Layout, State, Random);
        }
        if (Before >= 9 && Random.nextFloat() < 0.5f) {
            Scp4666Happenings.doorAjar(World, Layout);
        }
        if (Before >= 10 && Random.nextFloat() < CellarAjarChance) {
            Scp4666Happenings.cellarAjar(World, Layout);
        }
        if (Day == 11 && !State.fired("once:stocking")) {
            State.fire("once:stocking");
            Scp4666StockingBlock.hang(World, Layout, Scp4666World.living(World));
        }
        sync(World, State);
    }

    public static void restorePrints(ServerLevel World, Scp4666State State) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        for (BlockPos Pos : State.prints()) {
            if (World.getBlockState(Pos).is(Scp4666Registry.Footprints.get())) {
                int Layers = Math.max(1, Scp4666Builder.designedSnow(World, Layout, Pos));
                World.setBlock(Pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, Layers), Block.UPDATE_CLIENTS);
            }
        }
        State.clearPrints();
    }

    public static void refreshLamps(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        boolean On = State.lightsOn();
        for (BlockPos Pos : Layout.Lamps) {
            BlockState Found = World.getBlockState(Pos);
            if (Found.getBlock() instanceof Scp4666LampBlock && Found.getValue(Scp4666LampBlock.Lit) != On) {
                World.setBlock(Pos, Found.setValue(Scp4666LampBlock.Lit, On), Block.UPDATE_ALL);
            }
        }
    }

    public static void setTree(ServerLevel World, Scp4666Map Layout, Scp4666State State, int Lights) {
        State.setTree(Lights);
        BlockState Found = World.getBlockState(Layout.Tree);
        if (Found.getBlock() instanceof Scp4666TreeBlock) {
            Scp4666TreeBlock.setLights(World, Layout.Tree, Found, Lights);
        }
    }

    public static void joined(ServerPlayer User) {
        if (User.isSpectator()) {
            return;
        }
        ServerLevel World = User.serverLevel();
        Scp4666State State = Scp4666State.get(User.server);
        Scp4666Map Layout = Scp4666Map.get(User.server);
        boolean First = !State.started();
        State.start();
        if (First) {
            Scp4666Builder.queueAll(World);
        }
        Scp4666State.Visitor Entry = State.visitor(User.getUUID());
        Entry.Name = User.getGameProfile().getName();
        markEpoch(User, State.epoch());
        State.touch();
        refreshLamps(World, Layout, State);
        sync(World, State);
        User.displayClientMessage(Component.translatable(Prefix + (First ? "arrive" : "arrive_late"), State.night()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp4666Registry.WindGust.get(), SoundSource.AMBIENT, 1.0f, 0.8f);
    }

    public static void left(ServerPlayer User) {
        Scp4666Endings.release(User);
        Scp4666Chores.strip(User);
    }

    public static void died(ServerPlayer User) {
        Scp4666Endings.release(User);
    }

    public static void drank(ServerPlayer User) {
        Scp4666State State = Scp4666State.get(User.server);
        Scp4666State.Visitor Entry = State.visitor(User.getUUID());
        if (Entry.Cocoas <= Entry.Mornings) {
            Entry.Cocoas++;
            State.touch();
        }
    }

    public static void saw(ServerPlayer User, boolean AtWindow) {
        Scp4666State State = Scp4666State.get(User.server);
        Scp4666State.Visitor Entry = State.visitor(User.getUUID());
        if (AtWindow) {
            Entry.Stares++;
        } else {
            Entry.Sightings++;
        }
        State.touch();
    }

    public static float luck(Scp4666State.Visitor Entry) {
        float Cocoa = Entry.Mornings > 0 ? Math.min(1.0f, (float) Entry.Cocoas / Entry.Mornings) : 0.5f;
        float Tree = Entry.Nights > 0 ? Math.min(1.0f, (float) Entry.TreeNights / Entry.Nights) : 0.5f;
        float Chance = BaseLuck + CocoaLuck * Cocoa + TreeLuck * Tree - StarePenalty * Entry.Stares - SightingPenalty * Entry.Sightings - OutsidePenalty * Entry.OutsideNights
                - LoudPenalty * Entry.LoudNights - ColdPenalty * Entry.ColdNights;
        return Mth.clamp(Chance, LuckFloor, LuckCeiling);
    }

    public static void markEpoch(Player User, int Epoch) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        Persisted.putInt("scp_unleashed_scp4666_epoch", Epoch);
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static int epochOf(Player User) {
        CompoundTag Persisted = User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return Persisted.contains("scp_unleashed_scp4666_epoch") ? Persisted.getInt("scp_unleashed_scp4666_epoch") : -1;
    }

    public static void loggedIn(ServerPlayer User) {
        if (!Scp4666World.inside(User) || !Scp4666World.participant(User)) {
            return;
        }
        Scp4666State State = Scp4666State.get(User.server);
        ServerLevel World = User.serverLevel();
        if (User.isSpectator()) {
            sync(World, State);
            return;
        }
        Scp4666World.ensureBuilt(World);
        if (epochOf(User) != State.epoch()) {
            Scp4666Map Layout = Scp4666Map.get(User.server);
            GatewayTravel.move(User, World, Layout.Spawn, Layout.SpawnYaw);
            joined(User);
            return;
        }
        Scp4666Endings.rejoin(User);
        sync(World, State);
    }

    public static void sync(ServerLevel World, Scp4666State State) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        int Flags = Scp4666SyncPayload.pack(State.phase(), State.tree(), State.lightsDead(), running(World, State), State.started());
        float Rate = Scp4666Clock.rate(World.getServer());
        List<Integer> Bounds = List.of(Layout.CabinMinX, Layout.CabinMinY, Layout.CabinMinZ, Layout.CabinMaxX, Layout.CabinMaxY, Layout.CabinMaxZ);
        for (ServerPlayer User : World.players()) {
            if (Scp4666World.standingIn() && !Scp4666World.nearSite(World.getServer(), User.blockPosition())) {
                continue;
            }
            PacketDistributor.sendToPlayer(User, new Scp4666SyncPayload(State.night(), State.clock(), Rate, Flags, Layout.Centre.getX(), Layout.Centre.getZ(), Bounds,
                    Scp4666Objectives.forPlayer(World, Layout, State, User)));
        }
    }

    public static void sendOut(ServerPlayer User) {
        GatewayDestination Here = GatewayDestinations.inDimension(User.level().dimension());
        if (Here != null) {
            GatewayTravel.leave(User);
        }
    }

    public static Map<String, Object> describe(MinecraftServer Server) {
        Scp4666State State = Scp4666State.get(Server);
        Map<String, Object> Facts = new HashMap<>();
        Facts.put("night", State.night());
        Facts.put("clock", State.clock());
        Facts.put("phase", State.phase());
        Facts.put("epoch", State.epoch());
        Facts.put("visitors", State.visitors().size());
        Facts.put("activity", Math.round(State.activity()));
        Facts.put("fire", State.fire());
        Facts.put("coal", State.coal());
        return Facts;
    }

    public static boolean doorOpen(ServerLevel World, BlockPos Pos) {
        BlockState Found = World.getBlockState(Pos);
        return Found.getBlock() instanceof DoorBlock && Found.getValue(DoorBlock.OPEN);
    }

    public static void setHatch(ServerLevel World, BlockPos Pos, boolean Open) {
        BlockState Found = World.getBlockState(Pos);
        if (Found.getBlock() instanceof TrapDoorBlock && Found.getValue(TrapDoorBlock.OPEN) != Open) {
            World.setBlock(Pos, Found.setValue(TrapDoorBlock.OPEN, Open), Block.UPDATE_ALL);
        }
    }

    public static Vec3 ringPoint(Scp4666Map Layout, double Angle, double Radius) {
        return new Vec3(Layout.Centre.getX() + 0.5 + Math.cos(Angle) * Radius, Layout.Ground + 1.0, Layout.Centre.getZ() + 0.5 + Math.sin(Angle) * Radius);
    }

    public static Direction faceToward(Vec3 From, Vec3 To) {
        return Direction.getNearest(To.x - From.x, 0.0, To.z - From.z);
    }

    public static UUID none() {
        return new UUID(0L, 0L);
    }

    public static void stamp(Scp4666OwnedBlockEntity Entity, ServerPlayer User) {
        Entity.setOwner(User.getUUID(), User.getGameProfile().getName());
    }

    public static void gramophone(ServerLevel World, Scp4666Map Layout, boolean Wrong) {
        if (World.getBlockEntity(Layout.Gramophone) instanceof Scp4666GramophoneBlockEntity Player) {
            Player.start(Wrong);
        }
    }

    public static boolean box(ServerLevel World, Scp4666Map Layout) {
        return World.getBlockEntity(Layout.BoxSpot) instanceof Scp4666BoxBlockEntity;
    }
}
