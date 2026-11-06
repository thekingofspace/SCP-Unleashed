package net.rai.scp_unleashed.scp020.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.CameraBlock;
import net.rai.scp_unleashed.cameras.CameraLinks;
import net.rai.scp_unleashed.cameras.CameraRegistry;
import net.rai.scp_unleashed.cameras.CameraSessions;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.labware.DisinfectingWipeItem;
import net.rai.scp_unleashed.scp020.Scp020Growth;
import net.rai.scp_unleashed.scp020.Scp020Infection;
import net.rai.scp_unleashed.scp020.Scp020Registry;
import net.rai.scp_unleashed.scp020.Scp020SporeMemory;
import net.rai.scp_unleashed.scp020.Scp020Spores;
import net.rai.scp_unleashed.scp020.network.Scp020SporesPayload;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp020SporeGameTests {
    public static final String Arena = Scp020GameTests.Arena;
    public static final BlockPos Wall = new BlockPos(5, 2, 9);
    public static final BlockPos SecondWall = new BlockPos(9, 2, 9);
    public static final BlockPos CameraSpot = new BlockPos(7, 3, 4);

    private Scp020SporeGameTests() {
    }

    private record Tapped(ServerPlayer User, EmbeddedChannel Wire, List<Scp020SporesPayload> Heard) {
        private List<Scp020SporesPayload> pull() {
            Wire.flushOutbound();
            Object Each;
            while ((Each = Wire.readOutbound()) != null) {
                add(Each);
            }
            return Heard;
        }

        private void add(Object Each) {
            if (Each instanceof ClientboundBundlePacket Bundle) {
                for (Packet<?> Inner : Bundle.subPackets()) {
                    add(Inner);
                }
            } else if (Each instanceof ClientboundCustomPayloadPacket Custom && Custom.payload() instanceof Scp020SporesPayload Spores) {
                Heard.add(Spores);
            }
        }

        private List<Scp020SporesPayload> fresh() {
            int Before = Heard.size();
            pull();
            return List.copyOf(Heard.subList(Before, Heard.size()));
        }
    }

    private static Tapped player(GameTestHelper Helper, String Name, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        CommonListenerCookie Cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), Name), false);
        ServerPlayer User = new ServerPlayer(World.getServer(), World, Cookie.gameProfile(), Cookie.clientInformation());
        Connection Link = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel Wire = new EmbeddedChannel(Link);
        openChannels(Link);
        World.getServer().getPlayerList().placeNewPlayer(Link, User, Cookie);
        User.setGameMode(GameType.SURVIVAL);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Tapped Made = new Tapped(User, Wire, new ArrayList<>());
        Made.pull();
        Made.Heard().clear();
        return Made;
    }

    @SuppressWarnings("unchecked")
    private static void openChannels(Connection Link) {
        try {
            Field Registrations = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
            Registrations.setAccessible(true);
            Map<ConnectionProtocol, Map<ResourceLocation, ?>> All = (Map<ConnectionProtocol, Map<ResourceLocation, ?>>) Registrations.get(null);
            ChannelAttributes.getOrCreateCommonChannels(Link, ConnectionProtocol.PLAY).addAll(All.getOrDefault(ConnectionProtocol.PLAY, Map.of()).keySet());
        } catch (ReflectiveOperationException Error) {
            throw new IllegalStateException(Error);
        }
    }

    private static void leave(Tapped Made) {
        ServerPlayer User = Made.User();
        CameraSessions.end(User, CameraSessions.End.Quiet);
        Scp020Spores.forget(User.getUUID());
        if (User.server.getPlayerList().getPlayer(User.getUUID()) != null) {
            User.server.getPlayerList().remove(User);
        }
    }

    private static void wall(GameTestHelper Helper, BlockPos Host) {
        Helper.setBlock(Host, Blocks.STONE);
        Helper.assertTrue(Scp020Infection.infect(Helper.getLevel(), Helper.absolutePos(Host), Direction.NORTH), "the wall at " + Host + " could not be infected");
    }

    private static void cleanup(GameTestHelper Helper) {
        Scp020Infection.clearIn(Helper.getLevel(), Helper.absolutePos(new BlockPos(8, 4, 8)), 8, (Host, Side) -> {
        });
    }

    private static Scp020SporeMemory.Face face(int X, int Y, int Z, Direction Side) {
        return new Scp020SporeMemory.Face(new BlockPos(X, Y, Z), Side);
    }

    private static Scp020SporesPayload layout(List<Scp020SporeMemory.Face> Faces) {
        LinkedHashMap<Long, Integer> Masks = new LinkedHashMap<>();
        for (Scp020SporeMemory.Face Patch : Faces) {
            Masks.merge(Patch.Host().asLong(), Scp020Growth.bit(Patch.Side()), (First, Second) -> First | Second);
        }
        long[] Hosts = new long[Masks.size()];
        byte[] Bits = new byte[Masks.size()];
        int Index = 0;
        for (Map.Entry<Long, Integer> Entry : Masks.entrySet()) {
            Hosts[Index] = Entry.getKey();
            Bits[Index] = (byte) (int) Entry.getValue();
            Index++;
        }
        return new Scp020SporesPayload(true, false, Hosts, Bits);
    }

    private static void update(GameTestHelper Helper, Tapped Made, boolean Force) {
        Scp020Spores.update(Made.User(), Helper.getLevel().getServer().getTickCount(), Force);
    }

    private static boolean holds(GameTestHelper Helper, Scp020SporesPayload Payload, BlockPos Host) {
        Scp020SporeMemory Memory = new Scp020SporeMemory();
        Memory.apply(Payload);
        return Memory.holds(Helper.absolutePos(Host), Direction.NORTH);
    }

    private static BlockPos mountCamera(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Camera = Helper.absolutePos(CameraSpot);
        World.setBlock(Camera.relative(Direction.NORTH), Blocks.STONE.defaultBlockState(), 3);
        World.setBlock(Camera, CameraRegistry.Camera.get().defaultBlockState().setValue(CameraBlock.Face, AttachFace.WALL).setValue(CameraBlock.Facing, Direction.SOUTH), 3);
        return Camera;
    }

    private static void openFeed(GameTestHelper Helper, ServerPlayer User, BlockPos Camera) {
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CameraRegistry.Tablet.get()));
        ItemStack Tablet = User.getItemInHand(InteractionHand.MAIN_HAND);
        CameraSessions.link(User, Tablet, Helper.getLevel(), Camera);
        int Index = CameraLinks.of(Tablet).indexOf(GlobalPos.of(Helper.getLevel().dimension(), Camera));
        Helper.assertTrue(Index >= 0, "the camera was not linked");
        CameraSessions.Outcome Result = CameraSessions.open(User, InteractionHand.MAIN_HAND, Index);
        Helper.assertTrue(Result == CameraSessions.Outcome.Opened && CameraViews.isViewing(User), "the camera feed did not open: " + Result);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp020vis_onlyViewers")
    public static void sporesReachOnlyViewers(GameTestHelper Helper) {
        wall(Helper, Wall);
        BlockPos Camera = mountCamera(Helper);
        Tapped Plain = player(Helper, "scp020_plain", 8.5, 1.0, 6.5);
        Tapped Goggled = player(Helper, "scp020_goggled", 4.5, 1.0, 6.5);
        Tapped Watcher = player(Helper, "scp020_watcher", 13.5, 1.0, 1.5);
        try {
            Goggled.User().setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp020Registry.Goggles.get()));
            openFeed(Helper, Watcher.User(), Camera);
            for (Tapped Each : List.of(Plain, Goggled, Watcher)) {
                update(Helper, Each, true);
            }
            Helper.assertTrue(Plain.fresh().isEmpty() && !Scp020Spores.isSyncing(Plain.User()), "a player with no camera and no goggles was told where the mold is");
            List<Scp020SporesPayload> Worn = Goggled.fresh();
            Helper.assertTrue(Worn.size() == 1 && Worn.get(0).Active() && !Worn.get(0).Camera() && holds(Helper, Worn.get(0), Wall), "the goggles wearer did not get the mold beside them: " + Worn.size());
            List<Scp020SporesPayload> Feed = Watcher.fresh();
            Helper.assertTrue(Feed.size() == 1 && Feed.get(0).Active() && Feed.get(0).Camera() && holds(Helper, Feed.get(0), Wall), "the camera viewer did not get the mold in view: " + Feed.size());
            Helper.assertTrue(Scp020Spores.lens(Watcher.User()).distanceTo(Vec3.atCenterOf(Camera)) < 1.0, "the camera viewer's spores are not centred on the camera");
            for (int Round = 0; Round < Scp020Spores.SyncTicks + 2; Round++) {
                for (Tapped Each : List.of(Plain, Goggled, Watcher)) {
                    update(Helper, Each, false);
                }
            }
            Helper.assertTrue(Goggled.fresh().isEmpty() && Watcher.fresh().isEmpty(), "an unchanged colony was sent again");
            Scp020SporesPayload Far = Scp020Spores.snapshot(Helper.getLevel(), Helper.absoluteVec(new Vec3(5.5, 2.5, 1.0)), 3.0, false);
            Helper.assertTrue(Far.size() == 0, "a patch out of range was included");
        } finally {
            for (Tapped Each : List.of(Plain, Goggled, Watcher)) {
                leave(Each);
            }
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp020vis_goggles")
    public static void gogglesToggleSight(GameTestHelper Helper) {
        wall(Helper, Wall);
        Tapped Wearer = player(Helper, "scp020_wearer", 5.5, 1.0, 6.5);
        Scp020SporeMemory Memory = new Scp020SporeMemory();
        try {
            ServerPlayer User = Wearer.User();
            update(Helper, Wearer, true);
            Helper.assertTrue(!Scp020Spores.canSee(User) && Wearer.fresh().isEmpty(), "spores were sent before the goggles went on");
            for (int Round = 0; Round < 2; Round++) {
                User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp020Registry.Goggles.get()));
                Helper.assertTrue(Scp020Spores.canSee(User) && Scp020Spores.goggled(User), "wearing the goggles did not turn the spores on");
                update(Helper, Wearer, false);
                List<Scp020SporesPayload> On = Wearer.fresh();
                Helper.assertTrue(On.size() == 1 && On.get(0).Active(), "putting the goggles on sent " + On.size() + " updates");
                Memory.apply(On.get(0));
                Helper.assertTrue(Memory.shows(false, true) && Memory.holds(Helper.absolutePos(Wall), Direction.NORTH), "the wearer cannot see the patch");
                Helper.assertTrue(!Memory.shows(false, false), "the patch shows without the goggles on");
                Helper.assertTrue(!Memory.shows(true, false), "a goggles view leaks onto a camera feed");
                User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                Helper.assertTrue(!Scp020Spores.canSee(User), "taking the goggles off left the spores on");
                update(Helper, Wearer, false);
                List<Scp020SporesPayload> Off = Wearer.fresh();
                Helper.assertTrue(Off.size() == 1 && !Off.get(0).Active(), "taking the goggles off did not clear the spores");
                Memory.apply(Off.get(0));
                Helper.assertTrue(!Memory.shows(false, true) && Memory.faces().isEmpty() && !Scp020Spores.isSyncing(User), "the spores stayed after the goggles came off");
            }
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp020Registry.Goggles.get()));
            update(Helper, Wearer, false);
            Memory.apply(Wearer.fresh().get(0));
            wall(Helper, SecondWall);
            update(Helper, Wearer, false);
            List<Scp020SporesPayload> Grown = Wearer.fresh();
            Helper.assertTrue(Grown.size() == 1 && holds(Helper, Grown.get(0), SecondWall) && holds(Helper, Grown.get(0), Wall), "a new patch was not sent to the wearer");
            Scp020Infection.clear(Helper.getLevel(), Helper.absolutePos(SecondWall));
            update(Helper, Wearer, false);
            List<Scp020SporesPayload> Cleaned = Wearer.fresh();
            Helper.assertTrue(Cleaned.size() == 1 && !holds(Helper, Cleaned.get(0), SecondWall) && holds(Helper, Cleaned.get(0), Wall), "a removed patch was not taken back from the wearer");
        } finally {
            leave(Wearer);
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp020vis_feed")
    public static void leavingFeedClearsSpores(GameTestHelper Helper) {
        wall(Helper, Wall);
        BlockPos Camera = mountCamera(Helper);
        Tapped Watcher = player(Helper, "scp020_feed", 13.5, 1.0, 1.5);
        Scp020SporeMemory Memory = new Scp020SporeMemory();
        try {
            ServerPlayer User = Watcher.User();
            update(Helper, Watcher, true);
            Helper.assertTrue(Watcher.fresh().isEmpty(), "spores were sent before the feed opened");
            openFeed(Helper, User, Camera);
            update(Helper, Watcher, false);
            List<Scp020SporesPayload> Opened = Watcher.fresh();
            Helper.assertTrue(Opened.size() == 1 && Opened.get(0).Active() && Opened.get(0).Camera(), "opening the feed did not send the spores");
            Memory.apply(Opened.get(0));
            Helper.assertTrue(Memory.shows(true, false) && !Memory.shows(false, false) && !Memory.shows(false, true), "the feed spores show outside the feed");
            CameraSessions.end(User, CameraSessions.End.Left);
            Helper.assertTrue(!Scp020Spores.canSee(User), "the spores stayed on after leaving the feed");
            update(Helper, Watcher, false);
            List<Scp020SporesPayload> Closed = Watcher.fresh();
            Helper.assertTrue(Closed.size() == 1 && !Closed.get(0).Active(), "leaving the feed did not clear the spores");
            Memory.apply(Closed.get(0));
            Helper.assertTrue(!Memory.shows(true, false) && Memory.faces().isEmpty() && !Scp020Spores.isSyncing(User), "the spores were kept after leaving the feed");
            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp020Registry.Goggles.get()));
            openFeed(Helper, User, Camera);
            update(Helper, Watcher, false);
            Memory.apply(Watcher.fresh().get(0));
            Helper.assertTrue(Memory.forCamera(), "a goggles wearer on a feed got the body view instead of the camera view");
            CameraSessions.end(User, CameraSessions.End.Left);
            update(Helper, Watcher, false);
            List<Scp020SporesPayload> Back = Watcher.fresh();
            Helper.assertTrue(Back.size() == 1 && Back.get(0).Active() && !Back.get(0).Camera(), "leaving the feed with goggles on did not switch back to the body view");
        } finally {
            leave(Watcher);
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp020vis_othersWipe")
    public static void othersRemovalReachesViewers(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (int X = 2; X <= 10; X++) {
            wall(Helper, new BlockPos(X, 2, 9));
        }
        BlockPos Camera = mountCamera(Helper);
        Tapped Cleaner = player(Helper, "scp020_cleaner", 6.5, 1.0, 6.0);
        Tapped Wearer = player(Helper, "scp020_onlooker", 6.5, 1.0, 6.5);
        Tapped Watcher = player(Helper, "scp020_monitor", 13.5, 1.0, 1.5);
        Scp020SporeMemory Seen = new Scp020SporeMemory();
        Scp020SporeMemory Feed = new Scp020SporeMemory();
        try {
            Wearer.User().setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp020Registry.Goggles.get()));
            openFeed(Helper, Watcher.User(), Camera);
            Scp020Spores.tick(World.getServer());
            Seen.apply(Wearer.fresh().get(0));
            Feed.apply(Watcher.fresh().get(0));
            BlockPos Wiped = Helper.absolutePos(new BlockPos(6, 2, 9));
            BlockPos Border = Helper.absolutePos(new BlockPos(9, 2, 9));
            Helper.assertTrue(Seen.faces().size() == 9 && Feed.faces().size() == 9, "the viewers did not get the whole patch: " + Seen.faces().size() + " and " + Feed.faces().size());
            Helper.assertTrue((Seen.connections(new Scp020SporeMemory.Face(Border, Direction.NORTH)) & Scp020SporeMemory.Right) != 0, "the patch beside the wiped area was not joined to it");
            int Killed = DisinfectingWipeItem.wipe(World, Cleaner.User(), new BlockHitResult(Scp020Infection.faceCentre(Wiped, Direction.NORTH), Direction.NORTH, Wiped, false));
            Helper.assertTrue(Killed == 5, "the wipe killed " + Killed + " faces instead of 5");
            Scp020Spores.tick(World.getServer());
            List<Scp020SporesPayload> ToWearer = Wearer.fresh();
            List<Scp020SporesPayload> ToWatcher = Watcher.fresh();
            Helper.assertTrue(ToWearer.size() == 1 && ToWatcher.size() == 1, "the wipe reached the viewers as " + ToWearer.size() + " and " + ToWatcher.size() + " updates");
            Seen.apply(ToWearer.get(0));
            Feed.apply(ToWatcher.get(0));
            for (Scp020SporeMemory View : List.of(Seen, Feed)) {
                Helper.assertTrue(View.faces().size() == 4 && !View.holds(Wiped, Direction.NORTH), "a viewer still sees the wiped patch: " + View.faces().size() + " faces");
                Helper.assertTrue(View.holds(Border, Direction.NORTH), "a viewer lost a patch the wipe did not reach");
                Helper.assertTrue((View.connections(new Scp020SporeMemory.Face(Border, Direction.NORTH)) & Scp020SporeMemory.Right) == 0, "the patch beside the wiped area still joins the gap");
            }
            Helper.assertTrue(Cleaner.fresh().isEmpty(), "the player who wiped, with no goggles and no camera, was told where the mold is");
            BlockPos Broken = Helper.absolutePos(new BlockPos(2, 2, 9));
            Cleaner.User().gameMode.destroyBlock(Broken);
            Scp020Spores.tick(World.getServer());
            Seen.apply(Wearer.fresh().get(0));
            Helper.assertTrue(!Seen.holds(Broken, Direction.NORTH) && Seen.faces().size() == 3, "breaking an infected block did not take its patch away from the viewer");
        } finally {
            for (Tapped Each : List.of(Cleaner, Wearer, Watcher)) {
                leave(Each);
            }
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020vis_connections")
    public static void connectionsFollowNeighbours(GameTestHelper Helper) {
        Scp020SporeMemory.Face Corner = face(0, 1, 0, Direction.SOUTH);
        Scp020SporeMemory.Face Beside = face(1, 1, 0, Direction.SOUTH);
        Scp020SporeMemory.Face Floor = face(0, 0, 1, Direction.UP);
        Scp020SporeMemory.Face PillarFront = face(5, 1, 5, Direction.SOUTH);
        Scp020SporeMemory.Face Lonely = face(9, 1, 9, Direction.NORTH);
        List<Scp020SporeMemory.Face> All = List.of(Corner, Beside, face(0, 2, 0, Direction.SOUTH), face(1, 2, 0, Direction.SOUTH), Floor, face(1, 0, 1, Direction.UP),
                PillarFront, face(5, 1, 5, Direction.EAST), face(5, 1, 5, Direction.UP), Lonely);
        Scp020SporeMemory Memory = new Scp020SporeMemory();
        Memory.apply(layout(All));
        Helper.assertTrue(Scp020SporeMemory.VariantCount == 47 && Scp020SporeMemory.SpriteCount == 47 * Scp020SporeMemory.Quadrants, "the connection variants are not the 47 blob shapes");
        int Top = Scp020SporeMemory.Top;
        int Right = Scp020SporeMemory.Right;
        int Bottom = Scp020SporeMemory.Bottom;
        int Left = Scp020SporeMemory.Left;
        Helper.assertTrue(Memory.connections(Corner) == (Top | Right | Bottom | Scp020SporeMemory.TopRight | Scp020SporeMemory.BottomRight),
                "a wall patch in a 2x2 square over a floor patch has mask " + Memory.connections(Corner));
        Helper.assertTrue(Memory.connections(Floor) == (Top | Right | Scp020SporeMemory.TopRight), "the floor patch does not join the wall at the inside corner: " + Memory.connections(Floor));
        Helper.assertTrue(Memory.connections(PillarFront) == (Top | Right | Scp020SporeMemory.TopRight), "the pillar front does not wrap the outside corners: " + Memory.connections(PillarFront));
        Helper.assertTrue(Memory.connections(Lonely) == 0 && Scp020SporeMemory.variant(0) == 0, "a lone patch joins something");
        Helper.assertTrue((Memory.connections(Beside) & Left) != 0 && (Memory.connections(Beside) & Right) == 0, "the right hand patch of the square is not joined on its left only");
        Helper.assertTrue(Scp020SporeMemory.quadrant(Corner) != Scp020SporeMemory.quadrant(Beside), "side by side patches use the same part of the pattern");
        for (Scp020SporeMemory.Face Patch : All) {
            int Sprite = Scp020SporeMemory.sprite(Patch, Scp020SporeMemory.variant(Memory.connections(Patch)));
            Helper.assertTrue(Sprite >= 0 && Sprite < Scp020SporeMemory.SpriteCount, "a film sprite index is out of range: " + Sprite);
        }
        int Before = Scp020SporeMemory.variant(Memory.connections(Corner));
        Memory.apply(layout(All.stream().filter(Patch -> !Patch.equals(Beside)).toList()));
        Helper.assertTrue(Memory.connections(Corner) == (Top | Bottom), "removing the neighbour left the corner patch joined to it: " + Memory.connections(Corner));
        Helper.assertTrue(Scp020SporeMemory.variant(Memory.connections(Corner)) != Before, "removing the neighbour did not change the film shape");
        for (int Mask = 0; Mask < 256; Mask++) {
            Helper.assertTrue(Scp020SporeMemory.variant(Mask) == Scp020SporeMemory.variant(Scp020SporeMemory.canonical(Mask)), "mask " + Mask + " does not share its blob shape");
        }
        Helper.succeed();
    }
}
