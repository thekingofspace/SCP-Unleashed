package net.rai.scp_unleashed.scp420j;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Scp420jCommands {
    public static final String Prefix = "commands.scp_unleashed.scp420j.";

    private Scp420jCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp420j")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("float")
                                .executes(Context -> lift(Context, Context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(Context -> lift(Context, EntityArgument.getPlayer(Context, "player")))))));
    }

    private static int lift(CommandContext<CommandSourceStack> Context, ServerPlayer Target) {
        Scp420jFloat.lightUp(Target);
        Scp420jFloat.start(Target);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "float", Target.getDisplayName()), true);
        return 1;
    }
}
