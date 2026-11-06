package net.rai.scp_unleashed.storage.gel.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.OutletBlock;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.scp513.Scp513Affliction;
import net.rai.scp_unleashed.scp513.Scp513BellItem;
import net.rai.scp_unleashed.scp513.Scp513Chimes;
import net.rai.scp_unleashed.scp513.Scp513Registry;
import net.rai.scp_unleashed.scp513.gametest.Scp513GameTests;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.gel.GelPacked;
import net.rai.scp_unleashed.storage.gel.GelRules;
import net.rai.scp_unleashed.storage.gel.GelStatus;
import net.rai.scp_unleashed.storage.gel.GelStorageContainerBlock;
import net.rai.scp_unleashed.storage.gel.GelStorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.gel.GelStorageRegistry;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GelStorageGameTests {
    public static final String Arena = Scp513GameTests.Arena;
    public static final BlockPos GelSpot = new BlockPos(10, 1, 6);
    public static final BlockPos SourceSpot = new BlockPos(13, 1, 6);

    private GelStorageGameTests() {
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

    private static GelStorageContainerBlockEntity place(GameTestHelper Helper) {
        Helper.setBlock(GelSpot, GelStorageRegistry.GelContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH));
        GelStorageContainerBlockEntity Locker = (GelStorageContainerBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(GelSpot));
        Helper.assertTrue(Locker != null, "No gel storage container block entity");
        return Locker;
    }

    private static void power(GameTestHelper Helper) {
        Helper.setBlock(SourceSpot, ModBlocks.CreativeEnergySource.get());
        OutletBlockEntity Near = outlet(Helper, GelSpot, Direction.EAST);
        OutletBlockEntity Far = outlet(Helper, SourceSpot, Direction.WEST);
        Helper.assertTrue(Helper.getBlockState(GelSpot.east()).getBlock() instanceof OutletBlock, "An outlet could not mount on the gel container's side");
        link(Near, Far);
    }

    private static void unpower(GameTestHelper Helper) {
        Helper.setBlock(SourceSpot, Blocks.AIR);
    }

    private static void water(GelStorageContainerBlockEntity Locker) {
        Locker.getTank().fill(new FluidStack(Fluids.WATER, GelRules.TankCapacity), IFluidHandler.FluidAction.EXECUTE);
    }

    private static void sealed(GameTestHelper Helper, GelStorageContainerBlockEntity Locker) {
        power(Helper);
        water(Locker);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "storage_gel_faces")
    public static void faces(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        place(Helper);
        BlockPos Pos = Helper.absolutePos(GelSpot);
        BlockState State = World.getBlockState(Pos);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            boolean Mounts = ModBlocks.Outlet.get().defaultBlockState().setValue(OutletBlock.Facing, Side).canSurvive(World, Pos.relative(Side));
            boolean Wanted = Side == Direction.EAST || Side == Direction.WEST;
            Helper.assertTrue(Mounts == Wanted, "An outlet " + (Mounts ? "mounts" : "does not mount") + " on the " + Side + " face of a north facing gel container");
            GelStorageContainerBlock Owner = (GelStorageContainerBlock) State.getBlock();
            PortRole Role = Owner.portRole(State, Side);
            Helper.assertTrue((Role == PortRole.Input) == (Side == Direction.SOUTH), "The water inlet is wrong on the " + Side + " face: " + Role);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 440, batch = "storage_gel_forms")
    public static void forms(GameTestHelper Helper) {
        GelStorageContainerBlockEntity Locker = place(Helper);
        Helper.runAfterDelay(5, () -> Helper.assertTrue(Locker.getStatus() == GelStatus.Idle && Locker.getGel() == 0, "A dry, unpowered container made gel"));
        Helper.runAfterDelay(6, () -> sealed(Helper, Locker));
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Locker.isPowered(), "The container is not powered through its side outlet");
            Helper.assertTrue(Locker.getStatus() == GelStatus.Sealed, "The gel did not set: " + Locker.getStatus() + " at " + Locker.getGel());
            Helper.assertTrue(Locker.isSealed(), "The container does not report a set gel");
            Helper.assertTrue(Locker.getTank().getFluidAmount() < GelRules.TankCapacity, "Making gel used no water");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "storage_gel_keepsBellQuiet")
    public static void keepsBellQuiet(GameTestHelper Helper) {
        Mob Witness = Scp513GameTests.dclass(Helper, 4.5, 1, 6.5);
        GelStorageContainerBlockEntity Locker = place(Helper);
        sealed(Helper, Locker);
        Locker.setGel(GelRules.MaxGel, GelRules.HoldTicks);
        Helper.runAfterDelay(5, () -> Locker.setContents(Scp513GameTests.dueBell(Helper)));
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Locker.isSealed(), "The gel did not stay set while supplied");
            Helper.assertFalse(Scp513Affliction.isAfflicted(Witness), "The bell rang inside a set gel container");
            long Next = Locker.peekContents().getOrDefault(Scp513Registry.NextRing.get(), 0L);
            Helper.assertTrue(Next > Helper.getLevel().getGameTime(), "The set gel did not hold back the bell's ring");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "storage_gel_powerLoss")
    public static void powerLoss(GameTestHelper Helper) {
        Mob Witness = Scp513GameTests.dclass(Helper, 4.5, 1, 6.5);
        GelStorageContainerBlockEntity Locker = place(Helper);
        sealed(Helper, Locker);
        Locker.setGel(GelRules.MaxGel, GelRules.HoldTicks);
        Locker.setContents(Scp513GameTests.bell(Helper.getLevel().getGameTime() + 100000L));
        Helper.runAfterDelay(20, () -> {
            Helper.assertTrue(Locker.getStatus() == GelStatus.Sealed, "The gel was not set before the power was cut");
            unpower(Helper);
        });
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Locker.getStatus() == GelStatus.Failing, "Cutting the power did not start the gel failing: " + Locker.getStatus());
            Helper.assertTrue(Locker.isSealed(), "The gel broke down with no delay");
            Locker.setGel(GelRules.MaxGel, 0);
        });
        Helper.runAfterDelay(46, () -> {
            Helper.assertTrue(Locker.getGel() < GelRules.MaxGel, "The gel did not start breaking down after the delay");
            Locker.setGel(GelRules.SealLevel - 1, 0);
            Locker.peekContents().set(Scp513Registry.NextRing.get(), Helper.getLevel().getGameTime() - 1L);
        });
        Helper.runAfterDelay(47, () -> Helper.succeedWhen(() -> {
            Helper.assertFalse(Locker.isSealed(), "The gel did not break down without power");
            Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "The bell stayed silent after the gel broke down");
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "storage_gel_waterLoss")
    public static void waterLoss(GameTestHelper Helper) {
        Mob Witness = Scp513GameTests.dclass(Helper, 4.5, 1, 6.5);
        GelStorageContainerBlockEntity Locker = place(Helper);
        power(Helper);
        Locker.getTank().fill(new FluidStack(Fluids.WATER, 40), IFluidHandler.FluidAction.EXECUTE);
        Locker.setGel(GelRules.MaxGel, GelRules.HoldTicks);
        Locker.setContents(Scp513GameTests.bell(Helper.getLevel().getGameTime() + 100000L));
        Helper.runAfterDelay(20, () -> Helper.assertTrue(Locker.getStatus() == GelStatus.Sealed, "The gel was not set while water lasted"));
        Helper.runAfterDelay(21, () -> Locker.getTank().drain(GelRules.TankCapacity, IFluidHandler.FluidAction.EXECUTE));
        Helper.runAfterDelay(50, () -> {
            Helper.assertTrue(Locker.isPowered(), "The container lost power in the water test");
            Helper.assertTrue(Locker.getStatus() == GelStatus.Failing, "Running dry did not start the gel failing: " + Locker.getStatus());
            Locker.setGel(GelRules.MaxGel, 0);
        });
        Helper.runAfterDelay(56, () -> {
            Helper.assertTrue(Locker.getGel() < GelRules.MaxGel, "The dry gel did not start breaking down after the delay");
            Locker.setGel(GelRules.SealLevel - 1, 0);
            Locker.peekContents().set(Scp513Registry.NextRing.get(), Helper.getLevel().getGameTime() - 1L);
        });
        Helper.runAfterDelay(57, () -> Helper.succeedWhen(() -> {
            Helper.assertFalse(Locker.isSealed(), "The gel did not break down without water");
            Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "The bell stayed silent after the gel dried out");
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "storage_gel_carry")
    public static void carry(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        GelStorageContainerBlockEntity Locker = place(Helper);
        Locker.setGel(GelRules.MaxGel, GelRules.HoldTicks);
        Locker.setContents(Scp513GameTests.bell(World.getGameTime() + 100000L));
        BlockPos Pos = Helper.absolutePos(GelSpot);
        World.destroyBlock(Pos, true);
        List<ItemEntity> Drops = World.getEntitiesOfClass(ItemEntity.class, new AABB(Pos).inflate(2.0));
        Helper.assertTrue(Drops.size() == 1, "Breaking the gel container dropped " + Drops.size() + " items instead of one packed container");
        ItemStack Dropped = Drops.get(0).getItem();
        Helper.assertTrue(Dropped.is(GelStorageRegistry.GelContainerItem.get()), "The drop is not the gel container: " + Dropped);
        GelPacked Packed = Dropped.get(GelStorageRegistry.Packed.get());
        Helper.assertTrue(Packed != null && Scp513BellItem.is(Packed.Contents()), "The packed container lost the bell");
        Helper.assertTrue(Packed.sealed(), "The packed container lost its gel");
        GelPacked Aged = Packed.aged(GelRules.HoldTicks + GelRules.MaxGel);
        Helper.assertFalse(Aged.sealed(), "Carried gel never breaks down");
        Drops.forEach(ItemEntity::discard);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "storage_gel_carriedPack")
    public static void carriedPack(GameTestHelper Helper) {
        Mob Witness = Scp513GameTests.dclass(Helper, 12.5, 1, 6.5);
        ServerPlayer User = Scp513GameTests.player(Helper, "gel_carrier", 6.5, 6.5, false);
        try {
            ItemStack Pack = new ItemStack(GelStorageRegistry.GelContainerItem.get());
            Pack.set(GelStorageRegistry.Packed.get(), new GelPacked(Scp513GameTests.dueBell(Helper), GelRules.MaxGel, GelRules.HoldTicks));
            User.getInventory().setItem(5, Pack);
            Scp513Chimes.tickCarrier(User);
            Helper.assertFalse(Scp513Affliction.isAfflicted(Witness), "A bell rang inside a carried container with set gel");
            GelPacked Held = User.getInventory().getItem(5).get(GelStorageRegistry.Packed.get());
            Helper.assertTrue(Held != null && Held.Hold() < GelRules.HoldTicks, "Carried gel did not start to age");
            Pack.set(GelStorageRegistry.Packed.get(), new GelPacked(Scp513GameTests.dueBell(Helper), 0, 0));
            User.getInventory().setItem(5, Pack);
            Scp513Chimes.tickCarrier(User);
            Helper.assertTrue(Scp513Affliction.isAfflicted(Witness), "A bell in a carried container with spent gel stayed silent");
        } finally {
            Scp513GameTests.dismiss(Helper, User);
        }
        Helper.succeed();
    }
}
