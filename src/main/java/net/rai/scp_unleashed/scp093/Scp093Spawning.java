package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

public final class Scp093Spawning {
    public static final int Every = 40;
    public static final int BaseCap = 2;
    public static final int CityCap = 4;
    public static final float BaseChance = 0.1f;
    public static final float CityChance = 0.25f;
    public static final int GlobalCap = 40;
    public static final double LocalRange = 72.0;
    public static final double MinDistance = 36.0;
    public static final double MaxDistance = 64.0;
    public static final double PlayerClearance = 32.0;
    public static final int Attempts = 8;
    public static final int GroundDrop = 2;
    public static final int MaxLift = 2;
    public static final double StructureMargin = 2.0;

    private Scp093Spawning() {
    }

    public static void tick(MinecraftServer Server) {
        if (Server.getTickCount() % Every != 0) {
            return;
        }
        ServerLevel Wastes = Scp093Wastes.level(Server);
        if (Wastes == null || Wastes.players().isEmpty() || !Wastes.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        if (Wastes.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        int Total = Wastes.getEntities(Scp093Registry.Torso.get(), Found -> Found.isAlive()).size();
        if (Total >= GlobalCap) {
            return;
        }
        long Now = Wastes.getGameTime();
        for (ServerPlayer User : Wastes.players()) {
            if (User.isSpectator() || !User.isAlive() || Scp093Transit.inGrace(User, Now)) {
                continue;
            }
            boolean City = inCity(Wastes, User.blockPosition());
            int Cap = City ? CityCap : BaseCap;
            int Near = Wastes.getEntitiesOfClass(Scp093Torso.class, User.getBoundingBox().inflate(LocalRange), Scp093Torso::isAlive).size();
            if (Near >= Cap || Wastes.random.nextFloat() >= (City ? CityChance : BaseChance)) {
                continue;
            }
            spawnNear(Wastes, User);
        }
    }

    public static boolean inCity(ServerLevel Wastes, BlockPos Pos) {
        Structure City = Wastes.registryAccess().registryOrThrow(Registries.STRUCTURE).get(Scp093Registry.City);
        return City != null && Wastes.structureManager().getStructureAt(Pos, City).isValid();
    }

    public static boolean loaded(ServerLevel Wastes, AABB Box) {
        int MinX = Mth.floor(Box.minX) >> 4;
        int MaxX = Mth.floor(Box.maxX) >> 4;
        int MinZ = Mth.floor(Box.minZ) >> 4;
        int MaxZ = Mth.floor(Box.maxZ) >> 4;
        for (int X = MinX; X <= MaxX; X++) {
            for (int Z = MinZ; Z <= MaxZ; Z++) {
                if (!Wastes.hasChunk(X, Z)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean touchesStructure(ServerLevel Wastes, AABB Box) {
        Registry<Structure> Structures = Wastes.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Structure City = Structures.get(Scp093Registry.City);
        Structure Shack = Structures.get(Scp093Registry.Shack);
        AABB Wide = Box.inflate(StructureMargin);
        BoundingBox Area = new BoundingBox(Mth.floor(Wide.minX), Mth.floor(Wide.minY), Mth.floor(Wide.minZ), Mth.floor(Wide.maxX), Mth.floor(Wide.maxY), Mth.floor(Wide.maxZ));
        for (int X = Mth.floor(Wide.minX) >> 4; X <= Mth.floor(Wide.maxX) >> 4; X++) {
            for (int Z = Mth.floor(Wide.minZ) >> 4; Z <= Mth.floor(Wide.maxZ) >> 4; Z++) {
                if (!Wastes.hasChunk(X, Z)) {
                    return true;
                }
                for (StructureStart Start : Wastes.structureManager().startsForStructure(new ChunkPos(X, Z), Found -> Found == City || Found == Shack)) {
                    if (!Start.getBoundingBox().intersects(Area)) {
                        continue;
                    }
                    for (StructurePiece Piece : Start.getPieces()) {
                        if (Piece.getBoundingBox().intersects(Area)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean openGround(ServerLevel Wastes, AABB Box, BlockPos Spot) {
        if (!Wastes.getBlockState(Spot.below()).isFaceSturdy(Wastes, Spot.below(), Direction.UP)) {
            return false;
        }
        int Floor = Spot.getY() - 1;
        for (double X : new double[]{Box.minX + 0.2, Box.maxX - 0.2}) {
            for (double Z : new double[]{Box.minZ + 0.2, Box.maxZ - 0.2}) {
                boolean Found = false;
                for (int Drop = 0; Drop <= GroundDrop; Drop++) {
                    BlockPos Under = BlockPos.containing(X, Floor - Drop, Z);
                    if (!Wastes.getBlockState(Under).getCollisionShape(Wastes, Under).isEmpty()) {
                        Found = true;
                        break;
                    }
                }
                if (!Found) {
                    return false;
                }
            }
        }
        return true;
    }

    public static Scp093Torso spawnNear(ServerLevel Wastes, ServerPlayer User) {
        RandomSource Random = Wastes.random;
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Reach = MinDistance + Random.nextDouble() * (MaxDistance - MinDistance);
            int X = (int) Math.floor(User.getX() + Math.cos(Angle) * Reach);
            int Z = (int) Math.floor(User.getZ() + Math.sin(Angle) * Reach);
            if (!Wastes.hasChunkAt(new BlockPos(X, 0, Z))) {
                continue;
            }
            int Y = Wastes.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z);
            BlockPos Spot = new BlockPos(X, Y, Z);
            if (Y <= Wastes.getMinBuildHeight() + 1 || Wastes.getNearestPlayer(X + 0.5, Y, Z + 0.5, PlayerClearance, false) != null) {
                continue;
            }
            AABB Box = Scp093Registry.Torso.get().getDimensions().makeBoundingBox(X + 0.5, Y, Z + 0.5);
            if (!loaded(Wastes, Box.inflate(StructureMargin)) || !openGround(Wastes, Box, Spot) || touchesStructure(Wastes, Box)) {
                continue;
            }
            int Lift = 0;
            while (Lift <= MaxLift && (!Wastes.noCollision(Box.move(0.0, Lift, 0.0)) || Wastes.containsAnyLiquid(Box.move(0.0, Lift, 0.0)))) {
                Lift++;
            }
            if (Lift > MaxLift) {
                continue;
            }
            Scp093Torso Torso = Scp093Registry.Torso.get().create(Wastes);
            if (Torso == null) {
                return null;
            }
            Torso.moveTo(X + 0.5, Y + Lift, Z + 0.5, Random.nextFloat() * 360.0f, 0.0f);
            Torso.finalizeSpawn(Wastes, Wastes.getCurrentDifficultyAt(Spot), MobSpawnType.NATURAL, null);
            if (Wastes.addFreshEntity(Torso)) {
                return Torso;
            }
        }
        return null;
    }
}
