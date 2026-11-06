package net.rai.scp_unleashed.intro;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.props.OfficeChairBlock;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class IntroBuilder {
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    public static final long Seed = 20261029L;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<MinecraftServer, Integer> Signatures = new ConcurrentHashMap<>();

    private IntroBuilder() {
    }

    public static void forget(MinecraftServer Server) {
        Signatures.remove(Server);
    }

    public static Optional<StructureTemplate> template(ServerLevel World, net.minecraft.resources.ResourceLocation Id) {
        return World.getStructureManager().get(Id);
    }

    public static int signature(ServerLevel World) {
        Integer Known = Signatures.get(World.getServer());
        if (Known != null) {
            return Known;
        }
        Optional<StructureTemplate> Office = template(World, IntroRegistry.OfficeTemplate);
        Optional<StructureTemplate> Room = template(World, IntroRegistry.GateRoomTemplate);
        if (Office.isEmpty() || Room.isEmpty()) {
            return 0;
        }
        int Made = Objects.hash(IntroWorld.Signature, Office.get().save(new CompoundTag()).hashCode(), Room.get().save(new CompoundTag()).hashCode());
        if (Made == 0) {
            Made = 1;
        }
        Signatures.put(World.getServer(), Made);
        return Made;
    }

    public static boolean intact(ServerLevel World, int Plot) {
        BlockState Seat = World.getBlockState(IntroWorld.playerChair(Plot));
        BlockState Other = World.getBlockState(IntroWorld.scientistChair(Plot));
        BlockState Core = World.getBlockState(IntroWorld.gateCore(Plot));
        return Seat.getBlock() instanceof OfficeChairBlock && Other.getBlock() instanceof OfficeChairBlock && Core.is(IntroRegistry.GateCore.get());
    }

    public static boolean ensure(ServerLevel World, int Plot) {
        int Wanted = signature(World);
        if (Wanted == 0) {
            Log.warn("Intro room templates are missing; the intro cannot be built");
            return false;
        }
        IntroPlots Plots = IntroPlots.get(World.getServer());
        if (Plots.builtSignature(Plot) == Wanted && intact(World, Plot)) {
            return true;
        }
        build(World, Plot);
        Plots.markBuilt(Plot, Wanted);
        return intact(World, Plot);
    }

    public static boolean fresh(ServerLevel World, int Plot) {
        int Wanted = signature(World);
        if (Wanted == 0) {
            Log.warn("Intro room templates are missing; the intro cannot be built");
            return false;
        }
        build(World, Plot);
        IntroPlots.get(World.getServer()).markBuilt(Plot, Wanted);
        return intact(World, Plot);
    }

    public static void release(ServerLevel World, int Plot) {
        if (Plot < 0) {
            return;
        }
        clear(World, Plot);
        IntroPlots.get(World.getServer()).markBuilt(Plot, 0);
    }

    public static boolean empty(ServerLevel World, int Plot) {
        AABB Bounds = IntroWorld.plotBounds(Plot);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = (int) Bounds.minX; X < (int) Bounds.maxX; X++) {
            for (int Y = (int) Bounds.minY; Y < (int) Bounds.maxY; Y++) {
                for (int Z = (int) Bounds.minZ; Z < (int) Bounds.maxZ; Z++) {
                    if (!World.getBlockState(Cursor.set(X, Y, Z)).isAir()) {
                        return false;
                    }
                }
            }
        }
        return World.getEntities((Entity) null, Bounds.inflate(2.0), IntroPrivacy::cast).isEmpty();
    }

    public static void build(ServerLevel World, int Plot) {
        for (ChunkPos Chunk : IntroWorld.plotChunks(Plot)) {
            World.getChunk(Chunk.x, Chunk.z);
        }
        clear(World, Plot);
        place(World, IntroRegistry.OfficeTemplate, IntroWorld.officeOrigin(Plot));
        place(World, IntroRegistry.GateRoomTemplate, IntroWorld.gateRoomOrigin(Plot));
    }

    private static void place(ServerLevel World, net.minecraft.resources.ResourceLocation Id, BlockPos Origin) {
        Optional<StructureTemplate> Found = template(World, Id);
        if (Found.isEmpty()) {
            return;
        }
        Found.get().placeInWorld(World, Origin, Origin, new StructurePlaceSettings().setKnownShape(true), RandomSource.create(Seed), Block.UPDATE_CLIENTS);
    }

    public static void clear(ServerLevel World, int Plot) {
        AABB Bounds = IntroWorld.plotBounds(Plot);
        List<Entity> Doomed = new ArrayList<>();
        for (Entity Found : World.getEntities((Entity) null, Bounds.inflate(2.0), Found -> !(Found instanceof Player))) {
            Doomed.add(Found);
        }
        for (Entity Found : Doomed) {
            Found.discard();
        }
        BlockState Air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = (int) Bounds.minX; X < (int) Bounds.maxX; X++) {
            for (int Y = (int) Bounds.minY; Y < (int) Bounds.maxY; Y++) {
                for (int Z = (int) Bounds.minZ; Z < (int) Bounds.maxZ; Z++) {
                    Cursor.set(X, Y, Z);
                    if (!World.getBlockState(Cursor).isAir()) {
                        World.removeBlockEntity(Cursor);
                        World.setBlock(Cursor, Air, Flags);
                    }
                }
            }
        }
    }

    public static boolean inside(int Plot, BlockPos Pos) {
        return IntroWorld.plotBounds(Plot).contains(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5);
    }
}
