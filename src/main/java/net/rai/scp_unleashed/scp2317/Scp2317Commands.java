package net.rai.scp_unleashed.scp2317;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;

import java.util.Collection;

public final class Scp2317Commands {
    public static final String Prefix = "commands.scp_unleashed.scp2317.";

    private Scp2317Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId).then(Commands.literal("scp2317").requires(Source -> Source.hasPermission(2))
                .then(Commands.literal("send").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> send(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("return").then(Commands.argument("targets", EntityArgument.players()).executes(Context -> recall(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("status").executes(Scp2317Commands::status))
                .then(Commands.literal("rest").executes(Scp2317Commands::rest))));
    }

    private static int send(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer User : Targets) {
            if (Scp2317Travel.enter(User, User.serverLevel().dimension(), User.position(), User.getYRot())) {
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
            if (Scp2317Travel.leave(User)) {
                Count++;
            }
        }
        int Returned = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "returned", Returned), true);
        return Count;
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        MinecraftServer Server = Context.getSource().getServer();
        ScarletState State = ScarletState.get(Server);
        boolean Cooling = Scp2317Ritual.cooling(Server);
        int Inside = Scp2317World.players(Server).size();
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", State.read() ? "yes" : "no", String.format("%.1f%%", State.manifestation() * 100.0f),
                State.broken(), Cooling ? "yes" : "no", Inside), false);
        return 1;
    }

    private static int rest(CommandContext<CommandSourceStack> Context) {
        Scp2317Ritual.resetCooldown(Context.getSource().getServer());
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "rested"), true);
        return 1;
    }
}
