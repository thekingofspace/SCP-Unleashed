package net.rai.scp_unleashed.scp178;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Scp178Commands {
    public static final String Prefix = "commands.scp_unleashed.scp178.";

    private Scp178Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp178")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status").executes(Scp178Commands::status))
                        .then(Commands.literal("summon").executes(Context -> summon(Context, false))
                                .then(Commands.literal("hostile").executes(Context -> summon(Context, true))))
                        .then(Commands.literal("clear").executes(Scp178Commands::clear))
                        .then(Commands.literal("exposure")
                                .then(Commands.argument("minutes", IntegerArgumentType.integer(0, 60)).executes(Scp178Commands::exposure)))));
    }

    private static int status(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp178Watchers.Haunt State = Scp178Watchers.hauntFor(User.getUUID());
        int Count = Scp178Watchers.count(User.getUUID());
        int Hostile = Scp178Watchers.hostileCount(User.getUUID());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", Component.translatable(Prefix + (Scp178GlassesItem.isWorn(User) ? "worn" : "not_worn")),
                State.worn() / 20, Count, Scp178Watchers.cap(State.worn()), Hostile), false);
        return Count;
    }

    private static int summon(CommandContext<CommandSourceStack> Context, boolean Hostile) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp178Watchers.Haunt State = Scp178Watchers.hauntFor(User.getUUID());
        if (!State.wearing()) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "need_glasses"));
            return 0;
        }
        Scp178Phantom Phantom = Scp178Watchers.spawn(User, State, Hostile);
        if (Phantom == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "full", Scp178Watchers.MaxInstances));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "summoned", Math.round(Phantom.distance())), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        int Removed = Scp178Watchers.clear(User);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "cleared", Removed), false);
        return Removed;
    }

    private static int exposure(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        int Minutes = IntegerArgumentType.getInteger(Context, "minutes");
        Scp178Watchers.Haunt State = Scp178Watchers.hauntFor(User.getUUID());
        if (!State.wearing()) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "need_glasses"));
            return 0;
        }
        State.setWorn(Minutes * 1200);
        State.setTimer(Math.min(State.timer(), 40));
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "exposure", Minutes, Scp178Watchers.cap(State.worn())), false);
        return Minutes;
    }
}
