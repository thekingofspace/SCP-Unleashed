package net.rai.scp_unleashed.scp902;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Scp902Wastes {
    public static final ResourceKey<Level> Key = Scp902Registry.Wastes;
    public static final ResourceLocation Template = Scp902Registry.location("scp902/trench");
    public static final int Signature = 3;
    public static final int SlotSpacing = 2048;
    public static final int TemplateY = 54;
    public static final int TemplateWestX = -56;
    public static final int TemplateNorthZ = -168;
    public static final int TemplateEastX = 56;
    public static final int TemplateSouthZ = 24;
    public static final int TemplateTopY = 72;
    public static final int FlatDirtTop = 59;
    public static final int FlatMudTop = 62;
    public static final double ArrivalX = 0.5;
    public static final double ArrivalY = 61.2;
    public static final double ArrivalZ = 0.5;
    public static final float ArrivalYaw = 180.0f;
    public static final float ArrivalPitch = -4.0f;
    public static final int ArrivalTicketRadius = 3;
    public static final int ClimbLimit = 12;
    public static final int WalkEndZ = -96;
    public static final BlockPos BoxOffset = new BlockPos(-1, 61, -3);
    public static final double SunYaw = 180.0;
    public static final long Seed = 9020902L;

    public static final int StandinBaseX = 1536000;
    public static final int StandinBaseZ = 1536000;
    public static final int StandinSlots = 128;
    private static volatile ResourceKey<Level> Standin;

    private static final Logger Log = LogUtils.getLogger();

    private Scp902Wastes() {
    }

    public static ResourceKey<Level> key() {
        ResourceKey<Level> Found = Standin;
        return Found != null ? Found : Key;
    }

    public static void standIn(@Nullable ResourceKey<Level> Dimension) {
        Standin = Dimension;
    }

    public static boolean standingIn() {
        return Standin != null;
    }

    public static boolean strict(@Nullable Level World) {
        return inside(World) && !standingIn();
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(key());
    }

    public static boolean inside(@Nullable Level World) {
        return World != null && World.dimension() == key();
    }

    public static boolean inside(Entity Target) {
        return inside(Target.level());
    }

    public static boolean exempt(Player User) {
        return User.isSpectator() || User.isCreative() && User.hasPermissions(2);
    }

    public static int baseX() {
        return Standin != null ? StandinBaseX : 0;
    }

    public static int baseZ() {
        return Standin != null ? StandinBaseZ : 0;
    }

    public static boolean nearSlots(BlockPos Pos) {
        return Pos.getX() >= baseX() - SlotSpacing / 2 && Pos.getX() < baseX() + StandinSlots * SlotSpacing && Math.abs(Pos.getZ() - baseZ()) < SlotSpacing / 2;
    }

    public static BlockPos centre(int Slot) {
        return new BlockPos(baseX() + Slot * SlotSpacing, 0, baseZ());
    }

    public static BlockPos templateOrigin(int Slot) {
        return centre(Slot).offset(TemplateWestX, TemplateY, TemplateNorthZ);
    }

    public static Vec3 arrival(int Slot) {
        BlockPos Centre = centre(Slot);
        return new Vec3(Centre.getX() + ArrivalX, ArrivalY, Centre.getZ() + ArrivalZ);
    }

    public static BlockPos box(int Slot) {
        return centre(Slot).offset(BoxOffset);
    }

    public static boolean clear(ServerLevel World, Vec3 Spot) {
        AABB Body = EntityType.PLAYER.getDimensions().makeBoundingBox(Spot);
        return World.noCollision(Body) && !World.noCollision(Body.move(0.0, -0.25, 0.0)) && World.getFluidState(BlockPos.containing(Spot.add(0.0, 1.6, 0.0))).isEmpty();
    }

    public static Vec3 safeArrival(ServerLevel World, int Slot) {
        Vec3 Wanted = arrival(Slot);
        if (clear(World, Wanted)) {
            return Wanted;
        }
        Log.warn("SCP-902 arrival at {} was blocked, carving the trench floor clear", Wanted);
        carve(World, Slot);
        if (clear(World, Wanted)) {
            return Wanted;
        }
        for (int Step = 1; Step <= ClimbLimit * 8; Step++) {
            Vec3 Higher = Wanted.add(0.0, Step * 0.125, 0.0);
            if (clear(World, Higher)) {
                return Higher;
            }
        }
        return Wanted.add(0.0, ClimbLimit, 0.0);
    }

    public static void carve(ServerLevel World, int Slot) {
        BlockPos Centre = centre(Slot);
        BlockState Floor = Scp902Registry.TrenchMud.get().defaultBlockState();
        BlockState Board = Scp902Registry.Props.get(Scp902Prop.Duckboard).get().defaultBlockState();
        for (int Z = WalkEndZ - 4; Z <= 2; Z++) {
            for (int X = -1; X <= 1; X++) {
                World.setBlock(Centre.offset(X, 60, Z), Floor, Block.UPDATE_ALL);
                for (int Y = 61; Y <= 64; Y++) {
                    World.setBlock(Centre.offset(X, Y, Z), X == 0 && Y == 61 ? Board : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    public static Vec3 pathPoint(int Slot, double Z) {
        BlockPos Centre = centre(Slot);
        return new Vec3(Centre.getX() + ArrivalX, ArrivalY, Z);
    }

    public static int slotOf(Vec3 Spot) {
        return Math.max(0, (int) Math.floor((Spot.x - baseX() + SlotSpacing / 2.0) / SlotSpacing));
    }

    public static boolean ensureBuilt(ServerLevel World, int Slot) {
        Scp902State State = Scp902State.get(World.getServer());
        if (State.built(Slot, Signature)) {
            return false;
        }
        build(World, Slot);
        return true;
    }

    public static void build(ServerLevel World, int Slot) {
        Scp902State State = Scp902State.get(World.getServer());
        if (standingIn()) {
            Scp902Cleanup.wipe(World, Slot);
        }
        Optional<StructureTemplate> Found = World.getStructureManager().get(Template);
        if (Found.isEmpty()) {
            Log.error("SCP-902 trench template {} is missing, carving a bare trench instead", Template);
            carve(World, Slot);
        } else {
            BlockPos Origin = templateOrigin(Slot);
            Found.get().placeInWorld(World, Origin, Origin, new StructurePlaceSettings().setKnownShape(true).setIgnoreEntities(true), RandomSource.create(Seed),
                    Block.UPDATE_CLIENTS);
        }
        State.markBuilt(Slot, Signature);
    }

    public static BlockState flat(int Y) {
        if (Y > FlatMudTop) {
            return Blocks.AIR.defaultBlockState();
        }
        if (Y > FlatDirtTop) {
            return Blocks.MUD.defaultBlockState();
        }
        return Y > 50 ? Blocks.DIRT.defaultBlockState() : Y > 0 ? Blocks.STONE.defaultBlockState() : Blocks.BEDROCK.defaultBlockState();
    }

    public static void evict(ServerPlayer User) {
        Scp902Trip.sendToRespawn(User);
    }
}
