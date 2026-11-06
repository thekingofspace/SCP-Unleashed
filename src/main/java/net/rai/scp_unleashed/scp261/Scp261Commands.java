package net.rai.scp_unleashed.scp261;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp261Commands {
    public static final String Prefix = "commands.scp_unleashed.scp261.";
    public static final int SearchRadius = 12;

    private Scp261Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("scp261")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("vend")
                                .executes(Context -> vend(Context.getSource(), false))
                                .then(Commands.literal("gold").executes(Context -> vend(Context.getSource(), true))))
                        .then(Commands.literal("snacks").executes(Context -> snacks(Context.getSource())))
                        .then(Commands.literal("arrive").executes(Context -> arrive(Context.getSource())))
                        .then(Commands.literal("pals").executes(Context -> pals(Context.getSource())))));
    }

    @Nullable
    private static Scp261VendingBlockEntity nearest(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        BlockPos Centre = BlockPos.containing(Source.getPosition());
        Scp261VendingBlockEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Spot : BlockPos.betweenClosed(Centre.offset(-SearchRadius, -4, -SearchRadius), Centre.offset(SearchRadius, 4, SearchRadius))) {
            if (World.getBlockEntity(Spot) instanceof Scp261VendingBlockEntity Machine) {
                double Distance = Spot.distSqr(Centre);
                if (Distance < BestDistance) {
                    BestDistance = Distance;
                    Best = Machine;
                }
            }
        }
        return Best;
    }

    private static int vend(CommandSourceStack Source, boolean Gold) {
        Scp261VendingBlockEntity Machine = nearest(Source);
        if (Machine == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        if (!Machine.start(Gold, Scp261Odds.roll(Source.getLevel().getRandom(), Gold))) {
            Source.sendFailure(Component.translatable(Prefix + "busy"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "vend", Machine.getBlockPos().toShortString()), true);
        return 1;
    }

    private static int snacks(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        for (DeferredItem<Scp261SnackItem> Treat : Scp261Registry.snacks()) {
            ItemHandlerHelper.giveItemToPlayer(User, new ItemStack(Treat.get()));
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "snacks", Scp261Registry.snacks().size()), true);
        return Scp261Registry.snacks().size();
    }

    private static int arrive(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        Optional<BlockPos> Spot = Scp261ArrivalCondition.findSpot(World, User, 3, 10, false, false, User.getRandom());
        if (Spot.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "arrive.failed"));
            return 0;
        }
        Direction Front = Scp261ArrivalCondition.facingFor(World, Spot.get(), User.position());
        if (!Scp261ArrivalCondition.place(World, Spot.get(), Front)) {
            Source.sendFailure(Component.translatable(Prefix + "arrive.failed"));
            return 0;
        }
        BlockState Placed = World.getBlockState(Spot.get());
        Source.sendSuccess(() -> Component.translatable(Prefix + "arrive", Spot.get().toShortString(), Placed.getValue(Scp261VendingBlock.Facing).getName()), true);
        return 1;
    }

    private static int pals(CommandSourceStack Source) throws CommandSyntaxException {
        ServerPlayer User = Source.getPlayerOrException();
        int Count = Scp261Pals.palsOf(User.serverLevel(), User).size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "pals", Count, Scp261Pals.MaxPerOwner), false);
        return Count;
    }
}
