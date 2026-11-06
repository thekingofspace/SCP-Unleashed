package net.rai.scp_unleashed.scp472.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp472.Scp472BloodstoneBlock;
import net.rai.scp_unleashed.scp472.Scp472BloodstoneItem;
import net.rai.scp_unleashed.scp472.Scp472Hunger;
import net.rai.scp_unleashed.scp472.Scp472Mind;
import net.rai.scp_unleashed.scp472.Scp472Registry;
import net.rai.scp_unleashed.scp472.Scp472Stones;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp472GameTests {
    public static final String Arena = "scp472_arena";
    public static final BlockPos Stone = new BlockPos(1, 1, 4);
    public static final BlockPos FarStone = new BlockPos(21, 1, 4);
    public static final float[] ExpectedThresholds = {95.0f, 80.0f, 62.0f, 45.0f, 28.0f, 12.0f};

    private Scp472GameTests() {
    }

    private static Scp472TestPlayer player(GameTestHelper Helper, double Along, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp472TestPlayer User = new Scp472TestPlayer(World, Name);
        Vec3 Where = spot(Helper, Along);
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    private static Scp472TestPlayer player(GameTestHelper Helper, double Along, String Name) {
        return player(Helper, Along, Name, GameType.SURVIVAL);
    }

    private static Vec3 spot(GameTestHelper Helper, double Along) {
        return Helper.absoluteVec(new Vec3(Stone.getX() + 0.5 + Along, 1.0, Stone.getZ() + 0.5));
    }

    private static void move(GameTestHelper Helper, ServerPlayer User, double Along) {
        Vec3 Where = spot(Helper, Along);
        User.teleportTo(Where.x, Where.y, Where.z);
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void place(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, Scp472Registry.Bloodstone.get().defaultBlockState().setValue(Scp472BloodstoneBlock.Facing, Direction.NORTH));
    }

    private static void clear(GameTestHelper Helper, List<ServerPlayer> Users, BlockPos... Stones) {
        for (ServerPlayer User : Users) {
            dismiss(Helper, User);
        }
        for (BlockPos Relative : Stones) {
            Helper.setBlock(Relative, Blocks.AIR.defaultBlockState());
        }
    }

    private static double distance(GameTestHelper Helper, ServerPlayer User, BlockPos Relative) {
        return Scp472Hunger.centre(User).distanceTo(Vec3.atCenterOf(Helper.absolutePos(Relative)));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp472_rangeDrain")
    public static void rangeDrain(GameTestHelper Helper) {
        place(Helper, Stone);
        Scp472TestPlayer Far = player(Helper, 30.0, "scp472_far");
        Scp472TestPlayer Middle = player(Helper, 10.0, "scp472_middle");
        Scp472TestPlayer Near = player(Helper, 3.0, "scp472_near");
        List<ServerPlayer> Users = List.of(Far, Middle, Near);
        Helper.startSequence().thenIdle(80).thenExecute(() -> {
            try {
                float FarSanity = Scp472Mind.sanity(Far);
                float MiddleSanity = Scp472Mind.sanity(Middle);
                float NearSanity = Scp472Mind.sanity(Near);
                Helper.assertTrue(FarSanity == Scp472Mind.Full && !Scp472Mind.isTouched(Far), "A player 30 blocks away lost sanity: " + FarSanity);
                Helper.assertTrue(MiddleSanity < Scp472Mind.Full, "A player 10 blocks away kept full sanity");
                Helper.assertTrue(Scp472Mind.Full - NearSanity > (Scp472Mind.Full - MiddleSanity) * 1.5f,
                        "A player 3 blocks away (" + NearSanity + ") did not drain faster than one at 10 (" + MiddleSanity + ")");
                Helper.assertTrue(Scp472Mind.drainPerSecond(25.0) == 0.0f && Scp472Mind.drainPerSecond(3.0) > Scp472Mind.drainPerSecond(10.0),
                        "The drain curve does not fall off with distance");
            } finally {
                clear(Helper, Users, Stone);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp472_resetOutside")
    public static void resetOutside(GameTestHelper Helper) {
        place(Helper, Stone);
        Scp472TestPlayer User = player(Helper, 5.0, "scp472_leaver");
        List<ServerPlayer> Users = List.of(User);
        Helper.startSequence().thenIdle(40).thenExecute(() -> {
            Helper.assertTrue(Scp472Mind.sanity(User) < Scp472Mind.Full, "Sanity did not fall inside the radius");
            Scp472Mind.store(User, 30.0f);
            Helper.assertTrue(Scp472Mind.stageOf(User) == 4, "Thirty sanity is not stage 4");
            move(Helper, User, 25.0);
        }).thenIdle(1).thenExecute(() -> {
            try {
                Helper.assertTrue(distance(Helper, User, Stone) > Scp472Mind.Reach, "The player was not moved outside the radius");
                Helper.assertTrue(Scp472Mind.sanity(User) == Scp472Mind.Full && !Scp472Mind.isTouched(User), "Sanity did not reset at once: " + Scp472Mind.sanity(User));
                Helper.assertTrue(Scp472Mind.stageOf(User) == 0, "The stage did not reset");
            } finally {
                clear(Helper, Users, Stone);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp472_stageThresholds")
    public static void stageThresholds(GameTestHelper Helper) {
        Helper.assertTrue(Scp472Mind.Thresholds.length == ExpectedThresholds.length, "There are not six stage thresholds");
        for (int Index = 0; Index < ExpectedThresholds.length; Index++) {
            Helper.assertTrue(Scp472Mind.Thresholds[Index] == ExpectedThresholds[Index], "Threshold " + (Index + 1) + " is " + Scp472Mind.Thresholds[Index]);
        }
        int Last = 0;
        boolean[] Seen = new boolean[Scp472Mind.Stages + 1];
        for (int Step = 1000; Step >= 0; Step--) {
            float Sanity = Step / 10.0f;
            int Stage = Scp472Mind.stage(Sanity);
            Helper.assertTrue(Stage >= Last, "The stage fell from " + Last + " to " + Stage + " as sanity dropped to " + Sanity);
            Seen[Stage] = true;
            for (int Index = 0; Index < Scp472Mind.Thresholds.length; Index++) {
                boolean Past = Sanity < Scp472Mind.Thresholds[Index];
                Helper.assertTrue(Past == Stage >= Index + 1, "Sanity " + Sanity + " gives stage " + Stage + " against threshold " + Scp472Mind.Thresholds[Index]);
            }
            Last = Stage;
        }
        for (int Stage = 0; Stage <= Scp472Mind.Stages; Stage++) {
            Helper.assertTrue(Seen[Stage], "Stage " + Stage + " is never reached");
        }
        Helper.assertTrue(Scp472Mind.stage(Scp472Mind.Full) == 0 && Scp472Mind.stage(0.0f) == Scp472Mind.HarmStage, "The ends of the scale are wrong");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp472_stageSixHurts")
    public static void stageSixHurts(GameTestHelper Helper) {
        place(Helper, Stone);
        Scp472TestPlayer User = player(Helper, 2.0, "scp472_victim");
        List<ServerPlayer> Users = List.of(User);
        Helper.startSequence().thenIdle(5).thenExecute(() -> {
            Scp472Mind.store(User, 5.0f);
            User.setHealth(User.getMaxHealth());
        }).thenWaitUntil(() -> {
            Helper.assertTrue(User.getHealth() < User.getMaxHealth(), "Stage 6 has not hurt the player yet");
            Helper.assertTrue(User.getLastDamageSource() != null && User.getLastDamageSource().is(Scp472Registry.ConsumedDamage),
                    "The player was hurt by " + User.getLastDamageSource());
        }).thenExecute(() -> clear(Helper, Users, Stone)).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp472_dclassConsumed")
    public static void dclassConsumed(GameTestHelper Helper) {
        place(Helper, Stone);
        DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Stone.east(2));
        Scp472Mind.store(Body, 14.0f);
        float Full = Body.getMaxHealth();
        Body.setHealth(Full);
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(Scp472Mind.stageOf(Body) == Scp472Mind.HarmStage, "The D-Class has not reached stage 6: " + Scp472Mind.sanity(Body));
            Helper.assertTrue(Body.getHealth() < Full, "The D-Class next to the stone was not hurt");
            Helper.assertTrue(Body.getLastDamageSource() != null && Body.getLastDamageSource().is(Scp472Registry.ConsumedDamage),
                    "The D-Class was hurt by " + Body.getLastDamageSource());
        }).thenExecute(() -> {
            Body.discard();
            Helper.setBlock(Stone, Blocks.AIR.defaultBlockState());
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp472_creativeSpectatorImmune")
    public static void creativeSpectatorImmune(GameTestHelper Helper) {
        place(Helper, Stone);
        Scp472TestPlayer Builder = player(Helper, 3.0, "scp472_creative", GameType.CREATIVE);
        Scp472TestPlayer Watcher = player(Helper, 3.0, "scp472_spectator", GameType.SPECTATOR);
        Scp472TestPlayer Control = player(Helper, 3.0, "scp472_control");
        List<ServerPlayer> Users = List.of(Builder, Watcher, Control);
        Helper.startSequence().thenIdle(60).thenExecute(() -> {
            try {
                Helper.assertTrue(Scp472Mind.sanity(Control) < Scp472Mind.Full, "The survival control player was not affected");
                Helper.assertFalse(Scp472Mind.isTouched(Builder), "A creative player lost sanity");
                Helper.assertFalse(Scp472Mind.isTouched(Watcher), "A spectator lost sanity");
                Scp472Mind.store(Builder, 20.0f);
            } catch (RuntimeException Problem) {
                clear(Helper, Users, Stone);
                throw Problem;
            }
        }).thenIdle(2).thenExecute(() -> {
            try {
                Helper.assertFalse(Scp472Mind.isTouched(Builder), "A creative player kept a lowered sanity");
            } finally {
                clear(Helper, Users, Stone);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp472_carriedDormant")
    public static void carriedDormant(GameTestHelper Helper) {
        Scp472TestPlayer User = player(Helper, 3.0, "scp472_carrier");
        User.getInventory().setItem(0, new ItemStack(Scp472Registry.BloodstoneItem.get()));
        User.getInventory().setItem(9, new ItemStack(Scp472Registry.BloodstoneItem.get()));
        User.getInventory().selected = 0;
        List<ServerPlayer> Users = List.of(User);
        Helper.startSequence().thenIdle(60).thenExecute(() -> {
            try {
                Helper.assertTrue(Scp472BloodstoneItem.is(User.getMainHandItem()), "The test player is not holding the bloodstone");
                Helper.assertFalse(Scp472Stones.any(Helper.getLevel()), "A carried bloodstone counts as placed");
                Helper.assertFalse(Scp472Mind.isTouched(User), "A carried bloodstone drained sanity: " + Scp472Mind.sanity(User));
            } finally {
                clear(Helper, Users);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp472_nearestOnly")
    public static void nearestOnly(GameTestHelper Helper) {
        place(Helper, Stone);
        place(Helper, FarStone);
        Scp472TestPlayer User = player(Helper, 5.0, "scp472_between");
        List<ServerPlayer> Users = List.of(User);
        float[] Start = new float[1];
        Helper.startSequence().thenIdle(10).thenExecute(() -> {
            Scp472Stones.Nearest Found = Scp472Stones.nearest(Helper.getLevel(), Scp472Hunger.centre(User), Scp472Mind.Reach);
            Helper.assertTrue(Found != null && Found.Pos().equals(Helper.absolutePos(Stone)), "The nearest stone is not the one five blocks away");
            Start[0] = Scp472Mind.sanity(User);
        }).thenIdle(40).thenExecute(() -> {
            try {
                float Dropped = Start[0] - Scp472Mind.sanity(User);
                float Nearest = Scp472Mind.drainPerSecond(distance(Helper, User, Stone)) * 2.0f;
                float Stacked = Nearest + Scp472Mind.drainPerSecond(distance(Helper, User, FarStone)) * 2.0f;
                Helper.assertTrue(Math.abs(Dropped - Nearest) < Nearest * 0.1f, "Two seconds drained " + Dropped + ", the nearest stone alone drains " + Nearest);
                Helper.assertTrue(Dropped < Stacked - (Stacked - Nearest) * 0.5f, "The two stones stacked: " + Dropped + " against " + Stacked);
            } finally {
                clear(Helper, Users, Stone, FarStone);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp472_savedSanity")
    public static void savedSanity(GameTestHelper Helper) {
        place(Helper, Stone);
        ServerLevel World = Helper.getLevel();
        Scp472TestPlayer User = player(Helper, 4.0, "scp472_saver");
        Scp472TestPlayer Restored = new Scp472TestPlayer(World, "scp472_restored");
        List<ServerPlayer> Users = new ArrayList<>(List.of(User, Restored));
        Helper.startSequence().thenIdle(5).thenExecute(() -> {
            Scp472Mind.store(User, 40.0f);
            CompoundTag Saved = User.saveWithoutId(new CompoundTag());
            dismiss(Helper, User);
            Restored.load(Saved);
            Helper.assertTrue(Math.abs(Scp472Mind.sanity(Restored) - 40.0f) < 0.01f, "Sanity did not survive the save: " + Scp472Mind.sanity(Restored));
            Vec3 Where = spot(Helper, 4.0);
            Restored.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
            World.addNewPlayer(Restored);
            Restored.setGameMode(GameType.SURVIVAL);
        }).thenIdle(10).thenExecute(() -> {
            float Kept = Scp472Mind.sanity(Restored);
            Helper.assertTrue(Kept < 40.0f && Kept > 35.0f, "Sanity did not carry on from the saved value inside the radius: " + Kept);
            move(Helper, Restored, 26.0);
        }).thenIdle(2).thenExecute(() -> {
            try {
                Helper.assertFalse(Scp472Mind.isTouched(Restored), "A restored player outside the radius kept lowered sanity");
            } finally {
                clear(Helper, Users, Stone);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp472_pickaxeKeepsIt")
    public static void pickaxeKeepsIt(GameTestHelper Helper) {
        place(Helper, Stone);
        ServerLevel World = Helper.getLevel();
        BlockPos At = Helper.absolutePos(Stone);
        List<ItemStack> Drops = Block.getDrops(World.getBlockState(At), World, At, World.getBlockEntity(At), null, new ItemStack(Items.IRON_PICKAXE));
        Helper.assertTrue(Drops.size() == 1 && Scp472BloodstoneItem.is(Drops.get(0)) && Drops.get(0).getCount() == 1, "Mining the bloodstone dropped " + Drops);
        Helper.assertTrue(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(World.getBlockState(At)) || World.getBlockState(At).is(BlockTags.MINEABLE_WITH_PICKAXE),
                "The bloodstone is not mined with a pickaxe");
        Helper.assertTrue(Scp472Registry.BloodstoneItem.get().getDefaultMaxStackSize() == 1, "The bloodstone stacks");
        Helper.setBlock(Stone, Blocks.AIR.defaultBlockState());
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp472_strangerBringsIt")
    public static void strangerBringsIt(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp472Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-472 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Euclid, "SCP-472 is not filed as Euclid");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-472 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp472BloodstoneItem.is(Stack) && Stack.getCount() == 1, "The stranger's gift is not the bloodstone: " + Stack);
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp472Registry.DocumentId).isPresent(), "No spawner is registered for SCP-472");
        ServerLevel World = Helper.getLevel();
        Scp472TestPlayer Reader = player(Helper, 8.0, "scp472_reader");
        try {
            BlockPos Pos = Helper.absolutePos(new BlockPos(10, 1, 4));
            boolean Spawned = AnomalySpawners.spawnerFor(Scp472Registry.DocumentId).get()
                    .spawn(new AnomalySpawners.Discovery(World, Pos, Reader, Scp472Registry.DocumentId, Document, false));
            Helper.assertTrue(Spawned, "The SCP-472 spawner refused the delivery");
            for (BlockPos Each : BlockPos.betweenClosed(Pos.offset(-12, -2, -6), Pos.offset(12, 3, 6))) {
                Helper.assertFalse(World.getBlockState(Each).getBlock() instanceof Scp472BloodstoneBlock, "The delivery placed a bloodstone at " + Each);
            }
            Helper.assertFalse(Scp472Stones.any(World), "The delivery left a placed bloodstone");
        } finally {
            dismiss(Helper, Reader);
        }
        Helper.succeed();
    }
}
