package net.rai.scp_unleashed.z0003.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0003.Z0003Host;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import net.rai.scp_unleashed.z0003.Z0003Registry;
import net.rai.scp_unleashed.z0003.Z0003Statue;
import net.rai.scp_unleashed.z0003.Z0003VialItem;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Z0003PlayerGameTests {
    public static final String Arena = Z0003GameTests.Arena;
    public static final int Lead = 10;
    public static final int DirtCount = 17;

    private Z0003PlayerGameTests() {
    }

    private static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static Z0003TestPlayer player(GameTestHelper Helper, double X, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Z0003TestPlayer User = new Z0003TestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, stand(Helper), Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, Entity Subject) {
        if (Subject.isRemoved()) {
            return;
        }
        if (Subject instanceof Z0003TestPlayer User) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        } else {
            Subject.discard();
        }
    }

    private static Z0003TestPlayer stone(GameTestHelper Helper, double X, double Z, String Name) {
        Z0003TestPlayer Held = player(Helper, X, Z, Name);
        Held.getInventory().setItem(0, new ItemStack(Items.DIRT, DirtCount));
        Helper.assertTrue(Z0003Petrify.petrify(Held), Name + " could not be turned to stone");
        return Held;
    }

    private static int vials(Z0003TestPlayer User) {
        return User.getInventory().countItem(Z0003Registry.Vial.get());
    }

    private static Z0003Statue relogged(GameTestHelper Helper, Z0003TestPlayer User) {
        CompoundTag Saved = new CompoundTag();
        User.saveWithoutId(Saved);
        Z0003TestPlayer Returned = new Z0003TestPlayer(Helper.getLevel(), User.getGameProfile().getName() + "_back");
        Returned.load(Saved);
        return Z0003Petrify.statueOf(Returned);
    }

    private static void reset(ServerLevel World) {
        Z0003Host.force(null);
        Z0003Registry.setPlayersTimeOut(World.getServer(), false);
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "z0003_player_localFrees")
    public static void localWorldFreesPlayer(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        reset(World);
        Z0003Host.force(true);
        Z0003TestPlayer Waiting = stone(Helper, 12.5, 16.5, "z0003_alone");
        Waiting.getInventory().setItem(4, Z0003VialItem.create(1));
        Z0003Statue First = Z0003Petrify.statueOf(Waiting);
        Waiting.setData(Z0003Registry.Statue.get(), First.since(World.getGameTime() - (Z0003Petrify.TimeoutTicks - Z0003Petrify.CrackTicks) + Lead));
        Vec3 Spot = Waiting.position();
        long[] Passes = new long[1];
        Helper.startSequence()
                .thenWaitUntil(() -> {
                    Long Known = Waiting.getExistingDataOrNull(Z0003Registry.Passing.get());
                    Helper.assertTrue(Known != null && Known > 0L, "the local world did not mark the stone as passing");
                    Passes[0] = Known;
                })
                .thenExecute(() -> {
                    Z0003Statue Now = Z0003Petrify.statueOf(Waiting);
                    Helper.assertTrue(Passes[0] == Z0003Petrify.passesAt(Now), "the stone passes at " + Passes[0] + " instead of " + Z0003Petrify.passesAt(Now));
                    Helper.assertTrue(Now.equals(relogged(Helper, Waiting)), "the stone did not survive a relog");
                })
                .thenWaitUntil(() -> Helper.assertFalse(Z0003Petrify.isStatue(Waiting), "the player is still stone on a local world"))
                .thenExecute(() -> {
                    Helper.assertFalse(Waiting.hasData(Z0003Registry.Passing.get()), "the passing mark stayed after the stone broke");
                    Helper.assertTrue(Waiting.getInventory().getItem(0).is(Items.DIRT) && Waiting.getInventory().getItem(0).getCount() == DirtCount, "the freed player lost items");
                    Helper.assertTrue(Z0003VialItem.is(Waiting.getInventory().getItem(4)), "the freed player lost the cure");
                    Helper.assertTrue(relogged(Helper, Waiting) == null, "the freed player came back as stone after a relog");
                    Helper.assertTrue(Waiting.isAlive() && Waiting.position().distanceTo(Spot) < 0.1, "the freed player moved or died while held");
                    Waiting.moveTo(Spot.x + 1.0, Spot.y, Spot.z, 0.0f, 0.0f);
                })
                .thenExecuteAfter(5, () -> {
                    Helper.assertTrue(Math.abs(Waiting.getX() - (Spot.x + 1.0)) < 0.01, "the freed player was pulled back into place");
                    reset(World);
                    dismiss(Helper, Waiting);
                })
                .thenSucceed();
    }

    @AfterBatch(batch = "z0003_player_localFrees")
    public static void afterLocal(ServerLevel World) {
        reset(World);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_player_dedicatedStays")
    public static void dedicatedServerKeepsPlayer(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        reset(World);
        Z0003Host.force(false);
        Z0003TestPlayer Waiting = stone(Helper, 12.5, 16.5, "z0003_kept");
        Waiting.setData(Z0003Registry.Statue.get(), Z0003Petrify.statueOf(Waiting).since(World.getGameTime() - Z0003Petrify.TimeoutTicks * 2L));
        Helper.startSequence().thenExecuteAfter(60, () -> {
            Z0003Statue Now = Z0003Petrify.statueOf(Waiting);
            Helper.assertTrue(Now != null && !Now.cracking(), "the player broke free on a dedicated server");
            Helper.assertFalse(Waiting.hasData(Z0003Registry.Passing.get()), "a dedicated server marked the stone as passing");
            reset(World);
            dismiss(Helper, Waiting);
        }).thenSucceed();
    }

    @AfterBatch(batch = "z0003_player_dedicatedStays")
    public static void afterDedicated(ServerLevel World) {
        reset(World);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "z0003_player_takeCure")
    public static void rescuerTakesOneCure(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        reset(World);
        Z0003Host.force(false);
        Z0003TestPlayer Held = stone(Helper, 14.5, 16.5, "z0003_clutching");
        Held.getInventory().setItem(5, Z0003VialItem.create(1));
        Held.getInventory().setItem(30, Z0003VialItem.create(1));
        Z0003TestPlayer Rescuer = player(Helper, 16.5, 16.5, "z0003_rescuer");
        Helper.onEachTick(() -> Rescuer.getCooldowns().tick());
        Helper.assertTrue(Z0003VialItem.carried(Held), "the stone player does not count as carrying the cure");
        InteractionResult First = Rescuer.interactOn(Held, InteractionHand.MAIN_HAND);
        Helper.assertTrue(First.consumesAction(), "taking the cure did nothing: " + First);
        Helper.assertTrue(Z0003VialItem.is(Rescuer.getMainHandItem()) && Rescuer.getMainHandItem().getCount() == 1, "the cure did not reach the empty hand");
        Helper.assertTrue(Held.getInventory().getItem(5).isEmpty() && Z0003VialItem.is(Held.getInventory().getItem(30)), "the wrong cure was taken");
        Helper.assertTrue(vials(Held) == 1 && vials(Rescuer) == 1, "more than one cure moved");
        Helper.assertTrue(Held.getInventory().getItem(0).is(Items.DIRT) && Held.getInventory().getItem(0).getCount() == DirtCount, "something besides the cure was taken");
        Helper.assertTrue(Z0003Petrify.isStatue(Held), "taking the cure freed the player");
        InteractionResult Again = Rescuer.interactOn(Held, InteractionHand.OFF_HAND);
        Helper.assertFalse(Again.consumesAction(), "a second cure came out on the same click");
        Helper.assertTrue(vials(Held) == 1, "a second cure moved on the same click");
        Helper.startSequence().thenExecuteAfter(Z0003VialItem.TakeCooldownTicks + 2, () -> {
            Rescuer.getInventory().setItem(8, Rescuer.getMainHandItem().copy());
            Rescuer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            InteractionResult Busy = Rescuer.interactOn(Held, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Busy.consumesAction(), "taking the cure with a busy hand did nothing: " + Busy);
            Helper.assertTrue(Rescuer.getMainHandItem().is(Items.STICK), "the busy hand was emptied");
            Helper.assertTrue(vials(Rescuer) == 2 && vials(Held) == 0, "the cure did not go into the pack");
            reset(World);
            dismiss(Helper, Held);
            dismiss(Helper, Rescuer);
        }).thenSucceed();
    }

    @AfterBatch(batch = "z0003_player_takeCure")
    public static void afterTake(ServerLevel World) {
        reset(World);
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "z0003_player_takeNothing")
    public static void emptyHandedStoneGivesNothing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        reset(World);
        Z0003Host.force(false);
        Z0003TestPlayer Held = stone(Helper, 14.5, 16.5, "z0003_empty");
        Villager Kept = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(18.5, stand(Helper), 16.5));
        Helper.assertTrue(Z0003Petrify.petrify(Kept), "the villager could not be turned to stone");
        Z0003TestPlayer Rescuer = player(Helper, 16.5, 16.5, "z0003_searcher");
        Helper.assertFalse(Z0003VialItem.carried(Held), "the stone player counts as carrying a cure they do not have");
        InteractionResult Result = Rescuer.interactOn(Held, InteractionHand.MAIN_HAND);
        Helper.assertFalse(Result.consumesAction(), "a stone player without the cure gave something up: " + Result);
        Helper.assertTrue(Rescuer.getMainHandItem().isEmpty() && Rescuer.getInventory().isEmpty(), "the rescuer received something");
        Helper.assertTrue(Held.getInventory().getItem(0).getCount() == DirtCount, "the stone player lost items");
        InteractionResult Mob = Rescuer.interactOn(Kept, InteractionHand.MAIN_HAND);
        Helper.assertFalse(Mob.consumesAction(), "a stone villager reacted to an empty hand: " + Mob);
        Helper.assertTrue(Z0003Petrify.isStatue(Held) && Z0003Petrify.isStatue(Kept), "a statue broke free");
        reset(World);
        dismiss(Helper, Held);
        dismiss(Helper, Kept);
        dismiss(Helper, Rescuer);
        Helper.succeed();
    }

    @AfterBatch(batch = "z0003_player_takeNothing")
    public static void afterNothing(ServerLevel World) {
        reset(World);
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "z0003_player_cureFrees")
    public static void takenCureFreesPlayer(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        reset(World);
        Z0003Host.force(false);
        Z0003TestPlayer Held = stone(Helper, 14.5, 16.5, "z0003_saved_by_friend");
        Held.getInventory().setItem(3, Z0003VialItem.create(1));
        Z0003TestPlayer Rescuer = player(Helper, 16.5, 16.5, "z0003_friend");
        Helper.onEachTick(() -> Rescuer.getCooldowns().tick());
        Helper.assertTrue(Rescuer.interactOn(Held, InteractionHand.MAIN_HAND).consumesAction(), "the cure could not be taken");
        Helper.startSequence().thenExecuteAfter(Z0003VialItem.TakeCooldownTicks + 2, () -> {
            InteractionResult Poured = Rescuer.interactOn(Held, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Poured.consumesAction(), "pouring the taken cure did nothing: " + Poured);
        }).thenWaitUntil(() -> Helper.assertFalse(Z0003Petrify.isStatue(Held), "the taken cure did not free the player")).thenExecute(() -> {
            Helper.assertTrue(Z0003VialItem.is(Rescuer.getMainHandItem()), "the rescuer lost the cure after pouring it");
            Helper.assertTrue(vials(Held) == 0 && Held.getInventory().getItem(0).getCount() == DirtCount, "the freed player's pack changed");
            reset(World);
            dismiss(Helper, Held);
            dismiss(Helper, Rescuer);
        }).thenSucceed();
    }

    @AfterBatch(batch = "z0003_player_cureFrees")
    public static void afterCure(ServerLevel World) {
        reset(World);
    }
}
