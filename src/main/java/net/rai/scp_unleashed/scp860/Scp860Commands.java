package net.rai.scp_unleashed.scp860;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class Scp860Commands {
    public static final String Prefix = "commands.scp_unleashed.scp860.";

    private Scp860Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed").then(Commands.literal("scp860").requires(Source -> Source.hasPermission(2))
                .then(Commands.literal("send").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> send(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("return").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> recall(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("status").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> status(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("reset").executes(Scp860Commands::reset))));
    }

    private static int send(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer User : Targets) {
            Scp860Trip.Spot Return = new Scp860Trip.Spot(User.serverLevel(), User.position(), User.getYRot());
            if (!Scp860Forest.inside(User) && Scp860Trip.enter(User, Return, null, null)) {
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
            if (Scp860Trip.leave(User, Scp860Trip.Reason.Command)) {
                Count++;
            }
        }
        int Returned = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "returned", Returned), true);
        return Count;
    }

    private static int status(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        for (ServerPlayer User : Targets) {
            Scp860Record Ledger = Scp860Forest.record(User);
            String Key = Ledger.Active ? "inside" : "outside";
            int Held = Ledger.itemCount();
            float Patience = Scp860Director.patience(User);
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status." + Key, User.getDisplayName(), Held, String.format("%.2f", Patience)), false);
        }
        return Targets.size();
    }

    private static int reset(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = Scp860Forest.level(Context.getSource().getServer());
        if (World == null || Scp860Forest.occupied(Context.getSource().getServer())) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "reset_refused"));
            return 0;
        }
        Scp860Events.reset(World);
        Scp860Builder.ensure(World);
        int Generation = Scp860State.get(Context.getSource().getServer()).generation();
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "reset", Generation), true);
        return 1;
    }
}
