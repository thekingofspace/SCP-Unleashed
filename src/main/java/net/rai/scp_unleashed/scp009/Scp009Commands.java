package net.rai.scp_unleashed.scp009;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
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
public final class Scp009Commands {
    public static final String Prefix = "commands.scp_unleashed.scp009.";
    public static final int QueryLines = 12;

    private Scp009Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp009")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("doom", Scp009Commands::doom))
                        .then(targeted("clear", Scp009Commands::clear))
                        .then(targeted("query", Scp009Commands::query))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<? extends Entity>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getEntities(Context, "targets"))));
    }

    private static int doom(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        boolean Single = Targets.size() == 1;
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                }
                continue;
            }
            if (Scp009Doom.isExempt(Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "exempt", Target.getDisplayName()));
                }
            } else if (Scp009Doom.isDoomed(Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "doom.already", Target.getDisplayName()));
                }
            } else if (Scp009Doom.doom(Target)) {
                if (Single) {
                    Source.sendSuccess(() -> Component.translatable(Prefix + "doom.success", Target.getDisplayName(), Scp009Doom.TotalTicks / 20), true);
                }
                Count++;
            } else if (Single) {
                Source.sendFailure(Component.translatable(Prefix + "doom.failed", Target.getDisplayName()));
            }
        }
        if (!Single) {
            int Doomed = Count;
            if (Doomed > 0) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "doom.multiple", Doomed, Scp009Doom.TotalTicks / 20), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "doom.none"));
            }
        }
        return Count;
    }

    private static int clear(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        boolean Single = Targets.size() == 1;
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target)) {
                if (Single) {
                    Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                }
                continue;
            }
            if (Scp009Doom.clear(Target)) {
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

    private static int query(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        int Shown = 0;
        for (Entity Candidate : Targets) {
            if (Shown >= QueryLines) {
                break;
            }
            Shown++;
            if (!(Candidate instanceof LivingEntity Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "not_living", Candidate.getDisplayName()), false);
                continue;
            }
            MobEffectInstance Instance = Scp009Doom.instance(Target);
            if (Instance != null) {
                float Progress = Scp009Doom.progress(Instance);
                int Seconds = Instance.isInfiniteDuration() ? -1 : (Instance.getDuration() + 19) / 20;
                int Stage = Scp009Doom.stage(Progress);
                int Slower = (int) Math.round(Scp009Doom.slowFraction(Progress) * 100.0);
                Source.sendSuccess(() -> Seconds < 0
                        ? Component.translatable(Prefix + "query.doomed_forever", Target.getDisplayName(), Stage, Scp009Doom.Stages, Slower)
                        : Component.translatable(Prefix + "query.doomed", Target.getDisplayName(), Seconds, Stage, Scp009Doom.Stages, Slower), false);
                Count++;
            } else if (Scp009Doom.isExempt(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "exempt", Target.getDisplayName()), false);
            } else if (Scp009Doom.isProtected(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.protected", Target.getDisplayName()), false);
            } else {
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.clear", Target.getDisplayName()), false);
            }
        }
        int Hidden = Targets.size() - Shown;
        if (Hidden > 0) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "query.more", Hidden), false);
        }
        return Count;
    }
}
