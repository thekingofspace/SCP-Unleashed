package net.rai.scp_unleashed.scp3008.furniture.electronics.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.PropsRegistry;
import net.rai.scp_unleashed.scp3008.Scp3008Department;
import net.rai.scp_unleashed.scp3008.block.Scp3008FloorBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008SignBlock;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008DepartmentFinder;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008ElectronicsLedger;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008ElectronicsSurvey;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008FurnitureEBlock;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008FurnitureEKind;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008FurnitureERegistry;
import net.rai.scp_unleashed.scp3008.furniture.electronics.Scp3008FurnitureEStorageEntity;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.world.Scp3008Catalogue;
import net.rai.scp_unleashed.scp3008.world.Scp3008Palette;
import net.rai.scp_unleashed.scp3008.world.Scp3008Showrooms;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008FurnitureEGameTests {
    public static final String Arena = "gun_perf_arena";
    public static final int Floor = 2;
    public static final int SettleTicks = 10;

    private Scp3008FurnitureEGameTests() {
    }

    private static TagKey<Block> tag(String Path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    private static BlockPos spot(int Index) {
        return new BlockPos(3 + (Index % 7) * 4, Floor, 3 + (Index / 7) * 4);
    }

    private static FakePlayer user(GameTestHelper Helper) {
        FakePlayer User = new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), "scp3008_furne_tester"));
        Vec3 Where = Helper.absoluteVec(new Vec3(1.5, Floor, 1.5));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        return User;
    }

    private static BlockPos place(GameTestHelper Helper, Scp3008FurnitureEKind Type, BlockPos Local, Direction Facing) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Local);
        for (int[] Cell : Type.Cells) {
            World.setBlock(Scp3008FurnitureEBlock.cellPos(Pos, Facing, Cell), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockState State = Scp3008FurnitureERegistry.block(Type).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Facing);
        World.setBlock(Pos, State, Block.UPDATE_ALL);
        Scp3008FurnitureEBlock.placeWhole(World, Pos, State);
        return Pos;
    }

    private static List<BlockPos> cells(Scp3008FurnitureEKind Type, BlockPos Main, Direction Facing) {
        List<BlockPos> Found = new ArrayList<>();
        for (int[] Cell : Type.Cells) {
            Found.add(Scp3008FurnitureEBlock.cellPos(Main, Facing, Cell));
        }
        return Found;
    }

    private static void use(GameTestHelper Helper, FakePlayer User, BlockPos Pos) {
        ServerLevel World = Helper.getLevel();
        World.getBlockState(Pos).useWithoutItem(World, User, new BlockHitResult(Vec3.atCenterOf(Pos), Direction.UP, Pos, false));
    }

    private static void placeAll(GameTestHelper Helper, Direction Facing) {
        ServerLevel World = Helper.getLevel();
        Scp3008FurnitureEKind[] Types = Scp3008FurnitureEKind.values();
        for (int Index = 0; Index < Types.length; Index++) {
            Scp3008FurnitureEKind Type = Types[Index];
            BlockPos Main = place(Helper, Type, spot(Index), Facing);
            List<BlockPos> Parts = cells(Type, Main, Facing);
            for (int Part = 0; Part < Parts.size(); Part++) {
                BlockPos Pos = Parts.get(Part);
                BlockState State = World.getBlockState(Pos);
                Helper.assertTrue(State.getBlock() instanceof Scp3008FurnitureEBlock Piece && Piece.kind() == Type, Type.Id + " part " + Part + " missing facing " + Facing);
                Scp3008FurnitureEBlock Piece = (Scp3008FurnitureEBlock) State.getBlock();
                Helper.assertTrue(State.getValue(HorizontalDirectionalBlock.FACING) == Facing, Type.Id + " part " + Part + " faces wrong");
                Helper.assertTrue(Piece.partOf(State) == Part, Type.Id + " part " + Part + " reads as " + Piece.partOf(State));
                Helper.assertTrue(Scp3008FurnitureEBlock.mainPos(Pos, State).equals(Main), Type.Id + " part " + Part + " cannot find its main block");
                Helper.assertFalse(State.getShape(World, Pos).isEmpty(), Type.Id + " part " + Part + " has an empty shape facing " + Facing);
                Helper.assertFalse(State.getCollisionShape(World, Pos).isEmpty(), Type.Id + " part " + Part + " has no collision facing " + Facing);
                Helper.assertTrue(State.getShape(World, Pos).bounds().maxY <= 1.5, Type.Id + " part " + Part + " is too tall to collide properly");
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_place_north")
    public static void everyPieceFacesNorth(GameTestHelper Helper) {
        placeAll(Helper, Direction.NORTH);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_place_east")
    public static void everyPieceFacesEast(GameTestHelper Helper) {
        placeAll(Helper, Direction.EAST);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_place_south")
    public static void everyPieceFacesSouth(GameTestHelper Helper) {
        placeAll(Helper, Direction.SOUTH);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_place_west")
    public static void everyPieceFacesWest(GameTestHelper Helper) {
        placeAll(Helper, Direction.WEST);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_multiblock")
    public static void multiblocksBreakTogether(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Slot = 0;
        for (Scp3008FurnitureEKind Type : Scp3008FurnitureEKind.values()) {
            if (!Type.multiblock()) {
                continue;
            }
            for (int Broken = 0; Broken < Type.parts(); Broken += Math.max(1, Type.parts() - 1)) {
                Direction Facing = Direction.Plane.HORIZONTAL.stream().toList().get(Slot % 4);
                BlockPos Main = place(Helper, Type, spot(Slot++ % 21), Facing);
                List<BlockPos> Parts = cells(Type, Main, Facing);
                World.destroyBlock(Parts.get(Broken), false);
                for (int Part = 0; Part < Parts.size(); Part++) {
                    Helper.assertTrue(World.getBlockState(Parts.get(Part)).isAir(), Type.Id + " part " + Part + " survived breaking part " + Broken);
                }
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp3008_furne_seat")
    public static void seatsHoldTheirRiders(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        List<PropSeatEntity> Seats = new ArrayList<>();
        List<Scp3008FurnitureEKind> Types = new ArrayList<>();
        int Index = 0;
        for (Scp3008FurnitureEKind Type : Scp3008FurnitureEKind.values()) {
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
            Types.add(Type);
        }
        Scp3008FurnitureEBlock Chair = Scp3008FurnitureERegistry.block(Scp3008FurnitureEKind.GamingChair);
        double Height = Chair.seatHeight(Chair.defaultBlockState());
        Helper.assertTrue(Height > 0.55 && Height < 0.7, "the gaming chair seats at " + Height);
        Helper.runAfterDelay(SettleTicks, () -> {
            for (int Entry = 0; Entry < Seats.size(); Entry++) {
                PropSeatEntity Seat = Seats.get(Entry);
                if (Types.get(Entry).Seat) {
                    Helper.assertTrue(Seat.isAlive() && Seat.isVehicle(), Types.get(Entry).Id + " threw its rider off");
                } else {
                    Helper.assertFalse(Seat.isAlive(), "a seat stayed on " + Types.get(Entry).Id);
                }
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_storage")
    public static void containersTakeLootTables(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = user(Helper);
        int Index = 0;
        int Checked = 0;
        for (Scp3008FurnitureEKind Type : Scp3008FurnitureEKind.values()) {
            BlockPos Pos = place(Helper, Type, spot(Index++), Direction.EAST);
            Helper.assertTrue((World.getBlockEntity(Pos) != null) == Type.storage(), Type.Id + " block entity presence is wrong");
            Helper.assertTrue(Scp3008Catalogue.container(Scp3008FurnitureERegistry.block(Type)) == Type.storage(), Type.Id + " catalogue container flag is wrong");
            if (!Type.storage()) {
                continue;
            }
            List<BlockPos> Parts = cells(Type, Pos, Direction.EAST);
            for (int Part = 1; Part < Parts.size(); Part++) {
                Helper.assertTrue(World.getBlockEntity(Parts.get(Part)) == null, Type.Id + " part " + Part + " has its own block entity");
            }
            Helper.assertTrue(World.getBlockEntity(Pos) instanceof RandomizableContainer, Type.Id + " is not a randomizable container");
            Scp3008FurnitureEStorageEntity Storage = (Scp3008FurnitureEStorageEntity) World.getBlockEntity(Pos);
            Helper.assertTrue(Storage.getContainerSize() == Type.Rows * 9, Type.Id + " holds " + Storage.getContainerSize() + " slots");
            Storage.setLootTable(BuiltInLootTables.SIMPLE_DUNGEON, 3008L);
            Storage.unpackLootTable(null);
            Helper.assertFalse(Storage.isEmpty(), Type.Id + " stayed empty after its loot table was applied");
            use(Helper, User, Parts.get(Parts.size() - 1));
            Checked++;
        }
        Helper.assertTrue(Checked == 4, "expected four storage pieces, found " + Checked);
        BlockPos Bench = place(Helper, Scp3008FurnitureEKind.TvBench, spot(20), Direction.NORTH);
        List<BlockPos> Parts = cells(Scp3008FurnitureEKind.TvBench, Bench, Direction.NORTH);
        use(Helper, User, Parts.get(1));
        Helper.assertFalse(World.getBlockState(Parts.get(2)).getValue(Scp3008FurnitureEBlock.On), "opening the bench drawers switched the screen on");
        use(Helper, User, Parts.get(3));
        Helper.assertTrue(World.getBlockState(Parts.get(0)).getValue(Scp3008FurnitureEBlock.On), "the bench screen did not switch on");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_switch")
    public static void screensAndLampsSwitch(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = user(Helper);
        int Index = 0;
        int Screens = 0;
        for (Scp3008FurnitureEKind Type : Scp3008FurnitureEKind.values()) {
            if (!Type.screen() && !Type.lamp()) {
                continue;
            }
            BlockPos Main = place(Helper, Type, spot(Index++), Direction.SOUTH);
            List<BlockPos> Parts = cells(Type, Main, Direction.SOUTH);
            for (BlockPos Pos : Parts) {
                Helper.assertTrue(World.getBlockState(Pos).getLightEmission(World, Pos) == 0, Type.Id + " glows before it is switched on");
                Helper.assertFalse(Scp3008FurnitureEBlock.switchedOn(World.getBlockState(Pos)), Type.Id + " starts switched on");
            }
            BlockPos Button = Parts.get(Parts.size() - 1);
            use(Helper, User, Button);
            int Want = Type.lamp() ? Type.Lamp : Type.Screen;
            for (int Part = 0; Part < Parts.size(); Part++) {
                BlockState State = World.getBlockState(Parts.get(Part));
                Helper.assertTrue(Scp3008FurnitureEBlock.switchedOn(State), Type.Id + " part " + Part + " did not switch on");
                Helper.assertTrue(State.getLightEmission(World, Parts.get(Part)) == Want, Type.Id + " part " + Part + " gives the wrong light");
            }
            use(Helper, User, Button);
            for (BlockPos Pos : Parts) {
                Helper.assertFalse(Scp3008FurnitureEBlock.switchedOn(World.getBlockState(Pos)), Type.Id + " did not switch off");
            }
            Screens++;
        }
        Helper.assertTrue(Screens >= 14, "only " + Screens + " pieces switch");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_furne_tags")
    public static void everyPieceIsTaggedOnce(GameTestHelper Helper) {
        TagKey<Block> Liftable = tag("scp3008_liftable");
        TagKey<Block> Multiblock = tag("scp3008_multiblock");
        TagKey<Block> Legs = tag("scp3008_salvage/table_legs");
        TagKey<Block> Doors = tag("scp3008_salvage/cabinet_doors");
        TagKey<Block> Unbreakable = tag("scp3008_unbreakable");
        for (Scp3008FurnitureEKind Type : Scp3008FurnitureEKind.values()) {
            BlockState State = Scp3008FurnitureERegistry.block(Type).defaultBlockState();
            List<String> Departments = new ArrayList<>();
            for (String Department : Scp3008Plan.Departments) {
                if (State.is(tag("scp3008_furniture/" + Department))) {
                    Departments.add(Department);
                }
            }
            Helper.assertTrue(Departments.equals(List.of(Scp3008FurnitureEKind.Department)), Type.Id + " sits in departments " + Departments);
            List<String> Mounts = new ArrayList<>();
            for (Scp3008FurnitureEKind.Mount Kind : Scp3008FurnitureEKind.Mount.values()) {
                if (State.is(tag(Kind.TagName))) {
                    Mounts.add(Kind.TagName);
                }
            }
            Helper.assertTrue(Mounts.equals(List.of(Type.Placement.TagName)), Type.Id + " has placement tags " + Mounts);
            Helper.assertTrue(State.is(Liftable), Type.Id + " cannot be lifted");
            Helper.assertTrue(State.is(Multiblock) == Type.multiblock(), Type.Id + " multiblock tag is wrong");
            Helper.assertTrue(State.is(Legs) == (Type == Scp3008FurnitureEKind.ComputerDesk), Type.Id + " table leg salvage tag is wrong");
            Helper.assertFalse(State.is(Doors), Type.Id + " drops cabinet doors");
            Helper.assertFalse(State.is(Unbreakable), Type.Id + " is unbreakable");
            Helper.assertTrue(State.is(BlockTags.MINEABLE_WITH_AXE) != State.is(BlockTags.MINEABLE_WITH_PICKAXE), Type.Id + " needs exactly one of axe or pickaxe");
            LootTable Drops = Helper.getLevel().getServer().reloadableRegistries().getLootTable(Scp3008FurnitureERegistry.block(Type).getLootTable());
            Helper.assertTrue(Drops != LootTable.EMPTY, Type.Id + " has no salvage loot table");
        }
        Helper.assertTrue(Scp3008FurnitureERegistry.Pieces.size() == Scp3008FurnitureEKind.values().length, "an item is missing");
        Helper.assertTrue(Scp3008FurnitureEKind.values().length >= 16, "fewer than sixteen pieces");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_furne_lang")
    public static void everyPieceHasWords(GameTestHelper Helper) {
        Language Words = Language.getInstance();
        for (Scp3008FurnitureEKind Type : Scp3008FurnitureEKind.values()) {
            String Key = Scp3008FurnitureERegistry.block(Type).getDescriptionId();
            Helper.assertTrue(Words.has(Key), Type.Id + " has no name");
            Helper.assertTrue(Words.has(Key + ".tooltip"), Type.Id + " has no tooltip");
            String Hint = Scp3008FurnitureERegistry.block(Type).hint();
            Helper.assertTrue(Hint == null || Words.has("tooltip.scp_unleashed.scp3008_furne." + Hint), Type.Id + " hint " + Hint + " has no words");
        }
        for (String Key : List.of("biome.scp_unleashed.scp3008_electronics", "commands.scp_unleashed.scp3008.find.found", "commands.scp_unleashed.scp3008.find.none",
                "commands.scp_unleashed.scp3008.find.unknown", "commands.scp_unleashed.scp3008.find.teleported")) {
            Helper.assertTrue(Words.has(Key), "missing words for " + Key);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_furne_department")
    public static void electronicsIsADepartment(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008Department Electronics = Scp3008Department.of(Scp3008Plan.Electronics);
        Helper.assertTrue(Electronics == Scp3008Department.Electronics && Electronics.Key.equals("electronics"), "the twelfth department is " + Electronics);
        Helper.assertTrue(World.registryAccess().registryOrThrow(Registries.BIOME).containsKey(Electronics.BiomeKey), "no electronics biome");
        Helper.assertTrue(World.getServer().reloadableRegistries().getLootTable(Electronics.Loot) != LootTable.EMPTY, "no electronics loot table");
        Helper.assertTrue(Scp3008Catalogue.members(Electronics.FurnitureTag).size() == Scp3008FurnitureEKind.values().length, "the electronics furniture tag is incomplete");
        BlockState Sign = Scp3008Palette.state(Scp3008Code.of(Scp3008Code.Sign, Scp3008Code.sign(Scp3008Plan.Electronics, 5, 1)));
        Helper.assertTrue(Sign != null && Sign.getValue(Scp3008SignBlock.Department) == Scp3008Department.Electronics && Sign.getValue(Scp3008SignBlock.Part) == 5,
                "the electronics sign code paints " + Sign);
        BlockState Ground = Scp3008Palette.state(Scp3008Code.of(Scp3008Code.Floor, Scp3008Plan.Electronics + 1));
        Helper.assertTrue(Ground != null && Ground.getValue(Scp3008FloorBlock.Finish) == Scp3008FloorBlock.Style.Electronics, "the electronics floor paints " + Ground);
        int Rooms = 0;
        for (int Ux = -60; Ux <= 60 && Rooms < 3; Ux++) {
            for (int Uz = -20; Uz <= 20 && Rooms < 3; Uz++) {
                if (Scp3008ElectronicsLedger.departmentWith(Ux, Uz, Set.of()) != Scp3008Plan.Electronics || Scp3008Plan.isExit(Ux, Uz)) {
                    continue;
                }
                for (Scp3008Plan.Plot Next : Scp3008Plan.plots(Ux, Uz)) {
                    if (Next.Type() != Scp3008Plan.RoomPlot || Scp3008Plan.cachedDepartment(Ux, Uz) != Scp3008Plan.Electronics) {
                        continue;
                    }
                    List<Scp3008Showrooms.Placement> Placed = Scp3008Showrooms.plot(Ux, Uz, Next);
                    Set<String> Names = new HashSet<>();
                    for (Scp3008Showrooms.Placement Piece : Placed) {
                        Names.add(BuiltInRegistries.BLOCK.getKey(Piece.State().getBlock()).getPath());
                    }
                    Helper.assertTrue(Placed.size() >= 30, "an electronics showroom only holds " + Placed.size() + " blocks");
                    for (String Wanted : List.of("scp3008_electronics_tv_wall", "scp3008_electronics_tv_bench", "scp3008_electronics_phone_table",
                            "scp3008_electronics_computer_desk", "scp3008_electronics_gaming_chair")) {
                        Helper.assertTrue(Names.contains(Wanted), "an electronics showroom at " + Ux + ", " + Uz + " has no " + Wanted);
                    }
                    Rooms++;
                }
            }
        }
        Helper.assertTrue(Rooms >= 1, "no electronics showroom near the origin");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp3008_furne_floor_names")
    public static void floorStylesKeepTheirNames(GameTestHelper Helper) {
        Helper.assertTrue(Scp3008FloorBlock.Style.of(Scp3008Code.StyleFoodCourt) == Scp3008FloorBlock.Style.FoodCourt, "the food court code paints another floor");
        Helper.assertTrue(Scp3008FloorBlock.Style.of(Scp3008Code.rugStyle(0, false)) == Scp3008FloorBlock.Style.RugGrey, "the first rug code paints another floor");
        Helper.assertTrue(Scp3008FloorBlock.Style.of(Scp3008Code.Styles - 1) == Scp3008FloorBlock.Style.RugTealEdge, "the last rug code paints another floor");
        Helper.assertTrue(Scp3008FloorBlock.Style.values().length == Scp3008Code.Styles, "floor codes and styles disagree");
        for (int Department = 0; Department < Scp3008Plan.Departments.length; Department++) {
            Helper.assertTrue(Scp3008FloorBlock.Style.of(Department + 1).getSerializedName().equals(Scp3008Plan.Departments[Department]),
                    "department " + Scp3008Plan.Departments[Department] + " paints floor " + Scp3008FloorBlock.Style.of(Department + 1));
        }
        Block Ground = Scp3008Palette.state(Scp3008Code.of(Scp3008Code.Floor, 0)).getBlock();
        for (Scp3008FloorBlock.Style Style : Scp3008FloorBlock.Style.values()) {
            BlockState State = Ground.defaultBlockState().setValue(Scp3008FloorBlock.Finish, Style);
            CompoundTag Saved = NbtUtils.writeBlockState(State);
            Helper.assertTrue(Saved.getCompound("Properties").getString("style").equals(Style.getSerializedName()), Style + " saves as " + Saved);
            BlockState Loaded = NbtUtils.readBlockState(Helper.getLevel().holderLookup(Registries.BLOCK), Saved);
            Helper.assertTrue(Loaded.equals(State), Style + " loads back as " + Loaded);
        }
        CompoundTag Old = new CompoundTag();
        Old.putString("Name", BuiltInRegistries.BLOCK.getKey(Ground).toString());
        CompoundTag Properties = new CompoundTag();
        Properties.putString("style", "food_court");
        Old.put("Properties", Properties);
        BlockState Loaded = NbtUtils.readBlockState(Helper.getLevel().holderLookup(Registries.BLOCK), Old);
        Helper.assertTrue(Loaded.getValue(Scp3008FloorBlock.Finish) == Scp3008FloorBlock.Style.FoodCourt, "an old food court floor loads as " + Loaded);
        Helper.succeed();
    }

    private static Set<Long> electronicSitesNear() {
        Set<Long> Found = new HashSet<>();
        for (int Ux = -60; Ux <= 60; Ux += 2) {
            for (int Uz = -20; Uz <= 20; Uz++) {
                if (Scp3008ElectronicsLedger.departmentWith(Ux, Uz, Set.of()) == Scp3008Plan.Electronics) {
                    Found.add(Scp3008Plan.site(Ux, Uz));
                }
            }
        }
        return Found;
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_furne_compat")
    public static void generatedAreasKeepTheirDepartments(GameTestHelper Helper) throws IOException {
        Set<Long> Rolled = electronicSitesNear();
        Helper.assertTrue(Rolled.size() >= 3, "only " + Rolled.size() + " electronics sites near the origin");
        Path Folder = Files.createDirectories(Helper.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("scp3008_furne_region"));
        try {
            int[] Cell = null;
            for (int Ux = -60; Ux <= 60 && Cell == null; Ux += 2) {
                for (int Uz = -20; Uz <= 20 && Cell == null; Uz++) {
                    if (Scp3008ElectronicsLedger.departmentWith(Ux, Uz, Set.of()) == Scp3008Plan.Electronics) {
                        Cell = new int[]{Ux, Uz};
                    }
                }
            }
            Helper.assertTrue(Cell != null, "no electronics cell to freeze");
            int Cx = Math.floorDiv(Scp3008Plan.originX(Cell[0]) + 20, 16);
            int Cz = Math.floorDiv(Scp3008Plan.originZ(Cell[1]) + 60, 16);
            int Rx = Math.floorDiv(Cx, 32);
            int Rz = Math.floorDiv(Cz, 32);
            Path Region = Folder.resolve("r." + Rx + "." + Rz + ".mca");
            try (OutputStream Raw = Files.newOutputStream(Region); DataOutputStream Out = new DataOutputStream(Raw)) {
                for (int Index = 0; Index < Scp3008ElectronicsLedger.HeaderEntries * 2; Index++) {
                    int Lx = Index & 31;
                    int Lz = Index >> 5;
                    boolean Present = Index < Scp3008ElectronicsLedger.HeaderEntries && Rx * 32 + Lx == Cx && Rz * 32 + Lz == Cz;
                    Out.writeInt(Present ? 2 << 8 | 1 : 0);
                }
            }
            Files.write(Folder.resolve("r.bad.name.mca"), new byte[]{1, 2, 3});
            Scp3008ElectronicsSurvey Survey = Scp3008ElectronicsSurvey.survey(Folder);
            Helper.assertTrue(Survey.chunks() == 1, "the survey found " + Survey.chunks() + " chunks instead of one");
            Set<Long> Frozen = Set.copyOf(Survey.sites());
            Helper.assertTrue(Frozen.contains(Scp3008Plan.site(Cell[0], Cell[1])), "the explored electronics site was not frozen");
            int MinX = Cx * 16 - Scp3008ElectronicsLedger.Margin;
            int MaxX = Cx * 16 + 15 + Scp3008ElectronicsLedger.Margin;
            int MinZ = Cz * 16 - Scp3008ElectronicsLedger.Margin;
            int MaxZ = Cz * 16 + 15 + Scp3008ElectronicsLedger.Margin;
            for (int Ux = Scp3008Plan.unitX(MinX); Ux <= Scp3008Plan.unitX(MaxX); Ux++) {
                for (int Uz = Scp3008Plan.unitZ(MinZ); Uz <= Scp3008Plan.unitZ(MaxZ); Uz++) {
                    Helper.assertTrue(Scp3008ElectronicsLedger.departmentWith(Ux, Uz, Frozen) == Scp3008Plan.classicDepartment(Ux, Uz),
                            "explored cell " + Ux + ", " + Uz + " changed its department");
                }
            }
            Map<Long, Integer> Seen = new HashMap<>();
            for (int Ux = -60; Ux <= 60; Ux++) {
                for (int Uz = -20; Uz <= 20; Uz++) {
                    long Site = Scp3008Plan.site(Ux, Uz);
                    int Department = Scp3008ElectronicsLedger.departmentWith(Ux, Uz, Frozen);
                    Integer Earlier = Seen.putIfAbsent(Site, Department);
                    Helper.assertTrue(Earlier == null || Earlier == Department, "site " + Site + " is split between departments " + Earlier + " and " + Department);
                    if (Frozen.contains(Site)) {
                        Helper.assertTrue(Department == Scp3008Plan.classicDepartment(Ux, Uz), "a frozen site turned into " + Department);
                    } else if (Rolled.contains(Site)) {
                        Helper.assertTrue(Department == Scp3008Plan.Electronics, "an unexplored electronics site stayed " + Department);
                    }
                }
            }
            CompoundTag Saved = Survey.save(new CompoundTag(), Helper.getLevel().registryAccess());
            Scp3008ElectronicsSurvey Loaded = Scp3008ElectronicsSurvey.load(Saved, Helper.getLevel().registryAccess());
            Helper.assertTrue(Set.copyOf(Loaded.sites()).equals(Frozen) && Loaded.chunks() == 1, "the department survey does not load back the same");
        } finally {
            try (var Walk = Files.walk(Folder)) {
                Walk.sorted((Left, Right) -> Right.compareTo(Left)).forEach(Path0 -> Path0.toFile().delete());
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp3008_furne_compat_survey")
    public static void surveyIsTakenOnceAndKept(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008ElectronicsSurvey First = Scp3008ElectronicsSurvey.of(World);
        Helper.assertTrue(First != null, "the survey could not read the level");
        Helper.assertTrue(First.chunks() > 0 && !First.sites().isEmpty(), "the survey found no explored chunks in a level that has them");
        Scp3008ElectronicsSurvey Second = Scp3008ElectronicsSurvey.of(World);
        Helper.assertTrue(Second == First, "the survey was taken twice instead of being kept");
        CompoundTag Kept = First.save(new CompoundTag(), World.registryAccess());
        Scp3008ElectronicsSurvey Loaded = Scp3008ElectronicsSurvey.load(Kept, World.registryAccess());
        Helper.assertTrue(Loaded.sites().equals(First.sites()) && Loaded.chunks() == First.chunks(), "the survey does not survive saving");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_furne_compat_live")
    public static void ledgerChangesReachTheCache(GameTestHelper Helper) {
        Set<Long> Previous = Scp3008ElectronicsLedger.frozenSites();
        boolean Everything = Scp3008ElectronicsLedger.everythingFrozen();
        int[] Cell = null;
        for (int Ux = -60; Ux <= 60 && Cell == null; Ux += 2) {
            for (int Uz = -20; Uz <= 20 && Cell == null; Uz++) {
                if (Scp3008ElectronicsLedger.departmentWith(Ux, Uz, Set.of()) == Scp3008Plan.Electronics) {
                    Cell = new int[]{Ux, Uz};
                }
            }
        }
        Helper.assertTrue(Cell != null, "no electronics cell near the origin");
        long Site = Scp3008Plan.site(Cell[0], Cell[1]);
        int Classic = Scp3008Plan.classicDepartment(Cell[0], Cell[1]);
        int Frozen;
        int Open;
        try {
            Scp3008ElectronicsLedger.install(List.of());
            Open = Scp3008Plan.cachedDepartment(Cell[0], Cell[1]);
            Scp3008ElectronicsLedger.install(List.of(Site));
            Frozen = Scp3008Plan.cachedDepartment(Cell[0], Cell[1]);
        } finally {
            if (Everything) {
                Scp3008ElectronicsLedger.freezeAll();
            } else {
                Scp3008ElectronicsLedger.install(Previous);
            }
            Scp3008Showrooms.forget();
        }
        Helper.assertTrue(Open == Scp3008Plan.Electronics, "an open electronics site reads as " + Open);
        Helper.assertTrue(Frozen == Classic, "freezing the site left the cache at " + Frozen + " instead of " + Classic);
        int[] Found = Scp3008DepartmentFinder.nearest(Scp3008Plan.Electronics, 0, 0);
        Helper.assertTrue(Found != null && Scp3008Plan.departmentAt(Found[0], Found[2]) == Scp3008Plan.Electronics, "the finder points at " + (Found == null ? "nothing" : Found[0] + ", " + Found[2]));
        Helper.succeed();
    }
}
