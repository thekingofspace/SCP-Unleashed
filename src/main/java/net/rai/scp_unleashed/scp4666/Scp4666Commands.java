package net.rai.scp_unleashed.scp4666;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;
import net.rai.scp_unleashed.scp4666.block.Scp4666BoxBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666CalendarBlock;
import net.rai.scp_unleashed.scp4666.block.Scp4666StockingBlock;
import net.rai.scp_unleashed.scp4666.network.Scp4666FxPayload;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class Scp4666Commands {
    public static final String Prefix = "commands.scp_unleashed.scp4666.";
    public static final int SnowReach = 24;
    public static final int SnowListed = 6;
    private static final SimpleCommandExceptionType Missing = new SimpleCommandExceptionType(Component.translatable(Prefix + "missing"));
    public static final List<String> Extra = List.of("window", "prints", "door", "cellar", "stocking", "box", "morning", "midnight_wake");

    private Scp4666Commands() {
    }

    public static List<String> eventNames() {
        List<String> Names = new ArrayList<>();
        for (Scp4666Director.Plan Entry : Scp4666Director.plans()) {
            if (!Names.contains(Entry.Key())) {
                Names.add(Entry.Key());
            }
        }
        Names.addAll(Extra);
        return Names;
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp4666")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("night").then(Commands.argument("night", IntegerArgumentType.integer(1, Scp4666State.FinalNight)).executes(Scp4666Commands::night)))
                        .then(Commands.literal("time")
                                .then(Commands.literal("dawn").executes(Context -> time(Context, Scp4666Clock.Dawn + 100L, Scp4666State.PhaseDay)))
                                .then(Commands.literal("noon").executes(Context -> time(Context, 6000L, Scp4666State.PhaseDay)))
                                .then(Commands.literal("dusk").executes(Context -> time(Context, Scp4666Clock.Dusk - 100L, Scp4666State.PhaseDay)))
                                .then(Commands.literal("midnight").executes(Context -> time(Context, Scp4666Clock.Midnight, Scp4666State.PhaseMidnight))))
                        .then(Commands.literal("event").then(Commands.argument("name", StringArgumentType.word())
                                .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(eventNames(), Builder)).executes(Scp4666Commands::event)))
                        .then(Commands.literal("ending")
                                .then(Commands.literal("good").executes(Context -> ending(Context, Scp4666Endings.FateGood, null))
                                        .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> ending(Context, Scp4666Endings.FateGood, EntityArgument.getPlayers(Context, "targets")))))
                                .then(Commands.literal("bad").executes(Context -> ending(Context, Scp4666Endings.FateBad, null))
                                        .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> ending(Context, Scp4666Endings.FateBad, EntityArgument.getPlayers(Context, "targets"))))))
                        .then(Commands.literal("cold")
                                .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> cold(Context, EntityArgument.getPlayers(Context, "targets"), false))
                                        .then(Commands.literal("now").executes(Context -> cold(Context, EntityArgument.getPlayers(Context, "targets"), true)))))
                        .then(Commands.literal("reset").executes(Scp4666Commands::reset))
                        .then(Commands.literal("enter").executes(Scp4666Commands::enter))
                        .then(Commands.literal("leave").executes(Scp4666Commands::leave))
                        .then(Commands.literal("summon").executes(Scp4666Commands::summon))
                        .then(Commands.literal("status").executes(Scp4666Commands::status))
                        .then(Commands.literal("snowcheck").executes(Scp4666Commands::snowcheck))
                        .then(Commands.literal("clockcheck").executes(Scp4666Commands::clockcheck))));
    }

    private static ServerLevel winter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = Scp4666World.level(Context.getSource().getServer());
        if (World == null) {
            throw Missing.create();
        }
        Scp4666World.ensureBuilt(World);
        return World;
    }

    private static int night(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        int Night = IntegerArgumentType.getInteger(Context, "night");
        Scp4666State State = Scp4666State.get(World.getServer());
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        State.setNight(Night);
        State.newNight();
        State.setPhase(Scp4666State.PhaseDay);
        State.setClock(Scp4666Clock.Dusk - 200L);
        State.start();
        Scp4666Director.refreshLamps(World, Layout, State);
        Scp4666CalendarBlock.mark(World, Layout, Night);
        Scp4666Director.sync(World, State);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "night", Night), true);
        return Night;
    }

    private static int time(CommandContext<CommandSourceStack> Context, long Clock, int Phase) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        Scp4666State State = Scp4666State.get(World.getServer());
        State.setClock(Clock);
        State.setPhase(Phase);
        if (Phase == Scp4666State.PhaseMidnight) {
            Scp4666Director.rollNight(World, State);
        }
        Scp4666Director.sync(World, State);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "time", Clock), true);
        return 1;
    }

    public static boolean trigger(ServerLevel World, String Name) {
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        Scp4666State State = Scp4666State.get(World.getServer());
        RandomSource Random = World.getRandom();
        switch (Name) {
            case "window" -> {
                Scp4666Happenings.windowVisit(World, Layout, Random);
                return true;
            }
            case "prints" -> {
                Scp4666Happenings.prints(World, Layout, State, Random);
                return true;
            }
            case "door" -> {
                Scp4666Happenings.doorAjar(World, Layout);
                return true;
            }
            case "cellar" -> {
                Scp4666Happenings.cellarAjar(World, Layout);
                return true;
            }
            case "stocking" -> {
                Scp4666StockingBlock.hang(World, Layout, Scp4666World.living(World));
                return true;
            }
            case "box" -> {
                Scp4666BoxBlock.deliver(World, Layout, State.night() >= Scp4666State.FoodStops ? 0 : 1, Random.nextLong());
                return true;
            }
            case "morning" -> {
                Scp4666Director.wakeAll(Scp4666World.living(World));
                Scp4666Director.morning(World, Layout, State);
                return true;
            }
            case "midnight_wake" -> {
                Scp4666Director.midnightWake(World, Layout, State, Scp4666World.living(World));
                return true;
            }
            default -> {
                for (Scp4666Director.Plan Entry : Scp4666Director.plans()) {
                    if (Entry.Key().equals(Name)) {
                        boolean Ran = Entry.Action().run(World, Layout, Random);
                        if (Ran) {
                            State.fire(Entry.Key());
                        }
                        return Ran;
                    }
                }
                return false;
            }
        }
    }

    private static int event(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        String Name = StringArgumentType.getString(Context, "name").toLowerCase(Locale.ROOT);
        if (!eventNames().contains(Name)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "unknown_event", Name));
            return 0;
        }
        boolean Ran = trigger(World, Name);
        if (!Ran) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "event_skipped", Name));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "event", Name), true);
        return 1;
    }

    private static int ending(CommandContext<CommandSourceStack> Context, int Fate, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        List<ServerPlayer> Living = Scp4666World.living(World);
        if (Living.isEmpty()) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "nobody"));
            return 0;
        }
        for (ServerPlayer User : Targets == null ? Living : Targets) {
            Scp4666Endings.force(User.getUUID(), Fate);
        }
        Scp4666State State = Scp4666State.get(World.getServer());
        State.setNight(Scp4666State.FinalNight);
        Scp4666Director.wakeAll(Living);
        Scp4666Endings.begin(World, Scp4666Map.get(World.getServer()), State, Living);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + (Fate == Scp4666Endings.FateGood ? "ending_good" : "ending_bad")), true);
        return 1;
    }

    private static int cold(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets, boolean Now) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        Scp4666State State = Scp4666State.get(World.getServer());
        int Count = 0;
        for (ServerPlayer User : Targets) {
            if (User.level() != World) {
                continue;
            }
            Scp4666Cold.mark(World, State, User);
            if (Now) {
                Scp4666Cold.start(World, User);
            }
            Count++;
        }
        int Total = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "cold", Total), true);
        return Count;
    }

    private static int reset(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        for (ServerPlayer User : new ArrayList<>(World.players())) {
            Scp4666Endings.release(User);
            GatewayTravel.leave(User);
        }
        Scp4666Director.reset(World);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "reset"), true);
        return 1;
    }

    private static int enter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        winter(Context);
        GatewayDestination Destination = GatewayDestinations.get(Scp4666Destination.Id);
        if (Destination == null || Scp4666World.inside(User)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "already_inside"));
            return 0;
        }
        GatewayStash.Spot Back = new GatewayStash.Spot(User.level().dimension(), User.position(), User.getYRot());
        if (!GatewayTravel.enter(User, Destination, Back)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "refused"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "enter"), true);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp4666World.inside(User)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "not_inside"));
            return 0;
        }
        Scp4666Endings.release(User);
        GatewayTravel.leave(User);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "leave"), true);
        return 1;
    }

    private static int summon(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        boolean Ran = Scp4666Happenings.watcher(World, Scp4666Map.get(World.getServer()), World.getRandom());
        if (!Ran) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "event_skipped", "watcher"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "event", "watcher"), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        Scp4666State State = Scp4666State.get(World.getServer());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", State.night(), State.clock(), State.phase(), Scp4666World.visitors(World).size(), State.epoch()), false);
        for (Scp4666State.Visitor Entry : State.visitors()) {
            int Percent = Math.round(Scp4666Director.luck(Entry) * 100.0f);
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "visitor", Entry.Name, Entry.Cocoas, Entry.Mornings, Entry.TreeNights, Entry.Nights, Entry.Stares,
                    Entry.Sightings, Percent), false);
        }
        return State.night();
    }

    private static int snowcheck(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        Scp4666Map Layout = Scp4666Map.get(World.getServer());
        BlockPos Centre = BlockPos.containing(Context.getSource().getPosition());
        boolean Still = Scp4666World.stills(World.dimension(), Scp4666World.nearSite(World.getServer(), Centre));
        List<BlockPos> Deep = new ArrayList<>();
        int Count = 0;
        BlockPos.MutableBlockPos Probe = new BlockPos.MutableBlockPos();
        for (int X = Centre.getX() - SnowReach; X <= Centre.getX() + SnowReach; X++) {
            for (int Z = Centre.getZ() - SnowReach; Z <= Centre.getZ() + SnowReach; Z++) {
                if (!Scp4666Builder.covered(Layout, X, Z)) {
                    continue;
                }
                for (int Y = Layout.Ground + 1; Y <= Layout.Ground + Scp4666Builder.ClearAbove + 1; Y++) {
                    if (Scp4666Builder.excess(World, Layout, Probe.set(X, Y, Z)) > 0) {
                        Count++;
                        if (Deep.size() < SnowListed) {
                            Deep.add(Probe.immutable());
                        }
                    }
                }
            }
        }
        long Stilled = Scp4666World.stilled();
        int Over = Count;
        int Waiting = Scp4666Builder.queued(World.getServer());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "snowcheck", Still ? "yes" : "no", Stilled, SnowReach, Over, Waiting), false);
        for (BlockPos Spot : Deep) {
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "snowcheck_cell", Spot.toShortString(), World.getBlockState(Spot).getBlock().getName(),
                    Scp4666Builder.designedSnow(World, Layout, Spot)), false);
        }
        List<BlockPos> Edges = Scp4666Seal.openGlass(World, Centre.offset(-SnowReach, -SnowReach, -SnowReach), Centre.offset(SnowReach, SnowReach, SnowReach));
        Scp4666Seal.Report Seal = Scp4666Seal.scan(World, Layout);
        List<BlockPos> Seen = near(Seal.Snow(), Centre);
        List<BlockPos> Holes = near(Seal.Sky(), Centre);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "snowcheck_glass", Edges.size(), Seen.size(), Holes.size()), false);
        for (BlockPos Spot : Edges.subList(0, Math.min(SnowListed, Edges.size()))) {
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "snowcheck_glass_cell", Spot.toShortString(), World.getBlockState(Spot.below()).getBlock().getName(),
                    World.getBlockState(Spot.above()).getBlock().getName()), false);
        }
        for (BlockPos Spot : Seen.subList(0, Math.min(SnowListed, Seen.size()))) {
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "snowcheck_inside_cell", Spot.toShortString()), false);
        }
        for (BlockPos Spot : Holes.subList(0, Math.min(SnowListed, Holes.size()))) {
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "snowcheck_sky_cell", Spot.toShortString()), false);
        }
        if (Over > 0) {
            Scp4666Builder.queueAll(World);
        }
        return Over + Edges.size() + Seen.size() + Holes.size();
    }

    private static List<BlockPos> near(List<BlockPos> Found, BlockPos Centre) {
        List<BlockPos> Close = new ArrayList<>();
        for (BlockPos Spot : Found) {
            if (Spot.distManhattan(Centre) <= SnowReach * 3) {
                Close.add(Spot);
            }
        }
        return Close;
    }

    private static int clockcheck(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerLevel World = winter(Context);
        Scp4666State State = Scp4666State.get(World.getServer());
        long Clock = State.clock();
        long Shown = World.getDayTime();
        String Rate = String.format(Locale.ROOT, "%.3f", Scp4666Clock.rate(World.getServer()));
        int Minutes = Scp4666Registry.dayMinutes(World.getServer());
        int Audio = Math.round(Scp4666Registry.audioShare(World.getServer()) * 100.0f);
        boolean Moving = Scp4666Director.running(World, State);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "clockcheck", Clock, Shown, Rate, Minutes, State.phase(), Moving ? "yes" : "no", Audio), false);
        if (Context.getSource().getEntity() instanceof ServerPlayer User) {
            PacketDistributor.sendToPlayer(User, new Scp4666FxPayload(Scp4666FxPayload.Probe, 0.0, 0.0, 0.0, 0));
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.abs(Clock - Shown));
    }

    public static Vec3 nowhere() {
        return Vec3.ZERO;
    }
}
