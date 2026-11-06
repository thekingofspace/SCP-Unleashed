package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class DaeviteRuinsPlanner {
    private DaeviteRuinsPlanner() {
    }

    public static BlockPos overworldOrigin(ServerLevel From, BlockPos Origin) {
        if (From.dimension() == Level.OVERWORLD) {
            return Origin;
        }
        if (From.dimension() == Level.NETHER) {
            return new BlockPos(Origin.getX() * 8, From.getServer().overworld().getSeaLevel(), Origin.getZ() * 8);
        }
        return From.getServer().overworld().getSharedSpawnPos();
    }

    public static DaeviteRuin plan(ServerLevel World, int Page, BlockPos Origin) {
        DaeviteRuinsWorld Data = DaeviteRuinsWorld.get(World.getServer());
        DaeviteRuin Existing = Data.sameChronicle(Page, Origin);
        if (Existing != null) {
            return Existing;
        }
        RandomSource Random = RandomSource.create(World.getSeed() ^ Page * 0x9E3779B97F4A7C15L ^ Origin.asLong());
        DaeviteRuin Ruin = new DaeviteRuin();
        Ruin.Page = Page;
        Ruin.Dimension = Level.OVERWORLD;
        Ruin.Origin = Origin.immutable();
        Ruin.Target = DaeviteRuinsSites.preset(World, Origin, Page, Random, Data.ruins());
        Ruin.Created = World.getGameTime();
        return Data.add(Ruin);
    }
}
