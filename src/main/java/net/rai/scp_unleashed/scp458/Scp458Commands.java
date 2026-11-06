package net.rai.scp_unleashed.scp458;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class Scp458Commands {
    public static final String Prefix = "commands.scp_unleashed.scp458.";

    private Scp458Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp458")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("pizza")
                                .executes(Context -> show(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> show(Context.getSource(), EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("pizza", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Scp458Pizza.values()).map(Scp458Pizza::id), Builder))
                                        .executes(Context -> set(Context, List.of(Context.getSource().getEntityOrException())))
                                        .then(Commands.argument("targets", EntityArgument.entities())
                                                .executes(Context -> set(Context, EntityArgument.getEntities(Context, "targets"))))))
                        .then(Commands.literal("clear")
                                .executes(Context -> clear(Context.getSource(), List.of(Context.getSource().getEntityOrException())))
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> clear(Context.getSource(), EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("slice")
                                .then(Commands.argument("pizza", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Scp458Pizza.values()).map(Scp458Pizza::id), Builder))
                                        .executes(Context -> slice(Context, List.of(Context.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(Context -> slice(Context, EntityArgument.getPlayers(Context, "targets"))))))));
    }

    @Nullable
    private static Scp458Pizza named(CommandContext<CommandSourceStack> Context) {
        String Name = StringArgumentType.getString(Context, "pizza");
        Scp458Pizza Found = Scp458Pizza.byId(Name);
        if (Found == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "unknown", Name));
        }
        return Found;
    }

    private static int show(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Each : Targets) {
            if (!(Each instanceof LivingEntity Person)) {
                continue;
            }
            Scp458Pizza Known = Scp458Favourites.of(Person);
            Component Line = Known == null ? Component.translatable(Prefix + "pizza.none", Person.getDisplayName())
                    : Component.translatable(Prefix + "pizza.known", Person.getDisplayName(), Known.displayName());
            Source.sendSuccess(() -> Line, false);
            Count++;
        }
        return Count;
    }

    private static int set(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) throws CommandSyntaxException {
        Scp458Pizza Chosen = named(Context);
        if (Chosen == null) {
            return 0;
        }
        int Count = 0;
        for (Entity Each : Targets) {
            if (Each instanceof LivingEntity Person) {
                Scp458Favourites.set(Person, Chosen);
                Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "set", Person.getDisplayName(), Chosen.displayName()), true);
                Count++;
            }
        }
        return Count;
    }

    private static int clear(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Each : Targets) {
            if (Each instanceof LivingEntity Person && Scp458Favourites.clear(Person)) {
                Count++;
            }
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "clear", Total), true);
        return Count;
    }

    private static int slice(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        Scp458Pizza Chosen = named(Context);
        if (Chosen == null) {
            return 0;
        }
        for (ServerPlayer Target : Targets) {
            Scp458Box.give(Target, Scp458Box.slice(Chosen));
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "slice", Target.getDisplayName(), Chosen.displayName()), true);
        }
        return Targets.size();
    }
}
