package net.rai.scp_unleashed.scp1730.guardian.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Collapse;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlock;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.block.RealityFolderLayout;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.gametest.Scp1730GameTests;
import net.rai.scp_unleashed.scp1730.guardian.Scp1730ContainedGuardian;
import net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianCapture;
import net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianRegistry;
import net.rai.scp_unleashed.stasis.StasisContents;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp1730GuardianGameTests {
    public static final String Arena = Scp1730GameTests.Arena;
    public static final BlockPos FolderSpot = new BlockPos(16, 1, 6);
    public static final BlockPos BossSpot = new BlockPos(16, 1, 26);
    public static final Vec3 CellSpot = new Vec3(16.5, 1.0, 13.5);
    public static final String VetoKey = "message.scp_unleashed.stasis.vetoed";
    public static final double TidyReach = 48.0;

    private Scp1730GuardianGameTests() {
    }

    private static ItemStack pod() {
        return new ItemStack(StasisRegistry.podFor(FragmentRarity.values()[0]));
    }

    private static boolean vetoed(Component Refusal) {
        return Refusal != null && Refusal.getContents() instanceof TranslatableContents Words && VetoKey.equals(Words.getKey());
    }

    private static ItemStack capturedPod(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730Guardian Boss = Helper.spawn(Scp1730Registry.GuardianType.get(), BossSpot);
        Boss.setHealth(Scp1730GuardianCapture.threshold(Boss));
        RealityAnchorField Field = RealityAnchorField.spawn(World, Boss.position().add(0.0, 1.0, 0.0));
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack Pod = pod();
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Boss), "setup: the pod failed to take a weakened, anchored Malidramagiuan");
        Field.discard();
        return Pod;
    }

    private static Scp1730ContainedGuardian released(GameTestHelper Helper, ItemStack Pod) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(StasisPods.breakOut(World, Pod, Helper.absoluteVec(CellSpot), null), "the pod did not release anything");
        List<Scp1730ContainedGuardian> Found = World.getEntitiesOfClass(Scp1730ContainedGuardian.class, Helper.getBounds().inflate(8.0));
        Helper.assertTrue(Found.size() == 1, "expected one recovered Malidramagiuan, found " + Found.size());
        return Found.get(0);
    }

    private static RealityFolderBlockEntity folder(GameTestHelper Helper, boolean[] Feeding) {
        Helper.setBlock(FolderSpot, Scp1730Registry.Folder.get().defaultBlockState().setValue(RealityFolderBlock.Part, RealityFolderLayout.MasterPart));
        RealityFolderBlockEntity Folder = (RealityFolderBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(FolderSpot));
        Helper.assertTrue(Folder != null, "the Reality Folder has no block entity");
        Folder.raiseForTest();
        Helper.onEachTick(() -> {
            if (Feeding[0]) {
                Folder.feed(RealityFolderBlockEntity.Demand);
            }
        });
        return Folder;
    }

    private static void tidy(GameTestHelper Helper) {
        for (Scp1730Guardian Each : Helper.getLevel().getEntitiesOfClass(Scp1730Guardian.class, Helper.getBounds().inflate(TidyReach))) {
            Each.discard();
        }
        Helper.killAllEntitiesOfClass(RealityAnchorField.class);
        Helper.killAllEntitiesOfClass(Pig.class);
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp1730guardian_refusal")
    public static void guardianPodRefusal(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730Guardian Boss = Helper.spawn(Scp1730Registry.GuardianType.get(), BossSpot);
        Player User = Helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack Pod = pod();
        float Threshold = Scp1730GuardianCapture.threshold(Boss);
        Helper.assertTrue(Threshold > 0.0f && Threshold < Boss.getMaxHealth() * 0.2f, "the capture threshold is not low: " + Threshold);
        Boss.setHealth(Threshold);
        Helper.assertTrue(vetoed(StasisPods.refusal(User, Pod, Boss)), "a pod did not refuse a weakened but unanchored Malidramagiuan");
        Helper.assertTrue(!StasisPods.capture(World, User, Pod, Boss), "a pod captured an unanchored Malidramagiuan");
        Boss.setHealth(Boss.getMaxHealth());
        RealityAnchorField.spawn(World, Boss.position().add(0.0, 1.0, 0.0));
        Helper.runAfterDelay(2, () -> {
            Helper.assertTrue(Boss.senseAnchor(), "the anchor field did not reach Malidramagiuan");
            Helper.assertTrue(vetoed(StasisPods.refusal(User, Pod, Boss)), "a pod did not refuse an anchored Malidramagiuan at full health");
            Boss.setHealth(Threshold + 5.0f);
            Helper.assertTrue(vetoed(StasisPods.refusal(User, Pod, Boss)), "a pod did not refuse an anchored Malidramagiuan above the threshold");
            Helper.assertTrue(!StasisPods.capture(World, User, Pod, Boss) && !Boss.isRemoved(), "a pod captured Malidramagiuan above the threshold");
            Boss.setHealth(Threshold);
            Helper.assertTrue(StasisPods.refusal(User, Pod, Boss) == null, "a pod refused an anchored Malidramagiuan at the threshold");
            Helper.assertTrue(!StasisPodItem.isOccupied(Pod), "a refused pod is holding something");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1730guardian_window")
    public static void guardianCaptureWindow(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp1730Guardian Boss = Helper.spawn(Scp1730Registry.GuardianType.get(), BossSpot);
        Boss.setHealth(Scp1730GuardianCapture.threshold(Boss) * 2.0f);
        RealityAnchorField.spawn(World, Boss.position().add(0.0, 1.0, 0.0));
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(Boss.anchored(), "the anchor field did not anchor Malidramagiuan");
            Boss.hurt(World.damageSources().generic(), 5000.0f);
            float Floor = Scp1730GuardianCapture.floor(Boss);
            Helper.assertTrue(Boss.isAlive() && !Boss.isDeadOrDying(), "one huge blow killed Malidramagiuan straight through its capture window");
            Helper.assertTrue(Math.abs(Boss.getHealth() - Floor) < 0.01f && Scp1730GuardianCapture.weakened(Boss), "the blow did not stop at the capture window: " + Boss.getHealth());
            Helper.runAfterDelay(25, () -> {
                Boss.hurt(World.damageSources().generic(), 5000.0f);
                Helper.assertTrue(Boss.isDeadOrDying(), "Malidramagiuan could not be killed from inside its capture window");
                tidy(Helper);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp1730guardian_captureDefeat")
    public static void guardianCaptureDefeat(GameTestHelper Helper) {
        Scp1730State State = Scp1730State.get(Helper.getLevel().getServer());
        if (State.active()) {
            State.endRun();
        }
        ServerPlayer User = Scp1730GameTests.enter(Helper, "scp1730g_keeper");
        ServerLevel Facility = Scp1730GameTests.facility(Helper);
        Helper.assertTrue(State.active() && !State.guardianDead(), "the fresh run already counts Malidramagiuan as defeated");
        BlockPos Spot = BlockPos.containing(User.getX() + 6.0, User.getY(), User.getZ());
        ChunkPos Area = new ChunkPos(Spot);
        boolean Forced = Facility.setChunkForced(Area.x, Area.z, true);
        Scp1730Guardian[] Boss = {null};
        RealityAnchorField[] Field = {null};
        BlockPos Thresher = Spot.above(12);
        ItemStack Pod = pod();
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Facility.isPositionEntityTicking(Spot) && Facility.isPositionEntityTicking(User.blockPosition()), "the arrival area is still loading"))
                .thenExecute(() -> {
                    Boss[0] = Scp1730Registry.GuardianType.get().create(Facility);
                    Helper.assertTrue(Boss[0] != null, "could not make Malidramagiuan");
                    Boss[0].moveTo(Spot.getX() + 0.5, User.getY(), Spot.getZ() + 0.5, 0.0f, 0.0f);
                    Facility.addFreshEntity(Boss[0]);
                    Helper.assertTrue(!Scp1730Collapse.canPull(Facility, Thresher), "the Thresher can be pulled while Malidramagiuan stands");
                    Boss[0].setHealth(Scp1730GuardianCapture.threshold(Boss[0]));
                    Field[0] = RealityAnchorField.spawn(Facility, Boss[0].position().add(0.0, 1.0, 0.0));
                })
                .thenExecuteAfter(2, () -> {
                    Helper.assertTrue(StasisPods.refusal(User, Pod, Boss[0]) == null, "a pod refused a weakened, anchored Malidramagiuan: " + diagnose(Facility, User, Pod, Boss[0], Field[0]));
                    Helper.assertTrue(StasisPods.capture(Facility, User, Pod, Boss[0]), "the pod failed to capture a weakened, anchored Malidramagiuan: " + diagnose(Facility, User, Pod, Boss[0], Field[0]));
                    Helper.assertTrue(Boss[0].isRemoved(), "Malidramagiuan is still in the facility after capture");
                    StasisContents Held = StasisPodItem.contents(Pod);
                    Helper.assertTrue(Held != null && Scp1730GuardianRegistry.ContainedType.getId().toString().equals(Held.EntityData().getString("id")),
                            "the pod does not hold the recovered form: " + (Held == null ? "nothing" : Held.EntityData().getString("id")));
                    Helper.assertTrue(State.guardianDead(), "capturing Malidramagiuan did not count as defeating it for the run");
                    Helper.assertTrue(Scp1730Collapse.canPull(Facility, Thresher), "the Thresher cannot be pulled after Malidramagiuan was captured");
                    Helper.assertTrue(!State.collapsing() && !State.corePulled(), "capturing Malidramagiuan changed the collapse state");
                    Field[0].discard();
                    if (Forced) {
                        Facility.setChunkForced(Area.x, Area.z, false);
                    }
                    State.endRun();
                    Scp1730GameTests.dismiss(User);
                })
                .thenSucceed();
    }

    public static String diagnose(ServerLevel World, Player User, ItemStack Pod, Scp1730Guardian Boss, RealityAnchorField Field) {
        Component Refusal = StasisPods.refusal(User, Pod, Boss);
        String Key = Refusal == null ? "none" : Refusal.getContents() instanceof TranslatableContents Words ? Words.getKey() : Refusal.getString();
        return "refusal " + Key + ", vetoed " + StasisPods.isVetoed(Boss) + ", capturable " + Scp1730GuardianCapture.capturable(Boss)
                + ", alive " + Boss.isAlive() + ", removed " + Boss.isRemoved() + " (" + Boss.getRemovalReason() + "), dying " + Boss.isDeadOrDying()
                + ", health " + Boss.getHealth() + " of " + Boss.getMaxHealth() + " (threshold " + Scp1730GuardianCapture.threshold(Boss) + ", weakened " + Scp1730GuardianCapture.weakened(Boss)
                + "), sense " + Boss.senseAnchor() + ", flag " + Boss.anchored() + ", thrown " + Scp1730Anchors.thrown(Boss) + ", folded " + Scp1730Anchors.folded(World, Boss.getBoundingBox())
                + ", boss at " + Boss.position() + " ticks " + Boss.tickCount + " level " + (Boss.level() == World) + " found " + (World.getEntity(Boss.getUUID()) == Boss)
                + ", field at " + Field.position() + " alive " + Field.isAlive() + " removed " + Field.getRemovalReason() + " ticks " + Field.tickCount + " found " + (World.getEntity(Field.getUUID()) == Field)
                + ", ticking " + World.isPositionEntityTicking(Boss.blockPosition()) + ", loaded " + World.isLoaded(Boss.blockPosition())
                + ", pod left " + StasisPodItem.remaining(Pod, World.getGameTime()) + ", user " + User.position() + " in " + User.level().dimension().location();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp1730guardian_releasedForm")
    public static void guardianReleasedForm(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        boolean[] Feeding = {true};
        folder(Helper, Feeding);
        ServerPlayer Watcher = Scp1730GameTests.player(Helper, World, Helper.absoluteVec(new Vec3(2.5, 1.0, 20.5)), "scp1730g_watcher");
        Scp1730Guardian Boss = Helper.spawn(Scp1730Registry.GuardianType.get(), BossSpot);
        Helper.runAfterDelay(4, () -> {
            Helper.assertTrue(Boss.barShownTo(Watcher), "control: the boss bar is not shown to a nearby player");
            Boss.setHealth(Scp1730GuardianCapture.threshold(Boss));
            RealityAnchorField Field = RealityAnchorField.spawn(World, Boss.position().add(0.0, 1.0, 0.0));
            ItemStack Pod = pod();
            Helper.assertTrue(StasisPods.capture(World, Watcher, Pod, Boss), "the pod failed to take a weakened, anchored Malidramagiuan");
            Field.discard();
            Scp1730ContainedGuardian Contained = released(Helper, Pod);
            Helper.assertTrue(Contained.getType() == Scp1730GuardianRegistry.ContainedType.get(), "breaking the pod did not give the recovered form");
            Helper.assertTrue(World.getEntitiesOfClass(Scp1730Guardian.class, Helper.getBounds().inflate(8.0), Each -> Each.getType() == Scp1730Registry.GuardianType.get()).isEmpty(),
                    "breaking the pod brought the boss back");
            Helper.assertTrue(Contained.getBbWidth() == Boss.getBbWidth() && Contained.getBbHeight() == Boss.getBbHeight(), "the recovered form is not the boss's size");
            Helper.assertTrue(Contained.getMaxHealth() == Boss.getMaxHealth() && Contained.getHealth() >= Contained.getMaxHealth() - 0.01f,
                    "the released form did not come back at full health: " + Contained.getHealth());
            Helper.runAfterDelay(6, () -> {
                Helper.assertTrue(!Contained.isRemoved(), "the recovered form left a running Reality Folder field");
                Helper.assertTrue(!Contained.barShownTo(Watcher), "the recovered form shows a boss bar");
                tidy(Helper);
                Scp1730GameTests.dismiss(Watcher);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp1730guardian_escapeRebind")
    public static void guardianEscapeRebind(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ItemStack Pod = capturedPod(Helper);
        Scp1730ContainedGuardian Contained = released(Helper, Pod);
        Vec3 Start = Contained.position();
        Helper.assertTrue(!Contained.isRemoved() && !Contained.free(), "the recovered form was free before it escaped");
        BlockPos[] Placed = {null};
        int[] Ticks = {0};
        Helper.onEachTick(() -> {
            Ticks[0]++;
            if (Ticks[0] == 6) {
                Helper.assertTrue(!Contained.isRemoved() && Contained.isAlive(), "the recovered form vanished instead of escaping");
                Helper.assertTrue(Contained.free() && Contained.bound() == null, "the recovered form is still bound after escaping");
                double Moved = Math.sqrt(Math.pow(Contained.getX() - Start.x, 2.0) + Math.pow(Contained.getZ() - Start.z, 2.0));
                Helper.assertTrue(Moved >= Scp1730ContainedGuardian.BreakoutMin - 1.0 && Moved <= Scp1730ContainedGuardian.BreakoutMax + 1.5,
                        "the recovered form did not resurface 8 to 24 blocks away: " + Moved);
                CompoundTag Saved = Contained.saveWithoutId(new CompoundTag());
                Scp1730ContainedGuardian Copy = Scp1730GuardianRegistry.ContainedType.get().create(World);
                Helper.assertTrue(Copy != null, "could not rebuild the recovered form");
                Copy.load(Saved);
                Helper.assertTrue(Copy.free() && Copy.bound() == null, "the escaped state did not survive a save and load");
                Placed[0] = Contained.blockPosition().offset(3, 0, 0);
                World.setBlock(Placed[0], Scp1730Registry.Folder.get().defaultBlockState().setValue(RealityFolderBlock.Part, RealityFolderLayout.MasterPart), 3);
                if (World.getBlockEntity(Placed[0]) instanceof RealityFolderBlockEntity Folder) {
                    Folder.raiseForTest();
                }
            }
            if (Placed[0] != null && World.getBlockEntity(Placed[0]) instanceof RealityFolderBlockEntity Folder) {
                Folder.feed(RealityFolderBlockEntity.Demand);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Placed[0] != null, "the recovered form has not escaped yet");
            Helper.assertTrue(!Contained.isRemoved() && Contained.held() && !Contained.free() && Placed[0].equals(Contained.bound()),
                    "the escaped form was not held again by the new running Folder");
            World.removeBlock(Placed[0], false);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp1730guardian_peaceful")
    public static void guardianPeaceful(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        boolean[] Feeding = {true};
        folder(Helper, Feeding);
        Scp1730ContainedGuardian Contained = Helper.spawn(Scp1730GuardianRegistry.ContainedType.get(), new BlockPos(16, 1, 13));
        Difficulty Before = World.getDifficulty();
        World.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        try {
            Contained.checkDespawn();
            Helper.assertTrue(!Contained.isRemoved(), "Peaceful deleted the recovered form");
        } finally {
            World.getServer().setDifficulty(Before, true);
        }
        Helper.assertTrue(Contained.isPersistenceRequired(), "the recovered form is not persistent");
        tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp1730guardian_folderStates")
    public static void guardianFolderStates(GameTestHelper Helper) {
        RealityFolderBlockEntity Folder = new RealityFolderBlockEntity(BlockPos.ZERO, Scp1730Registry.Folder.get().defaultBlockState());
        float[] Last = {0.0f};
        Runnable Step = () -> {
            Folder.animate();
            float Change = Folder.shownCharge(1.0f) - Folder.shownCharge(0.0f);
            Helper.assertTrue(Math.abs(Change) <= 1.0f / RealityFolderBlockEntity.ShutdownTicks + 1.0e-4f, "the Folder glow jumped by " + Change);
            float Turn = Folder.coilAngle(1.0f) - Folder.coilAngle(0.0f);
            Helper.assertTrue(Turn >= -1.0e-4f, "the Folder coils wound backwards by " + Turn);
            if (Folder.status() == RealityFolderBlockEntity.StatusOff && Folder.shownCharge(0.0f) <= 0.0f) {
                Helper.assertTrue(Math.abs(Turn) < 1.0e-4f, "the Folder coils moved while powered down");
            }
            Last[0] = Folder.shownCharge(1.0f);
        };
        Helper.assertTrue("off".equals(RealityFolderBlockEntity.animationFor(RealityFolderBlockEntity.StatusOff))
                && "spinup".equals(RealityFolderBlockEntity.animationFor(RealityFolderBlockEntity.StatusSpinning))
                && "running".equals(RealityFolderBlockEntity.animationFor(RealityFolderBlockEntity.StatusRunning))
                && "shutdown".equals(RealityFolderBlockEntity.animationFor(RealityFolderBlockEntity.StatusShutdown)), "the Folder picks the wrong animation for a state");
        for (int Tick = 0; Tick < 30; Tick++) {
            Folder.advance(false);
            Step.run();
            Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusOff, "an unpowered Folder left Off");
        }
        Folder.advance(true);
        Step.run();
        Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusSpinning, "power did not start the spin");
        for (int Tick = 0; Tick < 30; Tick++) {
            Folder.advance(true);
            Step.run();
        }
        int Reached = Folder.progress();
        for (int Tick = 0; Tick < 5; Tick++) {
            Folder.advance(false);
            Step.run();
            Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusSpinning && Folder.progress() == Reached && Folder.stalled(),
                    "a short power gap did not pause the spin (status " + Folder.status() + ", progress " + Folder.progress() + ")");
        }
        int Powered = 0;
        while (Folder.status() == RealityFolderBlockEntity.StatusSpinning && Powered < RealityFolderBlockEntity.SpinTicks * 2) {
            Folder.advance(true);
            Step.run();
            Powered++;
        }
        Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusRunning && Folder.fieldUp(), "the Folder never finished spinning up");
        Helper.assertTrue(Reached + Powered == RealityFolderBlockEntity.SpinTicks, "the spin did not resume where it paused: " + Reached + " + " + Powered);
        for (int Tick = 0; Tick < RealityFolderBlockEntity.GraceTicks; Tick++) {
            Folder.advance(false);
            Step.run();
            Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusRunning, "a short power gap dropped the running field");
        }
        Folder.advance(true);
        Step.run();
        for (int Tick = 0; Tick <= RealityFolderBlockEntity.GraceTicks; Tick++) {
            Folder.advance(false);
            Step.run();
        }
        Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusShutdown && RealityFolderBlockEntity.litFor(Folder.status()), "losing power did not start the shutdown");
        int Stopping = 0;
        while (Folder.status() == RealityFolderBlockEntity.StatusShutdown && Stopping < RealityFolderBlockEntity.ShutdownTicks * 2) {
            Folder.advance(false);
            Step.run();
            Stopping++;
        }
        Helper.assertTrue(Stopping == RealityFolderBlockEntity.ShutdownTicks && Folder.status() == RealityFolderBlockEntity.StatusOff, "the shutdown did not play once: " + Stopping);
        Helper.assertTrue(!RealityFolderBlockEntity.litFor(Folder.status()), "a powered-down Folder stayed lit");
        for (int Tick = 0; Tick < 120; Tick++) {
            Folder.advance(false);
            Step.run();
            Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusOff, "a powered-down Folder reset itself (status " + Folder.status() + ")");
        }
        Helper.assertTrue(Last[0] <= 0.0f, "the powered-down Folder still glows: " + Last[0]);
        float Rest = Folder.coilAngle(1.0f);
        Folder.advance(true);
        Step.run();
        Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusSpinning && Math.abs(Mth.wrapDegrees(Folder.coilAngle(0.0f) - Rest)) < 1.0e-3f, "the spin-up did not start from the resting pose");
        for (int Tick = 0; Tick < 39; Tick++) {
            Folder.advance(true);
            Step.run();
        }
        float Partway = Folder.charge();
        for (int Tick = 0; Tick <= RealityFolderBlockEntity.GraceTicks; Tick++) {
            Folder.advance(false);
            Step.run();
        }
        Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusShutdown && Math.abs(Folder.charge() - Partway) <= 1.0f / RealityFolderBlockEntity.ShutdownTicks + 1.0e-4f,
                "a long gap mid-spin did not wind down from where it was: " + Partway + " -> " + Folder.charge());
        Folder.advance(true);
        Step.run();
        Helper.assertTrue(Folder.status() == RealityFolderBlockEntity.StatusSpinning && Math.abs(Folder.charge() - Partway) <= 2.0f / RealityFolderBlockEntity.SpinTicks + 1.0e-4f,
                "power returning mid-shutdown did not resume the spin from its pose: " + Partway + " -> " + Folder.charge());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp1730guardian_folderHolds")
    public static void guardianFolderHolds(GameTestHelper Helper) {
        boolean[] Feeding = {true};
        folder(Helper, Feeding);
        ItemStack Pod = capturedPod(Helper);
        Scp1730ContainedGuardian Contained = released(Helper, Pod);
        Pig Bait = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(20.5, 1.0, 13.5));
        float Start = Bait.getHealth();
        BlockPos Master = Helper.absolutePos(FolderSpot);
        Helper.runAfterDelay(4, () -> Helper.assertTrue(Contained.held() && Master.equals(Contained.bound()), "the recovered form is not held by the running Folder"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(!Contained.isRemoved(), "the recovered form left a running Reality Folder field");
            Helper.assertTrue(Contained.held(), "the recovered form is not held");
            double Dx = Contained.getX() - (Master.getX() + 0.5);
            double Dz = Contained.getZ() - (Master.getZ() + 0.5);
            Helper.assertTrue(Math.sqrt(Dx * Dx + Dz * Dz) <= 14.0, "the recovered form wandered out of its cell");
            Helper.assertTrue(!Bait.isAlive() || Bait.getHealth() < Start, "the recovered form has not attacked the pig yet");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp1730guardian_folderPower")
    public static void guardianFolderPower(GameTestHelper Helper) {
        boolean[] Feeding = {true};
        RealityFolderBlockEntity Folder = folder(Helper, Feeding);
        ItemStack Pod = capturedPod(Helper);
        Scp1730ContainedGuardian Contained = released(Helper, Pod);
        int[] Ticks = {0};
        int[] Waited = {0};
        boolean[] HeldWhilePowered = {false};
        Helper.onEachTick(() -> {
            Ticks[0]++;
            if (Ticks[0] == 20) {
                HeldWhilePowered[0] = !Contained.isRemoved() && Contained.held();
                Feeding[0] = false;
            } else if (Ticks[0] > 20 && !Contained.free()) {
                Waited[0]++;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Ticks[0] > 20, "the Folder has not lost power yet");
            Helper.assertTrue(HeldWhilePowered[0], "the recovered form did not stay in a powered Folder field");
            Helper.assertTrue(!Folder.fieldUp(), "the starved Folder is still running");
            Helper.assertTrue(!Contained.isRemoved() && Contained.isAlive(), "the recovered form vanished after the Folder lost power");
            Helper.assertTrue(Contained.free() && Contained.bound() == null, "the recovered form stayed bound after the Folder lost power");
            double Dx = Contained.getX() - (Helper.absolutePos(FolderSpot).getX() + 0.5);
            double Dz = Contained.getZ() - (Helper.absolutePos(FolderSpot).getZ() + 0.5);
            Helper.assertTrue(Math.sqrt(Dx * Dx + Dz * Dz) > 4.0, "the recovered form did not leave its cell");
            Helper.assertTrue(Waited[0] <= RealityFolderBlockEntity.GraceTicks + 6, "the recovered form lingered " + Waited[0] + " ticks after power was cut");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp1730guardian_reload")
    public static void guardianReload(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        boolean[] Feeding = {true};
        folder(Helper, Feeding);
        ItemStack Pod = capturedPod(Helper);
        Scp1730ContainedGuardian Contained = released(Helper, Pod);
        BlockPos Master = Helper.absolutePos(FolderSpot);
        Helper.runAfterDelay(5, () -> {
            Helper.assertTrue(Contained.held(), "setup: the recovered form is not held");
            float Before = Contained.getHealth();
            CompoundTag Saved = new CompoundTag();
            Contained.saveWithoutId(Saved);
            Helper.assertTrue(Saved.contains(Scp1730ContainedGuardian.BoundKey), "the saved recovered form forgot its Folder");
            Contained.discard();
            Scp1730ContainedGuardian Loaded = Scp1730GuardianRegistry.ContainedType.get().create(World);
            Helper.assertTrue(Loaded != null, "could not rebuild the recovered form");
            Loaded.load(Saved);
            Loaded.setUUID(UUID.randomUUID());
            Helper.assertTrue(Math.abs(Loaded.getHealth() - Before) < 0.01f, "the recovered form's health changed on reload: " + Before + " -> " + Loaded.getHealth());
            Helper.assertTrue(Master.equals(Loaded.bound()), "the reloaded form lost its Folder");
            World.addFreshEntity(Loaded);
            Helper.runAfterDelay(60, () -> {
                Helper.assertTrue(!Loaded.isRemoved() && Loaded.held(), "the reloaded form escaped a running Folder");
                Helper.assertTrue(Loaded.getHealth() >= Before - 0.01f, "the reloaded form lost health: " + Before + " -> " + Loaded.getHealth());
                tidy(Helper);
                Helper.succeed();
            });
        });
    }
}
