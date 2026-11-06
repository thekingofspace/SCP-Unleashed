package net.rai.scp_unleashed.goggles.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.goggles.Goggles;
import net.rai.scp_unleashed.scp020.Scp020Spores;
import net.rai.scp_unleashed.scp096.Scp096Persons;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GogglesGameTests {
    public static final String Arena = "gear_arena";

    private GogglesGameTests() {
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
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static IDynamicStackHandler slot(GameTestHelper Helper, ServerPlayer User) {
        ICurioStacksHandler Handler = CuriosApi.getCuriosInventory(User).map(Inventory -> Inventory.getCurios().get(Goggles.SlotId)).orElse(null);
        Helper.assertTrue(Handler != null, "players have no goggles accessory slot");
        Helper.assertTrue(Handler.getStacks().getSlots() == 1, "the goggles slot does not hold exactly one item: " + Handler.getStacks().getSlots());
        return Handler.getStacks();
    }

    private static boolean works(ServerPlayer User, Goggles.Kind Kind) {
        return switch (Kind) {
            case Scramble -> Scp096Persons.shielded(User);
            case Hyperspectral -> Scp020Spores.goggled(User);
            case RiftDetector -> Goggles.isWearing(User, Goggles.Kind.RiftDetector);
        };
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "goggles_sharedSlot")
    public static void sharedSlot(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "goggles_slot");
        try {
            IDynamicStackHandler Slot = slot(Helper, User);
            for (Goggles.Kind Kind : Goggles.Kind.values()) {
                Helper.assertTrue(Slot.isItemValid(0, new ItemStack(Kind.item())), Kind + " does not fit the goggles slot");
                Helper.assertTrue(Goggles.kindOf(new ItemStack(Kind.item())) == Kind, Kind + " is not recognised as goggles");
            }
            Helper.assertFalse(Slot.isItemValid(0, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET)), "a plain helmet fits the goggles slot");
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Goggles.Kind.Scramble.item()));
            User.gameMode.useItem(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND);
            Helper.assertTrue(Slot.getStackInSlot(0).is(Goggles.Kind.Scramble.item()), "right clicking the scramble goggles did not put them in the goggles slot");
            Helper.assertTrue(User.getItemBySlot(EquipmentSlot.HEAD).isEmpty(), "the goggles went to the head as well");
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Goggles.Kind.Hyperspectral.item()));
            User.gameMode.useItem(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND);
            Helper.assertTrue(Slot.getStackInSlot(0).is(Goggles.Kind.Hyperspectral.item()), "the hyperspectral goggles did not take the goggles slot");
            Helper.assertTrue(User.getMainHandItem().is(Goggles.Kind.Scramble.item()), "the scramble goggles were not handed back when swapped out");
            User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Goggles.Kind.RiftDetector.item()));
            User.gameMode.useItem(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND);
            Helper.assertTrue(User.getMainHandItem().is(Goggles.Kind.RiftDetector.item()), "right clicking the rift detector strapped it on");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "goggles_workFromSlot")
    public static void workFromSlot(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "goggles_effects");
        IDynamicStackHandler Slot = slot(Helper, User);
        GameTestSequence Steps = Helper.startSequence();
        for (Goggles.Kind Kind : Goggles.Kind.values()) {
            Steps.thenExecute(() -> {
                Helper.assertFalse(works(User, Kind), Kind + " worked before it was worn");
                Slot.setStackInSlot(0, new ItemStack(Kind.item()));
            }).thenIdle(1).thenExecute(() -> {
                Helper.assertTrue(works(User, Kind), Kind + " did nothing from the goggles slot");
                for (Goggles.Kind Other : Goggles.Kind.values()) {
                    if (Other != Kind) {
                        Helper.assertFalse(works(User, Other), Other + " worked while " + Kind + " was in the slot");
                    }
                }
                Slot.setStackInSlot(0, ItemStack.EMPTY);
            }).thenIdle(1);
        }
        Steps.thenExecute(() -> dismiss(Helper, User)).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "goggles_workOnHead")
    public static void workOnHead(GameTestHelper Helper) {
        ServerPlayer User = wearer(Helper, "goggles_head");
        try {
            for (Goggles.Kind Kind : java.util.List.of(Goggles.Kind.Scramble, Goggles.Kind.Hyperspectral)) {
                User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Kind.item()));
                Helper.assertTrue(works(User, Kind), Kind + " stopped working on the head");
                User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                Helper.assertFalse(works(User, Kind), Kind + " kept working after leaving the head");
            }
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }
}
