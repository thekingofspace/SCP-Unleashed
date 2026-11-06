package net.rai.scp_unleashed.scp008;

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
public final class Scp008Commands {
    public static final String Prefix = "commands.scp_unleashed.scp008.";
    public static final int ListedRisen = 8;

    private Scp008Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp008")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("infect", Scp008Commands::infect))
                        .then(targeted("cure", Scp008Commands::cure))
                        .then(targeted("query", Scp008Commands::query))
                        .then(Commands.literal("count").executes(Context -> count(Context.getSource())))));
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
            if (Scp008Infection.infect(Target)) {
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
            if (Scp008Infection.cure(Target)) {
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
            boolean Carrying = Scp008Infection.isInfected(Target);
            Source.sendSuccess(() -> Component.translatable(Prefix + (Carrying ? "query.positive" : "query.negative"), Target.getDisplayName()), false);
            if (Carrying) {
                Positive++;
            }
        }
        return Positive;
    }

    private static int count(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        List<? extends Scp008Zombie> Found = World.getEntities(Scp008Registry.ZombieType.get(), Risen -> Risen.isAlive());
        Source.sendSuccess(() -> Component.translatable(Prefix + "count", Found.size(), Scp008Events.ZombieCap), false);
        for (Scp008Zombie Risen : Found.subList(0, Math.min(ListedRisen, Found.size()))) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "count.entry", Risen.getDisplayName(), (Risen.getRemaining() + 19) / 20,
                    Risen.blockPosition().toShortString()), false);
        }
        return Found.size();
    }
}
