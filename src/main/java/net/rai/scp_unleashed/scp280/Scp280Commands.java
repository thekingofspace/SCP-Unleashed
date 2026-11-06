package net.rai.scp_unleashed.scp280;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp280.network.Scp280EyesPayload;

import java.util.UUID;

public final class Scp280Commands {
    public static final String Prefix = "commands.scp_unleashed.scp280.";

    private Scp280Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp280")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status").executes(Scp280Commands::status))
                        .then(Commands.literal("unlock").executes(Scp280Commands::unlock))
                        .then(Commands.literal("meter")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100)).executes(Scp280Commands::meter)))
                        .then(Commands.literal("sight").executes(Scp280Commands::sight))
                        .then(Commands.literal("reveal").executes(Scp280Commands::reveal))
                        .then(Commands.literal("release").executes(Scp280Commands::release))
                        .then(Commands.literal("reset").executes(Scp280Commands::reset))));
    }

    private static int status(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp280World Data = Scp280World.get(User.server);
        Scp280Haunt.Haunt State = Scp280Haunt.haunt(User.getUUID());
        Component Coverage = Component.translatable(Prefix + (Data.isUnlocked() ? "world" : Data.covers(User.getUUID()) ? "personal" : "locked"));
        Component Held = Component.translatable(Prefix + (Data.isCaptured() ? "captured" : Data.looseActive(User.serverLevel().getGameTime()) ? "loose" : "unseen"));
        int Meter = Math.round(Data.meter(User.getUUID()));
        int Light = Scp280Light.of(User);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", Coverage, Meter, Light, State.darkSeconds(), Math.max(0, State.countdown()), Held,
                Data.reveals(), Data.captures()), false);
        return Meter;
    }

    private static int unlock(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp280Haunt.unlock(User);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "unlocked"), true);
        return 1;
    }

    private static int meter(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        int Value = IntegerArgumentType.getInteger(Context, "value");
        Scp280Haunt.setMeter(User, Value);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "meter", Value), true);
        return Value;
    }

    private static int sight(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp280Haunt.realm(User.level())) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "realm"));
            return 0;
        }
        int Pairs = Scp280Haunt.sight(User, Scp280World.get(User.server).meter(User.getUUID()));
        if (Pairs <= 0) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "no_spot"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "sighted", Pairs), false);
        return Pairs;
    }

    private static int reveal(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        if (!Scp280Haunt.realm(User.level())) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "realm"));
            return 0;
        }
        if (!Scp280Haunt.reveal(User)) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "no_reveal"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "revealed"), true);
        return 1;
    }

    private static int release(CommandContext<CommandSourceStack> Context) {
        Scp280World Data = Scp280World.get(Context.getSource().getServer());
        UUID Held = Data.held();
        if (!Data.isCaptured()) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "not_captured"));
            return 0;
        }
        ServerLevel World = Context.getSource().getServer().overworld();
        if (Held != null && World.getEntity(Held) instanceof Scp280Shade Shade) {
            Scp280Haunt.released(World, Shade, true);
        } else {
            Data.release(null);
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "released"), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> Context) {
        Scp280World Data = Scp280World.get(Context.getSource().getServer());
        Data.lockAll();
        Data.release(null);
        Scp280Haunt.reset();
        for (ServerPlayer Online : Context.getSource().getServer().getPlayerList().getPlayers()) {
            Scp280Haunt.send(Online, Scp280EyesPayload.clear());
            Scp280Haunt.sync(Online, Scp280Haunt.haunt(Online.getUUID()), 0.0f, true);
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "reset"), true);
        return 1;
    }
}
