package net.rai.scp_unleashed.scp055;

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

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp055Commands {
    public static final String Prefix = "commands.scp_unleashed.scp055.";

    private Scp055Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp055")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("episode")
                                .executes(Context -> episode(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> episode(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))));
    }

    private static int episode(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            Target.getCooldowns().removeCooldown(Scp055Registry.CaseItem.get());
            if (Scp055Episodes.begin(Target)) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "episode.success", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "episode.busy", Target.getDisplayName()));
            }
        }
        return Count;
    }
}
