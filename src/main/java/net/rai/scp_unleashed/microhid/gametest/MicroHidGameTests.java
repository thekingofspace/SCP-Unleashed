package net.rai.scp_unleashed.microhid.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlock;
import net.rai.scp_unleashed.microhid.MicroChargingPlatformBlockEntity;
import net.rai.scp_unleashed.microhid.MicroChargingStatus;
import net.rai.scp_unleashed.microhid.MicroHidBeam;
import net.rai.scp_unleashed.microhid.MicroHidEnergy;
import net.rai.scp_unleashed.microhid.MicroHidItem;
import net.rai.scp_unleashed.microhid.MicroHidRegistry;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MicroHidGameTests {
    public static final String Arena = "micro_hid_arena";
    public static final float Sturdy = 200.0F;
    public static final double Lane = 5.5;
    public static final double ShooterX = 2.5;

    private MicroHidGameTests() {
    }

    public static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static MicroHidTestPlayer shooter(GameTestHelper Helper, String Name, ItemStack Gun, Vec3 Target) {
        ServerLevel World = Helper.getLevel();
        MicroHidTestPlayer User = new MicroHidTestPlayer(World, Name);
        Vec3 Spot = Helper.absoluteVec(new Vec3(ShooterX, stand(Helper), Lane));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0F, 0.0F);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        User.setItemInHand(InteractionHand.MAIN_HAND, Gun);
        aim(User, Helper.absoluteVec(Target));
        return User;
    }

    private static void aim(MicroHidTestPlayer User, Vec3 Point) {
        double Dx = Point.x - User.getX();
        double Dz = Point.z - User.getZ();
        double Dy = Point.y - User.getEyeY();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0F;
        float Pitch = (float) (-(Mth.atan2(Dy, Math.sqrt(Dx * Dx + Dz * Dz)) * Mth.RAD_TO_DEG));
        User.setYRot(Yaw);
        User.setXRot(Pitch);
        User.setYHeadRot(Yaw);
        User.yRotO = Yaw;
        User.xRotO = Pitch;
    }

    private static void dismiss(GameTestHelper Helper, MicroHidTestPlayer User) {
        if (User.isUsingItem()) {
            User.stopUsingItem();
        }
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static Husk target(GameTestHelper Helper, double X, double Z) {
        Husk Dummy = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(X, stand(Helper), Z));
        Dummy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Dummy.setHealth(Sturdy);
        Dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        return Dummy;
    }

    private static Vec3 eyeLevel(GameTestHelper Helper, double X, double Z) {
        return new Vec3(X, stand(Helper) + 1.62, Z);
    }

    private static String hurt(Husk Dummy) {
        return Dummy.getLastDamageSource() == null ? " (no damage source)" : " (last hit by " + Dummy.getLastDamageSource().getMsgId() + ")";
    }

    private static ItemStack gun(int Energy) {
        return MicroHidItem.charged(Energy);
    }

    private static boolean fire(MicroHidTestPlayer User) {
        InteractionResultHolder<ItemStack> Result = User.getMainHandItem().use(User.level(), User, InteractionHand.MAIN_HAND);
        return Result.getResult().consumesAction() && User.isUsingItem();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "micro_hid_charge_up")
    public static void chargeUpDelaysDamage(GameTestHelper Helper) {
        Husk Dummy = target(Helper, 9.5, Lane);
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_spin", gun(MicroHidEnergy.Capacity), eyeLevel(Helper, 9.5, Lane));
        Helper.assertTrue(fire(User), "The charged Micro H.I.D. did not start spinning up");
        Helper.onEachTick(User::doTick);
        Helper.runAfterDelay(MicroHidEnergy.SpinUpTicks - 3, () -> {
            Helper.assertTrue(User.isUsingItem(), "The Micro H.I.D. stopped during its spin up");
            Helper.assertTrue(Dummy.getHealth() >= Sturdy, "The target took " + (Sturdy - Dummy.getHealth()) + " damage before the spin up finished" + hurt(Dummy));
        });
        Helper.runAfterDelay(MicroHidEnergy.SpinUpTicks + 10, () -> {
            Helper.assertTrue(Dummy.getHealth() < Sturdy, "The beam never hurt the target after the spin up");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "micro_hid_line")
    public static void beamHitsLineOnly(GameTestHelper Helper) {
        Husk InLine = target(Helper, 12.5, Lane);
        Husk Aside = target(Helper, 12.5, Lane + 3.5);
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_line", gun(MicroHidEnergy.Capacity), eyeLevel(Helper, 12.5, Lane));
        Helper.assertTrue(fire(User), "The charged Micro H.I.D. did not start");
        Helper.onEachTick(User::doTick);
        Helper.runAfterDelay(MicroHidEnergy.SpinUpTicks + 30, () -> {
            Helper.assertTrue(InLine.getHealth() < Sturdy, "The target in the beam was not hurt");
            Helper.assertTrue(InLine.getHealth() > 0.0F, "The beam killed a sturdy target outright");
            Helper.assertTrue(Aside.getHealth() >= Sturdy, "The target beside the beam took " + (Sturdy - Aside.getHealth()) + " damage" + hurt(Aside));
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "micro_hid_wall")
    public static void wallBlocksBeam(GameTestHelper Helper) {
        int Base = stand(Helper);
        for (int Y = Base; Y <= Base + 3; Y++) {
            for (int Z = 2; Z <= 9; Z++) {
                Helper.setBlock(new BlockPos(7, Y, Z), Blocks.STONE);
            }
        }
        Husk Hidden = target(Helper, 11.5, Lane);
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_wall", gun(MicroHidEnergy.Capacity), eyeLevel(Helper, 11.5, Lane));
        Helper.assertTrue(fire(User), "The charged Micro H.I.D. did not start");
        Helper.onEachTick(User::doTick);
        Helper.runAfterDelay(MicroHidEnergy.SpinUpTicks + 30, () -> {
            Helper.assertTrue(MicroHidEnergy.get(User.getMainHandItem()) < MicroHidEnergy.Capacity, "The Micro H.I.D. never fired at the wall");
            Helper.assertTrue(Hidden.getHealth() >= Sturdy, "The beam reached through the wall for " + (Sturdy - Hidden.getHealth()) + " damage" + hurt(Hidden));
            Helper.assertTrue(Helper.getBlockState(new BlockPos(7, Base + 1, 5)).is(Blocks.STONE), "The beam broke the wall");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "micro_hid_drain")
    public static void energyDrainsToEmpty(GameTestHelper Helper) {
        int Start = 1600;
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_drain", gun(Start), eyeLevel(Helper, 20.5, Lane));
        Helper.assertTrue(fire(User), "The Micro H.I.D. did not start with a partial charge");
        Helper.onEachTick(User::doTick);
        Helper.runAfterDelay(12, () -> {
            int Now = MicroHidEnergy.get(User.getMainHandItem());
            Helper.assertTrue(Now < Start && Now > 0, "The spin up did not drain the charge (" + Now + " of " + Start + ")");
        });
        Helper.runAfterDelay(MicroHidEnergy.SpinUpTicks + 2, () -> Helper.assertTrue(User.isUsingItem(), "The Micro H.I.D. stopped before its charge ran out"));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(MicroHidEnergy.get(User.getMainHandItem()) == 0, "The charge never ran out: " + MicroHidEnergy.get(User.getMainHandItem()));
            Helper.assertFalse(User.isUsingItem(), "The Micro H.I.D. kept firing at zero charge");
            Helper.assertTrue(User.getCooldowns().isOnCooldown(MicroHidRegistry.MicroHid.get()), "The drained Micro H.I.D. was not put on a cooldown");
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "micro_hid_full_charge")
    public static void fullChargeFiresSevenToEightSeconds(GameTestHelper Helper) {
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_full", gun(MicroHidEnergy.Capacity), eyeLevel(Helper, 20.5, Lane));
        Helper.assertTrue(fire(User), "The charged Micro H.I.D. did not start");
        int[] Ticks = {0};
        int[] Stopped = {-1};
        Helper.onEachTick(() -> {
            if (Stopped[0] >= 0) {
                return;
            }
            User.doTick();
            Ticks[0]++;
            if (!User.isUsingItem()) {
                Stopped[0] = Ticks[0];
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Stopped[0] >= 0, "The Micro H.I.D. was still firing after " + Ticks[0] + " ticks with " + MicroHidEnergy.get(User.getMainHandItem()) + " charge left");
            int Firing = Stopped[0] - MicroHidEnergy.SpinUpTicks;
            Helper.assertTrue(Firing >= 140 && Firing <= 160, "A full charge fired for " + Firing + " ticks, expected 7 to 8 seconds");
            Helper.assertTrue(MicroHidEnergy.get(User.getMainHandItem()) == 0, "The Micro H.I.D. stopped with charge left");
            dismiss(Helper, User);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "micro_hid_slow")
    public static void firingSlowsAndStopsSprinting(GameTestHelper Helper) {
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_slow", gun(MicroHidEnergy.Capacity), eyeLevel(Helper, 20.5, Lane));
        User.setSprinting(true);
        Helper.assertTrue(fire(User), "The charged Micro H.I.D. did not start");
        Helper.onEachTick(User::doTick);
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(User.isUsingItem() && !MicroHidBeam.firing(User.getTicksUsingItem()), "The Micro H.I.D. was not spinning up");
            Helper.assertTrue(MicroHidBeam.moveFactor(User.getTicksUsingItem()) == MicroHidBeam.SpinSpeed, "Spin up walk speed is " + MicroHidBeam.moveFactor(User.getTicksUsingItem()));
            Helper.assertFalse(User.isSprinting(), "The player kept sprinting while spinning up");
            User.setSprinting(true);
        });
        Helper.runAfterDelay(MicroHidEnergy.SpinUpTicks + 10, () -> {
            Helper.assertTrue(MicroHidBeam.firing(User.getTicksUsingItem()), "The Micro H.I.D. was not firing");
            Helper.assertTrue(MicroHidBeam.moveFactor(User.getTicksUsingItem()) == MicroHidBeam.FireSpeed, "Firing walk speed is " + MicroHidBeam.moveFactor(User.getTicksUsingItem()));
            Helper.assertTrue(MicroHidBeam.FireSpeed <= 0.35F && MicroHidBeam.SpinSpeed <= 0.55F, "The Micro H.I.D. barely slows its holder");
            Helper.assertFalse(User.isSprinting(), "The player sprinted while firing");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "micro_hid_empty")
    public static void emptyGunCannotFire(GameTestHelper Helper) {
        Husk Dummy = target(Helper, 8.5, Lane);
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_empty", gun(0), eyeLevel(Helper, 8.5, Lane));
        InteractionResultHolder<ItemStack> Result = User.getMainHandItem().use(User.level(), User, InteractionHand.MAIN_HAND);
        Helper.assertTrue(Result.getResult() == InteractionResult.FAIL, "An empty Micro H.I.D. answered " + Result.getResult());
        Helper.assertFalse(User.isUsingItem(), "An empty Micro H.I.D. started spinning up");
        Helper.assertTrue(User.getCooldowns().isOnCooldown(MicroHidRegistry.MicroHid.get()), "The dry click did not set a cooldown");
        Helper.onEachTick(User::doTick);
        Helper.runAfterDelay(MicroHidEnergy.SpinUpTicks + 20, () -> {
            Helper.assertFalse(User.isUsingItem(), "The empty Micro H.I.D. is in use");
            Helper.assertTrue(Dummy.getHealth() >= Sturdy, "An empty Micro H.I.D. hurt the target" + hurt(Dummy));
            InteractionResultHolder<ItemStack> Again = User.getMainHandItem().use(User.level(), User, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Again.getResult() == InteractionResult.FAIL && !User.isUsingItem(), "A second try started the empty Micro H.I.D.");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    private static OutletBlockEntity outlet(GameTestHelper Helper, BlockPos Host, Direction Facing) {
        BlockPos Rel = Host.relative(Facing);
        Helper.setBlock(Rel, ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Facing));
        return Helper.getBlockEntity(Rel);
    }

    private static void link(OutletBlockEntity First, OutletBlockEntity Second) {
        int Slot = WireLink.freeSlot(First.getLinks(), Second.getLinks());
        First.addLink(new WireLink(Second.getBlockPos(), Second.getFacing(), Slot, WireTier.Coated, false, null));
        Second.addLink(new WireLink(First.getBlockPos(), First.getFacing(), Slot, WireTier.Coated, false, null));
    }

    private static String network(GameTestHelper Helper, OutletBlockEntity Near) {
        WireNetwork Network = WireNetworkManager.get(Helper.getLevel()).networkAt(Helper.getLevel(), Near.getBlockPos());
        return Network == null ? " (no network)" : " (supply " + Network.getLastSupply() + ", demand " + Network.getLastDemand() + ", devices " + Network.getLastDeviceCount() + ")";
    }

    private static MicroChargingPlatformBlockEntity platform(GameTestHelper Helper, BlockPos Rel) {
        Helper.setBlock(Rel, MicroHidRegistry.Platform.get().defaultBlockState().setValue(MicroChargingPlatformBlock.Facing, Direction.NORTH));
        return Helper.getBlockEntity(Rel);
    }

    private static Direction back(GameTestHelper Helper, BlockPos Rel) {
        return Helper.getBlockState(Rel).getValue(MicroChargingPlatformBlock.Facing).getOpposite();
    }

    @GameTest(template = Arena, timeoutTicks = 220, batch = "micro_hid_platform_power")
    public static void platformChargesOnlyWhenPowered(GameTestHelper Helper) {
        int Base = stand(Helper);
        BlockPos Powered = new BlockPos(4, Base, 3);
        BlockPos Dark = new BlockPos(12, Base, 3);
        BlockPos Source = new BlockPos(4, Base, 8);
        MicroChargingPlatformBlockEntity Charger = platform(Helper, Powered);
        MicroChargingPlatformBlockEntity Idle = platform(Helper, Dark);
        Helper.setBlock(Source, ModBlocks.CreativeEnergySource.get());
        OutletBlockEntity Near = outlet(Helper, Powered, back(Helper, Powered));
        OutletBlockEntity Far = outlet(Helper, Source, Direction.NORTH);
        Helper.assertTrue(Helper.getBlockState(Powered.relative(back(Helper, Powered))).getBlock() instanceof OutletBlock, "The outlet could not mount on the platform's back");
        link(Near, Far);
        Charger.dock(gun(0));
        Idle.dock(gun(0));
        Helper.succeedWhen(() -> {
            int Gained = MicroHidEnergy.get(Charger.getDocked());
            Helper.assertTrue(Gained > MicroHidEnergy.ChargeDraw * 10, "The powered platform only charged " + Gained + network(Helper, Near));
            Helper.assertTrue(Charger.getStatus() == MicroChargingStatus.Charging, "The powered platform shows " + Charger.getStatus());
            Helper.assertTrue(MicroHidEnergy.get(Idle.getDocked()) == 0, "The unpowered platform charged to " + MicroHidEnergy.get(Idle.getDocked()));
            Helper.assertTrue(Idle.getStatus() == MicroChargingStatus.Unpowered, "The unpowered platform shows " + Idle.getStatus());
            Helper.assertTrue(Helper.getBlockState(Powered).getValue(MicroChargingPlatformBlock.Status) == MicroChargingStatus.Charging, "The powered platform's lights are off");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "micro_hid_platform_dock")
    public static void dockingKeepsItemData(GameTestHelper Helper) {
        BlockPos Rel = new BlockPos(6, stand(Helper), 5);
        MicroChargingPlatformBlockEntity Charger = platform(Helper, Rel);
        ItemStack Original = gun(4321);
        Original.set(DataComponents.CUSTOM_NAME, Component.literal("Sparky"));
        MicroHidTestPlayer User = shooter(Helper, "micro_hid_dock", Original.copy(), eyeLevel(Helper, 6.5, 5.5));
        BlockPos Pos = Helper.absolutePos(Rel);
        BlockState State = Helper.getLevel().getBlockState(Pos);
        BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false);
        Helper.assertTrue(State.useItemOn(User.getMainHandItem(), Helper.getLevel(), User, InteractionHand.MAIN_HAND, Hit).consumesAction(), "Docking was refused");
        Helper.assertTrue(User.getMainHandItem().isEmpty(), "The hand still holds the docked gun");
        Helper.assertTrue(ItemStack.isSameItemSameComponents(Charger.getDocked(), Original), "Docking changed the gun's data");
        Helper.runAfterDelay(20, () -> {
            Helper.assertTrue(Charger.getStatus() == MicroChargingStatus.Unpowered, "The unwired platform shows " + Charger.getStatus());
            BlockState Now = Helper.getLevel().getBlockState(Pos);
            Helper.assertTrue(Now.useWithoutItem(Helper.getLevel(), User, Hit).consumesAction(), "Taking the gun back was refused");
            ItemStack Back = User.getMainHandItem();
            Helper.assertTrue(ItemStack.isSameItemSameComponents(Back, Original), "Undocking changed the gun's data: " + Back.getComponentsPatch());
            Helper.assertTrue(Charger.getDocked().isEmpty(), "The platform kept a copy of the gun");
            Helper.assertTrue(Charger.getStatus() == MicroChargingStatus.Empty, "The empty platform shows " + Charger.getStatus());
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "micro_hid_platform_drop")
    public static void brokenPlatformDropsGun(GameTestHelper Helper) {
        BlockPos Rel = new BlockPos(6, stand(Helper), 5);
        MicroChargingPlatformBlockEntity Charger = platform(Helper, Rel);
        Charger.dock(gun(777));
        Helper.runAfterDelay(5, () -> {
            Helper.getLevel().destroyBlock(Helper.absolutePos(Rel), false);
            AABB Area = new AABB(Helper.absolutePos(Rel)).inflate(2.0);
            List<ItemEntity> Drops = Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Area, Drop -> MicroHidItem.isMicroHid(Drop.getItem()));
            Helper.assertTrue(Drops.size() == 1, "The broken platform dropped " + Drops.size() + " guns");
            Helper.assertTrue(MicroHidEnergy.get(Drops.getFirst().getItem()) == 777, "The dropped gun lost its charge");
            Helper.succeed();
        });
    }
}
