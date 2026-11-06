package net.rai.scp_unleashed.storage.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageRegistry;
import net.rai.scp_unleashed.storage.StorageTags;
import net.rai.scp_unleashed.storage.TallStorageContainerBlock;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class StorageRosterGameTests {
    public static final String Arena = "scp444_arena";
    public static final List<String> Snacks = List.of("gravity_gum", "echo_crisps", "static_soda", "mirror_mints", "hollow_bar", "frost_fizz", "lucky_wafer",
            "ember_jerky", "ink_toffee", "weight_gummies", "taste_of_tomorrow", "pocket_pretzels", "rewind_raisins", "hiccup_pops", "mob_morsels", "fright_bites");
    public static final List<String> Candies = List.of("blue", "red", "green", "purple", "black", "pink");
    public static final List<String> Others = List.of("scp205_lamp", "scp024_doll_hairs", "scp024_participation_ribbon", "scp066_ball");
    public static final List<String> Kept = List.of("scp330_bowl", "scp420j_joint", "scp335_disk", "scp407_cassette", "scp458_box", "scp093_tears", "scp098_sand_vial",
            "scp148_helmet");
    private static final BlockPos SmallSpot = new BlockPos(6, 2, 8);
    private static final BlockPos TallSpot = new BlockPos(10, 2, 8);

    private StorageRosterGameTests() {
    }

    public static List<String> refused() {
        List<String> Names = new ArrayList<>(Others);
        for (String Snack : Snacks) {
            Names.add("scp261_" + Snack);
        }
        for (String Colour : Candies) {
            Names.add("scp330_candy_" + Colour);
        }
        return Names;
    }

    private static Item item(GameTestHelper Helper, String Name) {
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
        Helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Id), "missing item " + Id);
        return BuiltInRegistries.ITEM.get(Id);
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

    @GameTest(template = Arena, timeoutTicks = 40, batch = "storage_roster")
    public static void lockersRefuseRetiredItems(GameTestHelper Helper) {
        List<StorageContainerBlockEntity> Lockers = List.of(small(Helper), tall(Helper));
        List<String> Names = refused();
        Helper.assertTrue(Names.size() == 26, "expected 26 retired items, found " + Names.size());
        for (String Name : Names) {
            ItemStack Stack = new ItemStack(item(Helper, Name));
            Helper.assertFalse(StorageTags.isStorable(Stack), Name + " is still tagged as storable");
            for (StorageContainerBlockEntity Locker : Lockers) {
                Helper.assertTrue(Locker.refusalFor(Stack) != null, (Locker.isTall() ? "the tall" : "the small") + " locker accepted " + Name);
            }
        }
        for (String Name : Kept) {
            ItemStack Stack = new ItemStack(item(Helper, Name));
            Helper.assertTrue(StorageTags.isStorable(Stack), Name + " is no longer tagged as storable");
            for (StorageContainerBlockEntity Locker : Lockers) {
                Helper.assertTrue(Locker.refusalFor(Stack) == null, (Locker.isTall() ? "the tall" : "the small") + " locker refused " + Name);
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "storage_roster")
    public static void bowlStoresAndCandyCannot(GameTestHelper Helper) {
        StorageContainerBlockEntity Locker = small(Helper);
        Player Keeper = Helper.makeMockPlayer(GameType.SURVIVAL);
        Keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(Helper, "scp330_candy_red"), 3));
        Locker.store(Keeper, Keeper.getMainHandItem());
        Helper.assertTrue(Locker.peekContents().isEmpty(), "the locker took a candy");
        Helper.assertTrue(Keeper.getMainHandItem().getCount() == 3, "a refused candy left the hand");
        Item Bowl = item(Helper, "scp330_bowl");
        Keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Bowl));
        Locker.store(Keeper, Keeper.getMainHandItem());
        Helper.assertTrue(Locker.peekContents().is(Bowl), "the locker did not take the SCP-330 bowl");
        Helper.assertTrue(Keeper.getMainHandItem().isEmpty(), "storing the bowl left it in the hand");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "storage_roster")
    public static void storedRetiredItemsStayRetrievable(GameTestHelper Helper) {
        StorageContainerBlockEntity Locker = small(Helper);
        Player Keeper = Helper.makeMockPlayer(GameType.SURVIVAL);
        for (String Name : refused()) {
            Item Retired = item(Helper, Name);
            Locker.setContents(new ItemStack(Retired));
            Helper.assertTrue(Locker.hasContents(), "an old locker lost " + Name);
            Keeper.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            Locker.take(Keeper, InteractionHand.MAIN_HAND);
            Helper.assertTrue(Keeper.getMainHandItem().is(Retired), "taking " + Name + " out of an old locker gave " + Keeper.getMainHandItem());
            Helper.assertFalse(Locker.hasContents(), "the locker still holds " + Name + " after it was taken");
        }
        Helper.succeed();
    }
}
