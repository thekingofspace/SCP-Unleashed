package net.rai.scp_unleashed.scp427;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp427Commands {
    public static final String Prefix = "commands.scp_unleashed.scp427.";
    public static final int MaxSeconds = 600;

    private Scp427Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp427")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("exposure")
                                .executes(Context -> status(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, MaxSeconds))
                                                .executes(Context -> set(Context.getSource(), List.of(Context.getSource().getEntityOrException()),
                                                        IntegerArgumentType.getInteger(Context, "seconds")))))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> status(Context.getSource(), EntityArgument.getEntities(Context, "targets")))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("seconds", IntegerArgumentType.integer(0, MaxSeconds))
                                                        .executes(Context -> set(Context.getSource(), EntityArgument.getEntities(Context, "targets"),
                                                                IntegerArgumentType.getInteger(Context, "seconds")))))))
                        .then(Commands.literal("transform")
                                .executes(Context -> transform(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> transform(Context.getSource(), EntityArgument.getEntities(Context, "targets")))))));
    }

    private static int status(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target)) {
                Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                continue;
            }
            int Ticks = Scp427Exposure.exposure(Target);
            String Key = Scp427Exposure.isTransforming(Target) ? "status.transforming" : Scp427Exposure.isOpen(Target) ? "status.open" : "status.closed";
            Source.sendSuccess(() -> Component.translatable(Prefix + Key, Target.getDisplayName(), Ticks / 20, Scp427Exposure.Limit / 20,
                    Scp427Exposure.stageOf(Ticks)), false);
            Count++;
        }
        return Count;
    }

    private static int set(CommandSourceStack Source, Collection<? extends Entity> Targets, int Seconds) {
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target)) {
                Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                continue;
            }
            if (Scp427Exposure.isTransforming(Target)) {
                Source.sendFailure(Component.translatable(Prefix + "busy", Target.getDisplayName()));
                continue;
            }
            Scp427Exposure.setExposure(Target, Seconds * 20);
            Source.sendSuccess(() -> Component.translatable(Prefix + "set", Target.getDisplayName(), Seconds), true);
            Count++;
        }
        return Count;
    }

    private static int transform(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Candidate : Targets) {
            if (!(Candidate instanceof LivingEntity Target) || !(Target.level() instanceof ServerLevel World)) {
                Source.sendFailure(Component.translatable(Prefix + "not_living", Candidate.getDisplayName()));
                continue;
            }
            if (Scp427Exposure.isExempt(Target) || !Target.isAlive()) {
                Source.sendFailure(Component.translatable(Prefix + "exempt", Target.getDisplayName()));
                continue;
            }
            if (Scp427Exposure.isTransforming(Target)) {
                Source.sendFailure(Component.translatable(Prefix + "busy", Target.getDisplayName()));
                continue;
            }
            Scp427State State = Target.getData(Scp427Registry.Exposure.get());
            State.Ticks = Scp427Exposure.Limit + 1;
            Scp427Transformation.begin(World, Target, State);
            Source.sendSuccess(() -> Component.translatable(Prefix + "transform", Target.getDisplayName()), true);
            Count++;
        }
        return Count;
    }
}
