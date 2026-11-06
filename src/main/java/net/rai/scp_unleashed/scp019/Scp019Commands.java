package net.rai.scp_unleashed.scp019;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp019Commands {
    public static final double SearchRange = 48.0;
    public static final double AimReach = 8.0;
    public static final int ChunkReach = 3;

    private Scp019Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp019")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("open").executes(Context -> open(Context.getSource())))
                        .then(Commands.literal("close").executes(Context -> close(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    @Nullable
    private static Scp019PotBlockEntity aimedPot(CommandSourceStack Source) {
        Entity Looker = Source.getEntity();
        if (Looker == null) {
            return null;
        }
        HitResult Hit = Looker.pick(AimReach, 1.0f, false);
        if (!(Hit instanceof BlockHitResult BlockHit) || Hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockState State = Source.getLevel().getBlockState(BlockHit.getBlockPos());
        if (!(State.getBlock() instanceof Scp019PotBlock)) {
            return null;
        }
        return Source.getLevel().getBlockEntity(Scp019PotBlock.lowerPos(State, BlockHit.getBlockPos())) instanceof Scp019PotBlockEntity Pot ? Pot : null;
    }

    @Nullable
    private static Scp019PotBlockEntity nearestPot(ServerLevel World, Vec3 From) {
        ChunkPos Centre = new ChunkPos(BlockPos.containing(From));
        Scp019PotBlockEntity Best = null;
        double BestDistance = SearchRange * SearchRange;
        for (int Dx = -ChunkReach; Dx <= ChunkReach; Dx++) {
            for (int Dz = -ChunkReach; Dz <= ChunkReach; Dz++) {
                if (!World.hasChunk(Centre.x + Dx, Centre.z + Dz)) {
                    continue;
                }
                LevelChunk Chunk = World.getChunk(Centre.x + Dx, Centre.z + Dz);
                for (BlockEntity Found : Chunk.getBlockEntities().values()) {
                    if (Found instanceof Scp019PotBlockEntity Pot) {
                        double Distance = Vec3.atCenterOf(Pot.getBlockPos()).distanceToSqr(From);
                        if (Distance < BestDistance) {
                            Best = Pot;
                            BestDistance = Distance;
                        }
                    }
                }
            }
        }
        return Best;
    }

    @Nullable
    private static Scp019PotBlockEntity findPot(CommandSourceStack Source) {
        Scp019PotBlockEntity Aimed = aimedPot(Source);
        if (Aimed != null) {
            return Aimed;
        }
        Scp019PotBlockEntity Near = nearestPot(Source.getLevel(), Source.getPosition());
        if (Near == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp019.none", (int) SearchRange));
        }
        return Near;
    }

    private static int open(CommandSourceStack Source) {
        Scp019PotBlockEntity Pot = findPot(Source);
        if (Pot == null) {
            return 0;
        }
        BlockPos Pos = Pot.getBlockPos();
        if (Pot.isOpen()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp019.already_open", Pos.getX(), Pos.getY(), Pos.getZ()));
            return 0;
        }
        Pot.open();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp019.opened", Pos.getX(), Pos.getY(), Pos.getZ()), true);
        return 1;
    }

    private static int close(CommandSourceStack Source) {
        Scp019PotBlockEntity Pot = findPot(Source);
        if (Pot == null) {
            return 0;
        }
        BlockPos Pos = Pot.getBlockPos();
        if (!Pot.isOpen()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp019.already_closed", Pos.getX(), Pos.getY(), Pos.getZ()));
            return 0;
        }
        int Drones = Pot.droneCount();
        int Brutes = Pot.bruteCount();
        Pot.close();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp019.closed", Pos.getX(), Pos.getY(), Pos.getZ(), Drones, Brutes), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        Scp019PotBlockEntity Pot = findPot(Source);
        if (Pot == null) {
            return 0;
        }
        BlockPos Pos = Pot.getBlockPos();
        if (!Pot.isOpen()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp019.status.sealed", Pos.getX(), Pos.getY(), Pos.getZ()), false);
            return 1;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp019.status.open", Pos.getX(), Pos.getY(), Pos.getZ(), Pot.droneCount(),
                Scp019PotBlockEntity.MaxDrones, Pot.bruteCount(), Scp019PotBlockEntity.MaxBrutes, Pot.reforming()), false);
        if (Pot.alarmed()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp019.status.alarm", Pot.alarmSeconds()), false);
        }
        Scp019SearchMap Survey = Pot.map();
        if (Survey != null) {
            Component Mapping = Component.translatable(Survey.flooded() ? "commands.scp_unleashed.scp019.status.mapped" : "commands.scp_unleashed.scp019.status.mapping");
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp019.status.search", Math.round(Survey.progress() * 100.0f), Survey.size(), Mapping,
                    Survey.sweeps()), false);
        }
        return 1;
    }
}
