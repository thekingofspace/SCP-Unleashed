package net.rai.scp_unleashed.scp003;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.stranger.StrangerEncounters;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp003Commands {
    public static final double SearchRange = 48.0;
    public static final int ChunkReach = 3;

    private Scp003Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp003")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("hatch")
                                .executes(Context -> hatch(Context.getSource())))
                        .then(Commands.literal("grow")
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, 4))
                                        .executes(Context -> grow(Context.getSource(), IntegerArgumentType.getInteger(Context, "stage")))))
                        .then(Commands.literal("pending")
                                .executes(Context -> pending(Context.getSource())))));
    }

    @Nullable
    private static Scp003BoardBlockEntity nearestBoard(ServerLevel World, Vec3 From) {
        ChunkPos Centre = new ChunkPos(BlockPos.containing(From));
        Scp003BoardBlockEntity Best = null;
        double BestDistance = SearchRange * SearchRange;
        for (int Dx = -ChunkReach; Dx <= ChunkReach; Dx++) {
            for (int Dz = -ChunkReach; Dz <= ChunkReach; Dz++) {
                if (!World.hasChunk(Centre.x + Dx, Centre.z + Dz)) {
                    continue;
                }
                LevelChunk Chunk = World.getChunk(Centre.x + Dx, Centre.z + Dz);
                for (BlockEntity Found : Chunk.getBlockEntities().values()) {
                    if (Found instanceof Scp003BoardBlockEntity Board) {
                        double Distance = Vec3.atCenterOf(Board.getBlockPos()).distanceToSqr(From);
                        if (Distance < BestDistance) {
                            Best = Board;
                            BestDistance = Distance;
                        }
                    }
                }
            }
        }
        return Best;
    }

    private static int hatch(CommandSourceStack Source) {
        Scp003BoardBlockEntity Board = nearestBoard(Source.getLevel(), Source.getPosition());
        if (Board == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp003.hatch.none", (int) SearchRange));
            return 0;
        }
        BlockPos Pos = Board.getBlockPos();
        Board.hatchNow();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.hatch.success", Pos.getX(), Pos.getY(), Pos.getZ()), true);
        return 1;
    }

    @Nullable
    private static Scp003Creature nearestCreature(ServerLevel World, Vec3 From) {
        return World.getEntitiesOfClass(Scp003Creature.class, new AABB(From, From).inflate(SearchRange), Creature -> Creature.isAlive() && !Creature.isTransferred())
                .stream().min(Comparator.comparingDouble(Creature -> Creature.distanceToSqr(From))).orElse(null);
    }

    private static int grow(CommandSourceStack Source, int Stage) {
        ServerLevel World = Source.getLevel();
        Scp003Creature Creature = nearestCreature(World, Source.getPosition());
        if (Creature == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp003.grow.none", (int) SearchRange));
            return 0;
        }
        Scp003Colony Colony = Scp003Colony.get(Source.getServer());
        if (Stage == 4 && Colony.hasAlpha() && !Colony.isAlpha(Creature.lineage())) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp003.grow.alpha_taken"));
            return 0;
        }
        if (Creature.stage() == Stage) {
            if (Creature instanceof Scp003Larva Larva) {
                Larva.growNow();
            } else if (Creature instanceof Scp003Crawler Crawler) {
                Crawler.growNow();
            } else if (Creature instanceof Scp003Host Builder) {
                Builder.growNow();
            }
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.grow.hastened", Creature.getDisplayName()), true);
            return 1;
        }
        EntityType<? extends Scp003Creature> Type = Scp003Growth.typeFor(Stage);
        if (Type == null) {
            return 0;
        }
        Scp003Creature Next = Stage == 4 ? Colony.promoteNow(World, Creature) : Scp003Growth.transform(Creature, Type, Scp003Creature.ActionEmerge, Scp003Creature.EmergeTicks);
        if (Next == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp003.grow.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.grow.success", Next.getDisplayName()), true);
        return 1;
    }

    private static int pending(CommandSourceStack Source) {
        List<StrangerEncounters.Encounter> Entries = StrangerEncounters.get(Source.getServer()).list();
        if (Entries.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.pending.none"), false);
        }
        for (StrangerEncounters.Encounter Entry : Entries) {
            ServerPlayer Reader = Source.getServer().getPlayerList().getPlayer(Entry.getReader());
            Component Name = Reader != null ? Reader.getDisplayName() : Component.literal(Entry.getReader().toString().substring(0, 8));
            Component Line = switch (Entry.getStage()) {
                case Waiting -> Component.translatable("commands.scp_unleashed.scp003.pending.waiting", Name, Math.max(0, Entry.getRemaining() / 20), Entry.getRetreats());
                case Present -> Component.translatable("commands.scp_unleashed.scp003.pending.present", Name, Entry.getSpot().getX(), Entry.getSpot().getY(), Entry.getSpot().getZ());
                case Delivered -> Component.translatable("commands.scp_unleashed.scp003.pending.delivered", Name, Entry.getDeliveredAt().getX(), Entry.getDeliveredAt().getY(),
                        Entry.getDeliveredAt().getZ());
            };
            Source.sendSuccess(() -> Line, false);
        }
        Scp003Colony Colony = Scp003Colony.get(Source.getServer());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.pending.colony", Colony.trackedCount(), Colony.pendingPurges()), false);
        if (Colony.hasAlpha()) {
            BlockPos Pos = Colony.alphaPos();
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.pending.alpha", Pos.getX(), Pos.getY(), Pos.getZ(), Colony.alphaDimension()), false);
        } else {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.pending.no_alpha"), false);
        }
        Scp003BoardBlockEntity Board = nearestBoard(Source.getLevel(), Source.getPosition());
        if (Board != null) {
            BlockPos Pos = Board.getBlockPos();
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp003.pending.board", Pos.getX(), Pos.getY(), Pos.getZ(), Math.round(Board.fraction() * 100.0f),
                    Board.ticksLeft() / 20), false);
        }
        return Entries.size();
    }
}
