package net.rai.scp_unleashed.scp268.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.scp268.Scp268Neglect;
import net.rai.scp_unleashed.scp268.Scp268Registry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp268GameTests {
    public static final String Arena = "gear_arena";

    private Scp268GameTests() {
    }

    private static ServerPlayer wearer(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp268Neglect.left(User);
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static void settle(ServerPlayer User) {
        for (int Tick = 0; Tick <= Scp268.SwapCooldown; Tick++) {
            User.getCooldowns().tick();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp268_swapCooldown")
    public static void swapCooldown(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "scp268_swap");
        Item Cap = Scp268Registry.Cap.get();
        Slot Head = User.inventoryMenu.getSlot(InventoryMenu.ARMOR_SLOT_START);
        try {
            Scp268Neglect.update(User, false);
            Helper.assertFalse(Scp268.settling(User), "the cap was cooling down before it was ever worn");
            Helper.assertTrue(Head.mayPlace(new ItemStack(Cap)), "the cap could not be put on at first");
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Cap));
            Scp268Neglect.update(User, false);
            Helper.assertTrue(Scp268.settling(User), "putting the cap on started no cooldown");
            Helper.assertFalse(Head.mayPickup(User), "the cap could be taken off straight after putting it on");
            settle(User);
            Helper.assertFalse(Scp268.settling(User), "the cooldown after putting the cap on never ended");
            Helper.assertTrue(Head.mayPickup(User), "the cap could not be taken off after the cooldown");
            User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            Scp268Neglect.update(User, false);
            Helper.assertTrue(Scp268.settling(User), "taking the cap off started no cooldown");
            Helper.assertFalse(Head.mayPlace(new ItemStack(Cap)), "the cap could be put back on straight after taking it off");
            Helper.assertFalse(new ItemStack(Cap).canEquip(EquipmentSlot.HEAD, User), "the cap could be equipped straight after taking it off");
            settle(User);
            Helper.assertTrue(Head.mayPlace(new ItemStack(Cap)), "the cap could not be put back on after the cooldown");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp268_wearerCannotAct")
    public static void wearerCannotAct(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "scp268_hands");
        Pig Target = Helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 2.0, 2.5));
        BlockPos Spot = Helper.absolutePos(new BlockPos(2, 1, 4));
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Spot), Direction.UP, Spot, false);
        try {
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp268Registry.Cap.get()));
            Helper.assertTrue(blocked(User, Target, Spot, Hit) == 6, "a player wearing SCP-268 could still attack or use things");
            User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            Helper.assertTrue(blocked(User, Target, Spot, Hit) == 0, "a player without SCP-268 could not attack or use things");
        } finally {
            Target.discard();
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp268_antiMemeticSeesWearer")
    public static void antiMemeticSeesWearer(GameTestHelper Helper) {
        ServerPlayer Wearer = wearer(Helper, "scp268_seen");
        ServerPlayer Viewer = wearer(Helper, "scp268_viewer");
        try {
            Wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp268Registry.Cap.get()));
            Helper.assertFalse(Wearer.broadcastToPlayer(Viewer), "a plain viewer could see a player wearing SCP-268");
            Helper.assertFalse(Scp268Neglect.seesThrough(Viewer), "a plain viewer saw through SCP-268");
            Viewer.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 200));
            Helper.assertTrue(Scp268Neglect.seesThrough(Viewer), "an anti-memetic viewer did not see through SCP-268");
            Helper.assertTrue(Wearer.broadcastToPlayer(Viewer), "an anti-memetic viewer could not see a player wearing SCP-268");
            Helper.assertTrue(Viewer.broadcastToPlayer(Wearer), "the wearer lost sight of an ordinary player");
        } finally {
            dismiss(Helper, Viewer);
            dismiss(Helper, Wearer);
        }
        Helper.succeed();
    }

    private static int blocked(ServerPlayer User, Entity Target, BlockPos Spot, BlockHitResult Hit) {
        List<Event> Attempts = List.of(new AttackEntityEvent(User, Target),
                new PlayerInteractEvent.LeftClickBlock(User, Spot, Direction.UP, PlayerInteractEvent.LeftClickBlock.Action.START),
                new PlayerInteractEvent.RightClickBlock(User, InteractionHand.MAIN_HAND, Spot, Hit),
                new PlayerInteractEvent.RightClickItem(User, InteractionHand.MAIN_HAND),
                new PlayerInteractEvent.EntityInteract(User, InteractionHand.MAIN_HAND, Target),
                new PlayerInteractEvent.EntityInteractSpecific(User, InteractionHand.MAIN_HAND, Target, Vec3.ZERO));
        int Count = 0;
        for (Event Attempt : Attempts) {
            NeoForge.EVENT_BUS.post(Attempt);
            if (((ICancellableEvent) Attempt).isCanceled()) {
                Count++;
            }
        }
        return Count;
    }
}
