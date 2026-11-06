package net.rai.scp_unleashed.scp444;

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
import net.rai.scp_unleashed.control.ControlSessions;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp444Commands {
    public static final String Prefix = "commands.scp_unleashed.scp444.";
    public static final int StatusLines = 12;
    public static final long TicksPerMinute = 1200L;

    private Scp444Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp444")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("afflict", Scp444Commands::afflict))
                        .then(targeted("clear", Scp444Commands::clear))
                        .then(targeted("status", Scp444Commands::status))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<ServerPlayer>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getPlayers(Context, "targets"))));
    }

    private static int afflict(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        boolean Single = Targets.size() == 1;
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp444Affliction.isAfflicted(Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "afflict.already", Target.getDisplayName()));
                }
            } else if (Scp444Affliction.afflict(Target)) {
                Scp444Sync.update(Target);
                if (Single) {
                    Source.sendSuccess(() -> Component.translatable(Prefix + "afflict.success", Target.getDisplayName()), true);
                }
                Count++;
            } else if (Single) {
                Source.sendFailure(Component.translatable(Prefix + "afflict.failed", Target.getDisplayName()));
            }
        }
        if (!Single) {
            int Afflicted = Count;
            if (Afflicted > 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "afflict.multiple", Afflicted), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "afflict.none"));
            }
        }
        return Count;
    }

    private static int clear(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        boolean Single = Targets.size() == 1;
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp444Affliction.clear(Target)) {
                Scp444Sync.update(Target);
                if (Single) {
                    Source.sendSuccess(() -> Component.translatable(Prefix + "clear.success", Target.getDisplayName()), true);
                }
                Count++;
            } else if (Single) {
                Source.sendFailure(Component.translatable(Prefix + "clear.none", Target.getDisplayName()));
            }
        }
        if (!Single) {
            int Cleared = Count;
            if (Cleared > 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "clear.multiple", Cleared), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "clear.none_any"));
            }
        }
        return Count;
    }

    private static int status(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Shown = 0;
        int Afflicted = 0;
        for (ServerPlayer Target : Targets) {
            Component Line = line(Target);
            if (Scp444Affliction.isAfflicted(Target)) {
                Afflicted++;
            }
            if (Shown < StatusLines) {
                Source.sendSuccess(() -> Line, false);
                Shown++;
            }
        }
        if (Targets.size() > StatusLines) {
            int More = Targets.size() - StatusLines;
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.more", More), false);
        }
        return Afflicted;
    }

    private static Component line(ServerPlayer Target) {
        String Body = ControlSessions.isControlling(Target) ? "body" : "own";
        if (Scp444Affliction.isAfflicted(Target)) {
            Long Since = Scp444Affliction.since(Target);
            long Minutes = Since == null ? 0L : Math.max(0L, (Target.level().getGameTime() - Since) / TicksPerMinute);
            return Component.translatable(Prefix + "status.afflicted." + Body, Target.getDisplayName(), Minutes);
        }
        if (Scp444Affliction.isShielded(Target)) {
            return Component.translatable(Prefix + "status.shielded." + Body, Target.getDisplayName());
        }
        return Component.translatable(Prefix + "status.clear." + Body, Target.getDisplayName());
    }
}
