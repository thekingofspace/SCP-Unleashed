package net.rai.scp_unleashed.scp513.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp500.Scp500Panacea;
import net.rai.scp_unleashed.scp513.Scp513Affliction;
import net.rai.scp_unleashed.scp513.Scp513BellItem;
import net.rai.scp_unleashed.scp513.Scp513Chimes;
import net.rai.scp_unleashed.scp513.Scp513Registry;
import net.rai.scp_unleashed.scp513.Scp513Ring;
import net.rai.scp_unleashed.scp513.Scp513Stage;
import net.rai.scp_unleashed.senses.SensesRegistry;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.storage.StorageTags;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp513GameTests {
    public static final String Arena = "scp513_arena";
    public static final BlockPos ChestSpot = new BlockPos(6, 1, 6);
    public static final BlockPos LockerSpot = new BlockPos(12, 1, 6);

    private Scp513GameTests() {
    }

    public static Mob dclass(GameTestHelper Helper, double X, double Y, double Z) {
        Mob Body = DClassRegistry.DClassType.get().create(Helper.getLevel());
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Body.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Body.setNoAi(true);
        Helper.getLevel().addFreshEntity(Body);
        return Body;
    }

    public static ServerPlayer player(GameTestHelper Helper, String Name, double X, double Z, boolean Mortal) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = Mortal ? new MortalPlayer(World, new GameProfile(UUID.randomUUID(), Name)) : new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Where = Helper.absoluteVec(new Vec3(X, 1.0, Z));
        User.moveTo(Where.x, Where.y, Where.z, 180.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    public static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    public static ItemStack bell(long NextRing) {
        ItemStack Stack = new ItemStack(Scp513Registry.Bell.get());
        Stack.set(Scp513Registry.NextRing.get(), NextRing);
        return Stack;
    }

    public static ItemStack dueBell(GameTestHelper Helper) {
        return bell(Helper.getLevel().getGameTime() - 1L);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_ringRange")
    public static void ringRange(GameTestHelper Helper) {
        Mob Near = dclass(Helper, 4.5, 1, 7.5);
        Mob Far = dclass(Helper, 20.5, 1, 7.5);
        Vec3 Source = Near.getEyePosition().add(-Scp513Ring.Range + 8.0, 0.0, 0.0);
        Scp513Ring.ring(Helper.getLevel(), Source, null);
        Helper.assertTrue(Scp513Affliction.isAfflicted(Near), "A D-Class 24 blocks from the ring was not affected");
        Helper.assertFalse(Scp513Affliction.isAfflicted(Far), "A D-Class 40 blocks from the ring was affected");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_earplugs")
    public static void earplugs(GameTestHelper Helper) {
        Mob Bare = dclass(Helper, 6.5, 1, 6.5);
        Mob Plugged = dclass(Helper, 8.5, 1, 6.5);
        Plugged.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Earplugs.get()));
        ServerPlayer Listener = player(Helper, "scp513_listener", 10.5, 6.5, false);
        ServerPlayer Deaf = player(Helper, "scp513_deaf", 12.5, 6.5, false);
        try {
            Deaf.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Earplugs.get()));
            Scp513Ring.ring(Helper.getLevel(), Helper.absoluteVec(new Vec3(9.5, 2.0, 9.5)), null);
            Helper.assertTrue(Scp513Affliction.isAfflicted(Bare), "A D-Class without earplugs was not affected");
            Helper.assertFalse(Scp513Affliction.isAfflicted(Plugged), "A D-Class with earplugs was affected");
            Helper.assertTrue(Scp513Affliction.isAfflicted(Listener), "A player without earplugs was not affected");
            Helper.assertFalse(Scp513Affliction.isAfflicted(Deaf), "A player with earplugs was affected");
        } finally {
            dismiss(Helper, Listener);
            dismiss(Helper, Deaf);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_rightClickRings")
    public static void rightClickRings(GameTestHelper Helper) {
        Mob Witness = dclass(Helper, 14.5, 1, 6.5);
        ServerPlayer User = player(Helper, "scp513_ringer", 6.5, 6.5, false);
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, bell(Helper.getLevel().getGameTime() + 100000L));
            User.getItemInHand(InteractionHand.MAIN_HAND).use(Helper.getLevel(), User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "Ringing the bell did not reach a nearby D-Class");
            Helper.assertTrue(Scp513Affliction.isAfflicted(User), "Ringing the bell did not affect the one who rang it");
            long Next = User.getItemInHand(InteractionHand.MAIN_HAND).getOrDefault(Scp513Registry.NextRing.get(), 0L);
            long Now = Helper.getLevel().getGameTime();
            Helper.assertTrue(Next >= Now + Scp513Chimes.MinGap && Next <= Now + Scp513Chimes.MaxGap, "Ringing by hand did not restart the bell's own schedule");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_tapedOnlyKnocks")
    public static void tapedOnlyKnocks(GameTestHelper Helper) {
        Mob Witness = dclass(Helper, 9.5, 1, 6.5);
        ServerPlayer User = player(Helper, "scp513_taped", 6.5, 6.5, false);
        try {
            ItemStack Taped = new ItemStack(Scp513Registry.Bell.get());
            Taped.set(Scp513Registry.Tape.get(), 0L);
            User.setItemInHand(InteractionHand.MAIN_HAND, Taped);
            int Before = Scp513Ring.rings();
            User.getItemInHand(InteractionHand.MAIN_HAND).use(Helper.getLevel(), User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Scp513Ring.rings() == Before, "A taped bell rang when used");
            Helper.assertFalse(Scp513Affliction.isAfflicted(Witness), "A taped bell affected a D-Class");
            Helper.assertTrue(Scp513BellItem.isTaped(User.getItemInHand(InteractionHand.MAIN_HAND)), "Using a taped bell removed the tape");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_tapeGivesWay")
    public static void tapeGivesWay(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ItemStack Taped = new ItemStack(Scp513Registry.Bell.get());
        Taped.set(Scp513Registry.Tape.get(), 0L);
        Helper.assertTrue(Scp513Chimes.pulse(World, Taped) == Scp513Chimes.Pulse.Taped, "Fresh tape did not hold the clapper");
        long Gives = Taped.getOrDefault(Scp513Registry.Tape.get(), 0L);
        Helper.assertTrue(Gives == World.getGameTime() + Scp513Chimes.TapeLife, "The tape did not start its timer when first checked");
        Helper.assertTrue(Scp513Chimes.pulse(World, Taped) == Scp513Chimes.Pulse.Taped, "The tape gave way too early");
        Taped.set(Scp513Registry.Tape.get(), World.getGameTime() - 1L);
        Helper.assertTrue(Scp513Chimes.pulse(World, Taped) == Scp513Chimes.Pulse.Loosened, "Old tape did not give way");
        Helper.assertFalse(Scp513BellItem.isTaped(Taped), "The tape is still on after giving way");
        Helper.assertTrue(Taped.has(Scp513Registry.NextRing.get()), "The bell has no ring scheduled after the tape gave way");
        Helper.assertTrue(Scp513Chimes.pulse(World, Taped) == Scp513Chimes.Pulse.Quiet, "The bell rang the moment the tape gave way");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_inventoryRings")
    public static void inventoryRings(GameTestHelper Helper) {
        Mob Witness = dclass(Helper, 12.5, 1, 6.5);
        ServerPlayer User = player(Helper, "scp513_carrier", 6.5, 6.5, false);
        try {
            User.getInventory().setItem(20, bell(Helper.getLevel().getGameTime() + 100000L));
            Scp513Chimes.tickCarrier(User);
            Helper.assertFalse(Scp513Affliction.isAfflicted(Witness), "A bell that was not due rang from the inventory");
            User.getInventory().setItem(20, dueBell(Helper));
            Scp513Chimes.tickCarrier(User);
            Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "A due bell in the inventory did not ring");
            Helper.assertTrue(Scp513Affliction.isAfflicted(User), "The carrier was not affected by their own bell");
            long Next = User.getInventory().getItem(20).getOrDefault(Scp513Registry.NextRing.get(), 0L);
            Helper.assertTrue(Next > Helper.getLevel().getGameTime(), "The bell did not schedule its next ring");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_enderChestRings")
    public static void enderChestRings(GameTestHelper Helper) {
        Mob Witness = dclass(Helper, 12.5, 1, 6.5);
        ServerPlayer User = player(Helper, "scp513_hoarder", 6.5, 6.5, false);
        try {
            User.getEnderChestInventory().setItem(3, dueBell(Helper));
            Scp513Chimes.tickCarrier(User);
            Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "A due bell in the ender chest did not ring");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp513_chestRings")
    public static void chestRings(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Mob Witness = dclass(Helper, 12.5, 1, 6.5);
        Helper.setBlock(ChestSpot, Blocks.CHEST);
        BlockPos Pos = Helper.absolutePos(ChestSpot);
        ChestBlockEntity Chest = (ChestBlockEntity) World.getBlockEntity(Pos);
        Chest.setItem(4, dueBell(Helper));
        Helper.assertTrue(Scp513Chimes.refresh(World, Pos), "A chest holding the bell was not tracked");
        Helper.assertTrue(Scp513Chimes.isTracked(World, Pos), "The chest is not in the bell record");
        Scp513Chimes.sweep(World);
        Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "A due bell in a chest did not ring");
        Chest.setItem(4, ItemStack.EMPTY);
        Scp513Chimes.sweep(World);
        Helper.assertFalse(Scp513Chimes.isTracked(World, Pos), "An empty chest stayed in the bell record");
        Helper.setBlock(ChestSpot, Blocks.AIR);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp513_chunkScanFindsChest")
    public static void chunkScanFindsChest(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.setBlock(ChestSpot, Blocks.CHEST);
        BlockPos Pos = Helper.absolutePos(ChestSpot);
        ChestBlockEntity Chest = (ChestBlockEntity) World.getBlockEntity(Pos);
        Chest.setItem(0, bell(World.getGameTime() + 100000L));
        LevelChunk Chunk = World.getChunkAt(Pos);
        Helper.assertTrue(Scp513Chimes.scanChunk(World, Chunk) >= 1, "Scanning the loaded chunk did not find the chest holding the bell");
        Helper.assertTrue(Scp513Chimes.isTracked(World, Pos), "The scanned chest is not in the bell record");
        Chest.setItem(0, ItemStack.EMPTY);
        Scp513Chimes.refresh(World, Pos);
        Helper.setBlock(ChestSpot, Blocks.AIR);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp513_trackerFollowsBell")
    public static void trackerFollowsBell(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos First = ChestSpot;
        BlockPos Second = ChestSpot.east(2);
        BlockPos Between = ChestSpot.east();
        Helper.setBlock(First, Blocks.CHEST);
        Helper.setBlock(Between, Blocks.HOPPER);
        Helper.setBlock(Second, Blocks.CHEST);
        ChestBlockEntity Start = (ChestBlockEntity) World.getBlockEntity(Helper.absolutePos(First));
        Start.setItem(0, bell(World.getGameTime() + 100000L));
        Scp513Chimes.refresh(World, Helper.absolutePos(First));
        Start.setItem(0, ItemStack.EMPTY);
        ChestBlockEntity End = (ChestBlockEntity) World.getBlockEntity(Helper.absolutePos(Second));
        End.setItem(0, bell(World.getGameTime() + 100000L));
        Scp513Chimes.sweep(World);
        Helper.assertFalse(Scp513Chimes.isTracked(World, Helper.absolutePos(First)), "The emptied chest is still tracked");
        Helper.assertTrue(Scp513Chimes.isTracked(World, Helper.absolutePos(Second)), "The record did not follow the bell into the next container");
        End.setItem(0, ItemStack.EMPTY);
        Scp513Chimes.sweep(World);
        Helper.setBlock(First, Blocks.AIR);
        Helper.setBlock(Between, Blocks.AIR);
        Helper.setBlock(Second, Blocks.AIR);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp513_storageContainerRings")
    public static void storageContainerRings(GameTestHelper Helper) {
        Mob Witness = dclass(Helper, 6.5, 1, 6.5);
        Helper.setBlock(LockerSpot, StorageRegistry.SmallContainer.get().defaultBlockState());
        StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(Helper.getLevel(), Helper.absolutePos(LockerSpot));
        Helper.assertTrue(Locker != null, "No storage container was placed");
        Helper.assertTrue(StorageTags.isStorable(new ItemStack(Scp513Registry.Bell.get())), "SCP-513 cannot be stored in a storage container");
        Locker.setContents(dueBell(Helper));
        Helper.succeedWhen(() -> Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "A due bell in a storage container did not ring"));
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp513_droppedBellRings")
    public static void droppedBellRings(GameTestHelper Helper) {
        Mob Witness = dclass(Helper, 6.5, 1, 6.5);
        Vec3 Where = Helper.absoluteVec(new Vec3(12.5, 1.2, 6.5));
        ItemEntity Dropped = new ItemEntity(Helper.getLevel(), Where.x, Where.y, Where.z, dueBell(Helper));
        Dropped.setPickUpDelay(32767);
        Helper.getLevel().addFreshEntity(Dropped);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "A due bell lying on the floor did not ring");
            Dropped.discard();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp513_ringDoesNotRestart")
    public static void ringDoesNotRestart(GameTestHelper Helper) {
        Mob Subject = dclass(Helper, 6.5, 1, 6.5);
        ServerLevel World = Helper.getLevel();
        Scp513Ring.ring(World, Subject.getEyePosition(), null);
        long Seed = Scp513Affliction.state(Subject).Seed();
        Scp513Affliction.setElapsed(Subject, 1000);
        Scp513Ring.ring(World, Subject.getEyePosition(), null);
        int Hastened = Scp513Affliction.state(Subject).Elapsed();
        Helper.assertTrue(Hastened > 1000 && Hastened < Scp513Stage.FreezeAt, "A second ring restarted or skipped the dread: " + Hastened);
        Helper.assertTrue(Scp513Affliction.state(Subject).Seed() == Seed, "A second ring replaced the affliction");
        Scp513Affliction.setElapsed(Subject, Scp513Stage.GrabAt + 5);
        Scp513Ring.ring(World, Subject.getEyePosition(), null);
        Helper.assertTrue(Scp513Affliction.state(Subject).Elapsed() == Scp513Stage.GrabAt + 5, "A ring changed the timeline after the grab");
        Scp513Affliction.cure(Subject);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp513_timelineKillsDClass")
    public static void timelineKillsDClass(GameTestHelper Helper) {
        Mob Subject = dclass(Helper, 6.5, 1, 6.5);
        Scp513Affliction.expose(Subject, Subject.getEyePosition());
        Scp513Affliction.setElapsed(Subject, Scp513Stage.DeathAt - 20);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Subject.isDeadOrDying() || Subject.isRemoved(), "The D-Class survived the end of the timeline");
            Helper.assertFalse(Subject.hasData(Scp513Registry.Affliction.get()), "The dead D-Class still carries the affliction");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp513_timelineKillsPlayer")
    public static void timelineKillsPlayer(GameTestHelper Helper) {
        ServerPlayer User = player(Helper, "scp513_victim", 6.5, 6.5, true);
        try {
            Scp513Ring.ring(Helper.getLevel(), User.getEyePosition(), null);
            Helper.assertTrue(Scp513Affliction.isAfflicted(User), "The player was not affected");
            Scp513Affliction.setElapsed(User, Scp513Stage.FreezeAt - 1);
            Scp513Affliction.tick(User);
            Helper.assertTrue(Scp513Affliction.isLocked(User), "The player can still walk after the dread");
            Scp513Affliction.setElapsed(User, Scp513Stage.DeathAt - 3);
            for (int Step = 0; Step < 5; Step++) {
                Scp513Affliction.tick(User);
            }
            Helper.assertTrue(User.isDeadOrDying(), "The server did not end the player's timeline in death");
            Helper.assertFalse(Scp513Affliction.isAfflicted(User), "The affliction outlived the player");
            Helper.assertFalse(Scp513Affliction.isLocked(User), "The movement lock outlived the affliction");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_scp500Cures")
    public static void scp500Cures(GameTestHelper Helper) {
        Mob Subject = dclass(Helper, 6.5, 1, 6.5);
        Scp513Affliction.expose(Subject, Subject.getEyePosition());
        Scp513Affliction.setElapsed(Subject, Scp513Stage.ArriveAt + 10);
        Scp513Affliction.tick(Subject);
        Helper.assertTrue(Scp513Affliction.isLocked(Subject), "The D-Class was not frozen before the cure");
        Helper.assertTrue(Scp500Panacea.dose(Subject, null), "SCP-500 could not be given");
        Helper.assertFalse(Scp513Affliction.isAfflicted(Subject), "SCP-500 did not cure SCP-513-A");
        Helper.assertFalse(Scp513Affliction.isLocked(Subject), "SCP-500 left the movement lock behind");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp513_dclassPanicFreezeCollapse")
    public static void dclassPanicFreezeCollapse(GameTestHelper Helper) {
        Mob Subject = dclass(Helper, 10.5, 1, 6.5);
        Subject.setNoAi(false);
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(Subject.onGround(), "The D-Class never landed");
            Scp513Affliction.expose(Subject, Subject.position().add(-3.0, 0.0, 0.0));
            Scp513Affliction.tick(Subject);
            Helper.assertTrue(Subject.getNavigation().isInProgress(), "The D-Class did not run from the ring");
            Scp513Affliction.setElapsed(Subject, Scp513Stage.FreezeAt - 1);
            Scp513Affliction.tick(Subject);
            Helper.assertTrue(Scp513Affliction.isLocked(Subject), "The D-Class did not freeze");
            Helper.assertFalse(Subject.getNavigation().isInProgress(), "The frozen D-Class kept walking");
            Scp513Affliction.setElapsed(Subject, Scp513Affliction.CollapseAt - 1);
            Scp513Affliction.tick(Subject);
            Helper.assertTrue(Subject instanceof DClass Body && Body.isResting(), "The D-Class did not collapse while strangled");
            Scp513Affliction.cure(Subject);
            Helper.assertFalse(Subject instanceof DClass Body && Body.isResting(), "The cured D-Class stayed down");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp513_strangerBringsTapedBell")
    public static void strangerBringsTapedBell(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp513Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-513 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Euclid, "SCP-513 is not filed as Euclid");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-513 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp513BellItem.is(Stack), "The stranger's gift is not the bell: " + Stack);
        Helper.assertTrue(Scp513BellItem.isTaped(Stack), "The delivered bell is not taped");
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp513Registry.DocumentId).isPresent(), "No spawner is registered for SCP-513");
        Helper.succeed();
    }

    private static final class MortalPlayer extends FakePlayer {
        private MortalPlayer(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return false;
        }
    }
}
