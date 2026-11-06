package net.rai.scp_unleashed.scp253;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
public final class Scp253Commands {
    public static final String Prefix = "commands.scp_unleashed.scp253.";

    private Scp253Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp253")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("infect", Scp253Commands::infect))
                        .then(targeted("cure", Scp253Commands::cure))
                        .then(targeted("query", Scp253Commands::query))
                        .then(Commands.literal("stage")
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, Scp253Infection.Stages))
                                        .executes(Context -> stage(Context.getSource(), List.of(Context.getSource().getPlayerOrException()),
                                                IntegerArgumentType.getInteger(Context, "stage")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(Context -> stage(Context.getSource(), EntityArgument.getPlayers(Context, "targets"),
                                                        IntegerArgumentType.getInteger(Context, "stage"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<ServerPlayer>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getPlayers(Context, "targets"))));
    }

    private static int infect(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Changed = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp253Infection.infect(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "infect.success", Target.getDisplayName()), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "infect.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int cure(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Changed = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp253Infection.cure(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "cure.success", Target.getDisplayName()), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int query(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Positive = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp253Infection.isInfected(Target)) {
                int Elapsed = Scp253Infection.elapsed(Target);
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.positive", Target.getDisplayName(), Scp253Infection.stageOf(Elapsed),
                        Scp253Infection.growthsAt(Elapsed), (Scp253Infection.Duration - Elapsed + 1199) / 1200), false);
                Positive++;
            } else {
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.negative", Target.getDisplayName()), false);
            }
        }
        return Positive;
    }

    private static int stage(CommandSourceStack Source, Collection<ServerPlayer> Targets, int Stage) {
        int Changed = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp253Infection.setElapsed(Target, Scp253Infection.StageStarts[Stage - 1])) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "stage.success", Target.getDisplayName(), Stage), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }
}
