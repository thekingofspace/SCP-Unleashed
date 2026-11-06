package net.rai.scp_unleashed.scp076;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp076Commands {
    public static final int PlaceAhead = 12;
    public static final double SearchRange = 96.0;

    private Scp076Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp076")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("vault")
                                .executes(Context -> vault(Context.getSource(), null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(Context -> vault(Context.getSource(), BlockPosArgument.getLoadedBlockPos(Context, "pos")))))
                        .then(Commands.literal("unseal").executes(Context -> unseal(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    private static int vault(CommandSourceStack Source, BlockPos Given) throws CommandSyntaxException {
        ServerLevel World = Source.getLevel();
        Vec3 From = Source.getPosition();
        BlockPos Centre = Given;
        if (Centre == null) {
            Entity Caller = Source.getEntity();
            Vec3 Ahead = Caller != null ? Caller.getLookAngle().multiply(1.0, 0.0, 1.0) : new Vec3(0.0, 0.0, 1.0);
            if (Ahead.lengthSqr() < 1.0e-4) {
                Ahead = new Vec3(0.0, 0.0, 1.0);
            }
            BlockPos Column = BlockPos.containing(From.add(Ahead.normalize().scale(PlaceAhead)));
            Centre = Column.atY(Scp076Vault.floorLevel(World, Column));
        }
        if (World.isOutsideBuildHeight(Centre) || World.isOutsideBuildHeight(Centre.above(Scp076Vault.Top))) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp076.no_room"));
            return 0;
        }
        Direction Front = Scp076Vault.facing(Centre, From);
        Scp076Vault.build(World, Centre, Front);
        BlockPos Placed = Centre;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp076.placed", Placed.getX(), Placed.getY(), Placed.getZ()), true);
        return 1;
    }

    private static Optional<Scp076DoorBlockEntity> nearest(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Optional<Scp076Vaults.Vault> Found = Scp076Vaults.get(World.getServer()).nearest(World.dimension(), BlockPos.containing(Source.getPosition()), SearchRange);
        if (Found.isEmpty() || !World.isLoaded(Found.get().Door()) || !(World.getBlockEntity(Found.get().Door()) instanceof Scp076DoorBlockEntity Door)) {
            return Optional.empty();
        }
        return Optional.of(Door);
    }

    private static int unseal(CommandSourceStack Source) {
        Optional<Scp076DoorBlockEntity> Door = nearest(Source);
        if (Door.isEmpty()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp076.none", (int) SearchRange));
            return 0;
        }
        Door.get().unseal();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp076.unsealed"), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        Optional<Scp076DoorBlockEntity> Door = nearest(Source);
        if (Door.isEmpty()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp076.none", (int) SearchRange));
            return 0;
        }
        Scp076DoorBlockEntity Found = Door.get();
        BlockPos Pos = Found.getBlockPos();
        Component Phase = Component.translatable("jade.scp_unleashed.scp076_door." + phaseKey(Found.phase()));
        long Left = Found.sealLeft();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp076.status", Pos.getX(), Pos.getY(), Pos.getZ(), Phase, Left / 20), false);
        return 1;
    }

    public static String phaseKey(byte Phase) {
        return switch (Phase) {
            case Scp076DoorBlockEntity.Unlocking -> "unlocking";
            case Scp076DoorBlockEntity.Opening -> "opening";
            case Scp076DoorBlockEntity.Open -> "open";
            case Scp076DoorBlockEntity.Closing -> "closing";
            case Scp076DoorBlockEntity.Locking -> "locking";
            default -> "sealed";
        };
    }
}
