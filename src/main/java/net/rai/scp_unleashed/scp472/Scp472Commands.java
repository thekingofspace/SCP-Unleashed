package net.rai.scp_unleashed.scp472;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class Scp472Commands {
    public static final String Prefix = "commands.scp_unleashed.scp472.";

    private Scp472Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp472")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context, Context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(Context -> status(Context, EntityArgument.getPlayer(Context, "player")))))
                        .then(Commands.literal("sanity")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, Scp472Mind.Full))
                                        .executes(Scp472Commands::sanity)))));
    }

    private static int status(CommandContext<CommandSourceStack> Context, ServerPlayer User) {
        float Sanity = Scp472Mind.sanity(User);
        int Stage = Scp472Mind.stage(Sanity);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", User.getDisplayName(), String.format(Locale.ROOT, "%.1f", Sanity), Stage), false);
        return Stage;
    }

    private static int sanity(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp472Mind.store(User, FloatArgumentType.getFloat(Context, "value"));
        Scp472Sync.mind(User, true);
        return status(Context, User);
    }
}
