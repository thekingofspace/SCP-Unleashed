package net.rai.scp_unleashed.space;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class SpaceCommands {
    private SpaceCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("space")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("moon")
                                .executes(Context -> travel(Context.getSource(), List.of(Context.getSource().getPlayerOrException()), SpaceRegistry.MoonLevel))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> travel(Context.getSource(), EntityArgument.getPlayers(Context, "targets"), SpaceRegistry.MoonLevel))))
                        .then(Commands.literal("earth")
                                .executes(Context -> travel(Context.getSource(), List.of(Context.getSource().getPlayerOrException()), Level.OVERWORLD))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> travel(Context.getSource(), EntityArgument.getPlayers(Context, "targets"), Level.OVERWORLD))))
                        .then(Commands.literal("fill")
                                .executes(Context -> fill(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> fill(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))));
    }

    private static int travel(CommandSourceStack Source, Collection<ServerPlayer> Targets, ResourceKey<Level> Destination) {
        ServerLevel World = Source.getServer().getLevel(Destination);
        if (World == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.space.missing", Destination.location().toString()));
            return 0;
        }
        for (ServerPlayer Target : Targets) {
            BlockPos Column = Destination == Level.OVERWORLD ? World.getSharedSpawnPos() : Target.blockPosition();
            World.getChunk(Column.getX() >> 4, Column.getZ() >> 4);
            int Surface = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Column.getX(), Column.getZ());
            Target.teleportTo(World, Column.getX() + 0.5, Surface + 0.1, Column.getZ() + 0.5, Set.of(), Target.getYRot(), Target.getXRot());
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.space.travel", Targets.size(), Destination.location().toString()), true);
        return Targets.size();
    }

    private static int fill(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        int Count = 0;
        for (ServerPlayer Target : Targets) {
            ItemStack Chest = Target.getItemBySlot(EquipmentSlot.CHEST);
            if (SpaceAir.hasTank(Chest)) {
                Chest.set(SpaceRegistry.SuitTank.get(), new SpaceSuitTank(true, AirCanisterItem.Capacity));
                Count++;
            }
        }
        int Filled = Count;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.space.filled", Filled), true);
        return Count;
    }
}
