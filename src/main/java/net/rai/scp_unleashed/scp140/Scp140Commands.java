package net.rai.scp_unleashed.scp140;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Scp140Commands {
    public static final String Prefix = "commands.scp_unleashed.scp140.";
    public static final double MaxInk = 1000000.0;

    private Scp140Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp140")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status").executes(Scp140Commands::status))
                        .then(Commands.literal("ink")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, MaxInk))
                                        .executes(Context -> ink(Context, DoubleArgumentType.getDouble(Context, "amount")))))
                        .then(Commands.literal("fill").executes(Context -> fill(Context, 1))
                                .then(Commands.argument("pages", IntegerArgumentType.integer(1, Scp140Pages.Total))
                                        .executes(Context -> fill(Context, IntegerArgumentType.getInteger(Context, "pages")))))
                        .then(Commands.literal("reset").executes(Scp140Commands::reset))));
    }

    private static BlockPos origin(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 Where = Source.getPosition();
        return Scp140Witnesses.closest(World, Where).map(Scp140Witnesses.Witness::Pos).orElse(BlockPos.containing(Where));
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        ServerLevel World = Source.getLevel();
        Scp140Chronicle Chronicle = Scp140Chronicle.get(Source.getServer());
        Scp140Mood Mood = Chronicle.mood(World.getGameTime());
        String Ink = String.format(Locale.ROOT, "%.1f", Chronicle.ink());
        String Cost = String.format(Locale.ROOT, "%.0f", Chronicle.currentCost());
        int Percent = (int) Math.floor(Chronicle.progress() * 100.0);
        String Total = String.format(Locale.ROOT, "%.0f", Chronicle.totalInk());
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", Chronicle.written(), Scp140Pages.Total, Ink, Cost, Percent, Total,
                Component.translatable("commands.scp_unleashed.scp140.mood." + Mood.getName())), false);
        Scp140Page Last = Chronicle.page(Chronicle.written());
        if (Last != null) {
            Component Site = Last.hasSite()
                    ? Component.translatable(Prefix + "site", Last.Site().getX(), Last.Site().getY(), Last.Site().getZ(), Last.SiteDimension().location().toString())
                    : Component.translatable(Prefix + "no_site");
            Source.sendSuccess(() -> Component.translatable(Prefix + "last", Last.Page(), Last.Day(), Site), false);
        }
        Map<Scp140Witnesses.Kind, Integer> Counts = new EnumMap<>(Scp140Witnesses.Kind.class);
        List<Scp140Witnesses.Witness> Seen = Scp140Witnesses.all(World);
        for (Scp140Witnesses.Witness Book : Seen) {
            Counts.merge(Book.Source(), 1, Integer::sum);
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "witnesses", Counts.getOrDefault(Scp140Witnesses.Kind.Placed, 0),
                Counts.getOrDefault(Scp140Witnesses.Kind.Carried, 0), Counts.getOrDefault(Scp140Witnesses.Kind.Dropped, 0),
                Counts.getOrDefault(Scp140Witnesses.Kind.Stored, 0)), false);
        return Chronicle.written();
    }

    private static int ink(CommandContext<CommandSourceStack> Context, double Amount) {
        CommandSourceStack Source = Context.getSource();
        Scp140Chronicle Chronicle = Scp140Chronicle.get(Source.getServer());
        if (Chronicle.isComplete()) {
            Source.sendFailure(Component.translatable(Prefix + "complete"));
            return 0;
        }
        BlockPos Origin = origin(Source);
        int Written = Chronicle.addInk(Source.getLevel(), Origin, Amount);
        String Shown = String.format(Locale.ROOT, "%.1f", Amount);
        Source.sendSuccess(() -> Component.translatable(Prefix + "ink", Shown, Written, Chronicle.written(), Scp140Pages.Total, Origin.getX(), Origin.getY(), Origin.getZ()), true);
        return Written;
    }

    private static int fill(CommandContext<CommandSourceStack> Context, int Pages) {
        CommandSourceStack Source = Context.getSource();
        Scp140Chronicle Chronicle = Scp140Chronicle.get(Source.getServer());
        if (Chronicle.isComplete()) {
            Source.sendFailure(Component.translatable(Prefix + "complete"));
            return 0;
        }
        BlockPos Origin = origin(Source);
        int Written = Chronicle.fill(Source.getLevel(), Origin, Pages);
        Source.sendSuccess(() -> Component.translatable(Prefix + "fill", Written, Chronicle.written(), Scp140Pages.Total, Origin.getX(), Origin.getY(), Origin.getZ()), true);
        return Written;
    }

    private static int reset(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        Scp140Chronicle.get(Source.getServer()).reset(Source.getServer());
        Source.sendSuccess(() -> Component.translatable(Prefix + "reset"), true);
        return 1;
    }
}
