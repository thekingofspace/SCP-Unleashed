package net.rai.scp_unleashed.scp2521;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp2521Commands {
    private Scp2521Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId).then(Commands.literal("scp2521").requires(Source -> Source.hasPermission(2))
                .then(Commands.literal("take").then(Commands.argument("player", EntityArgument.player()).executes(Context -> take(Context.getSource(),
                        EntityArgument.getPlayer(Context, "player")))))
                .then(Commands.literal("know").then(Commands.argument("player", EntityArgument.player()).executes(Context -> know(Context.getSource(),
                        EntityArgument.getPlayer(Context, "player"), true))))
                .then(Commands.literal("forget").then(Commands.argument("player", EntityArgument.player()).executes(Context -> know(Context.getSource(),
                        EntityArgument.getPlayer(Context, "player"), false))))
                .then(Commands.literal("release").executes(Context -> release(Context.getSource())))
                .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static int take(CommandSourceStack Source, ServerPlayer Target) {
        Scp2521Trips.uncool(Target.getUUID());
        if (!Scp2521Trips.start(Target)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp2521.busy", Target.getDisplayName()));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp2521.taken", Target.getDisplayName()), true);
        return 1;
    }

    private static int know(CommandSourceStack Source, ServerPlayer Target, boolean Knows) {
        Scp2521State State = Scp2521State.get(Source.getServer());
        if (Knows) {
            State.learn(Target.getUUID());
        } else {
            State.forget(Target.getUUID());
        }
        Source.sendSuccess(() -> Component.translatable(Knows ? "commands.scp_unleashed.scp2521.known" : "commands.scp_unleashed.scp2521.forgotten", Target.getDisplayName()), true);
        return 1;
    }

    private static int release(CommandSourceStack Source) {
        if (!Scp2521Containment.retire(Source.getServer(), false)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp2521.free"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp2521.released"), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        Scp2521State State = Scp2521State.get(Source.getServer());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp2521.status", State.readers(), Scp2521Trips.count(), State.held() != null), false);
        return 1;
    }
}
