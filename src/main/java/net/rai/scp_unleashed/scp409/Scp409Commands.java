package net.rai.scp_unleashed.scp409;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp409Commands {
    public static final String Prefix = "commands.scp_unleashed.scp409.";
    public static final int StatusLines = 12;

    private Scp409Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp409")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("infect", Scp409Commands::infect))
                        .then(targeted("cure", Scp409Commands::cure))
                        .then(targeted("status", Scp409Commands::status))
                        .then(Commands.literal("stage")
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, Scp409Infection.Stages))
                                        .executes(Context -> stage(Context.getSource(), List.of(Context.getSource().getEntityOrException()),
                                                IntegerArgumentType.getInteger(Context, "stage")))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(Context -> stage(Context.getSource(), EntityArgument.getEntities(Context, "targets"),
                                                        IntegerArgumentType.getInteger(Context, "stage"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<? extends Entity>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getEntities(Context, "targets"))));
    }

    private static int infect(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        boolean Single = Targets.size() == 1;
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                }
                continue;
            }
            if (Scp409Exposure.isExempt(Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "exempt", Target.getDisplayName()));
                }
            } else if (Scp409Infection.isInfected(Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "infect.already", Target.getDisplayName()));
                }
            } else if (Scp409Infection.infect(Target)) {
                if (Single) {
                    Source.sendSuccess(() -> Component.translatable(Prefix + "infect.success", Target.getDisplayName()), true);
                }
                Count++;
            } else if (Single) {
                Source.sendFailure(Component.translatable(Prefix + "infect.failed", Target.getDisplayName()));
            }
        }
        if (!Single) {
            int Infected = Count;
            if (Infected > 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "infect.multiple", Infected), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "infect.none"));
            }
        }
        return Count;
    }

    private static int cure(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        boolean Single = Targets.size() == 1;
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                }
                continue;
            }
            if (Scp409Infection.cure(Target)) {
                if (Single) {
                    Source.sendSuccess(() -> Component.translatable(Prefix + "cure.success", Target.getDisplayName()), true);
                }
                Count++;
            } else if (Single) {
                Source.sendFailure(Component.translatable(Prefix + "cure.none", Target.getDisplayName()));
            }
        }
        if (!Single) {
            int Cured = Count;
            if (Cured > 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "cure.multiple", Cured), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.none_any"));
            }
        }
        return Count;
    }

    private static int stage(CommandSourceStack Source, Collection<? extends Entity> Targets, int Stage) {
        boolean Single = Targets.size() == 1;
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                }
                continue;
            }
            if (Scp409Exposure.isExempt(Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "exempt", Target.getDisplayName()));
                }
            } else if (Scp409Infection.setStage(Target, Stage)) {
                if (Single) {
                    Source.sendSuccess(() -> Component.translatable(Prefix + "stage.success", Target.getDisplayName(), Stage, Scp409Infection.Stages), true);
                }
                Count++;
            } else if (Single) {
                Source.sendFailure(Component.translatable(Prefix + "infect.failed", Target.getDisplayName()));
            }
        }
        if (!Single) {
            int Staged = Count;
            if (Staged > 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "stage.multiple", Staged, Stage, Scp409Infection.Stages), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "infect.none"));
            }
        }
        return Count;
    }

    private static int status(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        int Shown = 0;
        for (Entity Candidate : Targets) {
            if (Shown >= StatusLines) {
                break;
            }
            Shown++;
            if (!(Candidate instanceof LivingEntity Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "not_living", Candidate.getDisplayName()), false);
                continue;
            }
            if (Scp409Infection.isInfected(Target)) {
                int Elapsed = Scp409Infection.elapsed(Target);
                int Stage = Scp409Infection.stageOf(Target, Elapsed);
                int Grown = Math.round(Scp409Infection.growth(Target, Elapsed) * 100.0f);
                int Slower = (int) Math.round(Scp409Infection.slowFraction(Target, Elapsed) * 100.0);
                int Seconds = Math.max(0, Scp409Infection.finalStart(Target) - Elapsed) / 20;
                String Key = Scp409Infection.isPaused(Target) ? "status.paused" : "status.infected";
                Source.sendSuccess(() -> Component.translatable(Prefix + Key, Target.getDisplayName(), Stage, Scp409Infection.Stages, Grown, Slower, Seconds), false);
                Count++;
            } else if (Scp409Exposure.isExempt(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "exempt", Target.getDisplayName()), false);
            } else if (Scp409Exposure.isProtected(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.protected", Target.getDisplayName()), false);
            } else {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.clear", Target.getDisplayName()), false);
            }
        }
        int Hidden = Targets.size() - Shown;
        if (Hidden > 0) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.more", Hidden), false);
        }
        return Count;
    }
}
