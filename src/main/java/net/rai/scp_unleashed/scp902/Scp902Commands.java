package net.rai.scp_unleashed.scp902;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class Scp902Commands {
    public static final String Prefix = "commands.scp_unleashed.scp902.";

    private Scp902Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed").then(Commands.literal("scp902").requires(Source -> Source.hasPermission(2))
                .then(Commands.literal("send").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> send(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("return").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> recall(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("forgive").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> forgive(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("status").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> status(Context, EntityArgument.getPlayers(Context, "targets")))))));
    }

    private static int send(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        int Count = 0;
        for (ServerPlayer User : Targets) {
            if (Scp902Trip.start(User)) {
                Count++;
            }
        }
        int Sent = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "sent", Sent), true);
        return Count;
    }

    private static int recall(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer User : Targets) {
            if (Scp902Trip.home(User)) {
                Count++;
            }
        }
        int Returned = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "returned", Returned), true);
        return Count;
    }

    private static int forgive(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer User : Targets) {
            if (Scp902State.get(User.server).forgive(User.getUUID())) {
                Count++;
            }
        }
        int Cleared = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "forgiven", Cleared), true);
        return Count;
    }

    private static int status(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        for (ServerPlayer User : Targets) {
            Scp902State State = Scp902State.get(User.server);
            String Key = State.trip(User.getUUID()) != null ? "away" : State.visited(User.getUUID()) ? "visited" : "fresh";
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status." + Key, User.getDisplayName()), false);
        }
        return Targets.size();
    }
}
