package net.rai.scp_unleashed.scp294;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Optional;
import java.util.stream.Collectors;

public final class Scp294Commands {
    public static final String Prefix = "commands.scp_unleashed.scp294.";

    private Scp294Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp294")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("reload").executes(Context -> reload(Context.getSource())))
                        .then(Commands.literal("list").executes(Context -> list(Context.getSource())))
                        .then(Commands.literal("dispense")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(Context -> dispense(Context.getSource(), StringArgumentType.getString(Context, "name")))))
                        .then(Commands.literal("arrive").executes(Context -> arrive(Context.getSource())))));
    }

    private static int reload(CommandSourceStack Source) {
        Scp294Resolver.forget();
        Scp294Config.Report Result = Scp294Config.load(Source.getServer());
        String Where = Source.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize()
                .relativize(Result.File().toAbsolutePath().normalize()).toString();
        if (Result.failed()) {
            Source.sendFailure(Component.translatable(Prefix + "reload.failed", Where, Result.Failure()));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "reload", Result.Count(), Where), true);
        if (!Result.Warnings().isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "reload.warnings", Result.Warnings().size()), false);
        }
        return Result.Count();
    }

    private static int list(CommandSourceStack Source) {
        if (Scp294Config.entries().isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "list.empty"), false);
            return 0;
        }
        String Names = Scp294Config.entries().stream().map(Scp294Config.Entry::Name).collect(Collectors.joining(", "));
        Source.sendSuccess(() -> Component.translatable(Prefix + "list", Scp294Config.entries().size(), Names), false);
        return Scp294Config.entries().size();
    }

    private static int dispense(CommandSourceStack Source, String Name) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        Scp294Resolver.Result Resolved = Scp294Resolver.resolve(Source.getServer(), Name);
        if (Resolved.refused()) {
            Source.sendFailure(Component.translatable(Prefix + "dispense.refused", Resolved.Typed()));
            return 0;
        }
        ItemHandlerHelper.giveItemToPlayer(User, Scp294CupItem.filled(Resolved.Drink()));
        Source.sendSuccess(() -> Component.translatable(Prefix + "dispense", Resolved.Drink().Title()), true);
        return 1;
    }

    private static int arrive(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        Optional<BlockPos> Spot = Scp294ArrivalCondition.findSpot(World, User, 3, 10, false, false, User.getRandom());
        if (Spot.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "arrive.failed"));
            return 0;
        }
        Direction Front = Scp294ArrivalCondition.facingFor(World, Spot.get(), User.position());
        if (!Scp294ArrivalCondition.place(World, Spot.get(), Front)) {
            Source.sendFailure(Component.translatable(Prefix + "arrive.failed"));
            return 0;
        }
        BlockState Placed = World.getBlockState(Spot.get());
        Source.sendSuccess(() -> Component.translatable(Prefix + "arrive", Spot.get().toShortString(), Placed.getValue(Scp294MachineBlock.Facing).getName()), true);
        return 1;
    }
}
