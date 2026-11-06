package net.rai.scp_unleashed.scp465.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.door.DoorGeometry;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.scp465.Scp465BoxBlock;
import net.rai.scp_unleashed.scp465.Scp465BoxBlockEntity;
import net.rai.scp_unleashed.scp465.Scp465BoxItem;
import net.rai.scp_unleashed.scp465.Scp465Celebration;
import net.rai.scp_unleashed.scp465.Scp465Decor;
import net.rai.scp_unleashed.scp465.Scp465Guest;
import net.rai.scp_unleashed.scp465.Scp465LetterItem;
import net.rai.scp_unleashed.scp465.Scp465Lid;
import net.rai.scp_unleashed.scp465.Scp465Music;
import net.rai.scp_unleashed.scp465.Scp465Names;
import net.rai.scp_unleashed.scp465.Scp465Parties;
import net.rai.scp_unleashed.scp465.Scp465Party;
import net.rai.scp_unleashed.scp465.Scp465PartyBlock;
import net.rai.scp_unleashed.scp465.Scp465Playhead;
import net.rai.scp_unleashed.scp465.Scp465Registry;
import net.rai.scp_unleashed.scp465.Scp465Room;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp465GameTests {
    public static final String Arena = "scp465_arena";
    public static final BlockPos RoomLow = new BlockPos(3, 1, 3);

    private Scp465GameTests() {
    }

    private record Room(BlockPos Low, BlockPos High, Map<BlockPos, BlockState> Shell) {
        BlockPos centre() {
            return new BlockPos((Low.getX() + High.getX()) / 2, Low.getY(), (Low.getZ() + High.getZ()) / 2);
        }
    }

    private static Room room(GameTestHelper Helper, BlockPos Low, int SizeX, int Height, int SizeZ, boolean Ceiling) {
        BlockPos High = Low.offset(SizeX - 1, Height - 1, SizeZ - 1);
        Map<BlockPos, BlockState> Shell = new HashMap<>();
        BlockState Wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState Floor = Blocks.OAK_PLANKS.defaultBlockState();
        for (int X = Low.getX() - 1; X <= High.getX() + 1; X++) {
            for (int Z = Low.getZ() - 1; Z <= High.getZ() + 1; Z++) {
                BlockPos Under = new BlockPos(X, Low.getY() - 1, Z);
                Helper.setBlock(Under, Floor);
                Shell.put(Under, Floor);
                for (int Y = Low.getY(); Y <= High.getY(); Y++) {
                    BlockPos Spot = new BlockPos(X, Y, Z);
                    boolean Edge = X < Low.getX() || X > High.getX() || Z < Low.getZ() || Z > High.getZ();
                    if (Edge) {
                        Helper.setBlock(Spot, Wall);
                        Shell.put(Spot, Wall);
                    } else {
                        Helper.setBlock(Spot, Blocks.AIR.defaultBlockState());
                    }
                }
                if (Ceiling) {
                    BlockPos Over = new BlockPos(X, High.getY() + 1, Z);
                    Helper.setBlock(Over, Wall);
                    Shell.put(Over, Wall);
                }
            }
        }
        return new Room(Low, High, Shell);
    }

    private static Scp465TestPlayer player(GameTestHelper Helper, Vec3 Relative, String Name) {
        ServerLevel World = Helper.getLevel();
        Scp465TestPlayer User = new Scp465TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Relative);
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static Scp465TestPlayer playerAt(GameTestHelper Helper, BlockPos Cell, String Name) {
        return player(Helper, Vec3.atBottomCenterOf(Cell), Name);
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static BlockPos placeBox(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, Scp465Registry.Box.get().defaultBlockState().setValue(Scp465BoxBlock.Facing, Direction.SOUTH));
        return Helper.absolutePos(Relative);
    }

    private static InteractionResult use(ServerPlayer User, BlockPos Absolute) {
        return User.gameMode.useItemOn(User, User.serverLevel(), User.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(Absolute).add(0.0, 0.3, 0.0), Direction.UP, Absolute, false));
    }

    private static Scp465Party partyAt(GameTestHelper Helper, BlockPos Absolute) {
        if (Helper.getLevel().getBlockEntity(Absolute) instanceof Scp465BoxBlockEntity Carton) {
            return Scp465Parties.find(Helper.getLevel(), Carton.partyId());
        }
        return null;
    }

    private static void cleanUp(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp465Parties Data = Scp465Parties.peek(World);
        if (Data == null) {
            return;
        }
        AABB Bounds = AABB.encapsulatingFullBlocks(Helper.absolutePos(BlockPos.ZERO), Helper.absolutePos(new BlockPos(31, 11, 31)));
        for (Scp465Party Party : Data.snapshotList()) {
            if (Bounds.contains(Vec3.atCenterOf(Party.boxPos()))) {
                Scp465Celebration.close(World, Party, null);
            }
        }
    }

    private static int count(ServerPlayer User, Item Wanted) {
        int Total = 0;
        for (int Slot = 0; Slot < User.getInventory().getContainerSize(); Slot++) {
            ItemStack Stack = User.getInventory().getItem(Slot);
            if (Stack.is(Wanted)) {
                Total += Stack.getCount();
            }
        }
        return Total;
    }

    private static boolean refusedOpen(Scp465Room.Verdict Result) {
        return Result == Scp465Room.Verdict.Leaks || Result == Scp465Room.Verdict.TooBig;
    }

    private static void dump(GameTestHelper Helper, Scp465Party Party, Room Built, String Name) {
        String Folder = System.getenv("SCP465_DUMP");
        if (Folder == null || Folder.isEmpty()) {
            return;
        }
        ServerLevel World = Helper.getLevel();
        StringBuilder Text = new StringBuilder("{\"blocks\":[");
        boolean First = true;
        BlockPos Low = Helper.absolutePos(Built.Low().offset(-1, -1, -1));
        BlockPos High = Helper.absolutePos(Built.High().offset(1, 1, 1));
        for (BlockPos Pos : BlockPos.betweenClosed(Low, High)) {
            BlockState State = World.getBlockState(Pos);
            if (State.isAir()) {
                continue;
            }
            Text.append(First ? "" : ",").append("[").append(Pos.getX() - Low.getX()).append(",").append(Pos.getY() - Low.getY()).append(",").append(Pos.getZ() - Low.getZ())
                    .append(",\"").append(State.toString().replace("\"", "")).append("\"]");
            First = false;
        }
        Text.append("],\"guests\":[");
        First = true;
        for (Scp465Guest Guest : Scp465Celebration.guests(World, Party)) {
            Text.append(First ? "" : ",").append("[").append(Guest.getX() - Low.getX()).append(",").append(Guest.getY() - Low.getY()).append(",").append(Guest.getZ() - Low.getZ())
                    .append(",").append(Guest.getYRot()).append(",").append(Guest.kind()).append(",").append(Guest.look()).append(",").append(Guest.action()).append("]");
            First = false;
        }
        Text.append("]}");
        try {
            Files.writeString(Path.of(Folder, Name + ".json"), Text.toString());
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    private static void checkCrowd(GameTestHelper Helper, Scp465Party Party, String Where) {
        ServerLevel World = Helper.getLevel();
        Set<String> Names = new HashSet<>();
        Set<BlockPos> Cells = new HashSet<>();
        Set<BlockPos> Targets = new HashSet<>();
        Set<BlockPos> Floor = new HashSet<>(Party.floor());
        for (Scp465Guest Guest : Scp465Celebration.guests(World, Party)) {
            Helper.assertTrue(Party.inArea(Guest), "An instance spawned outside " + Where + " at " + Guest.position());
            Helper.assertTrue(World.noCollision(Guest, Guest.getBoundingBox().deflate(0.01)), "An instance spawned inside a block in " + Where + " at " + Guest.position());
            BlockPos Cell = BlockPos.containing(Guest.getX(), Party.floorY(), Guest.getZ());
            Helper.assertTrue(Cells.add(Cell), "Two instances spawned in the same cell in " + Where + " at " + Cell);
            Helper.assertFalse(Scp465PartyBlock.isPartyBlock(World.getBlockState(Cell)) && !World.getBlockState(Cell).is(Scp465Registry.decor(Scp465Decor.DanceFloor)),
                    "An instance spawned in a party block in " + Where + " at " + Cell);
            if (Guest.isDj()) {
                continue;
            }
            Helper.assertTrue(Floor.contains(Cell), "An instance spawned off the open floor in " + Where + " at " + Cell);
            Helper.assertTrue(Guest.spot() != null && Targets.add(BlockPos.containing(Guest.spot())), "Two instances share a cheering spot in " + Where);
            if (Guest.kind() == Scp465Guest.KindGuest) {
                Helper.assertTrue(Guest.hasCustomName() && Names.add(Guest.getCustomName().getString()), "A guest name repeats in " + Where + ": " + Guest.getCustomName().getString());
            }
        }
    }

    private static Scp465Room.Verdict scanAt(GameTestHelper Helper, BlockPos Relative) {
        return Scp465Room.scan(Helper.getLevel(), Helper.absolutePos(Relative)).Result();
    }

    private static int partyBlocks(GameTestHelper Helper, Scp465Party Party) {
        int Found = 0;
        for (BlockPos Pos : BlockPos.betweenClosed(Party.low(), Party.high())) {
            if (Scp465PartyBlock.isPartyBlock(Helper.getLevel().getBlockState(Pos))) {
                Found++;
            }
        }
        return Found;
    }

    private static int decorCount(GameTestHelper Helper, Scp465Party Party, Scp465Decor Kind) {
        int Found = 0;
        for (BlockPos Pos : BlockPos.betweenClosed(Party.low(), Party.high())) {
            if (Helper.getLevel().getBlockState(Pos).is(Scp465Registry.decor(Kind))) {
                Found++;
            }
        }
        return Found;
    }

    private static void checkShell(GameTestHelper Helper, Room Built, Set<BlockPos> Allowed, String When) {
        for (Map.Entry<BlockPos, BlockState> Entry : Built.Shell().entrySet()) {
            if (Allowed.contains(Entry.getKey())) {
                continue;
            }
            BlockState Now = Helper.getBlockState(Entry.getKey());
            Helper.assertTrue(Now.equals(Entry.getValue()), When + ": the shell at " + Entry.getKey() + " changed to " + Now);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp465_sealedRoomOpens")
    public static void sealedRoomOpens(GameTestHelper Helper) {
        Room Built = room(Helper, RoomLow, 7, 4, 7, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer User = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_host");
        try {
            Helper.assertTrue(scanAt(Helper, Built.centre()) == Scp465Room.Verdict.Fits, "A sealed 7x4x7 room was not accepted: " + scanAt(Helper, Built.centre()));
            Helper.assertTrue(use(User, Box).consumesAction(), "Opening the box did not consume the click");
            Helper.assertTrue(Scp465BoxBlock.isOpen(Helper.getLevel().getBlockState(Box)), "The lid did not open");
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "No party was recorded");
            Helper.assertTrue(Party.isCelebrant(User), "The opener is not a celebrant");
            Helper.assertTrue(Party.guests().size() >= 4 && Party.dj() != null, "The party has " + Party.guests().size() + " instances");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.DjDeck) >= 1, "There is no DJ deck");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.SnackCounter) >= 2, "There is no snack table");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.DanceFloor) >= 4, "There is no dance floor");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.PunchBowl) == 1, "There is no punch bowl on the bar");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.DiscoBall) + decorCount(Helper, Party, Scp465Decor.PartyLights) >= 1, "There are no party lights");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.Balloons) >= 1, "There are no balloons");
            for (BlockPos Pos : BlockPos.betweenClosed(Party.low(), Party.high())) {
                BlockState State = Helper.getLevel().getBlockState(Pos);
                if (Scp465PartyBlock.isPartyBlock(State)) {
                    Helper.assertTrue(Party.contains(Pos), "A party block was built outside the room at " + Pos);
                }
            }
            checkShell(Helper, Built, Set.of(), "after opening");
            Set<String> Names = new HashSet<>();
            for (Scp465Guest Guest : Scp465Celebration.guests(Helper.getLevel(), Party)) {
                Helper.assertTrue(Party.inArea(Guest), "An instance spawned outside the room");
                if (Guest.kind() == Scp465Guest.KindGuest) {
                    Helper.assertTrue(Guest.hasCustomName() && Guest.isCustomNameVisible(), "A guest has no visible name");
                    Helper.assertTrue(Names.add(Guest.getCustomName().getString()), "Two guests share the name " + Guest.getCustomName().getString());
                }
            }
            Helper.assertTrue(Helper.getLevel().getBlockState(Box).getDestroyProgress(User, Helper.getLevel(), Box) == 0.0f, "The open box can be mined");
            Helper.assertTrue(Helper.getLevel().getBlockState(Box).getPistonPushReaction() == PushReaction.BLOCK, "Pistons can move the open box");
            Helper.assertFalse(User.gameMode.destroyBlock(Box), "A player broke the open box");
            Helper.assertTrue(Helper.getLevel().getBlockState(Box).is(Scp465Registry.Box.get()), "The open box is gone");
        } finally {
            cleanUp(Helper);
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp465_leakingRoomsRefuse")
    public static void leakingRoomsRefuse(GameTestHelper Helper) {
        Room Built = room(Helper, RoomLow, 7, 4, 7, true);
        BlockPos Door = new BlockPos(RoomLow.getX() - 1, 1, RoomLow.getZ() + 3);
        BlockState Lower = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST).setValue(DoorBlock.OPEN, true);
        Helper.setBlock(Door, Lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        Helper.setBlock(Door.above(), Lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        Helper.assertTrue(scanAt(Helper, Built.centre()) == Scp465Room.Verdict.Leaks || scanAt(Helper, Built.centre()) == Scp465Room.Verdict.TooBig,
                "A room with an open door was accepted: " + scanAt(Helper, Built.centre()));
        Helper.setBlock(Door, Lower.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN, false));
        Helper.setBlock(Door.above(), Lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER).setValue(DoorBlock.OPEN, false));
        BlockPos Window = new BlockPos(RoomLow.getX() + 3, 2, RoomLow.getZ() - 1);
        Helper.setBlock(Window, Blocks.GLASS);
        Helper.setBlock(Window.east(), Blocks.GLASS_PANE);
        Helper.assertTrue(scanAt(Helper, Built.centre()) == Scp465Room.Verdict.Fits, "A closed door, glass and a pane did not seal the room: " + scanAt(Helper, Built.centre()));
        BlockPos Hatch = new BlockPos(RoomLow.getX() + 2, RoomLow.getY() + 4, RoomLow.getZ() + 2);
        Helper.setBlock(Hatch, Blocks.AIR);
        Helper.assertTrue(refusedOpen(scanAt(Helper, Built.centre())), "A hole to the open sky did not leak: " + scanAt(Helper, Built.centre()));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp465_facilityDoorsSealRooms")
    public static void facilityDoorsSealRooms(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        int Checked = 0;
        for (Block Each : BuiltInRegistries.BLOCK) {
            if (!(Each instanceof PanelDoorBlock Frame)) {
                continue;
            }
            DoorKind Kind = Frame.getKind();
            if (Kind.getWidth() > 5 || Kind.getHeight() > 4) {
                continue;
            }
            String Name = BuiltInRegistries.BLOCK.getKey(Each).getPath();
            Room Built = room(Helper, RoomLow, 7, 4, 7, true);
            BlockPos Base = Helper.absolutePos(new BlockPos(RoomLow.getX() + 5, RoomLow.getY(), RoomLow.getZ() - 1));
            BlockState Shut = Frame.defaultBlockState().setValue(PanelDoorBlock.Facing, Direction.NORTH).setValue(PanelDoorBlock.Open, false);
            boolean Gap = false;
            for (int Part = 0; Part < Kind.getCellCount(); Part++) {
                World.setBlock(PanelDoorBlock.cellPos(Base, Direction.NORTH, Kind.columnOf(Part), Kind.rowOf(Part)), Shut.setValue(Frame.partProperty(), Part), Block.UPDATE_ALL);
                VoxelShape Closed = DoorGeometry.cellShape(Kind, Direction.NORTH, Kind.columnOf(Part), Kind.rowOf(Part), 0.0f);
                Helper.assertTrue(Scp465Room.blocksAcross(Closed, Direction.NORTH), "A closed " + Name + " lets air through cell " + Part);
                Gap |= !Scp465Room.blocksAcross(DoorGeometry.cellShape(Kind, Direction.NORTH, Kind.columnOf(Part), Kind.rowOf(Part), 1.0f), Direction.NORTH);
            }
            Helper.assertTrue(Gap, "An open " + Name + " still seals the room");
            Helper.assertTrue(scanAt(Helper, Built.centre()) == Scp465Room.Verdict.Fits, "A closed " + Name + " did not seal the room: " + scanAt(Helper, Built.centre()));
            Checked++;
        }
        Helper.assertTrue(Checked > 0, "No facility doors were checked");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp465_openSkyRefuses")
    public static void openSkyRefuses(GameTestHelper Helper) {
        Room Built = room(Helper, RoomLow, 7, 4, 7, false);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer User = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_sky");
        try {
            Helper.assertTrue(refusedOpen(scanAt(Helper, Built.centre())), "A roofless room did not leak: " + scanAt(Helper, Built.centre()));
            use(User, Box);
            Helper.assertFalse(Scp465BoxBlock.isOpen(Helper.getLevel().getBlockState(Box)), "The box opened under the open sky");
            Helper.assertTrue(partyAt(Helper, Box) == null, "A party started under the open sky");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp465_bigRoomRefuses")
    public static void bigRoomRefuses(GameTestHelper Helper) {
        Room Built = room(Helper, new BlockPos(2, 1, 2), 25, 3, 5, true);
        BlockPos Inside = new BlockPos(14, 1, 4);
        Helper.assertTrue(scanAt(Helper, Inside) == Scp465Room.Verdict.TooBig, "A 25 block long room was accepted: " + scanAt(Helper, Inside));
        Helper.setBlock(new BlockPos(26, 1, 4), Blocks.STONE_BRICKS);
        Helper.setBlock(new BlockPos(26, 2, 4), Blocks.STONE_BRICKS);
        Helper.setBlock(new BlockPos(26, 3, 4), Blocks.STONE_BRICKS);
        for (int Z = 2; Z <= 6; Z++) {
            for (int Y = 1; Y <= 3; Y++) {
                Helper.setBlock(new BlockPos(26, Y, Z), Blocks.STONE_BRICKS);
            }
        }
        Helper.assertTrue(scanAt(Helper, Inside) == Scp465Room.Verdict.Fits, "A 24 block long room was refused: " + scanAt(Helper, Inside));
        Helper.assertTrue(Built.Shell().size() > 0, "The test room has no shell");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp465_smallRoomRefuses")
    public static void smallRoomRefuses(GameTestHelper Helper) {
        Room Narrow = room(Helper, new BlockPos(2, 1, 2), 4, 3, 6, true);
        Helper.assertTrue(scanAt(Helper, Narrow.centre()) == Scp465Room.Verdict.TooSmall, "A 4x3x6 room was accepted: " + scanAt(Helper, Narrow.centre()));
        Room Low = room(Helper, new BlockPos(12, 1, 2), 6, 2, 6, true);
        Helper.assertTrue(scanAt(Helper, Low.centre()) == Scp465Room.Verdict.TooSmall, "A room two blocks tall was accepted: " + scanAt(Helper, Low.centre()));
        Room Least = room(Helper, new BlockPos(2, 1, 14), 5, 3, 5, true);
        Helper.assertTrue(scanAt(Helper, Least.centre()) == Scp465Room.Verdict.Fits, "The smallest allowed room was refused: " + scanAt(Helper, Least.centre()));
        BlockPos Box = placeBox(Helper, Least.centre());
        Scp465TestPlayer User = playerAt(Helper, Least.centre().offset(1, 0, 0), "scp465_tiny");
        try {
            use(User, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "The smallest room did not hold a party");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.DjDeck) == 1 && decorCount(Helper, Party, Scp465Decor.SnackCounter) >= 2, "The small party is missing its booth or bar");
            int People = Scp465Celebration.people(25);
            Helper.assertTrue(People >= 6 && Party.guests().size() == People, "A 5x5 room should hold " + People + " people, not " + Party.guests().size());
            checkCrowd(Helper, Party, "the smallest room");
            dump(Helper, Party, Least, "room_small");
        } finally {
            cleanUp(Helper);
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp465_snapshotRestores")
    public static void snapshotRestores(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 8, 4, 7, true);
        BlockPos Table = Built.centre().offset(-2, 0, 0);
        Helper.setBlock(Table, Blocks.CRAFTING_TABLE);
        Helper.setBlock(Table.offset(0, 0, 1), Blocks.BOOKSHELF);
        BlockPos Chest = new BlockPos(RoomLow.getX(), 1, RoomLow.getZ() + 1);
        Helper.setBlock(Chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
        if (World.getBlockEntity(Helper.absolutePos(Chest)) instanceof ChestBlockEntity Stored) {
            Stored.setItem(0, new ItemStack(Items.DIAMOND, 5));
            Stored.setItem(13, new ItemStack(Items.CAKE));
        }
        Helper.setBlock(new BlockPos(RoomLow.getX() + 4, 3, RoomLow.getZ()), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        for (int X = 0; X < 4; X++) {
            Helper.setBlock(new BlockPos(RoomLow.getX() + 2 + X, 1, RoomLow.getZ() + 5), Blocks.RED_CARPET);
        }
        Helper.setBlock(new BlockPos(RoomLow.getX() + 6, 1, RoomLow.getZ() + 1), Blocks.POTTED_POPPY);
        BlockPos BoxSpot = Built.centre().offset(1, 0, 1);
        BlockPos Box = placeBox(Helper, BoxSpot);
        Map<BlockPos, BlockState> Before = new HashMap<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Built.Low(), Built.High())) {
            Before.put(Pos.immutable(), Helper.getBlockState(Pos));
        }
        Scp465TestPlayer User = playerAt(Helper, Built.centre().offset(2, 0, -2), "scp465_restore");
        try {
            use(User, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "The furnished room did not open");
            Helper.assertTrue(Party.contains(Helper.absolutePos(Table)), "The crafting table in the middle of the room is not part of the inside");
            Helper.assertFalse(Helper.getBlockState(Table).is(Blocks.CRAFTING_TABLE), "The crafting table was left in the middle of the party");
            Helper.assertFalse(Helper.getBlockState(Chest).is(Blocks.CHEST), "The chest was left in the party");
            Helper.assertTrue(World.getEntitiesOfClass(ItemEntity.class, Party.bounds().inflate(2.0)).isEmpty(), "Clearing the room dropped items");
            checkShell(Helper, Built, Set.of(), "during the party");
            Helper.assertTrue(Scp465BoxBlock.isOpen(Helper.getBlockState(BoxSpot)), "The box is not open");
            BlockPos Placed = new BlockPos(RoomLow.getX() + 6, 1, RoomLow.getZ() + 3);
            if (!Party.contains(Helper.absolutePos(Placed)) || !Helper.getBlockState(Placed).isAir()) {
                Placed = null;
                for (BlockPos Spot : Party.floor()) {
                    if (World.getBlockState(Spot).isAir()) {
                        Placed = Helper.relativePos(Spot);
                        break;
                    }
                }
            }
            Helper.assertTrue(Placed != null, "No free floor to build on");
            Helper.setBlock(Placed, Blocks.BARREL);
            BlockPos Barrel = Helper.absolutePos(Placed);
            if (World.getBlockEntity(Barrel) instanceof Container Held) {
                Held.setItem(0, new ItemStack(Items.EMERALD, 3));
            }
            Helper.assertTrue(use(User, Box).consumesAction(), "The celebrant could not close the box");
            Helper.assertFalse(Scp465BoxBlock.isOpen(Helper.getBlockState(BoxSpot)), "The lid stayed open");
            for (Map.Entry<BlockPos, BlockState> Entry : Before.entrySet()) {
                BlockState Now = Helper.getBlockState(Entry.getKey());
                if (Entry.getKey().equals(BoxSpot)) {
                    continue;
                }
                Helper.assertTrue(Now.equals(Entry.getValue()), "The room was not restored at " + Entry.getKey() + ": " + Now + " instead of " + Entry.getValue());
            }
            Helper.assertTrue(World.getBlockEntity(Helper.absolutePos(Chest)) instanceof ChestBlockEntity, "The chest lost its block entity");
            ChestBlockEntity Restored = (ChestBlockEntity) World.getBlockEntity(Helper.absolutePos(Chest));
            Helper.assertTrue(Restored.getItem(0).is(Items.DIAMOND) && Restored.getItem(0).getCount() == 5 && Restored.getItem(13).is(Items.CAKE),
                    "The chest contents were not restored: " + Restored.getItem(0) + ", " + Restored.getItem(13));
            checkShell(Helper, Built, Set.of(), "after closing");
            boolean BarrelDropped = false;
            boolean EmeraldsDropped = false;
            for (ItemEntity Dropped : World.getEntitiesOfClass(ItemEntity.class, Party.bounds().inflate(2.0))) {
                BarrelDropped |= Dropped.getItem().is(Items.BARREL);
                EmeraldsDropped |= Dropped.getItem().is(Items.EMERALD) && Dropped.getItem().getCount() == 3;
            }
            Helper.assertTrue(BarrelDropped && EmeraldsDropped, "The barrel placed during the party was not dropped with its contents");
            Helper.assertTrue(Scp465Parties.find(World, Party.id()) == null, "The party is still on record");
        } finally {
            cleanUp(Helper);
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp465_breachedWallKeepsParty")
    public static void breachedWallKeepsParty(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 7, 4, 7, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer User = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_breach");
        use(User, Box);
        Scp465Party Party = partyAt(Helper, Box);
        Helper.assertTrue(Party != null, "The party did not start");
        int Cells = Party.cells().length;
        BlockPos Hole = new BlockPos(RoomLow.getX() + 7, 1, RoomLow.getZ() + 3);
        BlockPos HoleTop = Hole.above();
        Helper.setBlock(Hole, Blocks.AIR);
        Helper.setBlock(HoleTop, Blocks.AIR);
        Helper.startSequence().thenIdle(30).thenExecute(() -> {
            Scp465Party Still = Scp465Parties.find(World, Party.id());
            Helper.assertTrue(Still != null, "Breaking a wall ended the party");
            Helper.assertTrue(Still.cells().length == Cells, "The party area changed after the breach: " + Still.cells().length + " instead of " + Cells);
            Helper.assertFalse(Still.contains(Helper.absolutePos(Hole)), "The party spread into the broken wall");
            Helper.assertTrue(Scp465BoxBlock.isOpen(Helper.getBlockState(Built.centre())), "The lid closed after the breach");
            use(User, Box);
            Helper.assertTrue(Scp465Parties.find(World, Party.id()) == null, "The party did not end");
            Helper.assertTrue(Helper.getBlockState(Hole).isAir() && Helper.getBlockState(HoleTop).isAir(), "Closing repaired the broken wall");
            checkShell(Helper, Built, Set.of(Hole, HoleTop), "after the breach");
            Helper.assertTrue(partyBlocks(Helper, Party) == 0, "Party blocks were left behind");
            cleanUp(Helper);
            dismiss(Helper, User);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp465_celebrantsAndInvaders")
    public static void celebrantsAndInvaders(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 9, 4, 9, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_celebrant");
        Scp465TestPlayer Outside = player(Helper, new Vec3(1.5, 1.0, 1.5), "scp465_outside");
        try {
            use(Host, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "The party did not start");
            Helper.assertTrue(Party.isCelebrant(Host), "The player inside at opening is not a celebrant");
            Helper.assertFalse(Party.isCelebrant(Outside), "A player outside the room became a celebrant");
            Scp465TestPlayer Late = playerAt(Helper, Built.centre().offset(-2, 0, 2), "scp465_late");
            try {
                Helper.assertTrue(Party.inArea(Late) && !Party.isCelebrant(Late), "A player who walked in later is not treated as an invader");
                Helper.assertTrue(Scp465Celebration.invaders(World, Party).contains(Late), "The late player is not listed as an invader");
                Helper.assertFalse(Scp465Celebration.invaders(World, Party).contains(Host), "The celebrant is listed as an invader");
                Scp465Guest Guest = Scp465Celebration.guests(World, Party).stream().filter(Each -> !Each.isDj()).findFirst().orElseThrow();
                Helper.assertTrue(Host.interactOn(Guest, InteractionHand.MAIN_HAND).consumesAction(), "The celebrant could not greet an instance");
                Helper.assertTrue(Host.isAlive() && !Host.isDeadOrDying(), "The celebrant was hurt by greeting an instance");
                Helper.assertTrue(Guest.flashing(), "The instance did not react to the celebrant");
                Helper.assertTrue(Late.interactOn(Guest, InteractionHand.MAIN_HAND).consumesAction(), "The invader's click was ignored");
                Helper.assertTrue(Late.isDeadOrDying(), "The invader who clicked an instance is still standing");
                Helper.assertTrue(Late.getLastDamageSource() != null && Late.getLastDamageSource().is(Scp465Registry.ConfettiDamage),
                        "The invader died of " + Late.getLastDamageSource());
                String Message = Late.getLastDamageSource().getLocalizedDeathMessage(Late).getString();
                Helper.assertTrue(Message.contains("confetti"), "The death message is " + Message);
                Helper.assertTrue(Guest.isAlive(), "The instance was hurt");
                Helper.assertFalse(Guest.hurt(World.damageSources().playerAttack(Host), 50.0f), "An instance took damage");
            } finally {
                dismiss(Helper, Late);
            }
        } finally {
            cleanUp(Helper);
            dismiss(Helper, Host);
            dismiss(Helper, Outside);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp465_snackBarServesCelebrants")
    public static void snackBarServesCelebrants(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 9, 4, 9, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_hungry");
        try {
            use(Host, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null && !Party.bar().isEmpty(), "The party has no snack bar");
            BlockPos Punch = null;
            for (BlockPos Food : Party.bar()) {
                if (World.getBlockState(Food).is(Scp465Registry.decor(Scp465Decor.PunchBowl))) {
                    Punch = Food;
                }
            }
            Helper.assertTrue(Punch != null, "There is no punch bowl");
            Scp465TestPlayer Late = playerAt(Helper, Built.centre().offset(-2, 0, -2), "scp465_crasher");
            try {
                use(Late, Punch);
                Helper.assertTrue(count(Late, Scp465Registry.Punch.get()) == 0, "An invader was served punch");
                for (int Serving = 1; Serving <= Scp465Party.Servings; Serving++) {
                    use(Host, Punch);
                    Helper.assertTrue(count(Host, Scp465Registry.Punch.get()) == Serving, "Serving " + Serving + " left the celebrant with " + count(Host, Scp465Registry.Punch.get()));
                }
                use(Host, Punch);
                Helper.assertTrue(count(Host, Scp465Registry.Punch.get()) == Scp465Party.Servings, "The celebrant was served more than " + Scp465Party.Servings + " times");
                ItemStack Cup = new ItemStack(Scp465Registry.Punch.get());
                Helper.assertTrue(Cup.getFoodProperties(Host) != null && Cup.getFoodProperties(Host).nutrition() == Scp465Registry.PunchNutrition, "Punch is not food");
                Helper.assertTrue(new ItemStack(Scp465Registry.Cupcake.get()).getFoodProperties(Host) != null && new ItemStack(Scp465Registry.Chips.get()).getFoodProperties(Host) != null,
                        "The snacks are not food");
            } finally {
                dismiss(Helper, Late);
            }
        } finally {
            cleanUp(Helper);
            dismiss(Helper, Host);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp465_djIsGuarded")
    public static void djIsGuarded(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 11, 4, 11, true);
        BlockPos Box = placeBox(Helper, Built.centre().offset(0, 0, 3));
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(2, 0, 3), "scp465_dancer");
        try {
            use(Host, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "The party did not start");
            Vec3 Booth = Party.boothCentre();
            Vec3 Close = Booth.add(Vec3.atLowerCornerOf(Party.boothFacing().getNormal()).scale(1.6)).add(0.4, 0.0, 0.0);
            BlockPos Cell = BlockPos.containing(Close);
            Helper.assertTrue(Party.contains(Cell), "The test spot near the booth is not in the room");
            Scp465TestPlayer Late = player(Helper, Helper.relativeVec(new Vec3(Close.x, Party.floorY(), Close.z)), "scp465_pusher");
            try {
                Late.setDeltaMovement(Vec3.ZERO);
                Helper.assertTrue(Scp465Celebration.flatDistance(Late.position(), Booth) < Scp465Party.GuardRadius, "The invader is not near the booth");
                Scp465Celebration.guard(World, Party, Scp465Celebration.guests(World, Party), World.getGameTime());
                Vec3 Away = Late.position().subtract(Booth);
                Vec3 Push = Late.getDeltaMovement();
                Helper.assertTrue(Push.x * Away.x + Push.z * Away.z > 0.05, "The invader was not pushed away from the booth: " + Push);
                boolean Guarded = Scp465Celebration.guests(World, Party).stream().anyMatch(Guest -> "guard".equals(Guest.taskName()) && Late.getUUID().equals(Guest.faceId()));
                Helper.assertTrue(Guarded, "No instance stepped in front of the invader");
                Host.setDeltaMovement(Vec3.ZERO);
                Host.teleportTo(Close.x, Party.floorY(), Close.z);
                Scp465Celebration.guard(World, Party, Scp465Celebration.guests(World, Party), World.getGameTime());
                Helper.assertTrue(Host.getDeltaMovement().lengthSqr() < 1.0e-6, "A celebrant was pushed away from the booth");
            } finally {
                dismiss(Helper, Late);
            }
        } finally {
            cleanUp(Helper);
            dismiss(Helper, Host);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp465_closingCleansUp")
    public static void closingCleansUp(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 9, 4, 9, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_closer");
        Scp465TestPlayer Friend = playerAt(Helper, Built.centre().offset(-2, 0, 1), "scp465_friend");
        use(Host, Box);
        Scp465Party Party = partyAt(Helper, Box);
        Helper.assertTrue(Party != null, "The party did not start");
        List<UUID> Guests = new ArrayList<>(Party.guests());
        Helper.startSequence().thenIdle(Scp465Party.CheerTicks + 20).thenExecute(() -> {
            Helper.assertTrue(Scp465Celebration.guests(World, Party).stream().anyMatch(Guest -> !Guest.isDj() && !"cheer".equals(Guest.taskName())), "No instance left the cheering ring once the music began");
            Scp465Guest Dj = (Scp465Guest) World.getEntity(Party.dj());
            Helper.assertTrue(Dj != null && Dj.blockPosition().closerThan(Party.djStand(), 1.5), "The DJ is not at the booth");
            dump(Helper, Party, Built, "room_party");
            Scp465TestPlayer Crasher = playerAt(Helper, Built.centre().offset(-3, 0, -3), "scp465_crasher2");
            use(Crasher, Box);
            Helper.assertTrue(Scp465Parties.find(World, Party.id()) != null, "The invader ended the party");
            dismiss(Helper, Crasher);
            use(Friend, Box);
            Helper.assertTrue(Scp465Parties.find(World, Party.id()) == null, "A celebrant could not close the box");
            for (UUID Id : Guests) {
                Helper.assertTrue(World.getEntity(Id) == null, "An instance stayed after the party");
            }
            Helper.assertTrue(partyBlocks(Helper, Party) == 0, "Party blocks were left behind");
            Helper.assertTrue(count(Host, Scp465Registry.Letter.get()) == 1 && count(Friend, Scp465Registry.Letter.get()) == 1, "Not everyone present got a letter");
            ItemStack Letter = Host.getInventory().getItem(Host.getInventory().findSlotMatchingItem(new ItemStack(Scp465Registry.Letter.get())));
            Host.setItemInHand(InteractionHand.MAIN_HAND, Letter.copy());
            Letter.setCount(0);
            Host.getMainHandItem().use(World, Host, InteractionHand.MAIN_HAND);
            Helper.assertTrue(count(Host, Scp465Registry.Letter.get()) == 0, "Opening the letter did not use it up");
            Helper.assertTrue(count(Host, Scp465Registry.Chocolate.get()) == 1, "Opening the letter did not give a piece of chocolate");
            ItemStack Sweet = new ItemStack(Scp465Registry.Chocolate.get());
            Helper.assertTrue(Sweet.getFoodProperties(Host) != null, "The chocolate cannot be eaten");
            Language Words = Language.getInstance();
            int Named = 0;
            Set<String> Lines = new HashSet<>();
            for (int Line = 0; Line < Scp465LetterItem.Notes; Line++) {
                String Key = Scp465LetterItem.noteKey(Line);
                Helper.assertTrue(Words.has(Key), "No text for letter note " + Line);
                String Text = Words.getOrDefault(Key);
                Helper.assertTrue(Lines.add(Text), "Letter note " + Line + " repeats another");
                Helper.assertTrue(Text.contains("%s") == Scp465LetterItem.named(Line), "Letter note " + Line + " does not match its name slot");
                if (Text.contains("%s")) {
                    Named++;
                }
            }
            Helper.assertTrue(Lines.size() >= 20 && Named >= 5, "The letter has " + Lines.size() + " notes, " + Named + " with a name");
            dismiss(Helper, Host);
            dismiss(Helper, Friend);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp465_partySurvivesSaving")
    public static void partySurvivesSaving(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 8, 4, 8, true);
        Helper.setBlock(Built.centre().offset(-2, 0, -2), Blocks.CHEST);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_saver");
        try {
            use(Host, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "The party did not start");
            Party.serve(Host.getUUID());
            Scp465Parties Data = Scp465Parties.get(World);
            CompoundTag Saved = Data.save(new CompoundTag(), World.registryAccess());
            Scp465Parties Loaded = Scp465Parties.load(Saved, World.registryAccess());
            Scp465Party Copy = Loaded.party(Party.id());
            Helper.assertTrue(Copy != null, "The party was lost in saving");
            Helper.assertTrue(Arrays.equals(Copy.cells(), Party.cells()) && Arrays.equals(Copy.restoreCells(), Party.restoreCells()), "The party area changed in saving");
            Helper.assertTrue(Copy.snapshot().sameAs(Party.snapshot()), "The room snapshot changed in saving");
            Helper.assertTrue(Copy.celebrants().equals(Party.celebrants()) && Copy.guests().equals(Party.guests()) && Copy.dj().equals(Party.dj()), "The guest lists changed in saving");
            Helper.assertTrue(Copy.served(Host.getUUID()) == 1, "The serving count was lost in saving");
            Helper.assertTrue(Copy.booth().equals(Party.booth()) && Copy.bar().equals(Party.bar()) && Copy.dance().equals(Party.dance()), "The layout was lost in saving");
            Helper.assertTrue(Copy.musicStart() == Party.musicStart() && Copy.boxPos().equals(Party.boxPos()), "The party clock was lost in saving");
            Helper.assertTrue(Party.snapshot().palette().size() < Party.snapshot().size(), "The snapshot is not stored with a palette");
            Scp465BoxBlockEntity Carton = (Scp465BoxBlockEntity) World.getBlockEntity(Box);
            CompoundTag Tag = Carton.saveWithoutMetadata(World.registryAccess());
            Scp465BoxBlockEntity Reloaded = new Scp465BoxBlockEntity(Box, World.getBlockState(Box));
            Reloaded.loadWithComponents(Tag, World.registryAccess());
            Helper.assertTrue(Reloaded.partyId() == Party.id() && Reloaded.musicStart() == Party.musicStart() && Party.booth().equals(Reloaded.booth()), "The box forgot its party in saving");
        } finally {
            cleanUp(Helper);
            dismiss(Helper, Host);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp465_strayInstanceVanishes")
    public static void strayInstanceVanishes(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, RoomLow, 8, 4, 8, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_unloader");
        use(Host, Box);
        Scp465Party Party = partyAt(Helper, Box);
        Helper.assertTrue(Party != null, "The party did not start");
        Scp465Guest Away = Scp465Celebration.guests(World, Party).stream().filter(Guest -> !Guest.isDj()).findFirst().orElseThrow();
        CompoundTag Stored = new CompoundTag();
        Away.save(Stored);
        Away.discard();
        use(Host, Box);
        Helper.assertTrue(Scp465Parties.find(World, Party.id()) == null, "The party did not close");
        Entity Returned = EntityType.loadEntityRecursive(Stored, World, Made -> Made);
        Helper.assertTrue(Returned instanceof Scp465Guest, "The stored instance could not be reloaded");
        World.addFreshEntity(Returned);
        UUID Id = Returned.getUUID();
        Helper.startSequence().thenIdle(Scp465Guest.OrphanCheck + 5).thenExecute(() -> {
            Helper.assertTrue(World.getEntity(Id) == null, "An instance that came back after its party closed is still there");
            dismiss(Helper, Host);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp465_twoBoxesIndependent")
    public static void twoBoxesIndependent(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room First = room(Helper, new BlockPos(2, 1, 2), 7, 4, 7, true);
        Room Second = room(Helper, new BlockPos(12, 1, 2), 7, 4, 7, true);
        BlockPos FirstBox = placeBox(Helper, First.centre());
        BlockPos SecondBox = placeBox(Helper, Second.centre());
        Scp465TestPlayer One = playerAt(Helper, First.centre().offset(1, 0, 1), "scp465_one");
        Scp465TestPlayer Two = playerAt(Helper, Second.centre().offset(1, 0, 1), "scp465_two");
        try {
            use(One, FirstBox);
            use(Two, SecondBox);
            Scp465Party A = partyAt(Helper, FirstBox);
            Scp465Party B = partyAt(Helper, SecondBox);
            Helper.assertTrue(A != null && B != null && A.id() != B.id(), "Two boxes did not start two parties");
            Helper.assertFalse(A.isCelebrant(Two) || B.isCelebrant(One), "Celebrants were shared between parties");
            use(One, FirstBox);
            Helper.assertTrue(Scp465Parties.find(World, A.id()) == null, "The first party did not end");
            Helper.assertTrue(Scp465Parties.find(World, B.id()) != null && Scp465BoxBlock.isOpen(World.getBlockState(SecondBox)), "Closing one box ended the other party");
            Helper.assertTrue(Scp465Celebration.guests(World, B).size() == B.guests().size(), "The second party lost instances");
        } finally {
            cleanUp(Helper);
            dismiss(Helper, One);
            dismiss(Helper, Two);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp465_bigRoomScales")
    public static void bigRoomScales(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, new BlockPos(2, 1, 2), 20, 5, 20, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_crowd");
        try {
            use(Host, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "The big room did not open");
            int Clowns = 0;
            int Guests = 0;
            int Djs = 0;
            for (Scp465Guest Guest : Scp465Celebration.guests(World, Party)) {
                switch (Guest.kind()) {
                    case Scp465Guest.KindClown -> Clowns++;
                    case Scp465Guest.KindDj -> Djs++;
                    default -> Guests++;
                }
            }
            int People = Scp465Celebration.people(400);
            int Wanted = Scp465Celebration.clowns(People);
            Helper.assertTrue(People >= 25 && Clowns == Wanted && Guests == Scp465Celebration.guestCount(People) && Djs == 1,
                    "A 20x20 room holds " + Clowns + " clowns, " + Guests + " guests and " + Djs + " DJs");
            checkCrowd(Helper, Party, "a 20x20 room");
            dump(Helper, Party, Built, "room_big");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.DjDeck) == 3, "The big booth is not three decks wide");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.SnackCounter) >= 4, "The big bar is too short");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.DiscoBall) == 1, "There is no disco ball");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.StageLight) >= 1, "The booth has no lights");
            Helper.assertTrue(decorCount(Helper, Party, Scp465Decor.Streamers) >= 4, "There are no streamers under the ceiling");
            for (BlockPos Pos : BlockPos.betweenClosed(Party.low(), Party.high())) {
                BlockState State = World.getBlockState(Pos);
                if (State.is(Scp465Registry.decor(Scp465Decor.DanceFloor))) {
                    Helper.assertFalse(Party.contains(Pos.below()), "A dance panel replaced the floor at " + Pos);
                }
                if (State.getBlock() instanceof Scp465PartyBlock Decor && Decor.kind().hanging()) {
                    Helper.assertFalse(Party.contains(Pos.above()), "A hanging decoration is not under the ceiling at " + Pos);
                }
            }
        } finally {
            cleanUp(Helper);
            dismiss(Helper, Host);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp465_strangerBringsBox")
    public static void strangerBringsBox(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp465Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-465 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Safe, "SCP-465 is not filed as Safe");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-465 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue("wondertainment".equals(Plan.Variant()), "SCP-465 arrives with the wrong stranger: " + Plan.Variant());
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Scp465BoxItem.is(Stack) && Stack.getCount() == 1 && Stack.getMaxStackSize() == 1, "The stranger's gift is not the box: " + Stack);
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp465Registry.DocumentId).isPresent(), "SCP-465 has no arrival hook");
        Scp465TestPlayer Reader = player(Helper, new Vec3(8.5, 1.0, 8.5), "scp465_reader");
        try {
            BlockPos Spot = Helper.absolutePos(new BlockPos(8, 1, 8));
            AnomalySpawners.spawnerFor(Scp465Registry.DocumentId).get().spawn(new AnomalySpawners.Discovery(World, Spot, Reader, Scp465Registry.DocumentId, Document, false));
            for (BlockPos Pos : BlockPos.betweenClosed(Spot.offset(-16, -4, -16), Spot.offset(16, 8, 16))) {
                Helper.assertFalse(World.getBlockState(Pos).is(Scp465Registry.Box.get()), "The arrival hook placed the box in the world at " + Pos);
            }
        } finally {
            dismiss(Helper, Reader);
        }
        Helper.assertFalse(Scp465BoxBlock.isOpen(Scp465Registry.Box.get().defaultBlockState()), "The box does not default to closed");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp465_musicAndLid")
    public static void musicAndLid(GameTestHelper Helper) {
        Helper.assertTrue(Scp465Music.Bpm >= 125.0 && Scp465Music.Bpm <= 130.0, "The tempo is " + Scp465Music.Bpm + " BPM");
        int Kicks = 0;
        Set<String> Sounds = new HashSet<>();
        Set<Integer> Scale = Set.of(1, 3, 5, 6, 8, 10, 11);
        Set<Scp465Music.Instrument> Pitched = Set.of(Scp465Music.Instrument.Lead, Scp465Music.Instrument.LeadShine, Scp465Music.Instrument.Arp,
                Scp465Music.Instrument.Stab, Scp465Music.Instrument.Bass, Scp465Music.Instrument.BassAttack, Scp465Music.Instrument.KickSub);
        for (int Step = 0; Step < Scp465Music.Steps * 2; Step++) {
            List<Scp465Music.Note> Notes = Scp465Music.notes(Step);
            Helper.assertTrue(Notes.size() <= 12, "Step " + Step + " plays " + Notes.size() + " sounds at once");
            for (Scp465Music.Note Note : Notes) {
                Helper.assertTrue(Note.Pitch() >= 0.5f && Note.Pitch() <= 2.0f, "A note at step " + Step + " is pitched " + Note.Pitch());
                Helper.assertTrue(Note.Volume() > 0.0f && Note.Volume() <= 1.0f, "A note at step " + Step + " plays at volume " + Note.Volume());
                Helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Note.Voice().sound().getId()), "Unknown music sound " + Note.Voice().sound().getId());
                Sounds.add(Note.Voice().sound().getId().getPath());
                if (Pitched.contains(Note.Voice())) {
                    int Semitone = (int) Math.round(12.0 + 12.0 * Math.log(Note.Pitch()) / Math.log(2.0));
                    Helper.assertTrue(Scale.contains(Math.floorMod(Semitone, 12)), "A " + Note.Voice() + " note at step " + Step + " is outside the key");
                }
            }
            int Position = Step % Scp465Music.StepsPerBar;
            boolean Kick = Notes.stream().anyMatch(Note -> Note.Voice() == Scp465Music.Instrument.Kick);
            Helper.assertTrue(Kick == Scp465Music.kicking(Step), "The kick at step " + Step + " does not match the arrangement");
            Scp465Music.Section Stage = Scp465Music.section(Step);
            if (Position % Scp465Music.StepsPerBeat == 0 && Stage != Scp465Music.Section.Break && Stage != Scp465Music.Section.Build) {
                Helper.assertTrue(Kick, "No kick on the beat at step " + Step);
            }
            if (Kick) {
                Kicks++;
            }
        }
        Helper.assertTrue(Kicks > Scp465Music.Bars * 4, "Only " + Kicks + " kicks in two loops");
        Helper.assertTrue(Sounds.size() >= 10, "The music only uses " + Sounds);
        Helper.assertTrue(Scp465Music.section(0) == Scp465Music.Section.Intro && Scp465Music.section(Scp465Music.Steps) == Scp465Music.Section.Groove,
                "The intro does not give way to the groove on the second pass");
        for (int Bar = 0; Bar < Scp465Music.Bars; Bar++) {
            Scp465Music.Section Stage = Scp465Music.section(Scp465Music.Steps + Bar * Scp465Music.StepsPerBar);
            Scp465Music.Section Before = Scp465Music.section(Scp465Music.Steps + Bar * Scp465Music.StepsPerBar - 1);
            if (Stage != Before) {
                Helper.assertTrue(Bar % 4 == 0, "A section changes in the middle of a phrase at bar " + Bar);
            }
        }
        Helper.assertTrue(Scp465Lid.OpenAngle >= 100.0f && Scp465Lid.OpenAngle <= 120.0f, "The lid rests at " + Scp465Lid.OpenAngle);
        Helper.assertTrue(Math.abs(Scp465Lid.angle(true, 0.0f, 0.0f)) < 0.001f, "The lid does not start shut");
        Helper.assertTrue(Math.abs(Scp465Lid.angle(true, 0.0f, Scp465Lid.OpenTicks) - Scp465Lid.OpenAngle) < 0.01f, "The lid does not settle open");
        Helper.assertTrue(Math.abs(Scp465Lid.angle(false, Scp465Lid.OpenAngle, Scp465Lid.CloseTicks)) < 0.01f, "The lid does not end shut");
        float Previous = 0.0f;
        float Jump = 0.0f;
        float Lowest = 0.0f;
        for (float Tick = 0.0f; Tick <= Scp465Lid.OpenTicks; Tick += 0.25f) {
            float Now = Scp465Lid.angle(true, 0.0f, Tick);
            Jump = Math.max(Jump, Math.abs(Now - Previous));
            Previous = Now;
        }
        for (float Tick = 0.0f; Tick <= Scp465Lid.CloseTicks; Tick += 0.25f) {
            Lowest = Math.min(Lowest, Scp465Lid.angle(false, Scp465Lid.OpenAngle, Tick));
        }
        Helper.assertTrue(Jump < 12.0f, "The lid jumps " + Jump + " degrees in a quarter tick");
        Helper.assertTrue(Lowest > -0.001f, "The closing lid dips into the box");
        Language Words = Language.getInstance();
        for (String Name : Scp465Names.Guests) {
            Helper.assertTrue(Words.has(Scp465Names.key(Name)), "No text for the guest name " + Name);
        }
        Helper.assertTrue(Scp465Names.Guests.size() >= 40 && new HashSet<>(Scp465Names.Guests).size() == Scp465Names.Guests.size(), "The guest name pool is too small or repeats");
        Helper.assertTrue(Scp465Names.Guests.size() >= Scp465Celebration.guestCount(Scp465Celebration.MaxPeople), "There are fewer names than guests in the biggest party");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp465_lidHingeLine")
    public static void lidHingeLine(GameTestHelper Helper) {
        Map<Direction, Integer> StateTurns = Map.of(Direction.NORTH, 0, Direction.EAST, 90, Direction.SOUTH, 180, Direction.WEST, 270);
        float BoxTop = 11.0f / 16.0f;
        float BoxBack = 14.0f / 16.0f;
        float[][] LidCorners = new float[8][];
        int Corner = 0;
        for (float X : new float[]{1.0f, 15.0f}) {
            for (float Y : new float[]{11.0f, 14.0f}) {
                for (float Z : new float[]{1.0f, 14.0f}) {
                    LidCorners[Corner++] = new float[]{X / 16.0f, Y / 16.0f, Z / 16.0f};
                }
            }
        }
        for (Direction Facing : Direction.Plane.HORIZONTAL) {
            Matrix4f Base = new Matrix4f().translate(0.5f, 0.5f, 0.5f).rotateY((float) Math.toRadians(-StateTurns.get(Facing))).translate(-0.5f, -0.5f, -0.5f);
            Matrix4f Back = new Matrix4f(Base).invert();
            Vector3f Front = Base.transformPosition(new Vector3f(0.5f, 0.5f, 0.0f));
            Helper.assertTrue(Front.distance(0.5f + 0.5f * Facing.getStepX(), 0.5f, 0.5f + 0.5f * Facing.getStepZ()) < 1.0e-5f, "The box model does not face " + Facing);
            for (float Angle = 0.0f; Angle <= Scp465Lid.OpenAngle + Scp465Lid.Overshoot + 0.01f; Angle += 0.5f) {
                Matrix4f Lid = Scp465Lid.transform(Facing, Angle);
                for (int Pixel = 1; Pixel <= 15; Pixel++) {
                    Vector3f Hinge = Lid.transformPosition(new Vector3f(Pixel / 16.0f, Scp465Lid.HingeY, Scp465Lid.HingeZ));
                    Vector3f Edge = Base.transformPosition(new Vector3f(Pixel / 16.0f, BoxTop, BoxBack));
                    Helper.assertTrue(Hinge.distance(Edge) < 1.0e-5f, "The lid hinge leaves the box's back top edge facing " + Facing + " at " + Angle + " degrees by " + Hinge.distance(Edge));
                }
                for (float[] Point : LidCorners) {
                    Vector3f Local = Back.transformPosition(Lid.transformPosition(new Vector3f(Point[0], Point[1], Point[2])));
                    float Inside = Math.min(Math.min(Math.min(Local.x - 2.0f / 16.0f, 14.0f / 16.0f - Local.x), Math.min(Local.y, BoxTop - Local.y)),
                            Math.min(Local.z - 2.0f / 16.0f, BoxBack - Local.z));
                    Helper.assertTrue(Inside < 1.0e-4f, "The lid corner dips into the box facing " + Facing + " at " + Angle + " degrees");
                }
            }
            Vector3f Shut = Scp465Lid.transform(Facing, 0.0f).transformPosition(new Vector3f(8.0f / 16.0f, BoxTop, 1.0f / 16.0f));
            Helper.assertTrue(Shut.distance(Base.transformPosition(new Vector3f(8.0f / 16.0f, BoxTop, 1.0f / 16.0f))) < 1.0e-5f, "The shut lid is not seated on the box facing " + Facing);
        }
        Helper.succeed();
    }

    private static double playheadRun(Scp465Playhead Head, double FrameTicks, int Frames, double Start, double Offset, List<double[]> Log) {
        double Clock = Start;
        for (int Frame = 0; Frame < Frames; Frame++) {
            Clock += FrameTicks;
            double Before = Head.position();
            int Step = Head.advance(FrameTicks, Clock + Offset);
            Log.add(new double[]{Clock, Head.position() - Before, Step});
        }
        return Clock;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp465_playheadTiming")
    public static void playheadTiming(GameTestHelper Helper) {
        double Frame = 20.0 / 60.0;
        double Step = Scp465Music.StepTicks;
        Scp465Playhead Head = new Scp465Playhead();
        List<double[]> Log = new ArrayList<>();
        Helper.assertTrue(Head.advance(0.0, 0.0) == 0, "The first step did not play at the start");
        double Clock = playheadRun(Head, Frame, 60 * 30, 0.0, 0.0, Log);
        int Last = 0;
        double LastTime = 0.0;
        for (double[] Entry : Log) {
            if (Entry[2] == Scp465Playhead.Silent) {
                continue;
            }
            Helper.assertTrue((int) Entry[2] == Last + 1, "A steady clock skipped or repeated a step after " + Last);
            Helper.assertTrue(Math.abs(Entry[0] - LastTime - Step) < Frame + 1.0e-6, "A steady clock played step " + (int) Entry[2] + " off the grid");
            Last = (int) Entry[2];
            LastTime = Entry[0];
        }
        Helper.assertTrue(Last >= (int) (60 * 30 * Frame / Step) - 1, "The playhead only reached step " + Last + " in thirty seconds");
        int Before = Head.played();
        Clock += 30.0;
        int Hitch = Head.advance(30.0, Clock);
        Helper.assertTrue(Hitch == Scp465Playhead.Silent || Hitch == Head.played(), "A frame hitch played more than the latest step");
        Helper.assertTrue(Head.played() - Before > 5, "A frame hitch did not skip the missed steps");
        List<Double> Times = new ArrayList<>();
        if (Hitch != Scp465Playhead.Silent) {
            Times.add(Clock);
        }
        Log.clear();
        Clock = playheadRun(Head, Frame, 60 * 5, Clock, 0.0, Log);
        for (double[] Entry : Log) {
            if (Entry[2] != Scp465Playhead.Silent) {
                Times.add(Entry[0]);
            }
        }
        for (int Index = 1; Index < Times.size(); Index++) {
            Helper.assertTrue(Times.get(Index) - Times.get(Index - 1) > Step * 0.3, "Steps burst out " + (Times.get(Index) - Times.get(Index - 1)) + " ticks apart after a hitch");
        }
        double Offset = 0.0;
        for (double Shift : new double[]{17.0, -13.0, 40.0}) {
            Offset += Shift;
            Log.clear();
            Last = Head.played();
            Clock = playheadRun(Head, Frame, 60 * 40, Clock, Offset, Log);
            for (double[] Entry : Log) {
                Helper.assertTrue(Entry[1] >= Frame * (1.0 - Scp465Playhead.MaxSlew) - 1.0e-9 && Entry[1] <= Frame * (1.0 + Scp465Playhead.MaxSlew) + 1.0e-9,
                        "A start time shift of " + Shift + " ticks moved the playhead by " + Entry[1] + " in one frame");
                if (Entry[2] != Scp465Playhead.Silent) {
                    Helper.assertTrue((int) Entry[2] == Last + 1, "A start time shift of " + Shift + " ticks skipped or repeated a step");
                    Last = (int) Entry[2];
                }
            }
            double Error = Math.abs(Scp465Playhead.phaseError(Clock + Offset, Head.position()));
            Helper.assertTrue(Error < 0.5, "The playhead is still " + Error + " ticks off the beat 40 seconds after a start time shift of " + Shift);
        }
        Last = Head.played();
        RandomSource Noise = RandomSource.create(465L);
        double Jitter = 0.0;
        for (int Index = 0; Index < 60 * 30; Index++) {
            Clock += Frame;
            if (Index % 60 == 0) {
                Jitter = (Noise.nextDouble() - 0.5) * 4.0;
            }
            double Previous = Head.position();
            int Played = Head.advance(Frame, Clock + Offset + Jitter);
            Helper.assertTrue(Head.position() >= Previous && Head.position() - Previous <= Frame * (1.0 + Scp465Playhead.MaxSlew) + 1.0e-9,
                    "A jittery server clock moved the playhead unevenly");
            if (Played != Scp465Playhead.Silent) {
                Helper.assertTrue(Played == Last + 1, "A jittery server clock skipped or repeated a step");
                Last = Played;
            }
        }
        double Held = Head.position();
        int Paused = Head.played();
        for (int Index = 0; Index < 600; Index++) {
            Helper.assertTrue(Head.advance(0.0, Clock + Offset + Jitter) == Scp465Playhead.Silent, "The music played while paused");
        }
        Helper.assertTrue(Head.position() == Held && Head.played() == Paused, "Pausing moved the playhead");
        int Resumed = Scp465Playhead.Silent;
        for (int Index = 0; Index < 60 && Resumed == Scp465Playhead.Silent; Index++) {
            Clock += Frame;
            Resumed = Head.advance(Frame, Clock + Offset + Jitter);
        }
        Helper.assertTrue(Resumed == Paused + 1, "The music did not resume in place after a pause");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp465_largeRoomCrowd")
    public static void largeRoomCrowd(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Room Built = room(Helper, new BlockPos(3, 1, 3), 24, 4, 24, true);
        BlockPos Box = placeBox(Helper, Built.centre());
        Scp465TestPlayer Host = playerAt(Helper, Built.centre().offset(1, 0, 1), "scp465_packed");
        try {
            use(Host, Box);
            Scp465Party Party = partyAt(Helper, Box);
            Helper.assertTrue(Party != null, "The largest room did not open");
            List<Scp465Guest> Crowd = Scp465Celebration.guests(World, Party);
            Helper.assertTrue(Crowd.size() == Scp465Celebration.MaxPeople, "The largest room holds " + Crowd.size() + " people, not " + Scp465Celebration.MaxPeople);
            long Clowns = Crowd.stream().filter(Scp465Guest::isClown).count();
            long Djs = Crowd.stream().filter(Scp465Guest::isDj).count();
            Helper.assertTrue(Djs == 1 && Clowns >= 1 && Clowns <= Scp465Celebration.MaxClowns, "The largest room has " + Djs + " DJs and " + Clowns + " clowns");
            checkCrowd(Helper, Party, "the largest room");
            double Spread = 0.0;
            for (Scp465Guest Guest : Crowd) {
                double Nearest = Double.MAX_VALUE;
                for (Scp465Guest Other : Crowd) {
                    if (Other != Guest) {
                        Nearest = Math.min(Nearest, Guest.position().distanceTo(Other.position()));
                    }
                }
                Spread += Nearest;
            }
            Helper.assertTrue(Spread / Crowd.size() >= 2.0, "The crowd spawned bunched up: " + Spread / Crowd.size() + " blocks to the nearest neighbour");
        } finally {
            cleanUp(Helper);
            dismiss(Helper, Host);
        }
        Helper.succeed();
    }
}
