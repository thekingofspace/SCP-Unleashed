package net.rai.scp_unleashed.scp020;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp020Commands {
    public static final String Prefix = "commands.scp_unleashed.scp020.";

    private Scp020Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp020")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("afflict", Scp020Commands::afflict))
                        .then(targeted("cure", Scp020Commands::cure))
                        .then(targeted("query", Scp020Commands::query))
                        .then(Commands.literal("intensity")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, Scp020Affliction.MaxIntensity))
                                        .executes(Context -> intensity(Context.getSource(), List.of(Context.getSource().getPlayerOrException()),
                                                IntegerArgumentType.getInteger(Context, "value")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(Context -> intensity(Context.getSource(), EntityArgument.getPlayers(Context, "targets"),
                                                        IntegerArgumentType.getInteger(Context, "value"))))))
                        .then(Commands.literal("mold").executes(Context -> mold(Context.getSource())))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<ServerPlayer>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getPlayers(Context, "targets"))));
    }

    private static int afflict(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        for (ServerPlayer Target : Targets) {
            Scp020Affliction.afflict(Target);
            int Now = Scp020Affliction.intensity(Target);
            Source.sendSuccess(() -> Component.translatable(Prefix + "afflict.success", Target.getDisplayName(), Now), true);
        }
        return Targets.size();
    }

    private static int cure(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Changed = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp020Affliction.cure(Target)) {
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
            int Now = Scp020Affliction.intensity(Target);
            if (Now > 0) {
                int Seconds = Scp020Affliction.secondsLeft(Target);
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.positive", Target.getDisplayName(), Now, Scp020Affliction.MaxIntensity, Seconds), false);
                Positive++;
            } else {
                Source.sendSuccess(() -> Component.translatable(Prefix + "query.negative", Target.getDisplayName()), false);
            }
        }
        return Positive;
    }

    private static int intensity(CommandSourceStack Source, Collection<ServerPlayer> Targets, int Value) {
        for (ServerPlayer Target : Targets) {
            Scp020Affliction.setIntensity(Target, Value);
            Source.sendSuccess(() -> Component.translatable(Prefix + "intensity.success", Target.getDisplayName(), Value), true);
        }
        return Targets.size();
    }

    private static int mold(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        int Faces = Scp020MoldCensus.get(World).faces();
        int Limit = Scp020Registry.spreadLimit(World.getGameRules());
        int Chance = Scp020Registry.spreadChance(World.getGameRules());
        Source.sendSuccess(() -> Component.translatable(Prefix + "mold", Faces, Limit, Chance), false);
        return Faces;
    }
}
