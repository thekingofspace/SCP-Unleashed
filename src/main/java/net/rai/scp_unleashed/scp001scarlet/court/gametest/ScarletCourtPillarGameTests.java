package net.rai.scp_unleashed.scp001scarlet.court.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.court.CourtArena;
import net.rai.scp_unleashed.scp001scarlet.court.CourtDaughter;
import net.rai.scp_unleashed.scp001scarlet.court.CourtKeeper;
import net.rai.scp_unleashed.scp001scarlet.court.ScarletCourt;
import net.rai.scp_unleashed.scp001scarlet.court.ScarletCourtRegistry;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneEmitter;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneSite;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ScarletCourtPillarGameTests {
    public static final String Arena = "scarletcourt_arena";
    public static final int SettleTicks = 100;

    private ScarletCourtPillarGameTests() {
    }

    private static void hold(ServerLevel World, BlockPos Centre, boolean Forced) {
        int Reach = ScarletThrone.RimRadius + 2;
        for (int Cx = (Centre.getX() - Reach) >> 4; Cx <= (Centre.getX() + Reach) >> 4; Cx++) {
            for (int Cz = (Centre.getZ() - Reach) >> 4; Cz <= (Centre.getZ() + Reach) >> 4; Cz++) {
                World.setChunkForced(Cx, Cz, Forced);
            }
        }
    }

    private static Vec3 worst(BlockPos Throne, int Index, double Distance, double Turn) {
        int[] Centre = ScarletThrone.pillarOffset(Index);
        Direction Toward = ScarletThrone.inward(Index);
        double Angle = Math.atan2(Toward.getStepZ(), Toward.getStepX()) + Turn;
        return new Vec3(Throne.getX() + Centre[0] + 0.5 + Math.cos(Angle) * Distance, Throne.getY(), Throne.getZ() + Centre[1] + 0.5 + Math.sin(Angle) * Distance);
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scarletcourt_pillars")
    public static void daughtersWedgedAtPillarsWorkThemselvesFree(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        CourtArena.overrideForTests(-1);
        ScarletCourt.Instance.end(World);
        BlockPos Throne = new BlockPos(-48016, -59, -48016);
        hold(World, Throne, true);
        ScarletThroneSite.placeAt(Server, Throne);
        ScarletThroneEmitter.emitAll(World, ScarletThroneSite.get(Server));
        BlockPos Dais = ScarletThrone.dais(World, Throne);
        List<CourtDaughter> Placed = new ArrayList<>();
        for (int Kind = 0; Kind < 4; Kind++) {
            EntityType<? extends CourtDaughter> Type = ScarletCourtRegistry.daughterType(Kind);
            Vec3[] Spots = {worst(Throne, (Kind * 2) % ScarletThrone.Pillars, 3.0, 0.0).add(0.0, 1.0, 0.0), worst(Throne, (Kind * 2 + 1) % ScarletThrone.Pillars, 1.5, Math.PI / 2.0)};
            for (Vec3 Spot : Spots) {
                CourtDaughter Made = Type.create(World);
                Helper.assertTrue(Made != null, "daughter " + Kind + " did not create");
                Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
                Made.setup(1, Dais);
                Made.setPersistenceRequired();
                World.addFreshEntity(Made);
                Placed.add(Made);
            }
        }
        Helper.assertTrue(Placed.stream().anyMatch(Each -> CourtKeeper.inside(World, Each)), "the worst spots did not start wedged in a pillar");
        Helper.startSequence()
                .thenExecuteAfter(SettleTicks, () -> {
                    for (CourtDaughter Each : Placed) {
                        String Name = Each.getType().getDescriptionId();
                        Helper.assertTrue(Each.isAlive(), Name + " died while wedged");
                        Helper.assertFalse(CourtKeeper.inside(World, Each), Name + " is still inside a block at " + Each.position());
                        double Gap = CourtKeeper.pillarGap(Dais, Each.getX(), Each.getZ());
                        Helper.assertTrue(Gap >= CourtKeeper.Skirt + Each.getBbWidth() / 2.0, Name + " is still against a pillar, gap " + Gap);
                    }
                    AABB Area = new AABB(Throne).inflate(ScarletThrone.RimRadius + 8.0, ScarletThrone.ClearAbove, ScarletThrone.RimRadius + 8.0);
                    for (Entity Each : World.getEntitiesOfClass(Entity.class, Area, Each -> !(Each instanceof Player))) {
                        Each.discard();
                    }
                    hold(World, Throne, false);
                    ScarletThroneSite.get(Server).forget();
                })
                .thenSucceed();
    }
}
