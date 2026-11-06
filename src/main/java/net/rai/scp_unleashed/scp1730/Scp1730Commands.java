package net.rai.scp_unleashed.scp1730;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayDestination;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.gateway.GatewayStash;
import net.rai.scp_unleashed.gateway.GatewayTravel;

public final class Scp1730Commands {
    private Scp1730Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp1730")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("enter").executes(Scp1730Commands::enter))
                        .then(Commands.literal("leave").executes(Scp1730Commands::leave))
                        .then(Commands.literal("state").executes(Scp1730Commands::state))
                        .then(Commands.literal("reset").executes(Scp1730Commands::reset))
                        .then(Commands.literal("unseal").then(Commands.argument("player", EntityArgument.player()).executes(Scp1730Commands::unseal)))
                        .then(Commands.literal("open").then(Commands.argument("zone", IntegerArgumentType.integer(0, Scp1730State.SealedZones - 1))
                                .executes(Scp1730Commands::open)))
                        .then(Commands.literal("power").then(Commands.argument("zone", IntegerArgumentType.integer(0, Scp1730State.ZoneCount - 1))
                                .then(Commands.argument("on", BoolArgumentType.bool()).executes(Scp1730Commands::power))))));
    }

    private static int enter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        GatewayDestination Destination = GatewayDestinations.get(Scp1730World.standingIn() ? Scp1730Destination.StandinId : Scp1730Destination.Id);
        if (Destination == null) {
            return 0;
        }
        GatewayStash.Spot Back = new GatewayStash.Spot(User.level().dimension(), User.position(), User.getYRot());
        boolean Done = GatewayTravel.enter(User, Destination, Back);
        Context.getSource().sendSuccess(() -> Component.literal(Done ? "Entered SCP-1730." : "Could not enter SCP-1730."), true);
        return Done ? 1 : 0;
    }

    private static int leave(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp1730World.inside(User)) {
            return 0;
        }
        Scp1730Symbols.release(User);
        GatewayTravel.leave(User);
        return 1;
    }

    private static int state(CommandContext<CommandSourceStack> Context) {
        Scp1730State State = Scp1730State.get(Context.getSource().getServer());
        Context.getSource().sendSuccess(() -> Component.literal("run " + State.run() + " active " + State.active() + " power " + State.powered(0) + "/" + State.powered(1) + "/"
                + State.powered(2) + "/" + State.powered(3) + " open " + State.open(0) + "/" + State.open(1) + "/" + State.open(2) + " guardian dead "
                + State.guardianDead() + " collapsing " + State.collapsing()), false);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> Context) {
        Scp1730State.get(Context.getSource().getServer()).endRun();
        Context.getSource().sendSuccess(() -> Component.literal("SCP-1730 run ended; the next entry starts a fresh facility."), true);
        return 1;
    }

    private static int unseal(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = EntityArgument.getPlayer(Context, "player");
        Scp1730Destination.unseal(User);
        Context.getSource().sendSuccess(() -> Component.literal("SCP-1730 lockout cleared for " + User.getScoreboardName()), true);
        return 1;
    }

    private static int open(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = Scp1730World.level(Context.getSource().getServer());
        Scp1730State State = Scp1730State.get(Context.getSource().getServer());
        if (World == null || !State.active()) {
            return 0;
        }
        Scp1730Director.openSeal(World, State.run(), IntegerArgumentType.getInteger(Context, "zone"));
        return 1;
    }

    private static int power(CommandContext<CommandSourceStack> Context) {
        Scp1730State.get(Context.getSource().getServer()).setPower(IntegerArgumentType.getInteger(Context, "zone"), BoolArgumentType.getBool(Context, "on"));
        return 1;
    }
}
