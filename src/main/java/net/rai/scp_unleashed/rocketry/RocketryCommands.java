package net.rai.scp_unleashed.rocketry;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.GameProfileCache;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteNetwork;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class RocketryCommands {
    public static final long DayTicks = 24000L;
    private static final SuggestionProvider<CommandSourceStack> Names = (Context, Builder) -> SharedSuggestionProvider.suggest(
            SatelliteNetwork.get(Context.getSource().getServer()).all().stream().map(SatelliteNetwork.Satellite::Name), Builder);

    private RocketryCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("satellite")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(Context -> list(Context.getSource())))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests(Names)
                                        .executes(Context -> remove(Context.getSource(), StringArgumentType.getString(Context, "name")))))));
    }

    public static String ownerName(MinecraftServer Server, UUID Owner) {
        if (Util.NIL_UUID.equals(Owner)) {
            return "-";
        }
        GameProfileCache Profiles = Server.getProfileCache();
        Optional<GameProfile> Found = Profiles == null ? Optional.empty() : Profiles.get(Owner);
        return Found.map(GameProfile::getName).orElse(Owner.toString());
    }

    private static int list(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        List<SatelliteNetwork.Satellite> All = SatelliteNetwork.get(Server).all();
        if (All.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.satellite.none").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.satellite.header", All.size()).withStyle(ChatFormatting.AQUA), false);
        for (SatelliteNetwork.Satellite Entry : All) {
            long Day = Entry.DeployedAt() / DayTicks + 1;
            Component Line = Component.translatable("commands.scp_unleashed.satellite.entry", Component.literal(Entry.Name()).withStyle(ChatFormatting.WHITE),
                    ownerName(Server, Entry.Owner()), Day).withStyle(ChatFormatting.GRAY);
            Source.sendSuccess(() -> Line, false);
        }
        return All.size();
    }

    private static int remove(CommandSourceStack Source, String Name) {
        Optional<SatelliteNetwork.Satellite> Removed = SatelliteNetwork.get(Source.getServer()).removeNamed(Name);
        if (Removed.isEmpty()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.satellite.unknown", Name));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.satellite.removed", Removed.get().Name()), true);
        return 1;
    }
}
