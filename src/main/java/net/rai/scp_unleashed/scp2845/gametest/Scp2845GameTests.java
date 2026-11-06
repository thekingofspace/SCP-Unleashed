package net.rai.scp_unleashed.scp2845.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.scp002.MeteorEntity;
import net.rai.scp_unleashed.scp002.Scp002Registry;
import net.rai.scp_unleashed.scp2845.Scp2845;
import net.rai.scp_unleashed.scp2845.Scp2845Arrival;
import net.rai.scp_unleashed.scp2845.Scp2845Circle;
import net.rai.scp_unleashed.scp2845.Scp2845Petrify;
import net.rai.scp_unleashed.scp2845.Scp2845PillarBlock;
import net.rai.scp_unleashed.scp2845.Scp2845Records;
import net.rai.scp_unleashed.scp2845.Scp2845Registry;
import net.rai.scp_unleashed.scp2845.Scp2845Spirit;
import net.rai.scp_unleashed.scp2845.Scp2845StoneBlock;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp2845GameTests {
    public static final String Field = "scp2845_field";
    public static final String Crater = "scp2845_crater";
    public static final BlockPos Middle = new BlockPos(18, 2, 18);

    private Scp2845GameTests() {
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(37.0, 18.0, 37.0).inflate(4.0);
    }

    private static void sweep(GameTestHelper Helper) {
        for (Entity Each : Helper.getLevel().getEntitiesOfClass(Entity.class, area(Helper), Each -> !(Each instanceof ServerPlayer))) {
            Each.discard();
        }
    }

    private static void finish(GameTestHelper Helper) {
        sweep(Helper);
        Helper.succeed();
    }

    private static Scp2845 deer(GameTestHelper Helper, BlockPos Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        Scp2845 Made = Scp2845Registry.DeerType.get().create(World);
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Relative));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Made.setNoAi(Still);
        World.addFreshEntity(Made);
        Made.record(World);
        return Made;
    }

    private static <T extends Mob> T mob(GameTestHelper Helper, EntityType<T> Type, BlockPos Relative) {
        ServerLevel World = Helper.getLevel();
        T Made = Type.create(World);
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Relative));
        Made.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Made.setNoAi(true);
        Made.setPersistenceRequired();
        World.addFreshEntity(Made);
        return Made;
    }

    private static BlockPos stone(GameTestHelper Helper, BlockPos Relative) {
        BlockPos At = Helper.absolutePos(Relative);
        Helper.getLevel().setBlock(At, Scp2845Registry.Stone.get().defaultBlockState(), Block.UPDATE_ALL);
        return At;
    }

    private static void ring(GameTestHelper Helper, BlockPos Stone) {
        for (BlockPos Column : Scp2845Circle.ring(Stone)) {
            Helper.getLevel().setBlock(Column, Blocks.BONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static FakePlayer hand(GameTestHelper Helper, String Name) {
        return new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), Name));
    }

    private static void slay(GameTestHelper Helper, BlockPos Relative, String Name) {
        Chicken Bird = mob(Helper, EntityType.CHICKEN, Relative);
        Bird.hurt(Helper.getLevel().damageSources().playerAttack(hand(Helper, Name)), 100.0f);
        Helper.assertTrue(Bird.isDeadOrDying(), "the chicken survived the blow");
    }

    private static boolean sealed(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return State.is(Scp2845Registry.Stone.get()) && State.getValue(Scp2845StoneBlock.Sealed);
    }

    @GameTest(template = Crater, timeoutTicks = 100, batch = "scp2845_arrival")
    public static void arrival(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Ground = Helper.absolutePos(new BlockPos(16, 8, 16));
        BlockPos Stone = Scp2845Arrival.impact(World, Ground, Vec3.atCenterOf(Helper.absolutePos(new BlockPos(16, 9, 2))), null);
        Helper.assertTrue(Scp2845Circle.isStone(World, Stone), "the impact left no SCP-2845-2");
        Helper.assertTrue(Stone.getY() < Ground.getY(), "the impact carved no crater under the stone: stone " + Stone.toShortString() + " ground " + Ground.toShortString());
        Helper.assertTrue(World.getBlockState(Ground.above()).isAir(), "the crater centre is still filled");
        Helper.assertTrue(sealed(World, Stone), "the arrival stone is not sealed while the deer is free");
        List<Scp2845> Herd = World.getEntitiesOfClass(Scp2845.class, new AABB(Stone).inflate(8.0), Entity::isAlive);
        Helper.assertTrue(Herd.size() == 1, "the crater should hold exactly one SCP-2845-1, found " + Herd.size());
        Helper.assertTrue(Stone.equals(Herd.get(0).home()), "the arrival deer is not bound to its stone");
        Helper.assertTrue(World.getEntitiesOfClass(MeteorEntity.class, area(Helper)).isEmpty(), "a meteor entity was left behind");
        for (BlockPos Pos : BlockPos.betweenClosed(Helper.absolutePos(BlockPos.ZERO), Helper.absolutePos(new BlockPos(32, 18, 32)))) {
            if (World.getBlockState(Pos).is(Scp002Registry.Shell.get())) {
                Helper.fail("the arrival left a meteor rock at " + Pos.toShortString());
            }
        }
        finish(Helper);
    }

    @GameTest(template = Field, timeoutTicks = 100, batch = "scp2845_stoneLock")
    public static void stoneLock(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = deer(Helper, Middle.offset(3, 0, 0), true);
        Deer.arrive(World, Stone);
        FakePlayer User = hand(Helper, "scp2845_lock");
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        Helper.assertTrue(sealed(World, Stone), "the bound stone is not sealed");
        Helper.assertTrue(World.getBlockState(Stone).getDestroyProgress(User, World, Stone) == 0.0f, "the stone can be mined while the deer is free");
        Deer.recall(World, Stone);
        Deer.startBow(World);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Deer), "the pod failed to take the bowing deer");
        Helper.assertFalse(sealed(World, Stone), "the stone stayed sealed after the deer went into stasis");
        Helper.assertTrue(World.getBlockState(Stone).getDestroyProgress(User, World, Stone) > 0.0f, "the stone cannot be mined once the deer is in stasis");
        List<ItemStack> Drops = Block.getDrops(World.getBlockState(Stone), World, Stone, null, User, new ItemStack(Items.DIAMOND_PICKAXE));
        Helper.assertTrue(Drops.stream().anyMatch(Each -> Each.is(Scp2845Registry.StoneItem.get())), "SCP-2845-2 does not drop itself");
        Helper.assertTrue(Scp2845Records.get(World.getServer()).entry(Deer.getUUID()).map(Row -> Row.Phase() == Scp2845Records.State.Stasis).orElse(false),
                "the record does not show the deer in stasis");
        Scp2845Records.get(World.getServer()).forget(Deer.getUUID());
        finish(Helper);
    }

    @GameTest(template = Field, timeoutTicks = 500, batch = "scp2845_recall")
    public static void recall(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = deer(Helper, Middle.offset(12, 0, 4), false);
        slay(Helper, Middle.offset(2, 0, 2), "scp2845_recall");
        Helper.assertTrue(Deer.isRecalled() || Deer.isBowing(), "killing a chicken by the stone did not call the deer back");
        Helper.assertTrue(Stone.equals(Deer.home()), "the recalled deer is not bound to the stone");
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Deer.isBowing(), "the deer has not bowed over the stone yet");
            Helper.assertTrue(Deer.distanceToSqr(Vec3.atCenterOf(Stone)) < 16.0, "the deer bowed away from the stone: " + Deer.position() + " stone " + Stone);
            finish(Helper);
        });
    }

    @GameTest(template = Field, timeoutTicks = 100, batch = "scp2845_stasis")
    public static void stasis(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = deer(Helper, Middle.offset(3, 0, 0), true);
        Deer.arrive(World, Stone);
        FakePlayer User = hand(Helper, "scp2845_stasis");
        Helper.assertTrue(StasisPods.refusal(User, new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint)), Deer) != null, "a pod accepts the deer while it roams");
        Deer.recall(World, Stone);
        Helper.assertTrue(StasisPods.refusal(User, new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint)), Deer) != null, "a pod accepts the deer before it bows");
        Deer.startBow(World);
        ItemStack Pod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        Helper.assertTrue(StasisPods.refusal(User, Pod, Deer) == null, "a pod refuses the bowing deer");
        Helper.assertTrue(StasisPods.capture(World, User, Pod, Deer), "the pod failed to capture the bowing deer");
        Helper.assertTrue(Deer.isRemoved(), "the captured deer is still in the world");
        Scp2845Records.get(World.getServer()).forget(Deer.getUUID());
        finish(Helper);
    }

    @GameTest(template = Field, timeoutTicks = 100, batch = "scp2845_circle")
    public static void circle(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Stone = stone(Helper, Middle);
        Helper.assertTrue(Scp2845Circle.check(World, Stone).broken(), "an empty field counts as a sealing circle");
        ring(Helper, Stone);
        Helper.assertTrue(Scp2845Circle.check(World, Stone).valid(), "a complete ring at 16 does not seal");
        BlockPos Lifted = Stone.offset(16, 0, 5);
        World.setBlock(Lifted, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Lifted.above(), Blocks.BONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        Helper.assertTrue(Scp2845Circle.check(World, Stone).valid(), "a ring block one higher breaks the seal");
        BlockPos Gap = Stone.offset(-16, 0, -3);
        World.setBlock(Gap, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Scp2845Circle.Result Missing = Scp2845Circle.check(World, Stone);
        Helper.assertTrue(Missing.broken() && Missing.Missing() == 1, "a missing bone block does not break the seal");
        World.setBlock(Gap, Blocks.BONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        Helper.assertTrue(Scp2845Circle.check(World, Stone).valid(), "a repaired ring does not seal again");
        World.setBlock(Stone, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Stone.offset(2, 0, 0), Scp2845Registry.Stone.get().defaultBlockState(), Block.UPDATE_ALL);
        Helper.assertTrue(Scp2845Circle.check(World, Stone).broken(), "a moved stone still counts as sealed at the old spot");
        Helper.assertTrue(Scp2845Circle.check(World, Stone.offset(2, 0, 0)).broken(), "a moved stone is sealed by a ring that is no longer 16 away");
        BlockPos Wide = Stone.offset(17, 0, 0);
        Helper.assertTrue(!Scp2845Circle.inside(Stone, Vec3.atCenterOf(Wide)) && Scp2845Circle.inside(Stone, Vec3.atCenterOf(Stone.offset(15, 0, 15))),
                "the inside test does not match the ring");
        finish(Helper);
    }

    private static Scp2845 contained(GameTestHelper Helper, BlockPos Stone, BlockPos Relative, boolean Still) {
        ServerLevel World = Helper.getLevel();
        ring(Helper, Stone);
        Scp2845 Deer = deer(Helper, Relative, Still);
        Deer.releasedFromStasis(World);
        Helper.assertTrue(Deer.isContained(), "release inside a complete circle did not contain the deer");
        Helper.assertTrue(Stone.equals(Deer.circleStone()), "the contained deer is bound to the wrong stone");
        return Deer;
    }

    @GameTest(template = Field, timeoutTicks = 360, batch = "scp2845_staysInside")
    public static void staysInside(GameTestHelper Helper) {
        sweep(Helper);
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = contained(Helper, Stone, Middle.offset(6, 0, 3), false);
        Helper.assertFalse(sealed(Helper.getLevel(), Stone), "the circle stone is sealed while the deer is contained");
        Helper.onEachTick(() -> {
            Helper.assertTrue(Deer.isContained(), "the contained deer left containment");
            Helper.assertTrue(Scp2845Circle.inside(Stone, Deer.position()), "the contained deer wandered out of the circle");
        });
        Helper.runAtTickTime(340, () -> finish(Helper));
    }

    @GameTest(template = Field, timeoutTicks = 100, batch = "scp2845_upkeep")
    public static void upkeep(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = contained(Helper, Stone, Middle.offset(6, 0, 3), true);
        long Now = World.getGameTime();
        Deer.feed(World, Now - Scp2845.UpkeepTicks + 200L);
        slay(Helper, Middle.offset(-11, 0, 10), "scp2845_upkeep");
        Helper.assertTrue(Deer.lastOffering() == World.getGameTime(), "a chicken slain inside the circle did not reset the timer");
        Helper.assertTrue(Deer.isContained(), "a far offering made the contained deer leave its pen");
        finish(Helper);
    }

    @GameTest(template = Field, timeoutTicks = 260, batch = "scp2845_breakout")
    public static void breakout(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = contained(Helper, Stone, Middle.offset(4, 0, 0), false);
        List<BlockPos> Wall = new ArrayList<>();
        for (int Dx = -9; Dx <= 9; Dx++) {
            for (int Dz = -9; Dz <= 9; Dz++) {
                if (Math.max(Math.abs(Dx), Math.abs(Dz)) != 9) {
                    continue;
                }
                for (int Y = 0; Y < 3; Y++) {
                    BlockPos Pos = Stone.offset(Dx, Y, Dz);
                    World.setBlock(Pos, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
                    Wall.add(Pos);
                }
            }
        }
        Deer.feed(World, World.getGameTime() - Scp2845.UpkeepTicks - 5L);
        Helper.succeedWhen(() -> {
            Helper.assertFalse(Deer.isContained(), "the deer is still contained after the upkeep lapsed");
            long Cleared = Wall.stream().filter(Pos -> World.getBlockState(Pos).isAir()).count();
            Helper.assertTrue(Cleared >= 3, "the breakout did not clear the wall in its path, cleared " + Cleared);
            Helper.assertTrue(Scp2845Circle.check(World, Stone).broken(), "the breakout did not break the ring in its path");
            Helper.assertTrue(sealed(World, Stone), "the stone is not sealed again after the breakout");
            finish(Helper);
        });
    }

    @GameTest(template = Field, timeoutTicks = 260, batch = "scp2845_breakoutTame")
    public static void breakoutTame(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        World.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false, World.getServer());
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = contained(Helper, Stone, Middle.offset(4, 0, 0), false);
        BlockPos Brick = Helper.absolutePos(Middle.offset(9, 0, 0));
        World.setBlock(Brick, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        Deer.breakout(World);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Deer.isBreakingOut() || Deer.isFree(), "the deer is not leaving: mode " + Deer.mode());
            Helper.assertTrue(Deer.getX() > Stone.getX() + Scp2845Circle.Reach + 1.5, "the deer did not pass through the ring");
            Helper.assertTrue(World.getBlockState(Brick).is(Blocks.COBBLESTONE), "the deer broke a block with mobGriefing off");
            Helper.assertTrue(World.getBlockState(Stone.offset(16, 0, 0)).is(Blocks.BONE_BLOCK), "the deer broke the ring with mobGriefing off");
            Helper.assertTrue(Math.abs(Deer.getY() - Stone.getY()) < 0.6, "the walking-through deer sank or rose: " + Deer.getY());
            World.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true, World.getServer());
            finish(Helper);
        });
    }

    private static float yawOfMotion(Vec3 Moved) {
        return (float) (Mth.atan2(Moved.z, Moved.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    @GameTest(template = Field, timeoutTicks = 80, batch = "scp2845_spiritFacing")
    public static void spiritFacing(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Vec3 Start = Vec3.atBottomCenterOf(Helper.absolutePos(Middle.offset(-14, 0, 0)));
        Scp2845Spirit Spirit = Scp2845Spirit.charge(World, Start, new Vec3(1.0, 0.0, 0.0), null);
        float Expected = yawOfMotion(new Vec3(1.0, 0.0, 0.0));
        Helper.assertTrue(Math.abs(Mth.wrapDegrees(Spirit.getYRot() - Expected)) < 10.0f, "the fading spirit does not face its charge: yaw " + Spirit.getYRot());
        Helper.assertTrue(Math.abs(Mth.wrapDegrees(Spirit.headingYaw() - Expected)) < 10.0f, "the spirit renders facing away from its charge: " + Spirit.headingYaw());
        Helper.assertTrue(Spirit.cycles() == 0.0, "the gait runs before the charge starts");
        double[] Seen = new double[1];
        Helper.runAtTickTime(Scp2845Spirit.Delay + 6, () -> Seen[0] = Spirit.cycles());
        Helper.runAtTickTime(Scp2845Spirit.Delay + 16, () -> {
            Vec3 Moved = Spirit.position().subtract(Start);
            Helper.assertTrue(Moved.x > 2.0 && Math.abs(Moved.z) < 0.01, "the spirit did not charge along +X: " + Moved);
            float Motion = yawOfMotion(Moved);
            Helper.assertTrue(Math.abs(Mth.wrapDegrees(Spirit.getYRot() - Motion)) < 10.0f, "the spirit yaw " + Spirit.getYRot() + " does not match its motion " + Motion);
            Helper.assertTrue(Math.abs(Mth.wrapDegrees(Spirit.headingYaw() - Motion)) < 10.0f, "the rendered yaw does not match the motion");
            Helper.assertTrue(Seen[0] > 0.0 && Spirit.cycles() > Seen[0], "the gallop cycle does not advance with distance: " + Seen[0] + " then " + Spirit.cycles());
            double Expect = Moved.horizontalDistance() / Scp2845Spirit.Gallop;
            Helper.assertTrue(Math.abs(Spirit.cycles() - Expect) < 0.05, "the gallop cycle is not locked to distance: " + Spirit.cycles() + " vs " + Expect);
            finish(Helper);
        });
    }

    @GameTest(template = Field, timeoutTicks = 100, batch = "scp2845_petrify")
    public static void petrify(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp2845 Deer = deer(Helper, Middle, true);
        Pig Target = mob(Helper, EntityType.PIG, Middle.offset(5, 0, 0));
        BlockPos Base = Target.blockPosition();
        Helper.assertTrue(Scp2845Petrify.petrify(World, Target, Deer), "the pig was not petrified");
        Helper.assertTrue(Target.isRemoved(), "the petrified pig is still walking about");
        Helper.assertTrue(World.getBlockState(Base).is(Scp2845Registry.Pillar.get()), "the pig left no pillar at " + Base.toShortString() + ", found " + World.getBlockState(Base)
                + " above " + World.getBlockState(Base.above()));
        Scp538TestPlayer User = new Scp538TestPlayer(World, "scp2845_petrify");
        Vec3 Where = Vec3.atBottomCenterOf(Helper.absolutePos(Middle.offset(-5, 0, 0)));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.enlist(true);
        User.setGameMode(GameType.SURVIVAL);
        BlockPos Feet = User.blockPosition();
        Helper.assertTrue(Scp2845Petrify.petrify(World, User, Deer), "the player was not petrified");
        Helper.assertTrue(User.isDeadOrDying(), "the petrified player survived");
        Helper.assertTrue(World.getBlockState(Feet).is(Scp2845Registry.Pillar.get()) && World.getBlockState(Feet.above()).is(Scp2845Registry.Pillar.get()),
                "the player left no two block pillar");
        Helper.assertTrue(World.getBlockState(Feet).getValue(Scp2845PillarBlock.Part) == Scp2845PillarBlock.Segment.Base
                && World.getBlockState(Feet.above()).getValue(Scp2845PillarBlock.Part) == Scp2845PillarBlock.Segment.Top, "the pillar segments are wrong");
        User.enlist(false);
        World.removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        finish(Helper);
    }

    @GameTest(template = Field, timeoutTicks = 100, batch = "scp2845_invulnerable")
    public static void invulnerable(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        Scp2845 Deer = deer(Helper, Middle, true);
        float Health = Deer.getHealth();
        Helper.assertFalse(Deer.hurt(World.damageSources().playerAttack(hand(Helper, "scp2845_hit")), 1000.0f), "a heavy blow hurt the deer");
        Helper.assertFalse(Deer.hurt(World.damageSources().magic(), 1000.0f), "magic hurt the deer");
        Vec3 At = Deer.position();
        World.explode(null, At.x, At.y + 1.0, At.z, 6.0f, Level.ExplosionInteraction.NONE);
        Helper.assertTrue(Deer.isAlive() && Deer.getHealth() == Health, "an explosion hurt the deer");
        UUID Id = Deer.getUUID();
        Deer.kill();
        Helper.assertTrue(Deer.isRemoved(), "the kill command did not remove the deer");
        Helper.assertTrue(Scp2845Records.get(World.getServer()).entry(Id).isEmpty(), "the killed deer is still on record");
        finish(Helper);
    }

    @GameTest(template = Field, timeoutTicks = 100, batch = "scp2845_persistence")
    public static void persistence(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Stone = stone(Helper, Middle);
        Scp2845 Deer = contained(Helper, Stone, Middle.offset(6, 0, 3), true);
        Deer.feed(World, World.getGameTime() - 1234L);
        CompoundTag Saved = new CompoundTag();
        Deer.saveWithoutId(Saved);
        Scp2845 Copy = Scp2845Registry.DeerType.get().create(World);
        Copy.load(Saved);
        Helper.assertTrue(Copy.isContained(), "containment did not survive saving");
        Helper.assertTrue(Stone.equals(Copy.circleStone()), "the circle stone did not survive saving");
        Helper.assertTrue(Copy.lastOffering() == Deer.lastOffering(), "the upkeep timer did not survive saving");
        Scp2845Records Records = Scp2845Records.get(World.getServer());
        Scp2845Records Reloaded = Scp2845Records.load(Records.save(new CompoundTag(), World.registryAccess()), World.registryAccess());
        Helper.assertTrue(Reloaded.entry(Deer.getUUID()).map(Row -> Row.Phase() == Scp2845Records.State.Contained && Stone.equals(Row.Stone())).orElse(false),
                "the containment record did not survive saving");
        finish(Helper);
    }
}
