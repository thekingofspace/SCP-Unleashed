package net.rai.scp_unleashed.scp012;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp012Commands {
    public static final String Prefix = "commands.scp_unleashed.scp012.";

    private Scp012Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp012")
                        .requires(Source -> Source.hasPermission(2))
                        .then(targeted("claim", Scp012Commands::claim))
                        .then(targeted("release", Scp012Commands::release))
                        .then(targeted("query", Scp012Commands::query))
                        .then(Commands.literal("meter")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, Scp012CompositionItem.Complete))
                                        .executes(Context -> meter(Context.getSource(), IntegerArgumentType.getInteger(Context, "value")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> targeted(String Name, BiFunction<CommandSourceStack, Collection<ServerPlayer>, Integer> Action) {
        return Commands.literal(Name)
                .executes(Context -> Action.apply(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> Action.apply(Context.getSource(), EntityArgument.getPlayers(Context, "targets"))));
    }

    private static int claim(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp012Composer.claim(Target)) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "claim.success", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "claim.already", Target.getDisplayName()));
            }
        }
        return Count;
    }

    private static int release(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (Scp012Composer.release(Target)) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "release.success", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "release.none", Target.getDisplayName()));
            }
        }
        return Count;
    }

    private static int query(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        for (ServerPlayer Target : Targets) {
            String Key = Scp012Composer.isComposer(Target) ? "query.composer" : "query.clear";
            Component Exposure = Component.translatable(Prefix + (Scp012Exposure.isExposed(Target) ? "query.exposed" : "query.safe"));
            Source.sendSuccess(() -> Component.translatable(Prefix + Key, Target.getDisplayName(), Exposure), false);
        }
        return Targets.size();
    }

    private static int meter(CommandSourceStack Source, int Value) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ItemStack Held = User.getItemInHand(InteractionHand.MAIN_HAND);
        if (!Scp012CompositionItem.is(Held)) {
            Source.sendFailure(Component.translatable(Prefix + "meter.not_held"));
            return 0;
        }
        Scp012CompositionItem.setCompleteness(Held, Value);
        Source.sendSuccess(() -> Component.translatable(Prefix + "meter.success", Value), true);
        return Value;
    }
}
