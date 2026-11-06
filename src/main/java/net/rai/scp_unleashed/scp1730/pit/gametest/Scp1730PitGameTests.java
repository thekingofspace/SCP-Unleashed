package net.rai.scp_unleashed.scp1730.pit.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlock;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;
import net.rai.scp_unleashed.scp1730.gametest.Scp1730GameTests;
import net.rai.scp_unleashed.scp1730.pit.Scp1730Mass;
import net.rai.scp_unleashed.scp1730.pit.Scp1730PitEvents;
import net.rai.scp_unleashed.scp1730.pit.Scp1730PitForm;
import net.rai.scp_unleashed.scp1730.pit.Scp1730PitRegistry;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Pit;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;
import net.rai.scp_unleashed.stasis.StasisContents;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730PitGameTests {
    public static final String Arena = "gateway_arena";

    private Scp1730PitGameTests() {
    }

    public static Scp1730PitForm pit(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Low = Helper.absolutePos(new BlockPos(10, 1, 10));
        Scp1730PitForm Pit = Scp1730PitForm.spawnIn(World, Low.getX(), Low.getZ(), Low.getX() + 12.0, Low.getZ() + 12.0, Low.getY());
        Helper.assertTrue(Pit != null, "SCP-1730-3 could not be created");
        Pit.forceState(Scp1730PitForm.StateActive);
        Pit.setPos(Pit.home().x, Low.getY() - Scp1730PitForm.Depth, Pit.home().z);
        return Pit;
    }

    public static ServerPlayer watcher(GameTestHelper Helper, String Name, boolean Creative) {
        ServerPlayer User = Scp1730GameTests.player(Helper, Helper.getLevel(), Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), Name);
        User.setGameMode(Creative ? GameType.CREATIVE : GameType.SURVIVAL);
        return User;
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp1730pit_spawns")
    public static void spawnsInPit(GameTestHelper Helper) {
        ServerPlayer User = Scp1730GameTests.enter(Helper, "scp1730pit_visitor");
        ServerLevel Facility = Scp1730GameTests.facility(Helper);
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        State.endRun();
        net.rai.scp_unleashed.scp1730.Scp1730Director.prepare(Facility);
        Scp1730Plan Plan = Scp1730World.plan(State.run());
        Helper.assertTrue(!Plan.pits().isEmpty(), "the plan has no pit room");
        Scp1730Pit Pit = Plan.pits().getFirst();
        Vec3 Rim = Vec3.atBottomCenterOf(Scp1730World.world(State.run(), Pit.MinX() - 2, Pit.RimY(), (Pit.MinZ() + Pit.MaxZ()) / 2));
        User.teleportTo(Facility, Rim.x, Rim.y, Rim.z, 0.0f, 0.0f);
        int ChunkX = ((int) Math.floor(Scp1730World.originX(State.run()) + (Pit.MinX() + Pit.MaxX()) / 2.0)) >> 4;
        int ChunkZ = ((int) Math.floor(Scp1730World.originZ() + (Pit.MinZ() + Pit.MaxZ()) / 2.0)) >> 4;
        Facility.setChunkForced(ChunkX, ChunkZ, true);
        Facility.getChunk(ChunkX, ChunkZ);
        double MinX = Scp1730World.originX(State.run()) + Pit.MinX();
        double MinZ = Scp1730World.originZ() + Pit.MinZ();
        AABB Hole = new AABB(MinX, Pit.BottomY() - 4, MinZ, MinX + Pit.MaxX() - Pit.MinX() + 1.0, Pit.RimY() + 10, MinZ + Pit.MaxZ() - Pit.MinZ() + 1.0);
        Helper.startSequence().thenWaitUntil(() -> {
            User.teleportTo(Facility, Rim.x, Rim.y, Rim.z, 0.0f, 0.0f);
            Scp1730PitEvents.spawnFor(Facility, State, List.of(User));
            List<Scp1730PitForm> Found = Facility.getEntitiesOfClass(Scp1730PitForm.class, Hole);
            int Lx = Scp1730World.localX(State.run(), User.getX());
            int Lz = Scp1730World.localZ(User.getZ());
            Helper.assertTrue(!Found.isEmpty(), "no SCP-1730-3 lives in the pit after a player reached the rim: near " + Scp1730PitEvents.nearPit(Pit, Lx, User.getY(), Lz)
                    + " loaded " + Facility.hasChunkAt(BlockPos.containing(Hole.getCenter().x, Pit.RimY(), Hole.getCenter().z)) + " job "
                    + State.jobDone(Pit.Zone(), Scp1730PitEvents.JobDweller) + " level " + (User.level() == Facility) + " at " + User.blockPosition().toShortString() + " pit "
                    + Hole + " all " + Facility.getEntitiesOfClass(Scp1730PitForm.class, Hole.inflate(64.0)).size());
            Helper.assertTrue(State.jobDone(Pit.Zone(), Scp1730PitEvents.JobDweller), "the pit job was not marked");
            Helper.assertTrue(Found.getFirst().inRoom(User), "SCP-1730-3 does not count a player at the rim as inside its room");
        }).thenExecute(() -> {
            for (Scp1730PitForm Each : Facility.getEntitiesOfClass(Scp1730PitForm.class, Hole)) {
                Each.discard();
            }
            Facility.setChunkForced(ChunkX, ChunkZ, false);
            Scp1730GameTests.dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp1730pit_weaken")
    public static void anchorsDazeThenRecover(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730PitForm Pit = pit(Helper);
        ServerPlayer User = watcher(Helper, "scp1730pit_weaken", true);
        RealityAnchorField[] Field = {RealityAnchorField.spawn(World, Pit.getBoundingBox().getCenter())};
        Helper.startSequence().thenWaitUntil(() -> {
            if (!Field[0].isAlive()) {
                Field[0] = RealityAnchorField.spawn(World, Pit.getBoundingBox().getCenter());
            }
            Helper.assertTrue(Pit.anchored(), "the anchor field does not reach SCP-1730-3");
            Helper.assertTrue(Pit.weakness() > 10, "anchors do not weaken SCP-1730-3");
        }).thenWaitUntil(() -> Helper.assertTrue(Pit.dazed(), "SCP-1730-3 never dazed at the threshold, weakness " + Pit.weakness())).thenExecute(() -> {
            Field[0].discard();
            Pit.shortenState(2);
        }).thenWaitUntil(() -> Helper.assertTrue(Pit.state() == Scp1730PitForm.StateActive, "SCP-1730-3 never recovered from the daze")).thenExecute(() -> {
            Helper.assertTrue(Pit.weaknessExact() > 30.0f && Pit.weaknessExact() < 60.0f, "recovery did not keep part of the weakness: " + Pit.weaknessExact());
            Pit.discard();
            Scp1730GameTests.dismiss(User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1730pit_damage")
    public static void damageReducedOutsideDaze(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730PitForm Pit = pit(Helper);
        float Full = Pit.getHealth();
        Pit.hurt(World.damageSources().generic(), 40.0f);
        float Normal = Full - Pit.getHealth();
        Helper.assertTrue(Normal > 0.0f && Normal <= 40.0f * Scp1730PitForm.NormalShare + 0.01f, "unweakened SCP-1730-3 took " + Normal + " from 40");
        Helper.runAfterDelay(25, () -> {
            Pit.daze(World);
            float Before = Pit.getHealth();
            Pit.hurt(World.damageSources().generic(), 40.0f);
            float Dazed = Before - Pit.getHealth();
            Helper.assertTrue(Dazed >= 39.0f, "dazed SCP-1730-3 took only " + Dazed + " from 40");
            Pit.discard();
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1730pit_capture")
    public static void podOnlyWhileDazedThenReleases(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730PitForm Pit = pit(Helper);
        ServerPlayer User = watcher(Helper, "scp1730pit_keeper", false);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.values()[FragmentRarity.values().length - 1]));
        Helper.assertTrue(StasisPods.isVetoed(Pit), "an unweakened SCP-1730-3 is not refused by stasis pods");
        Helper.assertTrue(!StasisPods.capture(World, User, Pod, Pit), "a stasis pod captured SCP-1730-3 outside the daze");
        Pit.daze(World);
        Helper.assertTrue(!StasisPods.isVetoed(Pit), "a dazed SCP-1730-3 is still refused");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Pit), "the stasis pod failed to capture a dazed SCP-1730-3");
        Helper.assertTrue(Pit.isRemoved() || Pit.finished(), "the captured pit form stayed in the world, removed " + Pit.isRemoved() + " finished " + Pit.finished());
        StasisContents Held = StasisPodItem.contents(Pod);
        Helper.assertTrue(Held != null && Held.copyData().getString("id").equals(Scp1730PitRegistry.MassType.getId().toString()), "the pod does not hold SCP-1730-3 Mass");
        Vec3 Where = Helper.absoluteVec(new Vec3(16.0, 1.0, 16.0));
        Helper.assertTrue(StasisPods.breakOut(World, Pod.copy(), Where, null), "breaking the pod released nothing");
        Helper.runAfterDelay(2, () -> {
            List<Scp1730Mass> Found = World.getEntitiesOfClass(Scp1730Mass.class, new AABB(Where, Where).inflate(6.0));
            Helper.assertTrue(Found.size() == 1, "breaking the pod did not release exactly one SCP-1730-3 Mass: " + Found.size());
            Scp1730Mass Mass = Found.getFirst();
            Helper.assertTrue(Mass.getHealth() > Scp1730Mass.MaxHealth * 0.9f && Mass.getMaxHealth() == Scp1730Mass.MaxHealth, "the released Mass kept the pit form's health");
            Helper.assertTrue(World.getEntitiesOfClass(Scp1730PitForm.class, new AABB(Where, Where).inflate(32.0)).isEmpty(), "a pit form came out of the pod");
            Mass.discard();
            Scp1730GameTests.dismiss(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730pit_folder")
    public static void folderHoldsMass(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Master = new BlockPos(6, 1, 6);
        Helper.setBlock(Master, Scp1730Registry.Folder.get().defaultBlockState().setValue(RealityFolderBlock.Part, 62));
        RealityFolderBlockEntity Folder = (RealityFolderBlockEntity) World.getBlockEntity(Helper.absolutePos(Master));
        Helper.assertTrue(Folder != null, "the Reality Folder has no block entity");
        Helper.onEachTick(() -> Folder.feed(RealityFolderBlockEntity.Demand));
        Scp1730Mass Mass = Helper.spawn(Scp1730PitRegistry.MassType.get(), new BlockPos(13, 1, 13));
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Folder.fieldUp(), "the Folder never raised its field")).thenExecute(() -> {
            Mass.setUnrest(Scp1730Mass.GraceTicks - 5);
            Mass.check(World);
        }).thenIdle(60).thenExecute(() -> {
            Helper.assertTrue(Mass.isAlive() && !Mass.isRemoved(), "a Folder-held Mass escaped");
            Helper.assertTrue(Mass.contained(), "the Folder field does not count as containment");
            Helper.assertTrue(!Mass.digging(), "a Folder-held Mass started digging");
            Helper.assertTrue(Mass.unrest() < Scp1730Mass.GraceTicks - 5, "unrest kept rising inside the Folder field");
            Mass.discard();
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 420, batch = "scp1730pit_digout")
    public static void massDigsOutWithoutFolder(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Start = new BlockPos(16, 1, 16);
        Scp1730Mass Mass = Helper.spawn(Scp1730PitRegistry.MassType.get(), Start);
        RealityAnchorField Field = RealityAnchorField.spawn(World, Mass.getBoundingBox().getCenter());
        BlockPos Floor = Helper.absolutePos(Start.below());
        Helper.startSequence().thenIdle(12).thenExecute(() -> {
            Helper.assertTrue(!Mass.contained(), "a thrown anchor counted as containment");
            Mass.setUnrest(Scp1730Mass.GraceTicks - 5);
        }).thenWaitUntil(() -> Helper.assertTrue(Mass.digging(), "an uncontained Mass never started digging after the grace period")).thenExecute(Field::discard)
                .thenWaitUntil(() -> Helper.assertTrue(Mass.isRemoved(), "the digging Mass never burrowed away, progress " + Mass.digProgress())).thenExecute(() -> {
                    Helper.assertTrue(World.getBlockState(Floor).isAir(), "the Mass escaped without breaking the floor under it");
                    Entity Leftover = World.getEntity(Mass.getUUID());
                    Helper.assertTrue(Leftover == null, "the Mass is still in the world");
                }).thenSucceed();
    }
}
