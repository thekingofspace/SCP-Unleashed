package net.rai.scp_unleashed.orbital;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public final class OrbitalCommands {
    public static final String Everything = "all";
    private static final SuggestionProvider<CommandSourceStack> PartNames = (Context, Builder) -> SharedSuggestionProvider.suggest(
            Stream.concat(Stream.of(Everything), Arrays.stream(OrbitalPart.values()).map(OrbitalPart::getSerializedName)), Builder);

    private OrbitalCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("orbital")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(Context -> status(Context.getSource(), EntityArgument.getPlayer(Context, "player")))))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("part", StringArgumentType.word())
                                                .suggests(PartNames)
                                                .executes(Context -> grant(Context.getSource(), EntityArgument.getPlayer(Context, "player"),
                                                        StringArgumentType.getString(Context, "part"))))))
                        .then(Commands.literal("recharge")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(Context -> recharge(Context.getSource(), EntityArgument.getPlayer(Context, "player")))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(Context -> clear(Context.getSource(), EntityArgument.getPlayer(Context, "player")))))));
    }

    private static int status(CommandSourceStack Source, ServerPlayer Target) {
        OrbitalArray Array = OrbitalArray.get(Source.getServer());
        int Count = Array.count(Target.getUUID());
        long Now = Source.getServer().overworld().getGameTime();
        long Left = Math.max(0L, (Array.readyAt(Target.getUUID()) - Now + 19L) / 20L);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.orbital.status", Target.getDisplayName(), Count, OrbitalPart.Count, Left,
                Array.shots(Target.getUUID())).withStyle(ChatFormatting.AQUA), false);
        return Count;
    }

    private static int grant(CommandSourceStack Source, ServerPlayer Target, String Wanted) throws CommandSyntaxException {
        OrbitalArray Array = OrbitalArray.get(Source.getServer());
        long Now = Source.getServer().overworld().getGameTime();
        int Added = 0;
        if (Everything.equals(Wanted)) {
            for (OrbitalPart Part : OrbitalPart.values()) {
                Added += Array.record(Target.getUUID(), Part, Now) ? 1 : 0;
            }
        } else {
            Optional<OrbitalPart> Part = OrbitalPart.byName(Wanted);
            if (Part.isEmpty()) {
                Source.sendFailure(Component.translatable("commands.scp_unleashed.orbital.unknown_part", Wanted));
                return 0;
            }
            Added += Array.record(Target.getUUID(), Part.get(), Now) ? 1 : 0;
        }
        int Total = Added;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.orbital.granted", Total, Target.getDisplayName()), true);
        OrbitalLaunch.sync(Target);
        return Total;
    }

    private static int recharge(CommandSourceStack Source, ServerPlayer Target) {
        OrbitalArray.get(Source.getServer()).recharge(Target.getUUID());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.orbital.recharged", Target.getDisplayName()), true);
        OrbitalLaunch.sync(Target);
        return 1;
    }

    private static int clear(CommandSourceStack Source, ServerPlayer Target) {
        boolean Removed = OrbitalArray.get(Source.getServer()).clear(Target.getUUID());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.orbital.cleared", Target.getDisplayName()), true);
        OrbitalLaunch.sync(Target);
        return Removed ? 1 : 0;
    }
}
