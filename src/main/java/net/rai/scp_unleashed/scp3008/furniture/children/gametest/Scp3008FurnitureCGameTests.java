package net.rai.scp_unleashed.scp3008.furniture.children.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.scp3008.furniture.children.Scp3008FurnitureCBlock;
import net.rai.scp_unleashed.scp3008.furniture.children.Scp3008FurnitureCKind;
import net.rai.scp_unleashed.scp3008.furniture.children.Scp3008FurnitureCRegistry;
import net.rai.scp_unleashed.scp3008.furniture.children.Scp3008FurnitureCStorageEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008FurnitureCGameTests {
    public static final String Arena = "gun_perf_arena";
    public static final int Floor = 2;
    public static final int SettleTicks = 10;

    private Scp3008FurnitureCGameTests() {
    }

    private static TagKey<Block> tag(String Path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    private static BlockPos spot(int Index) {
        return new BlockPos(3 + (Index % 7) * 4, Floor, 3 + (Index / 7) * 4);
    }

    private static FakePlayer user(GameTestHelper Helper) {
        FakePlayer User = new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), "scp3008_furnc_tester"));
        Vec3 Where = Helper.absoluteVec(new Vec3(1.5, Floor, 1.5));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        return User;
    }

    private static BlockPos place(GameTestHelper Helper, Scp3008FurnitureCKind Type, BlockPos Local, Direction Facing) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Local);
        for (int[] Cell : Type.Cells) {
            World.setBlock(Scp3008FurnitureCBlock.cellPos(Pos, Facing, Cell), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockState State = Scp3008FurnitureCRegistry.block(Type).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Facing);
        World.setBlock(Pos, State, Block.UPDATE_ALL);
        Scp3008FurnitureCBlock.placeWhole(World, Pos, State);
        return Pos;
    }

    private static List<BlockPos> cells(Scp3008FurnitureCKind Type, BlockPos Main, Direction Facing) {
        List<BlockPos> Found = new ArrayList<>();
        for (int[] Cell : Type.Cells) {
            Found.add(Scp3008FurnitureCBlock.cellPos(Main, Facing, Cell));
        }
        return Found;
    }

    private static void placeAll(GameTestHelper Helper, Direction Facing) {
        ServerLevel World = Helper.getLevel();
        Scp3008FurnitureCKind[] Types = Scp3008FurnitureCKind.values();
        for (int Index = 0; Index < Types.length; Index++) {
            Scp3008FurnitureCKind Type = Types[Index];
            BlockPos Main = place(Helper, Type, spot(Index), Facing);
            List<BlockPos> Parts = cells(Type, Main, Facing);
            for (int Part = 0; Part < Parts.size(); Part++) {
                BlockPos Pos = Parts.get(Part);
                BlockState State = World.getBlockState(Pos);
                Helper.assertTrue(State.getBlock() instanceof Scp3008FurnitureCBlock Piece && Piece.kind() == Type, Type.Id + " part " + Part + " missing facing " + Facing);
                Scp3008FurnitureCBlock Piece = (Scp3008FurnitureCBlock) State.getBlock();
                Helper.assertTrue(State.getValue(HorizontalDirectionalBlock.FACING) == Facing, Type.Id + " part " + Part + " faces " + State.getValue(HorizontalDirectionalBlock.FACING));
                Helper.assertTrue(Piece.partOf(State) == Part, Type.Id + " part " + Part + " reads as " + Piece.partOf(State));
                Helper.assertTrue(Scp3008FurnitureCBlock.mainPos(Pos, State).equals(Main), Type.Id + " part " + Part + " cannot find its main block");
                Helper.assertFalse(State.getShape(World, Pos).isEmpty(), Type.Id + " part " + Part + " has an empty shape facing " + Facing);
                Helper.assertFalse(State.getCollisionShape(World, Pos).isEmpty(), Type.Id + " part " + Part + " has no collision facing " + Facing);
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_place_north")
    public static void everyPieceFacesNorth(GameTestHelper Helper) {
        placeAll(Helper, Direction.NORTH);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_place_east")
    public static void everyPieceFacesEast(GameTestHelper Helper) {
        placeAll(Helper, Direction.EAST);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_place_south")
    public static void everyPieceFacesSouth(GameTestHelper Helper) {
        placeAll(Helper, Direction.SOUTH);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_place_west")
    public static void everyPieceFacesWest(GameTestHelper Helper) {
        placeAll(Helper, Direction.WEST);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_multiblock")
    public static void multiblocksBreakTogether(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Slot = 0;
        for (Scp3008FurnitureCKind Type : Scp3008FurnitureCKind.values()) {
            if (!Type.multiblock()) {
                Helper.assertTrue(Type.parts() == 1, Type.Id + " has a broken cell list");
                continue;
            }
            for (int Broken = 0; Broken < Type.parts(); Broken++) {
                Direction Facing = Direction.Plane.HORIZONTAL.stream().toList().get(Slot % 4);
                BlockPos Main = place(Helper, Type, spot(Slot++), Facing);
                List<BlockPos> Parts = cells(Type, Main, Facing);
                World.destroyBlock(Parts.get(Broken), false);
                for (int Part = 0; Part < Parts.size(); Part++) {
                    Helper.assertTrue(World.getBlockState(Parts.get(Part)).isAir(), Type.Id + " part " + Part + " survived breaking part " + Broken);
                }
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_furnc_seat")
    public static void seatsHoldTheirRiders(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<PropSeatEntity> Seats = new ArrayList<>();
        List<String> Names = new ArrayList<>();
        int Index = 0;
        for (Scp3008FurnitureCKind Type : Scp3008FurnitureCKind.values()) {
            BlockPos Pos = place(Helper, Type, spot(Index++), Direction.NORTH);
            PropSeatEntity Seat = new PropSeatEntity(PropsRegistry.Seat.get(), World);
            Seat.moveTo(Pos.getX() + 0.5, Pos.getY(), Pos.getZ() + 0.5, 0.0f, 0.0f);
            World.addFreshEntity(Seat);
            ArmorStand Rider = EntityType.ARMOR_STAND.create(World);
            Helper.assertTrue(Rider != null, "no armor stand");
            Rider.moveTo(Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 0.0f, 0.0f);
            World.addFreshEntity(Rider);
            Helper.assertTrue(Rider.startRiding(Seat, true), "the rider could not sit on " + Type.Id);
            Seats.add(Seat);
            Names.add(Type.Id);
        }
        Helper.runAfterDelay(SettleTicks, () -> {
            for (int Entry = 0; Entry < Seats.size(); Entry++) {
                Scp3008FurnitureCKind Type = Scp3008FurnitureCKind.values()[Entry];
                PropSeatEntity Seat = Seats.get(Entry);
                if (Type.Seat) {
                    Helper.assertTrue(Seat.isAlive() && Seat.isVehicle(), Names.get(Entry) + " threw its rider off");
                } else {
                    Helper.assertFalse(Seat.isAlive(), "a seat stayed on " + Names.get(Entry));
                }
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_storage")
    public static void containersTakeLootTables(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = user(Helper);
        int Index = 0;
        int Checked = 0;
        for (Scp3008FurnitureCKind Type : Scp3008FurnitureCKind.values()) {
            BlockPos Pos = place(Helper, Type, spot(Index++), Direction.EAST);
            boolean HasEntity = World.getBlockEntity(Pos) != null;
            Helper.assertTrue(HasEntity == Type.storage(), Type.Id + " block entity presence is wrong");
            if (!Type.storage()) {
                continue;
            }
            Helper.assertTrue(World.getBlockEntity(Pos) instanceof RandomizableContainer, Type.Id + " is not a randomizable container");
            Scp3008FurnitureCStorageEntity Storage = (Scp3008FurnitureCStorageEntity) World.getBlockEntity(Pos);
            Helper.assertTrue(Storage.getContainerSize() == Type.Rows * 9, Type.Id + " holds " + Storage.getContainerSize() + " slots");
            Storage.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 3008L);
            Storage.unpackLootTable(null);
            Helper.assertFalse(Storage.isEmpty(), Type.Id + " stayed empty after its loot table was applied");
            Storage.startOpen(User);
            Helper.assertTrue(World.getBlockState(Pos).getValue(Scp3008FurnitureCBlock.Open), Type.Id + " did not show as open");
            Storage.stopOpen(User);
            Helper.assertFalse(World.getBlockState(Pos).getValue(Scp3008FurnitureCBlock.Open), Type.Id + " did not close");
            Container Held = Storage;
            Helper.assertTrue(Held.getContainerSize() > 0, Type.Id + " has no slots");
            Checked++;
        }
        Helper.assertTrue(Checked == 4, "expected four storage pieces, found " + Checked);
        Helper.succeed();
    }

    private static void use(GameTestHelper Helper, FakePlayer User, BlockPos Pos) {
        ServerLevel World = Helper.getLevel();
        World.getBlockState(Pos).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_lamp")
    public static void lampsToggle(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = user(Helper);
        int Index = 0;
        int Checked = 0;
        for (Scp3008FurnitureCKind Type : Scp3008FurnitureCKind.values()) {
            if (!Type.Lamp) {
                continue;
            }
            BlockPos Main = place(Helper, Type, spot(Index++), Direction.SOUTH);
            List<BlockPos> Parts = cells(Type, Main, Direction.SOUTH);
            for (BlockPos Pos : Parts) {
                Helper.assertTrue(World.getBlockState(Pos).getLightEmission(World, Pos) == 0, Type.Id + " glows before it is switched on");
            }
            use(Helper, User, Parts.get(Parts.size() - 1));
            int Brightest = 0;
            for (int Part = 0; Part < Parts.size(); Part++) {
                BlockState State = World.getBlockState(Parts.get(Part));
                Helper.assertTrue(State.getValue(Scp3008FurnitureCBlock.Lit), Type.Id + " part " + Part + " did not switch on");
                Helper.assertTrue(State.getLightEmission(World, Parts.get(Part)) == Type.Glow[Part], Type.Id + " part " + Part + " gives the wrong light");
                Brightest = Math.max(Brightest, State.getLightEmission(World, Parts.get(Part)));
            }
            Helper.assertTrue(Brightest >= 10, Type.Id + " is too dim when lit");
            use(Helper, User, Parts.get(0));
            for (BlockPos Pos : Parts) {
                Helper.assertFalse(World.getBlockState(Pos).getValue(Scp3008FurnitureCBlock.Lit), Type.Id + " did not switch off");
            }
            Checked++;
        }
        Helper.assertTrue(Checked == 4, "expected four lamps, found " + Checked);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furnc_interact")
    public static void doorsParasolsAndSigns(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = user(Helper);
        BlockPos Kitchen = place(Helper, Scp3008FurnitureCKind.PlayKitchen, spot(0), Direction.NORTH);
        Helper.assertFalse(World.getBlockState(Kitchen).getValue(Scp3008FurnitureCBlock.Open), "the play kitchen starts open");
        use(Helper, User, Kitchen);
        Helper.assertTrue(World.getBlockState(Kitchen).getValue(Scp3008FurnitureCBlock.Open), "the play kitchen oven did not open");
        use(Helper, User, Kitchen);
        Helper.assertFalse(World.getBlockState(Kitchen).getValue(Scp3008FurnitureCBlock.Open), "the play kitchen oven did not close");
        BlockPos Parasol = place(Helper, Scp3008FurnitureCKind.Parasol, spot(1), Direction.WEST);
        List<BlockPos> Parts = cells(Scp3008FurnitureCKind.Parasol, Parasol, Direction.WEST);
        Helper.assertTrue(World.getBlockState(Parts.get(1)).getValue(Scp3008FurnitureCBlock.Open), "the parasol is not placed open");
        use(Helper, User, Parts.get(1));
        for (BlockPos Pos : Parts) {
            Helper.assertFalse(World.getBlockState(Pos).getValue(Scp3008FurnitureCBlock.Open), "the parasol did not fold everywhere");
        }
        BlockState Folded = World.getBlockState(Parts.get(1));
        Helper.assertTrue(Folded.getShape(World, Parts.get(1)).bounds().maxX - Folded.getShape(World, Parts.get(1)).bounds().minX < 0.5, "the folded parasol keeps a wide shape");
        BlockPos Sign = place(Helper, Scp3008FurnitureCKind.AisleSign, spot(2), Direction.EAST);
        Helper.assertTrue(World.getBlockState(Sign).getValue(Scp3008FurnitureCBlock.Aisle) == 1, "the aisle sign does not start at one");
        use(Helper, User, Sign);
        Helper.assertTrue(World.getBlockState(Sign).getValue(Scp3008FurnitureCBlock.Aisle) == 2, "the aisle sign did not count up");
        User.setShiftKeyDown(true);
        use(Helper, User, Sign);
        use(Helper, User, Sign);
        Helper.assertTrue(World.getBlockState(Sign).getValue(Scp3008FurnitureCBlock.Aisle) == Scp3008FurnitureCKind.AisleCount, "the aisle sign did not wrap backwards");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_furnc_tags")
    public static void everyPieceIsTaggedOnce(GameTestHelper Helper) {
        TagKey<Block> Liftable = tag("scp3008_liftable");
        TagKey<Block> Multiblock = tag("scp3008_multiblock");
        TagKey<Block> Legs = tag("scp3008_salvage/table_legs");
        TagKey<Block> Doors = tag("scp3008_salvage/cabinet_doors");
        TagKey<Block> Unbreakable = tag("scp3008_unbreakable");
        for (Scp3008FurnitureCKind Type : Scp3008FurnitureCKind.values()) {
            BlockState State = Scp3008FurnitureCRegistry.block(Type).defaultBlockState();
            List<String> Departments = new ArrayList<>();
            for (String Department : Scp3008FurnitureCKind.Departments) {
                if (State.is(tag("scp3008_furniture/" + Department))) {
                    Departments.add(Department);
                }
            }
            Helper.assertTrue(Departments.equals(List.of(Type.Department)), Type.Id + " sits in departments " + Departments);
            List<String> Mounts = new ArrayList<>();
            for (Scp3008FurnitureCKind.Mount Kind : Scp3008FurnitureCKind.Mount.values()) {
                if (State.is(tag(Kind.TagName))) {
                    Mounts.add(Kind.TagName);
                }
            }
            Helper.assertTrue(Mounts.equals(List.of(Type.Placement.TagName)), Type.Id + " has placement tags " + Mounts);
            Helper.assertTrue(State.is(Liftable), Type.Id + " cannot be lifted");
            Helper.assertTrue(State.is(Multiblock) == Type.multiblock(), Type.Id + " multiblock tag is wrong");
            boolean Table = Type == Scp3008FurnitureCKind.PlayTable || Type == Scp3008FurnitureCKind.GardenTable;
            Helper.assertTrue(State.is(Legs) == Table, Type.Id + " table leg salvage tag is wrong");
            Helper.assertFalse(State.is(Doors), Type.Id + " drops cabinet doors");
            Helper.assertFalse(State.is(Unbreakable), Type.Id + " is unbreakable");
            Helper.assertTrue(State.is(BlockTags.MINEABLE_WITH_AXE) != State.is(BlockTags.MINEABLE_WITH_PICKAXE), Type.Id + " needs exactly one of axe or pickaxe");
        }
        Helper.assertTrue(Scp3008FurnitureCRegistry.Pieces.size() == Scp3008FurnitureCKind.values().length, "an item is missing");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_furnc_lang")
    public static void everyPieceHasWords(GameTestHelper Helper) {
        Language Words = Language.getInstance();
        for (Scp3008FurnitureCKind Type : Scp3008FurnitureCKind.values()) {
            String Key = Scp3008FurnitureCRegistry.block(Type).getDescriptionId();
            Helper.assertTrue(Words.has(Key), Type.Id + " has no name");
            Helper.assertTrue(Words.has(Key + ".tooltip"), Type.Id + " has no tooltip");
        }
        for (String Hint : List.of("sit", "lamp", "aisle", "open")) {
            Helper.assertTrue(Words.has("tooltip.scp_unleashed.scp3008_furnc." + Hint), "missing hint " + Hint);
        }
        Helper.succeed();
    }
}
