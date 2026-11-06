package net.rai.scp_unleashed.scp063;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp063Commands {
    public static final String Prefix = "commands.scp_unleashed.scp063.";

    private Scp063Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp063")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("teeth")
                                .then(targeted("restore", Scp063Commands::restore))
                                .then(targeted("remove", Scp063Commands::remove)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<ServerPlayer>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getPlayers(Context, "targets"))));
    }

    private static int restore(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp063Teeth.restore(Target)) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "restore.success", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "restore.none", Target.getDisplayName()));
            }
        }
        return Count;
    }

    private static int remove(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp063Teeth.lose(Target)) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "remove.success", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "remove.already", Target.getDisplayName()));
            }
        }
        return Count;
    }
}
