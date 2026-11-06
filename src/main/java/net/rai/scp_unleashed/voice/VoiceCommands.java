package net.rai.scp_unleashed.voice;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public final class VoiceCommands {
    public static final String Prefix = "commands.scp_unleashed.voice.";

    private VoiceCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("voice");
        Root.then(Commands.literal("forgetme").executes(Context -> forgetMe(Context.getSource())));
        Root.then(Commands.literal("status").requires(Source -> Source.hasPermission(2)).executes(Context -> status(Context.getSource())));
        Root.then(Commands.literal("clear").requires(Source -> Source.hasPermission(2))
                .executes(Context -> clearAll(Context.getSource()))
                .then(Commands.literal("player").then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(Context -> clearPlayers(Context.getSource(), GameProfileArgument.getGameProfiles(Context, "player")))))
                .then(Commands.argument("anomaly", StringArgumentType.word())
                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(known(), Builder))
                        .executes(Context -> clearAnomaly(Context.getSource(), StringArgumentType.getString(Context, "anomaly")))));
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    private static List<String> known() {
        TreeSet<String> Names = new TreeSet<>(VoicePolicy.registered());
        Names.addAll(VoiceArchive.anomalies());
        return new ArrayList<>(Names);
    }

    public static String size(long Bytes) {
        if (Bytes < 1024L) {
            return Bytes + " B";
        }
        if (Bytes < 1024L * 1024L) {
            return String.format(Locale.ROOT, "%.1f KB", Bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", Bytes / (1024.0 * 1024.0));
    }

    private static int forgetMe(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        int Count = VoiceArchive.forget(User.getUUID());
        Source.sendSuccess(() -> Component.translatable(Prefix + "forgot", Count).withStyle(ChatFormatting.GRAY), false);
        return Count;
    }

    private static int clearAll(CommandSourceStack Source) {
        int Count = VoiceArchive.clearAll();
        Source.sendSuccess(() -> Component.translatable(Prefix + "cleared_all", Count), true);
        return Count;
    }

    private static int clearAnomaly(CommandSourceStack Source, String Anomaly) {
        if (!VoicePolicy.validId(Anomaly) || !VoiceArchive.knows(Anomaly)) {
            Source.sendFailure(Component.translatable(Prefix + "unknown", Anomaly));
            return 0;
        }
        int Count = VoiceArchive.clear(Anomaly);
        Source.sendSuccess(() -> Component.translatable(Prefix + "cleared_anomaly", Count, Anomaly), true);
        return Count;
    }

    private static int clearPlayers(CommandSourceStack Source, Collection<GameProfile> Profiles) {
        int Total = 0;
        for (GameProfile Profile : Profiles) {
            int Count = VoiceArchive.forget(Profile.getId());
            Total += Count;
            Source.sendSuccess(() -> Component.translatable(Prefix + "cleared_player", Count, Profile.getName()), true);
        }
        return Total;
    }

    private static int status(CommandSourceStack Source) {
        List<VoiceArchive.Status> Rows = VoiceArchive.status();
        int Clips = 0;
        long Bytes = 0L;
        for (VoiceArchive.Status Row : Rows) {
            Clips += Row.Clips();
            Bytes += Row.Bytes();
        }
        int ClipTotal = Clips;
        long ByteTotal = Bytes;
        Component State = Component.translatable(Prefix + (VoicePolicy.master() ? "status.on" : "status.off"))
                .withStyle(VoicePolicy.master() ? ChatFormatting.GREEN : ChatFormatting.GRAY);
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.header", State, ClipTotal, Rows.size(), size(ByteTotal)), false);
        if (Rows.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.empty").withStyle(ChatFormatting.GRAY), false);
        }
        for (VoiceArchive.Status Row : Rows) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.line", Row.Anomaly(), Row.Clips(), Row.Speakers(), size(Row.Bytes())).withStyle(ChatFormatting.GRAY), false);
        }
        return ClipTotal;
    }
}
