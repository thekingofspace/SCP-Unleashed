package net.rai.scp_unleashed.scp207;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class Scp207Commands {
    public static final String Prefix = "commands.scp_unleashed.scp207.";
    public static final double LookReach = 8.0;

    private Scp207Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp207")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context, Context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(Context -> status(Context, EntityArgument.getPlayer(Context, "player")))))
                        .then(Commands.literal("toll")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 1.0f))
                                        .executes(Context -> change(Context, Current -> new Scp207Toll.State(Current.Doses(), FloatArgumentType.getFloat(Context, "value"),
                                                Current.Calm(), Current.Fade(), Current.Primed(), Current.Fuse(), Current.Recent())))))
                        .then(Commands.literal("doses")
                                .then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                                        .executes(Context -> change(Context, Current -> new Scp207Toll.State(IntegerArgumentType.getInteger(Context, "count"), Current.Toll(),
                                                0, 0, Current.Primed(), Current.Fuse(), Current.Recent())))))
                        .then(Commands.literal("prime")
                                .executes(Context -> change(Context, Current -> new Scp207Toll.State(Current.Doses(), Current.Toll(), Current.Calm(), Current.Fade(),
                                        Scp207Toll.PrimeTicks, Current.Fuse(), Current.Recent()))))
                        .then(Commands.literal("clear")
                                .executes(Scp207Commands::clear))
                        .then(Commands.literal("ration")
                                .then(Commands.argument("taken", IntegerArgumentType.integer(0, Scp207Ration.Daily))
                                        .executes(Scp207Commands::ration)))));
    }

    private static int status(CommandContext<CommandSourceStack> Context, ServerPlayer User) {
        Scp207Toll.State Current = Scp207Toll.stateOf(User);
        int Percent = Math.round(Current.Toll() * 100.0f);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "status", User.getDisplayName(), Current.Doses(), Percent,
                Current.Primed() / 20, Current.Fuse(), crateLeft(User), Scp207Ration.Daily), false);
        return Current.Doses();
    }

    private static int change(CommandContext<CommandSourceStack> Context, Function<Scp207Toll.State, Scp207Toll.State> Edit) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp207Toll.set(User, Edit.apply(Scp207Toll.stateOf(User)));
        return status(Context, User);
    }

    private static int clear(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp207Toll.clear(User);
        return status(Context, User);
    }

    private static int ration(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Scp207CrateBlockEntity Crate = lookedAt(User);
        if (Crate == null) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "no_crate"));
            return 0;
        }
        Crate.setTaken(IntegerArgumentType.getInteger(Context, "taken"));
        return status(Context, User);
    }

    private static int crateLeft(ServerPlayer User) {
        Scp207CrateBlockEntity Crate = lookedAt(User);
        return Crate == null ? Scp207Ration.Daily : Crate.left();
    }

    @Nullable
    private static Scp207CrateBlockEntity lookedAt(ServerPlayer User) {
        HitResult Hit = User.pick(LookReach, 1.0f, false);
        if (Hit instanceof BlockHitResult Aimed && Hit.getType() == HitResult.Type.BLOCK
                && User.level().getBlockEntity(Aimed.getBlockPos()) instanceof Scp207CrateBlockEntity Crate) {
            return Crate;
        }
        return null;
    }
}
