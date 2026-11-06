package net.rai.scp_unleashed.scp001.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FlashEvent;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlock;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.ArchiveBlock;
import net.rai.scp_unleashed.archive.ArchiveBlockEntity;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.archive.document.DocumentClaims;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentRoller;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlRoster;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.extraction.ExtractionRegistry;
import net.rai.scp_unleashed.rifttech.ApocalypticKeys;
import net.rai.scp_unleashed.rifttech.RiftTechRegistry;
import net.rai.scp_unleashed.scp001.Scp001Flash;
import net.rai.scp_unleashed.scp001.Scp001Prototype;
import net.rai.scp_unleashed.scp001.Scp001Registry;
import net.rai.scp_unleashed.scp001.Scp001Restraint;
import net.rai.scp_unleashed.scp001.Scp001Returns;
import net.rai.scp_unleashed.scp001.Scp001Singularity;
import net.rai.scp_unleashed.scp001.Scp001Warp;
import net.rai.scp_unleashed.scp148.Scp148Registry;
import net.rai.scp_unleashed.scp268.Scp268Registry;
import net.rai.scp_unleashed.senses.SensesRegistry;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp001GameTests {
    public static final String Arena = "scp001_arena";

    private Scp001GameTests() {
    }

    private static String state(Scp001Prototype Creature) {
        return " [target " + (Creature.getTarget() == null ? "none" : Creature.getTarget().getName().getString()) + ", action " + Creature.action() + ", locked " + Creature.isLocked()
                + ", restrained " + Creature.isRestrained() + ", health " + Creature.getHealth() + ", at " + Creature.blockPosition() + ", alive " + Creature.isAlive() + "]";
    }

    private static Scp001Prototype spawn(GameTestHelper Helper, double X, double Z) {
        Scp001Prototype Creature = Helper.spawn(Scp001Registry.Prototype.get(), new Vec3(X, 2.0, Z));
        Creature.setYRot(0.0f);
        return Creature;
    }

    private static ServerPlayer survivor(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp001_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp001Prototype.class);
        Scp001Returns.get(Helper.getLevel()).clear();
    }

    private static void face(ServerPlayer User, Vec3 Target) {
        Vec3 Eye = User.getEyePosition();
        Vec3 Delta = Target.subtract(Eye);
        float Yaw = (float) (Math.toDegrees(Math.atan2(Delta.z, Delta.x)) - 90.0);
        float Pitch = (float) -Math.toDegrees(Math.atan2(Delta.y, Math.sqrt(Delta.x * Delta.x + Delta.z * Delta.z)));
        User.setYRot(Yaw);
        User.setXRot(Pitch);
        User.setYHeadRot(Yaw);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001_huntsMob")
    public static void huntsMob(GameTestHelper Helper) {
        Scp001Prototype Creature = spawn(Helper, 4.5, 4.5);
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, 2.0, 12.5));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.getTarget() == Prey, "SCP-001 has not picked the pig as prey" + state(Creature));
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001_huntsPlayerFirst")
    public static void huntsPlayerFirst(GameTestHelper Helper) {
        Scp001Prototype Creature = spawn(Helper, 3.5, 3.5);
        Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2.0, 3.5));
        ServerPlayer User = survivor(Helper, 20.5, 20.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.getTarget() == User, "SCP-001 did not prefer the player over a much nearer pig" + state(Creature));
            dismiss(Helper, User);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp001_dropsMobForPlayer")
    public static void dropsMobForPlayer(GameTestHelper Helper) {
        Scp001Prototype Creature = spawn(Helper, 4.5, 4.5);
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(9.5, 2.0, 4.5));
        ServerPlayer[] Arrived = new ServerPlayer[1];
        long[] Seen = new long[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.getTarget() == Prey, "SCP-001 did not hunt the pig while no player was there" + state(Creature)))
                .thenExecute(() -> {
                    Arrived[0] = survivor(Helper, 19.5, 19.5);
                    Seen[0] = Helper.getTick();
                })
                .thenWaitUntil(() -> Helper.assertTrue(Creature.getTarget() == Arrived[0], "SCP-001 stayed on the pig after a player appeared" + state(Creature)))
                .thenExecute(() -> {
                    Helper.assertTrue(Helper.getTick() - Seen[0] <= Scp001Prototype.RetargetInterval * 3L, "SCP-001 took " + (Helper.getTick() - Seen[0]) + " ticks to switch to the player");
                    Pig Biter = Helper.spawnWithNoFreeWill(EntityType.PIG, Creature.position().add(1.0, 0.0, 0.0).subtract(Helper.absoluteVec(Vec3.ZERO)));
                    Creature.hurt(Helper.getLevel().damageSources().mobAttack(Biter), 0.5f);
                    Helper.assertTrue(Creature.getTarget() == Arrived[0], "a pig biting SCP-001 pulled it off the player" + state(Creature));
                    dismiss(Helper, Arrived[0]);
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001_ignoresNeglectedPlayer")
    public static void ignoresNeglectedPlayer(GameTestHelper Helper) {
        Scp001Prototype Creature = spawn(Helper, 4.5, 4.5);
        ServerPlayer User = survivor(Helper, 9.5, 4.5);
        User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp268Registry.Cap.get()));
        Helper.runAfterDelay(60, () -> {
            Helper.assertTrue(Creature.getTarget() != User, "SCP-001 hunted a player wearing SCP-268");
            Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(14.5, 2.0, 14.5));
            Helper.runAfterDelay(30, () -> {
                Helper.assertTrue(Creature.getTarget() == Prey, "SCP-001 ignored the pig behind the SCP-268 wearer" + state(Creature));
                dismiss(Helper, User);
                tidy(Helper);
            Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_warpsLandOnValidSpots")
    public static void warpsLandOnValidSpots(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 6; X <= 18; X++) {
            for (int Y = 2; Y <= 4; Y++) {
                if (X % 3 == 0) {
                    Helper.setBlock(new BlockPos(X, Y, 15), Blocks.STONE);
                }
                Helper.setBlock(new BlockPos(X, Y, 7), Blocks.STONE);
            }
        }
                Scp001Prototype Creature = spawn(Helper, 6.5, 11.5);
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(13.5, 2.0, 11.5));
        RandomSource Random = RandomSource.create(1001L);
        int Found = 0;
        for (int Attempt = 0; Attempt < 60; Attempt++) {
            Optional<Vec3> Spot = Scp001Warp.find(World, Creature, Prey, Random);
            if (Spot.isEmpty()) {
                continue;
            }
            Found++;
            Vec3 Landing = Spot.get();
            Helper.assertTrue(World.noCollision(Creature, Scp001Warp.roomAt(Landing)), "a warp landed inside a wall at " + Landing);
            Helper.assertTrue(!World.containsAnyLiquid(Scp001Warp.roomAt(Landing)), "a warp landed in water at " + Landing);
            Helper.assertTrue(World.getBlockState(BlockPos.containing(Landing).below()).isSolid(), "a warp landed over open air at " + Landing);
            double Jump = Landing.distanceTo(Creature.position());
            Helper.assertTrue(Jump <= Scp001Warp.MaxJump + 0.01, "a warp jumped " + Jump + " blocks");
            Helper.assertTrue(Landing.distanceTo(Prey.position()) <= Scp001Warp.FarTarget + 1.0, "a warp landed too far from the target");
            Helper.assertTrue(Scp001Warp.sees(World, Landing.add(0.0, Scp001Prototype.EyeHeight, 0.0), Prey.getEyePosition()), "a warp landed out of sight of the target");
        }
        BlockPos Probe = BlockPos.containing(Helper.absoluteVec(new Vec3(16.5, 2.0, 11.5)));
        String Why = " probe " + Probe + " fits " + Scp001Warp.fits(World, Creature, Probe) + " below " + World.getBlockState(Probe.below()) + " clear "
                + World.noCollision(Creature, Scp001Warp.roomAt(Vec3.atBottomCenterOf(Probe))) + " type "
                + net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getPathTypeStatic(Creature, Probe) + " creature " + Creature.blockPosition() + " pig " + Prey.blockPosition();
        Helper.assertTrue(Found > 20, "too few warp spots were found: " + Found + Why);
        Vec3 Before = Creature.position();
        Helper.assertTrue(Creature.warpTo(Helper.absoluteVec(new Vec3(16.5, 2.0, 11.5))), "SCP-001 refused a clear warp spot");
        Helper.assertTrue(Creature.position().distanceTo(Before) > 5.0, "SCP-001 did not move when it warped");
        Helper.assertTrue(!Creature.warpTo(Helper.absoluteVec(new Vec3(9.5, 2.0, 7.5))), "SCP-001 warped into a wall");
        tidy(Helper);
            Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_flashBlindsFacingNotBlindfolded")
    public static void flashBlindsFacingNotBlindfolded(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        ServerPlayer Facing = survivor(Helper, 12.5, 6.5);
        ServerPlayer Covered = survivor(Helper, 6.5, 12.5);
        ServerPlayer Away = survivor(Helper, 18.5, 12.5);
        Covered.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
        Helper.runAfterDelay(2, () -> {
            Vec3 Orb = Creature.orbPosition();
            face(Facing, Orb);
            face(Covered, Orb);
            face(Away, Orb.add(Away.position().subtract(Orb).scale(2.0)));
            Helper.assertTrue(Scp001Flash.exposure(World, Facing, Orb) == Scp001Flash.Exposure.Full, "the facing player was not fully exposed");
            Helper.assertTrue(Scp001Flash.exposure(World, Covered, Orb) == Scp001Flash.Exposure.None, "the blindfolded player was exposed");
            Helper.assertTrue(Scp001Flash.exposure(World, Away, Orb) == Scp001Flash.Exposure.Glare, "the player facing away was not only glared");
            Scp001Flash.burst(World, Creature);
            Helper.assertTrue(Facing.hasEffect(MobEffects.CONFUSION), "the facing player has no nausea");
            Helper.assertTrue(!Covered.hasEffect(MobEffects.CONFUSION), "the blindfolded player got nausea");
            Helper.assertTrue(!Away.hasEffect(MobEffects.CONFUSION), "the player facing away got nausea");
            dismiss(Helper, Facing);
            dismiss(Helper, Covered);
            dismiss(Helper, Away);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001_escapesAtLowHealth")
    public static void escapesAtLowHealth(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(16.5, 2.0, 12.5));
        Creature.setHealth(Creature.getMaxHealth() * 0.15f);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isRemoved(), "SCP-001 has not left yet");
            Helper.assertTrue(!Scp001Returns.get(World).pending().isEmpty(), "no return was scheduled");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001_anchorFieldDazes")
    public static void anchorFieldDazes(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(16.5, 2.0, 12.5));
        RealityAnchorField Field = RealityAnchorField.spawn(World, Helper.absoluteVec(new Vec3(11.5, 3.0, 12.5)));
        Vec3[] Start = new Vec3[1];
        Helper.runAfterDelay(4, () -> {
            Helper.assertTrue(Creature.isLocked() && Creature.anchored(), "the anchor field did not daze SCP-001" + state(Creature));
            Helper.assertTrue(!Creature.isRestrained(), "a thrown anchor held SCP-001 instead of dazing it");
            Creature.setHealth(Creature.getMaxHealth() * 0.15f);
            Start[0] = Creature.position();
        });
        Helper.runAfterDelay(64, () -> {
            Helper.assertTrue(Creature.isAlive() && !Creature.isRemoved(), "SCP-001 escaped while dazed in the field");
            Helper.assertTrue(Creature.isLocked(), "the daze ended while SCP-001 was still in the field");
            Helper.assertTrue(Creature.position().distanceTo(Start[0]) < 0.3, "SCP-001 moved while dazed from " + Start[0] + " to " + Creature.position());
            Helper.assertTrue(Creature.action() != Scp001Prototype.ActionEscape, "SCP-001 started to escape while dazed");
            Field.discard();
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_flashbangNoLongerDazes")
    public static void flashbangNoLongerDazes(GameTestHelper Helper) {
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        FlashEvent.fire(Helper.getLevel(), Helper.absoluteVec(new Vec3(10.5, 3.0, 12.5)), 8.0, null);
        Helper.assertTrue(!Creature.isLocked(), "a flashbang dazed SCP-001");
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(!Creature.isLocked(), "a flashbang dazed SCP-001" + state(Creature));
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_podNeedsTheLock")
    public static void podNeedsTheLock(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Creature) != null, "a pod would take SCP-001 while it moves freely");
        Helper.assertTrue(!StasisPods.capture(World, User, Pod, Creature), "a pod captured SCP-001 while it moved freely");
        Creature.daze(Scp001Prototype.LockTicks);
        Helper.assertTrue(StasisPods.refusal(User, Pod, Creature) == null, "a pod refuses SCP-001 while it is dazed");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Creature), "the pod failed to capture dazed SCP-001");
        Helper.assertTrue(Creature.isRemoved(), "SCP-001 is still in the world after capture");
        Helper.assertTrue(StasisPodItem.isOccupied(Pod), "the pod holds nothing");
        tidy(Helper);
            Helper.succeed();
    }

    private static ItemStack captured(GameTestHelper Helper, double X, double Z) {
        Scp001Prototype Creature = spawn(Helper, X, Z);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Creature.daze(Scp001Prototype.LockTicks);
        Helper.assertTrue(StasisPods.capture(Helper.getLevel(), User, Pod, Creature), "setup capture failed");
        return Pod;
    }

    private static final BlockPos FolderSpot = new BlockPos(8, 1, 16);

    private static RealityFolderBlockEntity folder(GameTestHelper Helper) {
        Helper.setBlock(FolderSpot, Scp1730Registry.Folder.get().defaultBlockState().setValue(RealityFolderBlock.Part, 62));
        RealityFolderBlockEntity Folder = (RealityFolderBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(FolderSpot));
        Helper.assertTrue(Folder != null, "the Reality Folder has no block entity");
        Folder.raiseForTest();
        Helper.onEachTick(() -> Folder.feed(RealityFolderBlockEntity.Demand));
        return Folder;
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001_folderHoldsWithoutDaze")
    public static void folderHoldsWithoutDaze(GameTestHelper Helper) {
        folder(Helper);
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isRestrained(), "SCP-001 inside a running Reality Folder field was not held" + state(Creature));
            Helper.assertTrue(!Creature.isLocked(), "the Reality Folder dazed SCP-001 instead of holding it");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp001_releaseInCellIsPassive")
    public static void releaseInCellIsPassive(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        folder(Helper);
        ItemStack Pod = captured(Helper, 4.5, 4.5);
        Helper.assertTrue(Scp001Restraint.holds(World, Helper.absoluteVec(new Vec3(12.5, 3.0, 12.5))), "the Reality Folder field does not hold SCP-001");
        StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(12.5, 2.0, 12.5)), null);
        Scp001Prototype Creature = World.getEntitiesOfClass(Scp001Prototype.class, Helper.getBounds()).stream().findFirst().orElseThrow();
        Helper.assertTrue(Creature.isRestrained(), "SCP-001 released in a Reality Folder field is not restrained");
        Pig Watcher = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(12.5, 2.0, 21.5));
        float WatcherHealth = Watcher.getHealth();
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Creature.getTarget() != Watcher, "restrained SCP-001 hunted a pig six blocks away");
            Helper.assertTrue(Watcher.getHealth() >= WatcherHealth, "restrained SCP-001 hurt a distant pig" + state(Creature) + " pig " + Watcher.blockPosition() + " health " + Watcher.getHealth());
            Helper.assertTrue(Creature.position().distanceTo(Helper.absoluteVec(new Vec3(12.5, 2.0, 12.5))) < 5.0, "restrained SCP-001 left its cell");
            Pig Near = Helper.spawnWithNoFreeWill(EntityType.PIG, Helper.relativeVec(Creature.position()).add(1.6, 0.0, 0.0));
            float NearHealth = Near.getHealth();
            Helper.runAfterDelay(60, () -> {
                Helper.assertTrue(Near.getHealth() < NearHealth || !Near.isAlive(), "restrained SCP-001 ignored a pig within three blocks");
                Helper.assertTrue(Creature.isRestrained(), "SCP-001 lost its restraint");
                tidy(Helper);
            Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001_releaseOutsideHunts")
    public static void releaseOutsideHunts(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ItemStack Pod = captured(Helper, 4.5, 4.5);
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(18.5, 2.0, 18.5));
        StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(12.5, 2.0, 12.5)), null);
        Scp001Prototype Creature = World.getEntitiesOfClass(Scp001Prototype.class, Helper.getBounds()).stream().findFirst().orElseThrow();
        Helper.assertTrue(!Creature.isRestrained(), "SCP-001 is restrained without a Reality Folder nearby");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.getTarget() == Prey, "SCP-001 released in the open is not hunting" + state(Creature));
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001_restraintSurvivesReload")
    public static void restraintSurvivesReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        folder(Helper);
        ItemStack Pod = captured(Helper, 4.5, 4.5);
        StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(12.5, 2.0, 12.5)), null);
        Scp001Prototype Creature = World.getEntitiesOfClass(Scp001Prototype.class, Helper.getBounds()).stream().findFirst().orElseThrow();
        Helper.assertTrue(Creature.isRestrained(), "setup: not restrained");
        CompoundTag Saved = new CompoundTag();
        Creature.saveWithoutId(Saved);
        Creature.discard();
        Scp001Prototype Loaded = Scp001Registry.Prototype.get().create(World);
        Loaded.load(Saved);
        Loaded.setUUID(UUID.randomUUID());
        Helper.assertTrue(Loaded.isRestrained(), "the restraint was lost when the entity was saved and loaded");
        World.addFreshEntity(Loaded);
        Helper.runAfterDelay(60, () -> {
            Helper.assertTrue(Loaded.isRestrained(), "the reloaded SCP-001 broke free inside its cell");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 180, batch = "scp001_restraintLostReturnsToHunting")
    public static void restraintLostReturnsToHunting(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        folder(Helper);
        ItemStack Pod = captured(Helper, 4.5, 4.5);
        StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(12.5, 2.0, 12.5)), null);
        Scp001Prototype Creature = World.getEntitiesOfClass(Scp001Prototype.class, Helper.getBounds()).stream().findFirst().orElseThrow();
        Helper.assertTrue(Creature.isRestrained(), "setup: not restrained");
        Helper.setBlock(FolderSpot, Blocks.AIR);
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(20.5, 2.0, 20.5));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(!Creature.isRestrained(), "SCP-001 is still restrained after the Reality Folder was removed");
            Helper.assertTrue(Creature.getTarget() == Prey, "SCP-001 did not return to hunting" + state(Creature));
            tidy(Helper);
        });
    }

    private static boolean atFloor(Scp001Prototype Creature) {
        return Creature.isAlive() && !Creature.isRemoved() && Math.abs(Creature.getHealth() - Creature.floorHealth()) < 0.01f;
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp001_deathLeavesNoNeutralizedState")
    public static void deathLeavesNoNeutralizedState(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Creature.daze(Scp001Prototype.LockTicks);
        Creature.hurt(World.damageSources().magic(), 10000.0f);
        Helper.assertTrue(atFloor(Creature), "a dazed SCP-001 did not stop at its floor" + state(Creature));
        Creature.kill();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isRemoved(), "SCP-001 survived the kill command");
            Helper.assertTrue(Scp001Returns.get(World).pending().isEmpty(), "the kill command still scheduled a return");
            AnomalyDocument Document = AnomalyDocuments.server().get(Scp001Registry.DocumentId);
            Helper.assertTrue(Document != null && Document.Classification() == DocumentClass.Keter, "the SCP-001 file is no longer Keter");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp001_killRemovesForGood")
    public static void killRemovesForGood(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Creature.kill();
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isRemoved(), "the kill command did not remove SCP-001");
            Helper.assertTrue(Scp001Returns.get(World).pending().isEmpty(), "the kill command scheduled a return");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001_floorHoldsAgainstHugeHit")
    public static void floorHoldsAgainstHugeHit(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Creature.hurt(World.damageSources().magic(), 10000.0f);
        Helper.assertTrue(atFloor(Creature), "a 10000 damage hit went past the floor" + state(Creature));
        Helper.assertTrue(Creature.escaping(), "SCP-001 at its floor is not escaping" + state(Creature));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isRemoved(), "SCP-001 has not stepped through the seam");
            Helper.assertTrue(Scp001Returns.get(World).pending().size() == 1, "the escape did not schedule exactly one return");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp001_anchorPinsTheSeam")
    public static void anchorPinsTheSeam(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        RealityAnchorField Field = RealityAnchorField.spawn(World, Helper.absoluteVec(new Vec3(14.5, 3.0, 12.5)));
        Helper.assertTrue(Creature.isPinned(), "a thrown anchor within reach did not pin SCP-001" + state(Creature));
        Helper.assertTrue(!Scp001Restraint.holds(World, Creature.position().add(0.0, 1.0, 0.0)), "a thrown anchor should pin, not hold" + state(Creature));
        Creature.hurt(World.damageSources().magic(), 10000.0f);
        Helper.assertTrue(atFloor(Creature), "a 10000 damage hit went past the floor inside an anchor field" + state(Creature));
        Helper.assertTrue(!Creature.escaping(), "SCP-001 opened a seam inside an anchor field" + state(Creature));
        Helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    Helper.assertTrue(Creature.isAlive() && !Creature.isRemoved(), "SCP-001 left or died while pinned" + state(Creature));
                    Helper.assertTrue(!Creature.escaping(), "SCP-001 began escaping while pinned" + state(Creature));
                    Helper.assertTrue(Creature.isLocked(), "SCP-001 did not reel inside the anchor field" + state(Creature));
                    Field.discard();
                })
                .thenWaitUntil(() -> Helper.assertTrue(Creature.isRemoved(), "SCP-001 did not slip away once the anchor field was gone" + state(Creature)))
                .thenExecute(() -> {
                    Helper.assertTrue(Scp001Returns.get(World).pending().size() == 1, "the late escape did not schedule exactly one return");
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001_escapeIgnoresDamage")
    public static void escapeIgnoresDamage(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Creature.forceEscape();
        Helper.assertTrue(Creature.escaping(), "forced escape did not start");
        float Before = Creature.getHealth();
        Pig Biter = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(13.5, 2.0, 12.5));
        Helper.assertTrue(!Creature.hurt(World.damageSources().magic(), 40.0f), "magic hurt SCP-001 during its escape");
        Helper.assertTrue(!Creature.hurt(World.damageSources().mobAttack(Biter), 40.0f), "a bite hurt SCP-001 during its escape");
        Creature.setDeltaMovement(Vec3.ZERO);
        Creature.knockback(2.0, 1.0, 0.0);
        Helper.assertTrue(Creature.getHealth() == Before, "SCP-001 lost health during its escape");
        Helper.assertTrue(Creature.getDeltaMovement().lengthSqr() < 1.0E-6, "SCP-001 was knocked back during its escape");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isRemoved(), "SCP-001 did not finish its escape");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp001_dazedFloorWaitsForTheDaze")
    public static void dazedFloorWaitsForTheDaze(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Creature.daze(40);
        Creature.hurt(World.damageSources().magic(), 10000.0f);
        Helper.assertTrue(atFloor(Creature), "a dazed SCP-001 went past its floor" + state(Creature));
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(atFloor(Creature) && Creature.isLocked(), "the dazed SCP-001 did not hold at its floor" + state(Creature));
            Helper.assertTrue(!Creature.escaping(), "SCP-001 escaped while still dazed");
            Helper.runAfterDelay(20, () -> {
                Helper.assertTrue(!Creature.isLocked(), "the daze did not end");
                Helper.assertTrue(Creature.escaping() || Creature.isRemoved(), "SCP-001 did not escape once the daze ended" + state(Creature));
                tidy(Helper);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_podTakesDazedFloor")
    public static void podTakesDazedFloor(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Creature.daze(Scp001Prototype.LockTicks);
        Creature.hurt(World.damageSources().magic(), 10000.0f);
        Helper.assertTrue(atFloor(Creature), "a dazed SCP-001 went past its floor");
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Creature), "the pod failed to take SCP-001 dazed at its floor");
        Helper.assertTrue(Creature.isRemoved() && StasisPodItem.isOccupied(Pod), "the pod does not hold SCP-001");
        tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp001_restrainedFloorStaysAndHeals")
    public static void restrainedFloorStaysAndHeals(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        folder(Helper);
        ItemStack Pod = captured(Helper, 4.5, 4.5);
        StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(12.5, 2.0, 12.5)), null);
        Scp001Prototype Creature = World.getEntitiesOfClass(Scp001Prototype.class, Helper.getBounds()).stream().findFirst().orElseThrow();
        Helper.assertTrue(Creature.isRestrained(), "SCP-001 released in the cell is not restrained");
        Creature.hurt(World.damageSources().magic(), 10000.0f);
        Helper.assertTrue(atFloor(Creature), "restrained SCP-001 went past its floor" + state(Creature));
        Helper.assertTrue(!Creature.escaping(), "restrained SCP-001 started to escape");
        Helper.runAfterDelay(Scp001Prototype.HealInterval + 20, () -> {
            Helper.assertTrue(Creature.isAlive() && !Creature.isRemoved() && Creature.isRestrained(), "restrained SCP-001 left its cell" + state(Creature));
            Helper.assertTrue(!Creature.escaping(), "restrained SCP-001 escaped");
            Helper.assertTrue(Creature.getHealth() > Creature.floorHealth(), "restrained SCP-001 did not heal" + state(Creature));
            Helper.assertTrue(Scp001Returns.get(World).pending().isEmpty(), "a return was scheduled for a restrained SCP-001");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001_voidReturnsLater")
    public static void voidReturnsLater(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        BlockPos Ground = Creature.blockPosition();
        Helper.runAfterDelay(5, () -> {
            Creature.teleportTo(Creature.getX(), World.getMinBuildHeight() - 80.0, Creature.getZ());
            Helper.runAfterDelay(5, () -> {
                Helper.assertTrue(Creature.isRemoved(), "SCP-001 is still below the world");
                Helper.assertTrue(Scp001Returns.get(World).pending().size() == 1, "the void did not schedule a return");
                Helper.assertTrue(Scp001Returns.get(World).pending().get(0).Origin().distManhattan(Ground) <= 3, "the return is not anchored to where it last stood: "
                        + Scp001Returns.get(World).pending().get(0).Origin() + " vs " + Ground);
                tidy(Helper);
                Helper.succeed();
            });
        });
    }

    private static ArchiveBlockEntity archive(GameTestHelper Helper, BlockPos Spot) {
        Helper.setBlock(Spot, ArchiveRegistry.Archive.get().defaultBlockState());
        Helper.setBlock(Spot.above(), ArchiveRegistry.Archive.get().defaultBlockState().setValue(ArchiveBlock.Half, DoubleBlockHalf.UPPER));
        ArchiveBlockEntity Terminal = Helper.getBlockEntity(Spot);
        if (Terminal == null) {
            Helper.fail("the archive did not place", Spot);
        }
        return Terminal;
    }

    private static void feed(ArchiveBlockEntity Terminal, ItemStack Fragment) {
        Terminal.getStorage().setStackInSlot(ArchiveBlockEntity.FragmentSlot, Fragment);
        Terminal.getStorage().setStackInSlot(ArchiveBlockEntity.PaperSlot, new ItemStack(Items.PAPER, ArchiveBlockEntity.PaperCost * 2));
    }

    private static ItemStack golden() {
        return new ItemStack(RiftTechRegistry.ApocalypticFragment.get());
    }

    private static ServerPlayer reader(GameTestHelper Helper, String Name) {
        return new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), Name));
    }

    private static void rules(GameTestHelper Helper, boolean Repeat, boolean Personal) {
        MinecraftServer Server = Helper.getLevel().getServer();
        Server.getGameRules().getRule(ArchiveRules.RepeatFiles).set(Repeat, Server);
        Server.getGameRules().getRule(ArchiveRules.PerPlayer).set(Personal, Server);
    }

    private static void forget(GameTestHelper Helper, ServerPlayer... Readers) {
        MinecraftServer Server = Helper.getLevel().getServer();
        DocumentClaims Claims = DocumentClaims.get(Server);
        for (ServerPlayer Reader : Readers) {
            Claims.giveBack(Scp001Registry.DocumentId, ArchiveRules.Mode.World, Reader.getUUID());
        }
        Claims.release(Scp001Registry.DocumentId);
        for (java.util.Map.Entry<ResourceLocation, AnomalyDocument> Entry : AnomalyDocuments.server().entrySet()) {
            if (Entry.getValue().Pool() == ArchivePool.Apocalyptic && !Entry.getKey().equals(Scp001Registry.DocumentId)) {
                Claims.release(Entry.getKey());
                Claims.take(Entry.getKey(), ArchiveRules.Mode.World, OtherApocalypticHolder);
                for (ServerPlayer Reader : Readers) {
                    Claims.take(Entry.getKey(), ArchiveRules.Mode.PerPlayer, Reader.getUUID());
                }
            }
        }
        rules(Helper, false, false);
    }

    private static final java.util.UUID OtherApocalypticHolder = java.util.UUID.nameUUIDFromBytes("scp001_other_apocalyptic".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    private static boolean onlyApocalyptic(List<ResourceLocation> Cards) {
        for (ResourceLocation Card : Cards) {
            AnomalyDocument Document = AnomalyDocuments.server().get(Card);
            if (Document == null || Document.Pool() != ArchivePool.Apocalyptic) {
                return false;
            }
        }
        return true;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_goldenFragmentRollsOnlyItsPool")
    public static void goldenFragmentRollsOnlyItsPool(GameTestHelper Helper) {
        ServerPlayer User = reader(Helper, "scp001_reader");
        forget(Helper, User);
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp001Registry.DocumentId);
        Helper.assertTrue(Document != null && Document.Pool() == ArchivePool.Apocalyptic && Document.Weight() > 0.0f, "the SCP-001 file is not in the apocalyptic pool");
        Helper.assertTrue(!Document.isListed(), "the SCP-001 file is listed with the ordinary files");
        Map<ResourceLocation, AnomalyDocument> Pool = DocumentRoller.eligible(Helper.getLevel().getServer(), User.getUUID(), ArchivePool.Apocalyptic);
        Helper.assertTrue(Pool.containsKey(Scp001Registry.DocumentId), "the apocalyptic pool does not hold SCP-001");
        Helper.assertTrue(Pool.values().stream().allMatch(Entry -> Entry.Pool() == ArchivePool.Apocalyptic), "the apocalyptic pool holds ordinary files");
        ArchiveBlockEntity Terminal = archive(Helper, new BlockPos(6, 2, 6));
        feed(Terminal, golden());
        Helper.assertTrue(Terminal.currentPool() == ArchivePool.Apocalyptic, "the archive did not read the golden fragment as apocalyptic");
        Helper.assertTrue(Terminal.roll(User), "the archive refused a golden fragment with SCP-001 unclaimed");
        Helper.assertTrue(Terminal.getCards().contains(Scp001Registry.DocumentId), "the golden roll did not deal SCP-001: " + Terminal.getCards());
        Helper.assertTrue(onlyApocalyptic(Terminal.getCards()), "the golden roll dealt ordinary files: " + Terminal.getCards());
        Helper.assertTrue(Terminal.getStorage().getStackInSlot(ArchiveBlockEntity.FragmentSlot).isEmpty(), "the golden fragment was not used up");
        Helper.assertTrue(Terminal.getStorage().getStackInSlot(ArchiveBlockEntity.PaperSlot).getCount() == ArchiveBlockEntity.PaperCost, "the golden roll did not take its paper");
        forget(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_abyssalNeverRollsScp001")
    public static void abyssalNeverRollsScp001(GameTestHelper Helper) {
        ServerPlayer User = reader(Helper, "scp001_reader");
        forget(Helper, User);
        rules(Helper, true, false);
        ItemStack Abyssal = new ItemStack(ExtractionRegistry.fragmentFor(FragmentRarity.Abyssal));
        Helper.assertTrue(ArchivePool.poolOf(Abyssal) == ArchivePool.Normal, "an abyssal fragment reads from the apocalyptic pool");
        Map<ResourceLocation, AnomalyDocument> Pool = DocumentRoller.eligible(Helper.getLevel().getServer(), User.getUUID(), ArchivePool.Normal);
        Helper.assertTrue(!Pool.containsKey(Scp001Registry.DocumentId), "the ordinary pool holds SCP-001");
        RandomSource Random = RandomSource.create(1001L);
        for (int Roll = 0; Roll < 200; Roll++) {
            Helper.assertTrue(!DocumentRoller.roll(Pool, FragmentRarity.Abyssal.getTier(), ArchiveBlockEntity.CardCount, Random).contains(Scp001Registry.DocumentId), "an abyssal roll dealt SCP-001");
        }
        ArchiveBlockEntity Terminal = archive(Helper, new BlockPos(6, 2, 6));
        feed(Terminal, Abyssal);
        if (Terminal.roll(User)) {
            Helper.assertTrue(!Terminal.getCards().contains(Scp001Registry.DocumentId), "the archive dealt SCP-001 from an abyssal fragment");
        }
        forget(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001_claimedFileRefusesGold")
    public static void claimedFileRefusesGold(GameTestHelper Helper) {
        ServerPlayer First = reader(Helper, "scp001_first");
        ServerPlayer Second = reader(Helper, "scp001_second");
        forget(Helper, First, Second);
        ArchiveBlockEntity Terminal = archive(Helper, new BlockPos(6, 2, 6));
        feed(Terminal, golden());
        Helper.assertTrue(Terminal.roll(First), "the archive refused the first golden roll");
        int Card = Terminal.getCards().indexOf(Scp001Registry.DocumentId);
        Helper.assertTrue(Card >= 0 && Terminal.pick(First, Card), "the SCP-001 card could not be printed");
        Helper.runAfterDelay(ArchiveBlockEntity.PrintTicks + 10, () -> {
            ItemStack Printed = Terminal.getStorage().getStackInSlot(ArchiveBlockEntity.OutputSlot);
            DocumentStamp Stamp = Printed.get(ArchiveRegistry.StampComponent.get());
            Helper.assertTrue(Printed.is(ArchiveRegistry.SealedDocument.get()) && Stamp != null && Stamp.Document().equals(Scp001Registry.DocumentId)
                    && Stamp.Classification() == DocumentClass.Keter, "the archive did not print a sealed SCP-001 file: " + Printed);
            Terminal.getStorage().setStackInSlot(ArchiveBlockEntity.OutputSlot, ItemStack.EMPTY);
            feed(Terminal, golden());
            Helper.assertTrue(!Terminal.roll(Second), "the archive dealt SCP-001 a second time under the default rules");
            Helper.assertTrue(!Terminal.roll(First), "the archive dealt SCP-001 again to its claimant");
            Helper.assertTrue(ApocalypticKeys.isKey(Terminal.getStorage().getStackInSlot(ArchiveBlockEntity.FragmentSlot)), "a refused golden roll took the fragment");
            Helper.assertTrue(Terminal.getStorage().getStackInSlot(ArchiveBlockEntity.PaperSlot).getCount() == ArchiveBlockEntity.PaperCost * 2, "a refused golden roll took paper");
            Helper.assertTrue(Terminal.getCards().isEmpty(), "a refused golden roll dealt cards");
            forget(Helper, First, Second);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_perPlayerFilesGoldForEachReader")
    public static void perPlayerFilesGoldForEachReader(GameTestHelper Helper) {
        ServerPlayer First = reader(Helper, "scp001_first");
        ServerPlayer Second = reader(Helper, "scp001_second");
        forget(Helper, First, Second);
        rules(Helper, false, true);
        DocumentClaims.get(Helper.getLevel().getServer()).take(Scp001Registry.DocumentId, ArchiveRules.Mode.PerPlayer, First.getUUID());
        ArchiveBlockEntity Terminal = archive(Helper, new BlockPos(6, 2, 6));
        feed(Terminal, golden());
        Helper.assertTrue(!Terminal.roll(First), "the claimant rolled SCP-001 again with per-player files");
        Helper.assertTrue(ApocalypticKeys.isKey(Terminal.getStorage().getStackInSlot(ArchiveBlockEntity.FragmentSlot)), "a refused golden roll took the fragment");
        Helper.assertTrue(Terminal.roll(Second), "a second reader could not roll SCP-001 with per-player files");
        Helper.assertTrue(Terminal.getCards().contains(Scp001Registry.DocumentId), "the second reader was not dealt SCP-001");
        forget(Helper, First, Second);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_repeatFilesRollsGoldAgain")
    public static void repeatFilesRollsGoldAgain(GameTestHelper Helper) {
        ServerPlayer User = reader(Helper, "scp001_reader");
        forget(Helper, User);
        DocumentClaims.get(Helper.getLevel().getServer()).take(Scp001Registry.DocumentId, ArchiveRules.Mode.World, User.getUUID());
        ArchiveBlockEntity Terminal = archive(Helper, new BlockPos(6, 2, 6));
        feed(Terminal, golden());
        Helper.assertTrue(!Terminal.roll(User), "a claimed SCP-001 was dealt without repeat files");
        rules(Helper, true, false);
        Helper.assertTrue(Terminal.roll(User), "SCP-001 could not be rolled again with repeat files");
        Helper.assertTrue(Terminal.getCards().contains(Scp001Registry.DocumentId), "the repeat roll did not deal SCP-001");
        forget(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp001_hopperFeedsGold")
    public static void hopperFeedsGold(GameTestHelper Helper) {
        ArchiveBlockEntity Terminal = archive(Helper, new BlockPos(6, 2, 6));
        BlockPos Chute = new BlockPos(7, 2, 6);
        Helper.setBlock(Chute, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST));
        HopperBlockEntity Feeder = Helper.getBlockEntity(Chute);
        Feeder.setItem(0, golden());
        Feeder.setItem(1, new ItemStack(Items.DIRT));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(ApocalypticKeys.isKey(Terminal.getStorage().getStackInSlot(ArchiveBlockEntity.FragmentSlot)), "the hopper did not feed the golden fragment into the archive");
            Helper.assertTrue(Terminal.currentPool() == ArchivePool.Apocalyptic, "the archive did not switch to the apocalyptic pool");
            Helper.assertTrue(Feeder.getItem(1).is(Items.DIRT), "the archive accepted dirt from the hopper");
        });
    }

    private static void clearOrbs(GameTestHelper Helper) {
        for (Scp001Singularity Orb : Helper.getLevel().getEntitiesOfClass(Scp001Singularity.class, Helper.getBounds().inflate(48.0))) {
            Orb.discard();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp001_castFormsFourToSeven")
    public static void castFormsFourToSeven(GameTestHelper Helper) {
        Scp001Prototype Creature = spawn(Helper, 4.5, 4.5);
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(16.5, 2.0, 4.5));
        Helper.assertTrue(Creature.forceSummon(Prey), "SCP-001 refused to summon with a pig in sight" + state(Creature));
        Helper.runAfterDelay(Scp001Prototype.SummonTicks - 3, () -> {
            int Count = Helper.getLevel().getEntitiesOfClass(Scp001Singularity.class, Helper.getBounds().inflate(8.0)).size();
            Helper.assertTrue(Count >= Scp001Prototype.OrbMin && Count <= Scp001Prototype.OrbMax, "the summon produced " + Count + " orbs");
            Helper.assertTrue(Count == Creature.orbs().size(), "SCP-001 tracks " + Creature.orbs().size() + " orbs but " + Count + " exist");
            Helper.assertTrue(Creature.orbs().stream().allMatch(Scp001Singularity::waiting), "an orb left the orbit before the cast");
            clearOrbs(Helper);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp001_castHitsTheTarget")
    public static void castHitsTheTarget(GameTestHelper Helper) {
        Scp001Prototype Creature = spawn(Helper, 4.5, 12.5);
        IronGolem Golem = Helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(15.5, 2.0, 12.5));
        Helper.assertTrue(Creature.forceSummon(Golem), "SCP-001 refused to summon at a golem in sight" + state(Creature));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Golem.isDeadOrDying() || Golem.isRemoved(), "the cast did not kill the golem" + state(Creature));
            Helper.assertTrue(!Creature.casting(), "SCP-001 is still casting");
            Helper.assertTrue(Creature.isAlive() && Creature.getHealth() == Creature.getMaxHealth(), "SCP-001 was hurt by its own cast");
            clearOrbs(Helper);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001_orbKillsInstantly")
    public static void orbKillsInstantly(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        IronGolem Golem = Helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new Vec3(14.5, 2.0, 12.5));
        Helper.assertTrue(Golem.getHealth() >= 100.0f, "the golem is not at full health");
        Scp001Singularity.fire(World, Helper.absoluteVec(new Vec3(8.5, 3.2, 12.5)), Golem.getBoundingBox().getCenter(), null);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Golem.isDeadOrDying() || Golem.isRemoved(), "the golem survived the orb with " + Golem.getHealth() + " health");
            clearOrbs(Helper);
        });
    }

    private static void block(GameTestHelper Helper) {
        for (int X = 8; X <= 12; X++) {
            for (int Y = 2; Y <= 6; Y++) {
                for (int Z = 8; Z <= 12; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE);
                }
            }
        }
        Helper.setBlock(new BlockPos(9, 3, 9), Blocks.BEDROCK);
        Helper.setBlock(new BlockPos(11, 5, 11), Scp148Registry.Ward.get());
        Helper.setBlock(new BlockPos(11, 3, 9), Blocks.WATER);
        Helper.setBlock(new BlockPos(10, 4, 9), Blocks.CHEST);
        if (Helper.getBlockEntity(new BlockPos(10, 4, 9)) instanceof ChestBlockEntity Chest) {
            Chest.setItem(0, new ItemStack(Items.DIAMOND, 16));
        }
    }

    private static boolean inCore(int X, int Y, int Z) {
        return X >= 9 && X <= 11 && Y >= 3 && Y <= 5 && Z >= 9 && Z <= 11;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_craterIsExactlyThreeCubed")
    public static void craterIsExactlyThreeCubed(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        block(Helper);
        Pig Inside = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(10.5, 7.0, 10.5));
        Inside.teleportTo(Helper.absoluteVec(new Vec3(10.5, 3.0, 10.5)).x, Helper.absoluteVec(new Vec3(10.5, 3.0, 10.5)).y, Helper.absoluteVec(new Vec3(10.5, 3.0, 10.5)).z);
        int Taken = Scp001Singularity.consume(World, Helper.absolutePos(new BlockPos(10, 4, 10)), null, null);
        for (int X = 8; X <= 12; X++) {
            for (int Y = 2; Y <= 6; Y++) {
                for (int Z = 8; Z <= 12; Z++) {
                    BlockPos Pos = new BlockPos(X, Y, Z);
                    BlockState State = Helper.getBlockState(Pos);
                    if (X == 9 && Y == 3 && Z == 9) {
                        Helper.assertTrue(State.is(Blocks.BEDROCK), "the singularity took bedrock");
                    } else if (X == 11 && Y == 5 && Z == 11) {
                        Helper.assertTrue(State.is(Scp148Registry.Ward.get()), "the singularity took telekill alloy");
                    } else if (inCore(X, Y, Z)) {
                        Helper.assertTrue(State.isAir(), "the singularity left " + State + " inside the crater at " + Pos);
                    } else {
                        Helper.assertTrue(State.is(Blocks.STONE), "the singularity touched " + Pos + " outside the crater: " + State);
                    }
                }
            }
        }
        Helper.assertTrue(Taken == 25, "the crater removed " + Taken + " blocks instead of 25");
        Helper.assertTrue(World.getEntitiesOfClass(ItemEntity.class, Helper.getBounds(), Dropped -> Dropped.getItem().is(Items.DIAMOND) || Dropped.getItem().is(Items.STONE)
                || Dropped.getItem().is(Items.COBBLESTONE) || Dropped.getItem().is(Items.CHEST)).isEmpty(), "the crater dropped blocks or chest contents");
        Helper.assertTrue(Inside.isDeadOrDying() || Inside.isRemoved(), "a pig inside the crater survived");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp001_noGriefingNoCrater")
    public static void noGriefingNoCrater(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        GameRules.BooleanValue Griefing = World.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean Before = Griefing.get();
        block(Helper);
        Griefing.set(false, World.getServer());
        int Taken;
        try {
            Taken = Scp001Singularity.consume(World, Helper.absolutePos(new BlockPos(10, 4, 10)), null, null);
        } finally {
            Griefing.set(Before, World.getServer());
        }
        Helper.assertTrue(Taken == 0, "the singularity removed " + Taken + " blocks with mobGriefing off");
        Helper.assertTrue(Helper.getBlockState(new BlockPos(10, 4, 10)).is(Blocks.STONE) && Helper.getBlockState(new BlockPos(10, 4, 9)).is(Blocks.CHEST),
                "the crater changed blocks with mobGriefing off");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001_creativeIsUnharmed")
    public static void creativeIsUnharmed(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = survivor(Helper, 14.5, 12.5);
        User.setGameMode(GameType.CREATIVE);
        float Before = User.getHealth();
        Scp001Singularity Orb = Scp001Singularity.fire(World, Helper.absoluteVec(new Vec3(8.5, 3.0, 12.5)), User.getBoundingBox().getCenter(), null);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(!Orb.flying(), "the orb has not reached the creative player yet");
            Helper.assertTrue(User.isAlive() && User.getHealth() == Before, "the orb hurt a creative player: " + User.getHealth());
            dismiss(Helper, User);
            clearOrbs(Helper);
        });
    }

    @SuppressWarnings("unchecked")
    private static ServerPlayer realPlayer(GameTestHelper Helper, double X, double Z) {
        ServerLevel World = Helper.getLevel();
        CommonListenerCookie Cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "scp001_real"), false);
        ServerPlayer User = new ServerPlayer(World.getServer(), World, Cookie.gameProfile(), Cookie.clientInformation());
        Connection Link = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(Link);
        try {
            Field Registrations = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
            Registrations.setAccessible(true);
            Map<ConnectionProtocol, Map<ResourceLocation, ?>> All = (Map<ConnectionProtocol, Map<ResourceLocation, ?>>) Registrations.get(null);
            ChannelAttributes.getOrCreateCommonChannels(Link, ConnectionProtocol.PLAY).addAll(All.getOrDefault(ConnectionProtocol.PLAY, Map.of()).keySet());
        } catch (ReflectiveOperationException Error) {
            throw new IllegalStateException(Error);
        }
        World.getServer().getPlayerList().placeNewPlayer(Link, User, Cookie);
        User.setGameMode(GameType.SURVIVAL);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 2.0, Z));
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Helper.onEachTick(() -> User.connection.chunkSender.onChunkBatchReceivedByClient(64.0f));
        return User;
    }

    public static final int SpawnShield = 70;

    private static void leave(ServerPlayer User) {
        if (User.server.getPlayerList().getPlayer(User.getUUID()) != null) {
            User.server.getPlayerList().remove(User);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp001_controlledDeathCountsOnce")
    public static void controlledDeathCountsOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = realPlayer(Helper, 8.5, 8.5);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(10.5, 2.0, 8.5));
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "the test player could not take the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "the control session did not start");
        Helper.runAfterDelay(SpawnShield, () -> swallowController(Helper, World, User, BodyId));
    }

    private static void swallowController(GameTestHelper Helper, ServerLevel World, ServerPlayer User, UUID BodyId) {
        boolean Hit = Scp001Singularity.swallow(World, User, null, null);
        Helper.assertTrue(User.isAlive() && ControlManager.hasPendingDeath(User), "the controlled body did not go through the deferred death (hit " + Hit + ", health "
                + User.getHealth() + ", controlling " + ControlSessions.isControlling(User) + ")");
        Helper.assertTrue(!Scp001Singularity.swallow(World, User, null, null), "a second orb hurt the controller while its death was pending");
        Helper.assertTrue(User.isAlive(), "a second orb killed the controller outright");
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(!ControlManager.hasPendingDeath(User), "the deferred death was never processed");
            Helper.assertTrue(!ControlSessions.isControlling(User), "the controller is still inside the D-Class");
            Helper.assertTrue(ControlRoster.get(World.getServer()).status(BodyId) == ControlRoster.BodyStatus.Deceased, "the D-Class is not recorded dead");
            Helper.assertTrue(ControlManager.processDeath(User, Restored -> {
            }) == null, "the controlled death could be processed a second time");
            Helper.assertTrue(World.getEntitiesOfClass(DClass.class, Helper.getBounds().inflate(4.0)).size() <= 1, "the D-Class died more than once");
            leave(User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp001_noCastWhenHeld")
    public static void noCastWhenHeld(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Returns.get(World).clear();
        folder(Helper);
        ItemStack Pod = captured(Helper, 4.5, 20.5);
        StasisPods.breakOut(World, Pod, Helper.absoluteVec(new Vec3(12.5, 2.0, 12.5)), null);
        Scp001Prototype Held = World.getEntitiesOfClass(Scp001Prototype.class, Helper.getBounds()).stream().filter(Scp001Prototype::isRestrained).findFirst().orElseThrow();
        Pig Prey = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(20.5, 2.0, 20.5));
        Scp001Prototype Dazed = spawn(Helper, 3.5, 3.5);
        Dazed.daze(Scp001Prototype.LockTicks);
        Scp001Prototype Fleeing = spawn(Helper, 21.5, 3.5);
        Fleeing.forceEscape();
        Helper.assertTrue(!Held.forceSummon(Prey), "restrained SCP-001 summoned");
        Helper.assertTrue(!Dazed.forceSummon(Prey), "dazed SCP-001 summoned");
        Helper.assertTrue(!Fleeing.forceSummon(Prey), "escaping SCP-001 summoned");
        Helper.assertTrue(World.getEntitiesOfClass(Scp001Singularity.class, Helper.getBounds().inflate(8.0)).isEmpty(), "an orb formed anyway");
        tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp001_ownOrbsPassThrough")
    public static void ownOrbsPassThrough(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp001Prototype Creature = spawn(Helper, 12.5, 12.5);
        Creature.daze(Scp001Prototype.LockTicks);
        float Before = Creature.getHealth();
        Scp001Singularity Orb = Scp001Singularity.fire(World, Helper.absoluteVec(new Vec3(5.5, 3.0, 12.5)), Helper.absoluteVec(new Vec3(30.5, 3.0, 12.5)), Creature);
        Helper.runAfterDelay(16, () -> {
            Helper.assertTrue(Creature.isAlive() && Creature.getHealth() == Before, "SCP-001 was hit by its own orb" + state(Creature));
            Helper.assertTrue(Orb.flying() && Orb.position().x > Creature.getX() + 2.0, "the orb stopped at SCP-001 instead of passing through");
            clearOrbs(Helper);
            tidy(Helper);
            Helper.succeed();
        });
    }
}
