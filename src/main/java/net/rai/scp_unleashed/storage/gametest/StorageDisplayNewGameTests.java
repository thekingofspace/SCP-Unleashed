package net.rai.scp_unleashed.storage.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1762.Scp1762Registry;
import net.rai.scp_unleashed.scp860.Scp860Registry;
import net.rai.scp_unleashed.scp902.Scp902Latch;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.storage.StorageTags;
import net.rai.scp_unleashed.storage.TallStorageContainerBlock;
import net.rai.scp_unleashed.storage.gel.GelStorageRegistry;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class StorageDisplayNewGameTests {
    public static final String Arena = "scp444_arena";
    public static final String LayoutsClass = "net.rai.scp_unleashed.storage.client.StorageLayouts";
    public static final String AuditClass = "net.rai.scp_unleashed.storage.client.StorageLayoutAudit";
    public static final String KeyDisplay = "net.rai.scp_unleashed.scp860.client.Scp860Display";
    public static final String BoxDisplay = "net.rai.scp_unleashed.scp902.client.Scp902Display";
    public static final String ShoeboxDisplay = "net.rai.scp_unleashed.scp1762.client.Scp1762Display";
    public static final List<String> Displays = List.of(KeyDisplay, BoxDisplay, ShoeboxDisplay);
    public static final int HopperWait = 40;
    private static final BlockPos SmallSpot = new BlockPos(6, 2, 8);
    private static final BlockPos TallSpot = new BlockPos(10, 2, 8);
    private static final BlockPos GelSpot = new BlockPos(13, 2, 8);

    private StorageDisplayNewGameTests() {
    }

    private static StorageContainerBlockEntity small(GameTestHelper Helper) {
        Helper.setBlock(SmallSpot, StorageRegistry.SmallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH));
        Helper.assertTrue(Helper.getBlockEntity(SmallSpot) instanceof StorageContainerBlockEntity, "no small storage container");
        return Helper.getBlockEntity(SmallSpot);
    }

    private static StorageContainerBlockEntity tall(GameTestHelper Helper) {
        BlockState Lower = StorageRegistry.TallContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH);
        Helper.setBlock(TallSpot, Lower);
        Helper.setBlock(TallSpot.above(), Lower.setValue(TallStorageContainerBlock.Half, DoubleBlockHalf.UPPER));
        Helper.assertTrue(Helper.getBlockEntity(TallSpot) instanceof StorageContainerBlockEntity, "no tall storage container");
        StorageContainerBlockEntity Locker = Helper.getBlockEntity(TallSpot);
        Helper.assertTrue(Locker.isTall(), "the tall storage container does not report itself as tall");
        return Locker;
    }

    private static StorageContainerBlockEntity gel(GameTestHelper Helper) {
        Helper.setBlock(GelSpot, GelStorageRegistry.GelContainer.get().defaultBlockState().setValue(StorageContainerBlock.Facing, Direction.NORTH));
        Helper.assertTrue(Helper.getBlockEntity(GelSpot) instanceof StorageContainerBlockEntity, "no gel storage container");
        return Helper.getBlockEntity(GelSpot);
    }

    private static String describe(Throwable Problem) {
        Throwable Root = Problem;
        while (Root.getCause() != null && Root.getCause() != Root) {
            Root = Root.getCause();
        }
        return Root.getClass().getSimpleName() + ": " + Root.getMessage();
    }

    private static String name(StorageContainerBlockEntity Locker) {
        if (Locker.isTall()) {
            return "the tall locker";
        }
        return Locker.getBlockState().is(GelStorageRegistry.GelContainer.get()) ? "the gel locker" : "the small locker";
    }

    @SuppressWarnings("unchecked")
    @GameTest(template = Arena, timeoutTicks = 60, batch = "storage_display_new")
    public static void newAnomaliesUseTheirOwnLockerDisplays(GameTestHelper Helper) {
        ItemStack Key = new ItemStack(Scp860Registry.Key.get());
        ItemStack Door = new ItemStack(Scp860Registry.DoorItem.get());
        ItemStack Box = new ItemStack(Scp902Registry.BoxItem.get());
        ItemStack OpenBox = Box.copy();
        OpenBox.set(Scp902Registry.LatchComponent.get(), new Scp902Latch(true, 0L));
        ItemStack Shoebox = new ItemStack(Scp1762Registry.BoxItem.get());
        List<ItemStack> Shown = List.of(Key, Box, Shoebox);
        for (ItemStack Stack : Shown) {
            Helper.assertTrue(StorageTags.isStorable(Stack), Stack.getItem() + " is not tagged as storable, so lockers refuse it");
        }
        Helper.assertFalse(StorageTags.isStorable(Door), "the SCP-860 door is tagged as storable");
        StorageContainerBlockEntity Small = small(Helper);
        StorageContainerBlockEntity Tall = tall(Helper);
        List<String> Lines;
        try {
            ClientCodeLoader Loader = new ClientCodeLoader(StorageDisplayNewGameTests.class.getClassLoader());
            Class<?> Layouts = Loader.loadClass(LayoutsClass);
            Method ForStack = Layouts.getMethod("forStack", ItemStack.class, boolean.class);
            Method Preset = Layouts.getMethod("preset", ItemStack.class, boolean.class);
            List<Object> Mine = new ArrayList<>();
            for (String Display : Displays) {
                Class<?> Found = Loader.loadClass(Display);
                Found.getMethod("register").invoke(null);
                Mine.add(Found.getField("Layout").get(null));
            }
            List<ItemStack> Checked = List.of(Key, Box, OpenBox, Shoebox);
            List<Object> Expected = List.of(Mine.get(0), Mine.get(1), Mine.get(1), Mine.get(2));
            for (int Index = 0; Index < Checked.size(); Index++) {
                ItemStack Stack = Checked.get(Index);
                for (boolean IsTall : new boolean[]{false, true}) {
                    Object Found = ForStack.invoke(null, Stack, IsTall);
                    String Where = (IsTall ? "tall" : "small") + " locker";
                    Helper.assertTrue(Found == Expected.get(Index), Stack.getItem() + " in a " + Where + " resolved to " + Found + " instead of its own display");
                    Helper.assertFalse(Found.equals(Preset.invoke(null, Stack, IsTall)), Stack.getItem() + " in a " + Where + " still uses the generic preset");
                }
            }
            for (boolean IsTall : new boolean[]{false, true}) {
                Object Found = ForStack.invoke(null, Door, IsTall);
                Helper.assertTrue(Found != Mine.get(0), "the SCP-860 door picked up the key's locker display");
                Helper.assertTrue(Found.equals(Preset.invoke(null, Door, IsTall)), "the SCP-860 door resolved to a custom display: " + Found);
            }
            Class<?> Audit = Loader.loadClass(AuditClass);
            Method Run = Audit.getMethod("run", List.class, Level.class, StorageContainerBlockEntity.class, StorageContainerBlockEntity.class, List.class, Function.class, List.class);
            Function<String, InputStream> Resources = Loader::getResourceAsStream;
            Lines = (List<String>) Run.invoke(null, List.of(Key, Box, OpenBox, Shoebox), Helper.getLevel(), Small, Tall, List.of(), Resources, new ArrayList<String>());
        } catch (Throwable Problem) {
            Helper.fail("the new locker displays could not be checked: " + describe(Problem));
            return;
        }
        List<String> Problems = Lines.stream().filter(Line -> Line.startsWith("PROBLEM")).toList();
        Helper.assertTrue(Problems.isEmpty(), "the new locker displays have problems: " + String.join(" | ", Problems));
        long Placed = Lines.stream().filter(Line -> Line.startsWith("ok scp_unleashed:")).count();
        Helper.assertTrue(Placed == 8, "expected 8 audited placements (4 stacks in 2 locker sizes), found " + Placed + ": " + String.join(" | ", Lines));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "storage_refuses_860_door")
    public static void lockersRefuseTheDoorButKeepTheKey(GameTestHelper Helper) {
        Item DoorItem = Scp860Registry.DoorItem.get();
        Item KeyItem = Scp860Registry.Key.get();
        Helper.assertFalse(StorageTags.isStorable(new ItemStack(DoorItem)), "the SCP-860 door is tagged as storable");
        Helper.assertTrue(StorageTags.isStorable(new ItemStack(KeyItem)), "the SCP-860 key is no longer tagged as storable");
        List<StorageContainerBlockEntity> Lockers = List.of(small(Helper), tall(Helper), gel(Helper));
        Player Keeper = Helper.makeMockPlayer(GameType.SURVIVAL);
        for (StorageContainerBlockEntity Locker : Lockers) {
            String Name = name(Locker);
            Helper.assertTrue(Locker.refusalFor(new ItemStack(DoorItem)) != null, Name + " would accept the SCP-860 door");
            Keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DoorItem));
            Locker.store(Keeper, Keeper.getMainHandItem());
            Helper.assertTrue(Locker.peekContents().isEmpty(), Name + " took the SCP-860 door by hand");
            Helper.assertTrue(Keeper.getMainHandItem().is(DoorItem), "a refused SCP-860 door left the hand at " + Name);
            Helper.assertTrue(Locker.refusalFor(new ItemStack(KeyItem)) == null, Name + " refuses the SCP-860 key");
            Keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(KeyItem));
            Locker.store(Keeper, Keeper.getMainHandItem());
            Helper.assertTrue(Locker.peekContents().is(KeyItem), Name + " did not take the SCP-860 key");
            Helper.assertTrue(Keeper.getMainHandItem().isEmpty(), "storing the SCP-860 key left it in the hand at " + Name);
            Locker.release();
            Helper.assertFalse(Locker.hasContents(), Name + " still holds the key after it was released");
        }
        List<BlockPos> Hoppers = List.of(SmallSpot.above(), TallSpot.above(2), GelSpot.above());
        for (BlockPos Spot : Hoppers) {
            Helper.setBlock(Spot, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
            HopperBlockEntity Hopper = Helper.getBlockEntity(Spot);
            Hopper.setItem(0, new ItemStack(DoorItem));
        }
        Helper.runAfterDelay(HopperWait, () -> {
            for (int Index = 0; Index < Lockers.size(); Index++) {
                StorageContainerBlockEntity Locker = Lockers.get(Index);
                HopperBlockEntity Hopper = Helper.getBlockEntity(Hoppers.get(Index));
                Helper.assertTrue(Locker.peekContents().isEmpty(), "a hopper fed the SCP-860 door into " + name(Locker));
                Helper.assertTrue(Hopper.getItem(0).is(DoorItem), "the hopper above " + name(Locker) + " lost the SCP-860 door: " + Hopper.getItem(0));
            }
            Helper.succeed();
        });
    }
}
