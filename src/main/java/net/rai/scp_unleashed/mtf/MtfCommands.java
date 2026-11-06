package net.rai.scp_unleashed.mtf;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class MtfCommands {
    public static final String Prefix = "commands.scp_unleashed.mtf.";
    private static final DynamicCommandExceptionType UnknownTier = new DynamicCommandExceptionType(Name -> Component.translatable(Prefix + "unknown_tier", Name));

    private MtfCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("mtf")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("call")
                                .then(Commands.argument("tier", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(MtfTier.values()).map(MtfTier::commandName), Builder))
                                        .executes(Context -> call(Context.getSource(), StringArgumentType.getString(Context, "tier"), List.of(Context.getSource().getPlayerOrException())))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(Context -> call(Context.getSource(), StringArgumentType.getString(Context, "tier"), EntityArgument.getPlayers(Context, "targets"))))))
                        .then(Commands.literal("dismiss")
                                .executes(Context -> dismiss(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> dismiss(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource())))));
    }

    private static int call(CommandSourceStack Source, String Name, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        MtfTier Tier = MtfTier.byName(Name);
        if (Tier == null) {
            throw UnknownTier.create(Name);
        }
        int Called = 0;
        for (ServerPlayer Target : Targets) {
            MtfMissions.Outcome Result = MtfMissions.call(Target, Tier);
            if (Result == MtfMissions.Outcome.Called) {
                Called++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "called", Tier.displayName(), Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "refused." + Result.name().toLowerCase(java.util.Locale.ROOT), Target.getDisplayName()));
            }
        }
        return Called;
    }

    private static int dismiss(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            if (MtfMissions.requestExtraction(Target)) {
                Count++;
                Source.sendSuccess(() -> Component.translatable(Prefix + "dismissed", Target.getDisplayName()), true);
            } else {
                Source.sendFailure(Component.translatable(Prefix + "no_squad", Target.getDisplayName()));
            }
        }
        return Count;
    }

    private static int status(CommandSourceStack Source) {
        Collection<MtfMission> All = MtfMissions.all();
        if (All.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.none"), false);
            return 0;
        }
        long Now = Source.getLevel().getGameTime();
        for (MtfMission Mission : All) {
            ServerPlayer Caller = Source.getServer().getPlayerList().getPlayer(Mission.caller());
            Component Name = Caller != null ? Caller.getDisplayName() : Component.literal(Mission.caller().toString());
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.line", Name, Mission.tier().displayName(),
                    Component.translatable(Prefix + "stage." + Mission.stage().name().toLowerCase(java.util.Locale.ROOT)), Mission.alive(), Mission.deployed(),
                    Mission.remaining(Now) / 20), false);
        }
        return All.size();
    }
}
