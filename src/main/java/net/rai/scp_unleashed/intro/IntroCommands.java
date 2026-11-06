package net.rai.scp_unleashed.intro;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class IntroCommands {
    public static final String Prefix = "commands.scp_unleashed.intro.";

    private IntroCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("intro")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("replay")
                                .executes(Context -> replay(Context, List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(Context -> replay(Context, EntityArgument.getPlayers(Context, "player")))))
                        .then(Commands.literal("reset")
                                .executes(Context -> reset(Context, List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(Context -> reset(Context, EntityArgument.getPlayers(Context, "player")))))));
    }

    private static int replay(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Players) throws CommandSyntaxException {
        int Count = 0;
        for (ServerPlayer User : Players) {
            IntroDirector.replay(User);
            Count++;
        }
        int Shown = Count;
        if (Shown == 1) {
            String Name = Players.iterator().next().getGameProfile().getName();
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "replay", Name), true);
        } else {
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "replay_many", Shown), true);
        }
        return Count;
    }

    private static int reset(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Players) throws CommandSyntaxException {
        int Count = 0;
        for (ServerPlayer User : Players) {
            IntroDirector.reset(User);
            Count++;
        }
        int Shown = Count;
        if (Shown == 1) {
            String Name = Players.iterator().next().getGameProfile().getName();
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "reset", Name), true);
        } else {
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "reset_many", Shown), true);
        }
        return Count;
    }
}
