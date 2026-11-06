package net.rai.scp_unleashed.scp217;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp217Commands {
    public static final String Prefix = "commands.scp_unleashed.scp217.";
    public static final int MaxHaste = 2400;

    private Scp217Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp217")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("infect", Scp217Commands::infect))
                        .then(targeted("perfect", Scp217Commands::perfect))
                        .then(targeted("cure", Scp217Commands::cure))
                        .then(targeted("query", Scp217Commands::query))
                        .then(Commands.literal("hasten")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MaxHaste))
                                        .executes(Context -> hasten(Context.getSource(), List.of(Context.getSource().getEntityOrException()),
                                                IntegerArgumentType.getInteger(Context, "seconds")))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(Context -> hasten(Context.getSource(), EntityArgument.getEntities(Context, "targets"),
                                                        IntegerArgumentType.getInteger(Context, "seconds"))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<? extends Entity>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getEntities(Context, "targets"))));
    }

    private static List<LivingEntity> living(Collection<? extends Entity> Targets) {
        List<LivingEntity> Found = new ArrayList<>();
        for (Entity Target : Targets) {
            if (Target instanceof LivingEntity Subject) {
                Found.add(Subject);
            }
        }
        return Found;
    }

    private static int infect(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Changed = 0;
        for (LivingEntity Target : living(Targets)) {
            if (Scp217Clockwork.infect(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "infect.success", Target.getDisplayName()), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "infect.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int perfect(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Changed = 0;
        for (LivingEntity Target : living(Targets)) {
            if (Scp217Clockwork.perfect(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "perfect.success", Target.getDisplayName()), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "perfect.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int cure(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Changed = 0;
        for (LivingEntity Target : living(Targets)) {
            if (Scp217Clockwork.cure(Target)) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "cure.success", Target.getDisplayName()), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }

    private static int query(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Positive = 0;
        for (LivingEntity Target : living(Targets)) {
            if (Scp217Clockwork.isPerfect(Target)) {
                int Stage = Scp217Clockwork.stageOf(Scp217Clockwork.elapsed(Target));
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.perfect", Target.getDisplayName(), Stage, Scp217Clockwork.Stages), false);
                Positive++;
            } else if (Scp217Clockwork.isInfected(Target)) {
                int Stage = Scp217Clockwork.stageOf(Scp217Clockwork.elapsed(Target));
                int Seconds = (Scp217Clockwork.remaining(Target) + 19) / 20;
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.positive", Target.getDisplayName(), Stage, Scp217Clockwork.Stages, Seconds), false);
                Positive++;
            } else {
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.negative", Target.getDisplayName()), false);
            }
        }
        return Positive;
    }

    private static int hasten(CommandSourceStack Source, Collection<? extends Entity> Targets, int Seconds) {
        int Changed = 0;
        for (LivingEntity Target : living(Targets)) {
            if (Scp217Clockwork.hasten(Target, Seconds * 20)) {
                int Left = (Math.max(0, Scp217Clockwork.limit(Target) - Scp217Clockwork.elapsed(Target)) + 19) / 20;
                Source.sendSuccess(() -> Component.translatable(Prefix + "hasten.success", Target.getDisplayName(), Left), true);
                Changed++;
            } else {
                Source.sendFailure(Component.translatable(Prefix + "cure.failed", Target.getDisplayName()));
            }
        }
        return Changed;
    }
}
