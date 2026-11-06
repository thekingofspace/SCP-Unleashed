package net.rai.scp_unleashed.scp001scarlet.king.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKing;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingData;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletRite;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneEmitter;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneSite;
import net.rai.scp_unleashed.scp999.Scp999Blob;

import java.util.List;
import java.util.function.IntBinaryOperator;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ScarletEmergeGameTests {
    public static final String Arena = "scarletking_arena";
    public static final String Phrase = ScarletBuildGameTests.Phrase;
    public static final int Reach = ScarletThrone.RimRadius + 8;
    public static final double Slack = 0.1;

    private ScarletEmergeGameTests() {
    }

    private static void shape(ServerLevel World, BlockPos Centre, IntBinaryOperator Height) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Reach; Dx <= Reach; Dx++) {
            for (int Dz = -Reach; Dz <= Reach; Dz++) {
                int Top = Height.applyAsInt(Dx, Dz);
                for (int Y = World.getMinBuildHeight() + 1; Y <= Top; Y++) {
                    World.setBlock(Cursor.set(Centre.getX() + Dx, Y, Centre.getZ() + Dz), Y == Top ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                }
                for (int Y = Top + 1; Y <= Top + 30; Y++) {
                    if (!World.getBlockState(Cursor.set(Centre.getX() + Dx, Y, Centre.getZ() + Dz)).isAir()) {
                        World.setBlock(Cursor, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static double surface(ServerLevel World, BlockPos Column, int From) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(Column.getX(), From, Column.getZ());
        while (Cursor.getY() > World.getMinBuildHeight()) {
            if (!World.getBlockState(Cursor).getCollisionShape(World, Cursor).isEmpty()) {
                return Cursor.getY() + World.getBlockState(Cursor).getCollisionShape(World, Cursor).max(net.minecraft.core.Direction.Axis.Y);
            }
            Cursor.move(0, -1, 0);
        }
        return World.getMinBuildHeight();
    }

    private static void assertStanding(GameTestHelper Helper, ServerLevel World, BlockPos Throne, String Where) {
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        ScarletKing King = ScarletRite.king(World, Data);
        Helper.assertTrue(King != null, Where + ": no King after the emergence");
        List<ScarletKing> All = World.getEntitiesOfClass(ScarletKing.class, new AABB(Throne).inflate(80.0), Each -> !Each.isRemoved());
        Helper.assertTrue(All.size() == 1, Where + ": " + All.size() + " Kings in the arena");
        double Floor = surface(World, King.blockPosition(), Throne.getY() + 40);
        Helper.assertTrue(Math.abs(King.getY() - Floor) <= Slack, Where + ": the King's feet are at " + King.getY() + " but the floor is at " + Floor);
        Helper.assertTrue(World.noCollision(King, King.getBoundingBox().deflate(0.05)), Where + ": the King's box is inside blocks at " + King.position());
        Helper.assertTrue(Math.abs(King.getX() - (Throne.getX() + 0.5)) < 0.6 && Math.abs(King.getZ() - (Throne.getZ() + 0.5)) < 0.6, Where + ": the King is off the dais at "
                + King.position());
        Helper.assertTrue(King.fighting(), Where + ": the fight did not start");
    }

    private static void runRite(GameTestHelper Helper, BlockPos Middle, IntBinaryOperator Height, String Where, boolean Reload) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        ScarletBuildGameTests.freshState(Server);
        ScarletBuildGameTests.hold(World, Middle, true);
        shape(World, Middle, Height);
        BlockPos Feet = new BlockPos(Middle.getX(), Height.applyAsInt(0, 0) + 1, Middle.getZ());
        ScarletState State = ScarletState.get(Server);
        State.markRead();
        State.phase(ScarletState.Phase.Xk2, World.getGameTime());
        Scp999Blob Blob = ScarletBuildGameTests.blob(World, Feet);
        Helper.assertTrue(Blob != null, Where + ": 999 did not spawn");
        FakePlayer User = ScarletBuildGameTests.speaker(World, Vec3.atBottomCenterOf(Feet).add(2.0, 0.0, 0.0), "scarlet_emerge");
        ScarletRite.heard(User, Phrase);
        int Nonce = ScarletRite.pendingNonce(User.getUUID());
        Helper.assertTrue(Nonce >= 0, Where + ": the phrase was refused");
        ScarletRite.answer(User, Nonce, true);
        BlockPos Throne = State.throne();
        ScarletThroneSite Site = ScarletThroneSite.get(Server);
        long[] Began = new long[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!Site.rising() && ScarletKingData.get(Server).riteStart() >= 0L, Where + ": still building"))
                .thenExecute(() -> Began[0] = ScarletKingData.get(Server).riteStart())
                .thenWaitUntil(() -> Helper.assertTrue(World.getGameTime() - Began[0] > ScarletRite.KingAt + 20, Where + ": the King has not come yet"))
                .thenExecute(() -> {
                    if (Reload) {
                        reload(Helper, World, Where);
                    }
                })
                .thenWaitUntil(() -> Helper.assertTrue(World.getGameTime() - Began[0] > ScarletRite.FightAt + 4, Where + ": the fight has not begun"))
                .thenExecute(() -> {
                    assertStanding(Helper, World, Throne, Where);
                    ScarletBuildGameTests.clean(World, Throne);
                })
                .thenSucceed();
    }

    private static void reload(GameTestHelper Helper, ServerLevel World, String Where) {
        ScarletKing King = ScarletRite.king(World, ScarletKingData.get(World.getServer()));
        Helper.assertTrue(King != null, Where + ": no King to reload");
        CompoundTag Saved = new CompoundTag();
        Helper.assertTrue(King.save(Saved), Where + ": the King did not save");
        King.clearBar();
        King.discard();
        Entity Loaded = EntityType.loadEntityRecursive(Saved, World, Each -> Each);
        Helper.assertTrue(Loaded instanceof ScarletKing, Where + ": the King did not load");
        World.addFreshEntity(Loaded);
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarletking_emerge_clock")
    public static void emergenceFinishesOnTheSurfaceBeforeTheFight(GameTestHelper Helper) {
        Helper.assertTrue(ScarletKing.emergeDepth(0.0f) == -150.0f, "the emergence does not start deep");
        Helper.assertTrue(Math.abs(ScarletKing.emergeDepth(3.6f) + 40.0f) < 1.0E-3f, "the emergence curve left the clip's keys");
        float Ends = ScarletKing.EmergeKeys[ScarletKing.EmergeKeys.length - 1][0];
        Helper.assertTrue(ScarletKing.emergeDepth(Ends) == 0.0f && ScarletKing.emergeDepth(Ends + 30.0f) == 0.0f, "the emergence does not end on the surface");
        Helper.assertTrue(Ends * 20.0f <= ScarletRite.FightAt - ScarletRite.KingAt, "the fight starts before the King has surfaced");
        Helper.assertTrue(Ends * 20.0f <= ScarletKing.ManifestTicks, "the manifest ends before the King has surfaced");
        Helper.assertTrue(!ScarletKing.movesRoot(ScarletKing.Idle) && !ScarletKing.movesRoot(ScarletKing.Manifest), "idle or the manifest would keep a root offset");
        ScarletBuildGameTests.freshState(Helper.getLevel().getServer());
        ScarletKing King = net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry.KingType.get().create(Helper.getLevel());
        Helper.assertTrue(King != null, "no King");
        Vec3 At = Vec3.atBottomCenterOf(Helper.absolutePos(new BlockPos(12, 1, 12)));
        King.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        Helper.getLevel().addFreshEntity(King);
        King.setAction(ScarletKing.Manifest);
        long Start = Helper.getLevel().getGameTime();
        Helper.runAfterDelay(10, () -> {
            float Age = King.actionAge(0.0f);
            long Passed = Helper.getLevel().getGameTime() - Start;
            Helper.assertTrue(Math.abs(Age - Passed) < 0.5f, "the action clock is not world time: " + Age + " vs " + Passed);
            King.clearBar();
            King.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "scarletking_emerge_flat")
    public static void emergesOnTheDaisOnFlatGround(GameTestHelper Helper) {
        runRite(Helper, new BlockPos(64016, 0, 64016), (Dx, Dz) -> -54, "flat", false);
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "scarletking_emerge_hills")
    public static void emergesOnTheDaisOnHills(GameTestHelper Helper) {
        runRite(Helper, new BlockPos(-64016, 0, 64016),
                (Dx, Dz) -> -48 + (int) Math.round(7.0 * Math.sin(Dx * 0.13) + 5.0 * Math.cos(Dz * 0.11) + 4.0 * Math.sin((Dx + Dz) * 0.07)), "hills", false);
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "scarletking_emerge_reload")
    public static void emergesOnTheDaisAfterAReloadMidRite(GameTestHelper Helper) {
        runRite(Helper, new BlockPos(64016, 0, -64016), (Dx, Dz) -> -50 + (int) Math.round(4.0 * Math.sin(Dx * 0.2) * Math.cos(Dz * 0.17)), "reload", true);
    }

    @GameTest(template = Arena, timeoutTicks = 2400, batch = "scarletking_emerge_built")
    public static void emergesOnTheDaisOfAPrebuiltThrone(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        ScarletBuildGameTests.freshState(Server);
        BlockPos Middle = new BlockPos(-64016, 0, -64016);
        ScarletBuildGameTests.hold(World, Middle, true);
        shape(World, Middle, (Dx, Dz) -> -46 + (int) Math.round(6.0 * Math.sin(Dx * 0.09) + 6.0 * Math.cos(Dz * 0.12)));
        BlockPos Throne = new BlockPos(Middle.getX(), -46, Middle.getZ());
        ScarletThroneSite.placeAt(Server, Throne);
        ScarletThroneEmitter.emitAll(World, ScarletThroneSite.get(Server));
        ScarletState State = ScarletState.get(Server);
        State.markRead();
        State.throne(Throne);
        State.phase(ScarletState.Phase.Xk2, World.getGameTime());
        BlockPos Feet = ScarletThrone.dais(World, Throne);
        Scp999Blob Blob = ScarletBuildGameTests.blob(World, Feet);
        Helper.assertTrue(Blob != null, "built: 999 did not spawn");
        FakePlayer User = ScarletBuildGameTests.speaker(World, Vec3.atBottomCenterOf(Feet).add(2.0, 0.0, 0.0), "scarlet_emerge_built");
        ScarletRite.heard(User, Phrase);
        int Nonce = ScarletRite.pendingNonce(User.getUUID());
        Helper.assertTrue(Nonce >= 0, "built: the phrase was refused at a built throne");
        ScarletRite.answer(User, Nonce, true);
        Helper.assertTrue(ScarletKingData.get(Server).riteStart() >= 0L, "built: the rite did not start at once at a built throne");
        long Began = ScarletKingData.get(Server).riteStart();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(World.getGameTime() - Began > ScarletRite.FightAt + 4, "built: the fight has not begun"))
                .thenExecute(() -> {
                    assertStanding(Helper, World, Throne, "built");
                    ScarletBuildGameTests.clean(World, Throne);
                })
                .thenSucceed();
    }
}
