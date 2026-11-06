package net.rai.scp_unleashed.archive.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.scp179.SatelliteTerminalBlockEntity;
import net.rai.scp_unleashed.scp179.Scp179Registry;
import net.rai.scp_unleashed.scp179.SpaceCoordinatorItem;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.speaker.SpeakerRegistry;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteItem;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class UniqueGiftGameTests {
    public static final String Arena = "scp407_arena";
    public static final BlockPos TerminalSpot = new BlockPos(4, 2, 4);
    public static final BlockPos SecondTerminalSpot = new BlockPos(6, 2, 4);
    public static final BlockPos ConsoleSpot = new BlockPos(4, 2, 4);
    public static final BlockPos ComputerSpot = new BlockPos(8, 2, 4);
    public static final ResourceLocation PlainSound = ResourceLocation.withDefaultNamespace("block.note_block.harp");

    private UniqueGiftGameTests() {
    }

    private static FakePlayer reader(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(4.5, 2, 6.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void rules(GameTestHelper Helper, boolean Repeat, boolean Personal) {
        MinecraftServer Server = Helper.getLevel().getServer();
        Server.getGameRules().getRule(ArchiveRules.RepeatFiles).set(Repeat, Server);
        Server.getGameRules().getRule(ArchiveRules.PerPlayer).set(Personal, Server);
    }

    private static void use(GameTestHelper Helper, FakePlayer User, ItemStack Held, BlockPos Relative) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Relative);
        User.setItemInHand(InteractionHand.MAIN_HAND, Held);
        BlockState State = World.getBlockState(Pos);
        State.useItemOn(User.getItemInHand(InteractionHand.MAIN_HAND), World, User, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(Pos), Direction.SOUTH, Pos, false));
    }

    private static SatelliteTerminalBlockEntity terminal(GameTestHelper Helper, BlockPos Relative) {
        Helper.setBlock(Relative, Scp179Registry.Terminal.get().defaultBlockState());
        SatelliteTerminalBlockEntity Terminal = Helper.getBlockEntity(Relative);
        if (Terminal == null) {
            Helper.fail("the satellite terminal did not place", Relative);
        }
        return Terminal;
    }

    private static int coordinatorRun(GameTestHelper Helper, boolean Repeat, boolean Personal, String Name) {
        rules(Helper, Repeat, Personal);
        try {
            SatelliteTerminalBlockEntity First = terminal(Helper, TerminalSpot);
            SatelliteTerminalBlockEntity Second = terminal(Helper, SecondTerminalSpot);
            FakePlayer User = reader(Helper, Name);
            ItemStack Coordinator = SpaceCoordinatorItem.create(Scp179Registry.TargetId);
            use(Helper, User, Coordinator, TerminalSpot);
            Helper.assertTrue(First.hasLocation(Scp179Registry.TargetId), "the first terminal did not store the coordinator's location");
            ItemStack Left = User.getItemInHand(InteractionHand.MAIN_HAND);
            if (Left.isEmpty()) {
                return 0;
            }
            Helper.assertTrue(Left.is(Scp179Registry.Coordinator.get()) && Scp179Registry.TargetId.equals(SpaceCoordinatorItem.target(Left)),
                    "the kept coordinator lost its coordinates");
            use(Helper, User, Left, TerminalSpot);
            Helper.assertTrue(First.getLocations().size() == 1, "using the coordinator twice on one terminal stored the location twice");
            Helper.assertTrue(User.getItemInHand(InteractionHand.MAIN_HAND).is(Scp179Registry.Coordinator.get()), "a repeat use on the same terminal used the coordinator up");
            use(Helper, User, User.getItemInHand(InteractionHand.MAIN_HAND), SecondTerminalSpot);
            Helper.assertTrue(Second.hasLocation(Scp179Registry.TargetId), "the kept coordinator could not add its location to a second terminal");
            return User.getItemInHand(InteractionHand.MAIN_HAND).getCount();
        } finally {
            rules(Helper, false, false);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "unique_coordinatorKeptByDefault")
    public static void coordinatorKeptByDefault(GameTestHelper Helper) {
        Helper.assertTrue(coordinatorRun(Helper, false, false, "unique_coord_world") == 1, "the Space Coordinator was used up under the default archive rules");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "unique_coordinatorKeptPerPlayer")
    public static void coordinatorKeptPerPlayer(GameTestHelper Helper) {
        Helper.assertTrue(coordinatorRun(Helper, false, true, "unique_coord_personal") == 1, "the Space Coordinator was used up with per player files");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "unique_coordinatorSpentWhenUnlimited")
    public static void coordinatorSpentWhenUnlimited(GameTestHelper Helper) {
        Helper.assertTrue(coordinatorRun(Helper, true, false, "unique_coord_unlimited") == 0, "the Space Coordinator was kept although files can be printed again");
        Helper.succeed();
    }

    private static boolean cassetteRun(GameTestHelper Helper, boolean Repeat, boolean Personal, String Name) {
        rules(Helper, Repeat, Personal);
        try {
            Helper.setBlock(ConsoleSpot, SpeakerRegistry.SoundConsole.get().defaultBlockState());
            Helper.setBlock(ComputerSpot, ComputerRegistry.Computer.get().defaultBlockState());
            FakePlayer First = reader(Helper, Name + "_a");
            FakePlayer Second = reader(Helper, Name + "_b");
            use(Helper, First, new ItemStack(Scp407Registry.Cassette.get()), ConsoleSpot);
            Helper.assertTrue(AnomalousLibrary.find(First, Scp407Registry.SongId).isPresent(), "the SCP-407 track was not added to the library");
            ItemStack Left = First.getItemInHand(InteractionHand.MAIN_HAND);
            if (Left.isEmpty()) {
                return false;
            }
            Helper.assertTrue(Left.is(Scp407Registry.Cassette.get()), "the kept cassette changed into something else");
            int Size = AnomalousLibrary.of(First).Recordings().size();
            use(Helper, First, Left, ConsoleSpot);
            use(Helper, First, First.getItemInHand(InteractionHand.MAIN_HAND), ComputerSpot);
            Helper.assertTrue(AnomalousLibrary.of(First).Recordings().size() == Size, "inserting the cassette again into the same library added another track");
            Helper.assertTrue(First.getItemInHand(InteractionHand.MAIN_HAND).is(Scp407Registry.Cassette.get()), "a repeat insert used the cassette up");
            ItemStack Passed = First.getItemInHand(InteractionHand.MAIN_HAND).copy();
            First.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            use(Helper, Second, Passed, ComputerSpot);
            Helper.assertTrue(AnomalousLibrary.find(Second, Scp407Registry.SongId).isPresent(), "a second player could not add the kept cassette to their own library");
            Helper.assertTrue(Second.getItemInHand(InteractionHand.MAIN_HAND).is(Scp407Registry.Cassette.get()), "the second player's insert used the cassette up");
            use(Helper, First, CassetteItem.create(PlainSound, Component.literal("Harp"), null), ConsoleSpot);
            Helper.assertTrue(AnomalousLibrary.find(First, PlainSound).isPresent(), "an ordinary cassette was not added to the library");
            Helper.assertTrue(First.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "an ordinary cassette was kept although it is not one of a kind");
            return true;
        } finally {
            rules(Helper, false, false);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "unique_cassetteKeptByDefault")
    public static void cassetteKeptByDefault(GameTestHelper Helper) {
        Helper.assertTrue(cassetteRun(Helper, false, false, "unique_tape_world"), "the SCP-407 cassette was used up under the default archive rules");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "unique_cassetteKeptPerPlayer")
    public static void cassetteKeptPerPlayer(GameTestHelper Helper) {
        Helper.assertTrue(cassetteRun(Helper, false, true, "unique_tape_personal"), "the SCP-407 cassette was used up with per player files");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "unique_cassetteSpentWhenUnlimited")
    public static void cassetteSpentWhenUnlimited(GameTestHelper Helper) {
        Helper.assertTrue(!cassetteRun(Helper, true, false, "unique_tape_unlimited"), "the SCP-407 cassette was kept although files can be printed again");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "unique_ruleFollowsModes")
    public static void ruleFollowsModes(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        try {
            rules(Helper, false, false);
            Helper.assertTrue(ArchiveRules.uniqueItemsReusable(Server), "World mode does not count gifts as one of a kind");
            rules(Helper, false, true);
            Helper.assertTrue(ArchiveRules.uniqueItemsReusable(Server), "Per player mode does not count gifts as one of a kind");
            rules(Helper, true, false);
            Helper.assertTrue(!ArchiveRules.uniqueItemsReusable(Server), "Unlimited mode counts gifts as one of a kind");
            rules(Helper, true, true);
            Helper.assertTrue(!ArchiveRules.uniqueItemsReusable(Server), "Unlimited mode with per player files counts gifts as one of a kind");
        } finally {
            rules(Helper, false, false);
        }
        Helper.succeed();
    }
}
