package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp4666.block.Scp4666FurnitureBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666WindowBlock;
import net.rai.scp_unleashed.scp4666.item.Scp4666WinterGearItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Scp4666Chores {
    public static final float NightScale = 1.5f;
    public static final int DecayEvery = 300;
    public static final float DecayStep = 1.0f;
    public static final float MorningKeep = 0.5f;
    public static final float StirFloor = 25.0f;
    public static final float StirOdds = 150.0f;
    public static final int StirMin = 200;
    public static final int StirMax = 900;
    public static final float OfferingCalm = 25.0f;
    public static final float LoudLevel = 60.0f;
    public static final int PokeWindow = 8;
    public static final int SwingEvery = 10;
    public static final double Reach = 4.5;
    public static final int FirewoodCarry = 8;
    public static final int BoardsCarry = 4;
    public static final int BoardsPerWindow = 2;
    public static final int CrackFrom = 5;
    public static final float CrackChance = 0.3f;
    public static final float CrackActivity = 0.4f;
    public static final int PryFrom = 12;
    public static final float PryChance = 0.25f;
    public static final float VisitCrack = 0.5f;
    public static final String Prefix = Scp4666Director.Prefix;
    public static final List<String> StirKeys = List.of("whistle", "whistle_late", "watcher", "watcher_late", "roof", "roof_late", "knock_early", "knock");
    private static final Map<UUID, Channel> Channels = new ConcurrentHashMap<>();

    public enum Chore {
        Chop(4.0f, 240, 50),
        Feed(1.0f, 60, 0),
        Shovel(3.0f, 160, 40),
        Stoke(2.0f, 100, 0),
        Saw(3.0f, 160, 40),
        Board(6.0f, 120, 30),
        Wrap(1.0f, 400, 60),
        Offer(1.0f, 40, 0),
        Oats(1.0f, 40, 0),
        Bake(3.0f, 300, 0),
        Kettle(2.0f, 120, 0),
        Music(3.0f, 200, 0);

        public final float Activity;
        public final int Ticks;
        public final int Channel;

        Chore(float Activity, int Ticks, int Channel) {
            this.Activity = Activity;
            this.Ticks = Ticks;
            this.Channel = Channel;
        }
    }

    private static final class Channel {
        final Chore Kind;
        final BlockPos Pos;
        final Consumer<ServerPlayer> Done;
        int Ticks;
        long Poked;

        Channel(Chore Kind, BlockPos Pos, Consumer<ServerPlayer> Done, long Poked) {
            this.Kind = Kind;
            this.Pos = Pos.immutable();
            this.Done = Done;
            this.Poked = Poked;
        }
    }

    private Scp4666Chores() {
    }

    public static boolean active(ServerLevel World, Player User) {
        return Scp4666World.inside(World) && !Scp4666World.exempt(User) && Scp4666World.participant(User) && User.isAlive()
                && Scp4666State.get(World.getServer()).started() && (!Scp4666World.standingIn() || World.getServer() != null && Scp4666World.nearSite(World.getServer(), User.blockPosition()));
    }

    public static boolean night(Scp4666State State) {
        return State.phase() != Scp4666State.PhaseDay || Scp4666Clock.night(State.clock());
    }

    public static float perform(ServerLevel World, ServerPlayer User, Chore Kind) {
        Scp4666State State = Scp4666State.get(World.getServer());
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        boolean Dark = night(State);
        float Added = Kind.Activity * (Dark ? NightScale : 1.0f);
        float Before = State.activity();
        State.setActivity(Before + Added);
        if (Before < LoudLevel && State.activity() >= LoudLevel) {
            say(User, "noticed");
            Vec3 Far = User.position().add((World.getRandom().nextDouble() - 0.5) * 50.0, 3.0, (World.getRandom().nextDouble() - 0.5) * 50.0);
            World.playSound(null, Far.x, Far.y, Far.z, Scp4666Registry.Giggle.get(), SoundSource.HOSTILE, 1.6f, 0.8f);
        }
        long Moved = Scp4666Director.hasten(World, Layout, State, Kind.Ticks);
        if (Moved > 0) {
            Scp4666Heat.burn(World, Layout, State, (int) Moved, (int) Moved);
        }
        if (Dark) {
            stir(World, State, World.getRandom());
        }
        State.fire("chore:" + Kind.name().toLowerCase());
        return Added;
    }

    public static void stir(ServerLevel World, Scp4666State State, RandomSource Random) {
        float Level = State.activity();
        if (Level < StirFloor || State.phase() == Scp4666State.PhaseEnding || State.phase() == Scp4666State.PhaseOver || Random.nextFloat() >= Level / StirOdds) {
            return;
        }
        String Slot = !State.fired("stir_a") ? "stir_a" : !State.fired("stir_b") ? "stir_b" : null;
        if (Slot == null) {
            return;
        }
        List<Scp4666Director.Plan> Pool = new ArrayList<>();
        for (Scp4666Director.Plan Entry : Scp4666Director.plans()) {
            if (StirKeys.contains(Entry.Key()) && State.night() >= Entry.From() && State.night() <= Entry.To()) {
                Pool.add(Entry);
            }
        }
        if (Pool.isEmpty()) {
            return;
        }
        State.fire(Slot);
        Scp4666Director.stirred(World, Pool.get(Random.nextInt(Pool.size())), StirMin + Random.nextInt(StirMax - StirMin));
    }

    public static void tick(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        MinecraftServer Server = World.getServer();
        if (Server.getTickCount() % DecayEvery == 0) {
            State.setActivity(State.activity() - DecayStep);
        }
        long Now = Server.getTickCount();
        for (Map.Entry<UUID, Channel> Entry : new ArrayList<>(Channels.entrySet())) {
            ServerPlayer User = Server.getPlayerList().getPlayer(Entry.getKey());
            Channel Work = Entry.getValue();
            if (User == null || User.level() != World || !User.isAlive() || Now - Work.Poked > PokeWindow
                    || User.distanceToSqr(Vec3.atCenterOf(Work.Pos)) > Reach * Reach || !active(World, User)) {
                cancel(World, Entry.getKey(), Work);
                continue;
            }
            Work.Ticks++;
            if (Work.Ticks % SwingEvery == 1) {
                User.swing(InteractionHand.MAIN_HAND, true);
                beat(World, Work);
            }
            World.destroyBlockProgress(User.getId(), Work.Pos, Math.min(9, Work.Ticks * 10 / Math.max(1, Work.Kind.Channel)));
            if (Work.Ticks >= Work.Kind.Channel) {
                Channels.remove(Entry.getKey());
                World.destroyBlockProgress(User.getId(), Work.Pos, -1);
                Work.Done.accept(User);
            }
        }
    }

    private static void beat(ServerLevel World, Channel Work) {
        Vec3 Spot = Vec3.atCenterOf(Work.Pos);
        RandomSource Random = World.getRandom();
        Supplier<SoundEvent> Sound = switch (Work.Kind) {
            case Chop -> Scp4666Registry.Chop::get;
            case Shovel -> Scp4666Registry.CoalShovel::get;
            case Saw -> Scp4666Registry.Saw::get;
            case Board -> Scp4666Registry.Hammer::get;
            default -> Scp4666Registry.Rummage::get;
        };
        World.playSound(null, Spot.x, Spot.y, Spot.z, Sound.get(), SoundSource.PLAYERS, 0.9f, 0.9f + Random.nextFloat() * 0.2f);
        BlockState Shown = switch (Work.Kind) {
            case Chop, Saw -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case Shovel -> Blocks.COAL_BLOCK.defaultBlockState();
            case Board -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            default -> Blocks.BROWN_WOOL.defaultBlockState();
        };
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Shown), Spot.x, Spot.y + 0.4, Spot.z, 6, 0.25, 0.15, 0.25, 0.05);
    }

    private static void cancel(ServerLevel World, UUID Id, Channel Work) {
        Channels.remove(Id);
        ServerPlayer User = World.getServer().getPlayerList().getPlayer(Id);
        World.destroyBlockProgress(User == null ? Id.hashCode() : User.getId(), Work.Pos, -1);
    }

    public static boolean channel(ServerLevel World, ServerPlayer User, BlockPos Pos, Chore Kind, Consumer<ServerPlayer> Done) {
        long Now = World.getServer().getTickCount();
        Channel Found = Channels.get(User.getUUID());
        if (Found != null && Found.Kind == Kind && Found.Pos.equals(Pos)) {
            Found.Poked = Now;
            return false;
        }
        Channels.put(User.getUUID(), new Channel(Kind, Pos, Done, Now));
        return true;
    }

    public static boolean channelling(Player User) {
        return Channels.containsKey(User.getUUID());
    }

    public static void finish(ServerPlayer User) {
        Channel Work = Channels.remove(User.getUUID());
        if (Work != null) {
            User.serverLevel().destroyBlockProgress(User.getId(), Work.Pos, -1);
            Work.Done.accept(User);
        }
    }

    public static void clear(MinecraftServer Server) {
        Channels.clear();
    }

    public static void say(Player User, String Key, Object... Arguments) {
        User.displayClientMessage(Component.translatable(Prefix + Key, Arguments).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    public static boolean bound(ItemStack Stack) {
        return Stack.getItem() instanceof Scp4666WinterGearItem || Stack.is(Scp4666Registry.Firewood.get()) || Stack.is(Scp4666Registry.Scuttle.get())
                || Stack.is(Scp4666Registry.Boards.get()) || Stack.is(Scp4666Registry.Gift.get()) || Stack.is(Scp4666Registry.Oats.get());
    }

    public static int strip(Player User) {
        int Removed = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            if (bound(User.getInventory().getItem(Slot))) {
                User.getInventory().setItem(Slot, ItemStack.EMPTY);
                Removed++;
            }
        }
        return Removed;
    }

    public static int carried(Player User, Item Kind) {
        return User.getInventory().countItem(Kind);
    }

    public static void give(ServerPlayer User, ItemStack Stack) {
        if (!User.getInventory().add(Stack)) {
            User.drop(Stack, false);
        }
    }

    public static boolean take(Player User, Item Kind, int Count) {
        if (User.getAbilities().instabuild) {
            return true;
        }
        if (carried(User, Kind) < Count) {
            return false;
        }
        int Left = Count;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize() && Left > 0; Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Stack.is(Kind)) {
                int Used = Math.min(Left, Stack.getCount());
                Stack.shrink(Used);
                Left -= Used;
            }
        }
        return true;
    }

    public static boolean chop(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        if (carried(User, Scp4666Registry.Firewood.get()) + 2 > FirewoodCarry) {
            say(User, "arms_full");
            return false;
        }
        if (channel(World, User, Pos, Chore.Chop, Done -> {
            give(Done, new ItemStack(Scp4666Registry.Firewood.get(), 2));
            World.playSound(null, Pos, Scp4666Registry.ChopSplit.get(), SoundSource.PLAYERS, 1.0f, 0.95f);
            perform(World, Done, Chore.Chop);
            say(Done, "chop_done");
        })) {
            say(User, "chop_start");
        }
        return true;
    }

    public static boolean shovel(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        if (carried(User, Scp4666Registry.Scuttle.get()) > 0) {
            say(User, "scuttle_full");
            return false;
        }
        if (channel(World, User, Pos, Chore.Shovel, Done -> {
            give(Done, new ItemStack(Scp4666Registry.Scuttle.get()));
            perform(World, Done, Chore.Shovel);
            say(Done, "shovel_done");
        })) {
            say(User, "shovel_start");
        }
        return true;
    }

    public static boolean saw(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        if (carried(User, Scp4666Registry.Boards.get()) + 2 > BoardsCarry) {
            say(User, "arms_full");
            return false;
        }
        if (channel(World, User, Pos, Chore.Saw, Done -> {
            give(Done, new ItemStack(Scp4666Registry.Boards.get(), 2));
            perform(World, Done, Chore.Saw);
            say(Done, "saw_done");
        })) {
            say(User, "saw_start");
        }
        return true;
    }

    public static boolean rummage(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        Scp4666State State = Scp4666State.get(World.getServer());
        String Key = "gift:" + User.getUUID();
        if (State.daily(Key) || carried(User, Scp4666Registry.Gift.get()) > 0) {
            say(User, "trunk_empty");
            return false;
        }
        if (channel(World, User, Pos, Chore.Wrap, Done -> {
            State.markDaily(Key);
            give(Done, new ItemStack(Scp4666Registry.Gift.get()));
            World.playSound(null, Pos, Scp4666Registry.Wrapping.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
            perform(World, Done, Chore.Wrap);
            say(Done, "wrap_done");
        })) {
            say(User, "wrap_start");
        }
        return true;
    }

    public static boolean oats(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        Scp4666State State = Scp4666State.get(World.getServer());
        String Key = "oats:" + User.getUUID();
        if (State.daily(Key)) {
            say(User, "pantry_bare");
            return false;
        }
        State.markDaily(Key);
        give(User, new ItemStack(Scp4666Registry.Oats.get()));
        World.playSound(null, Pos, Scp4666Registry.Rummage.get(), SoundSource.PLAYERS, 0.6f, 1.2f);
        perform(World, User, Chore.Oats);
        say(User, "pantry_oats");
        return true;
    }

    public static boolean use(ServerLevel World, ServerPlayer User, BlockPos Pos, Scp4666FurnitureBlock.Kind Kind) {
        if (!active(World, User)) {
            return false;
        }
        return switch (Kind) {
            case ChoppingBlock -> chop(World, User, Pos);
            case CoalBin -> shovel(World, User, Pos);
            case Workbench -> saw(World, User, Pos);
            case Trunk -> rummage(World, User, Pos);
            case PantryShelf -> oats(World, User, Pos);
            default -> false;
        };
    }

    public static boolean feedFire(ServerLevel World, ServerPlayer User) {
        Scp4666State State = Scp4666State.get(World.getServer());
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        if (Scp4666Heat.fireFull(State)) {
            say(User, "fire_full");
            return false;
        }
        if (!take(User, Scp4666Registry.Firewood.get(), 1)) {
            return false;
        }
        Scp4666Heat.feedFire(World, Layout, State);
        World.playSound(null, Layout.Fireplace, Scp4666Registry.FireFeed.get(), SoundSource.BLOCKS, 0.9f, 0.9f + World.getRandom().nextFloat() * 0.2f);
        Vec3 Spot = Vec3.atCenterOf(Layout.Fireplace);
        World.sendParticles(ParticleTypes.LAVA, Spot.x, Spot.y - 0.2, Spot.z, 3, 0.2, 0.1, 0.2, 0.0);
        perform(World, User, Chore.Feed);
        return true;
    }

    public static boolean stoke(ServerLevel World, ServerPlayer User) {
        Scp4666State State = Scp4666State.get(World.getServer());
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        if (Scp4666Heat.boilerFull(State)) {
            say(User, "boiler_full");
            return false;
        }
        if (!take(User, Scp4666Registry.Scuttle.get(), 1)) {
            return false;
        }
        Scp4666Heat.feedBoiler(World, Layout, State);
        World.playSound(null, Layout.Heater, Scp4666Registry.BoilerDoor.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
        World.playSound(null, Layout.Heater, Scp4666Registry.CoalShovel.get(), SoundSource.BLOCKS, 0.8f, 0.7f);
        perform(World, User, Chore.Stoke);
        say(User, "boiler_fed");
        return true;
    }

    public static boolean board(ServerLevel World, ServerPlayer User, BlockPos Pos) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = Scp4666State.get(World.getServer());
        int Index = Layout.windowAt(Pos);
        if (Index < 0 || !State.cracked().contains(Index)) {
            return false;
        }
        if (carried(User, Scp4666Registry.Boards.get()) < BoardsPerWindow && !User.getAbilities().instabuild) {
            say(User, "boards_short");
            return false;
        }
        if (channel(World, User, Pos, Chore.Board, Done -> {
            if (!State.cracked().contains(Index) || !take(Done, Scp4666Registry.Boards.get(), BoardsPerWindow)) {
                return;
            }
            setWindow(World, Layout, State, Index, false, true);
            perform(World, Done, Chore.Board);
            say(Done, "board_done");
        })) {
            say(User, "board_start");
        }
        return true;
    }

    public static boolean offer(ServerLevel World, ServerPlayer User) {
        Scp4666State State = Scp4666State.get(World.getServer());
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        if (State.offering()) {
            say(User, "gift_waiting");
            return false;
        }
        if (!take(User, Scp4666Registry.Gift.get(), 1)) {
            return false;
        }
        State.setOffering(true);
        World.setBlock(Layout.Offering, Scp4666Registry.Parcel.get().defaultBlockState().setValue(Scp4666FurnitureBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        World.playSound(null, Layout.Offering, Scp4666Registry.Wrapping.get(), SoundSource.BLOCKS, 0.6f, 0.8f);
        perform(World, User, Chore.Offer);
        say(User, "gift_set");
        return true;
    }

    public static void takeOffering(ServerLevel World, Scp4666Map Layout, Scp4666State State) {
        if (!State.offering()) {
            return;
        }
        State.setOffering(false);
        State.setActivity(State.activity() - OfferingCalm);
        if (World.getBlockState(Layout.Offering).is(Scp4666Registry.Parcel.get())) {
            World.setBlock(Layout.Offering, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        World.playSound(null, Layout.Offering, Scp4666Registry.YuleRustle.get(), SoundSource.HOSTILE, 0.4f, 0.7f);
    }

    public static void morning(ServerLevel World, Scp4666Map Layout, Scp4666State State, int Before, RandomSource Random) {
        State.setActivity(State.activity() * MorningKeep);
        State.clearDaily();
        if (Before >= PryFrom && !State.boarded().isEmpty() && Random.nextFloat() < PryChance) {
            List<Integer> Shut = new ArrayList<>(State.boarded());
            crack(World, Layout, State, Shut.get(Random.nextInt(Shut.size())));
            return;
        }
        if (Before >= CrackFrom && Random.nextFloat() < CrackChance + CrackActivity * State.activity() / 100.0f) {
            List<Integer> Whole = new ArrayList<>();
            for (int Index = 0; Index < Layout.Windows.size(); Index++) {
                if (!State.cracked().contains(Index) && !State.boarded().contains(Index)) {
                    Whole.add(Index);
                }
            }
            if (!Whole.isEmpty()) {
                crack(World, Layout, State, Whole.get(Random.nextInt(Whole.size())));
            }
        }
    }

    public static boolean crack(ServerLevel World, Scp4666Map Layout, Scp4666State State, int Index) {
        if (Index < 0 || Index >= Layout.Windows.size() || State.cracked().contains(Index)) {
            return false;
        }
        setWindow(World, Layout, State, Index, true, false);
        Vec3 Glass = Layout.Windows.get(Index).glass();
        World.playSound(null, Glass.x, Glass.y, Glass.z, Scp4666Registry.GlassCrack.get(), SoundSource.BLOCKS, 0.9f, 0.7f);
        return true;
    }

    public static void setWindow(ServerLevel World, Scp4666Map Layout, Scp4666State State, int Index, boolean Cracked, boolean Boarded) {
        Scp4666Map.Window Glass = Layout.Windows.get(Index);
        State.setWindow(Index, Cracked, Boarded);
        Direction Inward = Glass.Out().getOpposite();
        for (BlockPos Cell : Glass.cells()) {
            BlockState Next;
            if (Cracked || Boarded) {
                Next = Scp4666Registry.Window.get().defaultBlockState().setValue(Scp4666WindowBlock.FACING, Inward).setValue(Scp4666WindowBlock.Boarded, Boarded);
            } else {
                Next = Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, Inward.getAxis() == Direction.Axis.Z)
                        .setValue(IronBarsBlock.WEST, Inward.getAxis() == Direction.Axis.Z)
                        .setValue(IronBarsBlock.NORTH, Inward.getAxis() == Direction.Axis.X)
                        .setValue(IronBarsBlock.SOUTH, Inward.getAxis() == Direction.Axis.X);
            }
            if (World.getBlockState(Cell) != Next) {
                World.setBlock(Cell, Next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
    }

    public static boolean open(Scp4666State State, int Index) {
        return Index >= 0 && !State.boarded().contains(Index);
    }
}
