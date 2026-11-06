package net.rai.scp_unleashed.control;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;

public final class ControlCommands {
    public static final String Prefix = "commands.scp_unleashed.control.";

    private ControlCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("control")
                        .then(Commands.literal("release")
                                .executes(Context -> release(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .requires(Source -> Source.hasPermission(2))
                                        .executes(Context -> release(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static int release(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Released = 0;
        for (ServerPlayer Target : Targets) {
            ControlSession Session = ControlSessions.session(Target);
            if (!Session.active()) {
                Source.sendFailure(Component.translatable(Prefix + "release.idle", Target.getDisplayName()));
                continue;
            }
            Component Body = nameOf(Session);
            ControlManager.release(Target);
            Source.sendSuccess(() -> Component.translatable(Prefix + "release.success", Target.getDisplayName(), Body), true);
            Released++;
        }
        return Released;
    }

    private static int status(CommandSourceStack Source) {
        int Shown = 0;
        ServerPlayer Self = Source.getPlayer();
        if (Self != null) {
            describe(Source, Self);
            Shown++;
        }
        if (Source.hasPermission(2)) {
            for (ServerPlayer Other : Source.getServer().getPlayerList().getPlayers()) {
                if (Other != Self && ControlSessions.isControlling(Other)) {
                    describe(Source, Other);
                    Shown++;
                }
            }
        }
        if (Shown == 0) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.none").withStyle(ChatFormatting.GRAY), false);
        }
        return Shown;
    }

    private static void describe(CommandSourceStack Source, ServerPlayer Target) {
        ControlSession Session = ControlSessions.session(Target);
        if (!Session.active()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.idle", Target.getDisplayName()).withStyle(ChatFormatting.GRAY), false);
            return;
        }
        long Seconds = Math.max(0L, (Target.serverLevel().getGameTime() - Session.Started()) / 20L);
        Component Body = nameOf(Session);
        String Since = String.format("%d:%02d", Seconds / 60L, Seconds % 60L);
        String Home = String.format("%s %.0f %.0f %.0f", Session.HomeDimension(), Session.HomeX(), Session.HomeY(), Session.HomeZ());
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.active", Target.getDisplayName(), Body, Since, Home).withStyle(ChatFormatting.AQUA), false);
    }

    private static Component nameOf(ControlSession Session) {
        String Name = Session.Identity().getString("ControlName");
        return Name.isEmpty() ? Component.translatable(Prefix + "unnamed") : Component.literal(Name);
    }
}
