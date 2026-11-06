package net.rai.scp_unleashed.scp268.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.scp268.Scp268Neglect;
import net.rai.scp_unleashed.scp268.Scp268Registry;
import net.rai.scp_unleashed.scp268.integration.Scp268Curios;
import net.rai.scp_unleashed.senses.Senses;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp268CuriosGameTests {
    public static final String Arena = "gear_arena";

    private Scp268CuriosGameTests() {
    }

    private static ServerPlayer wearer(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp268Neglect.left(User);
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static ItemStack accessory(ServerPlayer User) {
        ICurioStacksHandler Head = CuriosApi.getCuriosInventory(User).map(Inventory -> Inventory.getCurios().get(Scp268Curios.HeadSlot)).orElse(null);
        return Head == null || Head.getStacks().getSlots() == 0 ? ItemStack.EMPTY : Head.getStacks().getStackInSlot(0);
    }

    private static int caps(ServerPlayer User) {
        Item Cap = Scp268Registry.Cap.get();
        int Count = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Stack.is(Cap)) {
                Count += Stack.getCount();
            }
        }
        ItemStack Accessory = accessory(User);
        if (Accessory.is(Cap)) {
            Count += Accessory.getCount();
        }
        return Count;
    }

    private static void use(ServerPlayer User) {
        User.gameMode.useItem(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp268_curiosOverHelmet")
    public static void curiosOverHelmet(GameTestHelper Helper) {
        Helper.assertTrue(Senses.curiosLoaded(), "Curios is not loaded in the test server");
        ServerPlayer User = wearer(Helper, "scp268_helmet");
        try {
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp268Registry.Cap.get()));
            Scp268Neglect.update(User, false);
            Helper.assertFalse(Scp268.isWorn(User), "the cap counted as worn from the hand");
            use(User);
            Helper.assertTrue(User.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "putting the cap on took the helmet off");
            Helper.assertTrue(User.getMainHandItem().isEmpty(), "the cap stayed in the hand: " + User.getMainHandItem());
            Helper.assertTrue(accessory(User).is(Scp268Registry.Cap.get()), "the cap did not go to the head accessory slot");
            Helper.assertTrue(caps(User) == 1, "the cap was duplicated or lost: " + caps(User));
            Helper.assertTrue(Scp268.isWorn(User) && Scp268.isNeglected(User), "the cap in the accessory slot did nothing on the tick it went on");
            Scp268Neglect.update(User, false);
            Helper.assertTrue(Scp268.isNeglected(User), "the cap in the accessory slot stopped working after a tick");
            Helper.assertTrue(Scp268.settling(User), "putting the cap on in the accessory slot started no cooldown");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp268_curiosBareHead")
    public static void curiosBareHead(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "scp268_bare");
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp268Registry.Cap.get()));
            use(User);
            Helper.assertTrue(caps(User) == 1, "the cap was duplicated or lost: " + caps(User));
            Helper.assertTrue(User.getMainHandItem().isEmpty(), "the cap stayed in the hand");
            Helper.assertTrue(Scp268.isWorn(User), "the cap was not worn after a right click on a bare head");
            Scp268Neglect.update(User, false);
            Helper.assertTrue(Scp268.isNeglected(User), "the worn cap did nothing");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp268_withoutCurios")
    public static void withoutCurios(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "scp268_vanilla");
        try {
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp268Registry.Cap.get()));
            User.setItemInHand(InteractionHand.MAIN_HAND, Scp268Registry.Cap.get().wear(User.serverLevel(), User, InteractionHand.MAIN_HAND, false).getObject());
            Helper.assertTrue(User.getItemBySlot(EquipmentSlot.HEAD).is(Scp268Registry.Cap.get()), "without Curios the cap did not go on the head");
            Helper.assertTrue(User.getMainHandItem().is(Items.IRON_HELMET), "without Curios the helmet was not swapped into the hand");
            Helper.assertFalse(accessory(User).is(Scp268Registry.Cap.get()), "without Curios the cap went to an accessory slot");
            Helper.assertTrue(caps(User) == 1, "the cap was duplicated or lost: " + caps(User));
            Helper.assertTrue(Scp268.isWorn(User) && Scp268.isNeglected(User), "the cap on the head did nothing");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp268_noSecondCap")
    public static void noSecondCap(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "scp268_second");
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp268Registry.Cap.get()));
            use(User);
            Helper.assertTrue(Scp268.isWorn(User), "the first cap did not go on");
            Scp268Neglect.update(User, false);
            for (int Tick = 0; Tick <= Scp268.SwapCooldown; Tick++) {
                User.getCooldowns().tick();
            }
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp268Registry.Cap.get()));
            use(User);
            Helper.assertTrue(User.getMainHandItem().is(Scp268Registry.Cap.get()), "a second cap was taken from the hand");
            Helper.assertTrue(caps(User) == 2, "a second cap changed the cap count: " + caps(User));
            Helper.assertFalse(Scp268Registry.Cap.get().wear(User.serverLevel(), User, InteractionHand.MAIN_HAND, true).getResult().consumesAction(),
                    "a second cap could be worn over the first");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp268_takenOffAccessory")
    public static void takenOffAccessory(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "scp268_off");
        try {
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Scp268Registry.Cap.get()));
            use(User);
            Scp268Neglect.update(User, false);
            Helper.assertTrue(Scp268.isNeglected(User), "the cap did not go on");
            ICurioStacksHandler Head = CuriosApi.getCuriosInventory(User).map(Inventory -> Inventory.getCurios().get(Scp268Curios.HeadSlot)).orElseThrow();
            Head.getStacks().setStackInSlot(0, ItemStack.EMPTY);
            Scp268Neglect.update(User, false);
            Helper.assertFalse(Scp268.isWorn(User) || Scp268.isNeglected(User), "the cap kept working after it left the accessory slot");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }
}
