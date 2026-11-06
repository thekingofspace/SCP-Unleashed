package net.rai.scp_unleashed.cameras;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.network.CameraWindowPayload;
import net.rai.scp_unleashed.props.TallProps;
import net.rai.scp_unleashed.scp096.Scp096Persons;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import net.rai.scp_unleashed.scp151.Scp151Breath;
import net.rai.scp_unleashed.scp151.Scp151Gaze;
import net.rai.scp_unleashed.scp151.Scp151Registry;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp173.Scp173Sight;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import net.rai.scp_unleashed.senses.SensesRegistry;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class CameraGameTests {
    public static final String Room = "cameras_test_room";
    public static final TicketType<ChunkPos> SiteTicket = TicketType.create("scp_unleashed_camera_test", (First, Second) -> Long.compare(First.toLong(), Second.toLong()));
    public static final int SiteRadius = 3;
    public static final BlockPos StatueSite = new BlockPos(0, 0, 400);
    public static final int WarmRadius = 4;
    public static final BlockPos ShyGuySite = new BlockPos(0, 0, -400);
    public static final BlockPos PaintingSite = new BlockPos(600, 0, 0);
    public static final BlockPos UnloadedSite = new BlockPos(1200, 0, 0);
    public static final BlockPos ForcedSite = new BlockPos(-1200, 0, 0);
    public static final BlockPos FarSite = new BlockPos(0, 0, 2400);
    public static final BlockPos SwitchSiteA = new BlockPos(2400, 0, 2400);
    public static final BlockPos SwitchSiteB = new BlockPos(-2400, 0, 2400);
    public static final BlockPos BrokenSite = new BlockPos(2400, 0, -2400);
    public static final BlockPos RideSite = new BlockPos(-2400, 0, -2400);
    public static final int CoreRadius = 2;
    public static final int SettleTicks = 80;
    public static final long LingerNanos = 20_000_000L;
    public static final long PauseNanos = 1_000_000L;
    private static final Map<UUID, Wiretap> Taps = new ConcurrentHashMap<>();

    private CameraGameTests() {
    }

    private record Site(ServerLevel World, BlockPos Base, BlockPos Camera, BlockPos Subject) {
    }

    private static final class Wiretap {
        private final EmbeddedChannel Wire;
        private final List<Packet<?>> Heard = new ArrayList<>();

        private Wiretap(EmbeddedChannel Wire) {
            this.Wire = Wire;
        }

        private void pull() {
            Wire.flushOutbound();
            Object Each;
            while ((Each = Wire.readOutbound()) != null) {
                add(Each);
            }
        }

        private void add(Object Each) {
            if (Each instanceof ClientboundBundlePacket Bundle) {
                for (Packet<?> Inner : Bundle.subPackets()) {
                    add(Inner);
                }
            } else if (Each instanceof Packet<?> Found) {
                Heard.add(Found);
            }
        }

        private int mark() {
            pull();
            return Heard.size();
        }

        private List<Packet<?>> since(int Mark) {
            pull();
            return List.copyOf(Heard.subList(Mark, Heard.size()));
        }
    }

    private static ServerPlayer viewer(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        CommonListenerCookie Cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "camera-tester"), false);
        ServerPlayer User = new ServerPlayer(World.getServer(), World, Cookie.gameProfile(), Cookie.clientInformation());
        Connection Link = new Connection(PacketFlow.SERVERBOUND);
        EmbeddedChannel Wire = new EmbeddedChannel(Link);
        Taps.put(User.getUUID(), new Wiretap(Wire));
        openChannels(Link);
        World.getServer().getPlayerList().placeNewPlayer(Link, User, Cookie);
        User.setGameMode(GameType.SURVIVAL);
        Vec3 Spot = Helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        warm(World, new ChunkPos(BlockPos.containing(Spot)), User.requestedViewDistance() + 1);
        User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CameraRegistry.Tablet.get()));
        Helper.onEachTick(() -> User.connection.chunkSender.onChunkBatchReceivedByClient(64.0f));
        return User;
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

    private static void leave(ServerPlayer User) {
        if (User.server.getPlayerList().getPlayer(User.getUUID()) != null) {
            User.server.getPlayerList().remove(User);
        }
        Taps.remove(User.getUUID());
    }

    private static Wiretap tap(ServerPlayer User) {
        return Taps.get(User.getUUID());
    }

    private static List<ChunkPos> core(BlockPos Camera) {
        ChunkPos Centre = new ChunkPos(Camera);
        List<ChunkPos> Found = new ArrayList<>();
        for (int X = -CoreRadius; X <= CoreRadius; X++) {
            for (int Z = -CoreRadius; Z <= CoreRadius; Z++) {
                Found.add(new ChunkPos(Centre.x + X, Centre.z + Z));
            }
        }
        return Found;
    }

    private static List<ChunkPos> chunksIn(List<Packet<?>> Heard) {
        List<ChunkPos> Found = new ArrayList<>();
        for (Packet<?> Each : Heard) {
            if (Each instanceof ClientboundLevelChunkWithLightPacket Data) {
                Found.add(new ChunkPos(Data.getX(), Data.getZ()));
            }
        }
        return Found;
    }

    private static List<ChunkPos> forgotten(List<Packet<?>> Heard) {
        List<ChunkPos> Found = new ArrayList<>();
        for (Packet<?> Each : Heard) {
            if (Each instanceof ClientboundForgetLevelChunkPacket Forget) {
                Found.add(Forget.pos());
            }
        }
        return Found;
    }

    private static int centres(List<Packet<?>> Heard) {
        int Count = 0;
        for (Packet<?> Each : Heard) {
            if (Each instanceof ClientboundSetChunkCacheCenterPacket) {
                Count++;
            }
        }
        return Count;
    }

    private static int windowAt(List<Packet<?>> Heard, @Nullable ChunkPos Lens) {
        for (int Index = 0; Index < Heard.size(); Index++) {
            if (Heard.get(Index) instanceof ClientboundCustomPayloadPacket Custom && Custom.payload() instanceof CameraWindowPayload Frame) {
                boolean Matches = Lens == null ? !Frame.Open() : Frame.Open() && Frame.X() == Lens.x && Frame.Z() == Lens.z;
                if (Matches) {
                    return Index;
                }
            }
        }
        return -1;
    }

    private static List<String> windows(List<Packet<?>> Heard) {
        List<String> Found = new ArrayList<>();
        for (Packet<?> Each : Heard) {
            if (Each instanceof ClientboundCustomPayloadPacket Custom && Custom.payload() instanceof CameraWindowPayload Frame) {
                Found.add(Frame.Open() ? "open " + Frame.X() + "," + Frame.Z() : "closed");
            }
        }
        return Found;
    }

    private static int firstChunkOf(List<Packet<?>> Heard, List<ChunkPos> Wanted) {
        for (int Index = 0; Index < Heard.size(); Index++) {
            if (Heard.get(Index) instanceof ClientboundLevelChunkWithLightPacket Data && Wanted.contains(new ChunkPos(Data.getX(), Data.getZ()))) {
                return Index;
            }
        }
        return -1;
    }

    private static int lastForgetOf(List<Packet<?>> Heard, List<ChunkPos> Wanted) {
        int Last = -1;
        for (int Index = 0; Index < Heard.size(); Index++) {
            if (Heard.get(Index) instanceof ClientboundForgetLevelChunkPacket Forget && Wanted.contains(Forget.pos())) {
                Last = Index;
            }
        }
        return Last;
    }

    private static String sorted(List<ChunkPos> Found) {
        TreeSet<String> Names = new TreeSet<>();
        for (ChunkPos Each : Found) {
            Names.add(Each.x + "," + Each.z);
        }
        return Names.toString();
    }

    @SuppressWarnings("unchecked")
    public static String tickets(ServerLevel World, UUID Key) {
        try {
            DistanceManager Manager = World.getChunkSource().chunkMap.getDistanceManager();
            Field TicketsField = DistanceManager.class.getDeclaredField("tickets");
            TicketsField.setAccessible(true);
            Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> Tickets = (Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>>) TicketsField.get(Manager);
            Field KeyField = Ticket.class.getDeclaredField("key");
            KeyField.setAccessible(true);
            TreeSet<String> Found = new TreeSet<>();
            for (Long2ObjectOpenHashMap.Entry<SortedArraySet<Ticket<?>>> Slot : Tickets.long2ObjectEntrySet()) {
                ChunkPos Pos = new ChunkPos(Slot.getLongKey());
                for (Ticket<?> Each : Slot.getValue()) {
                    if (Key.equals(KeyField.get(Each))) {
                        Found.add(Each.getType() + "@" + Pos.x + "," + Pos.z + ":" + Each.getTicketLevel());
                    }
                }
            }
            return Found.toString();
        } catch (ReflectiveOperationException Error) {
            throw new IllegalStateException(Error);
        }
    }

    private static String state(ServerPlayer User) {
        CameraSession Session = CameraSessions.get(User.getUUID());
        String Described = Session == null ? "none" : Session.camera().toShortString() + " lens " + Session.centre() + " view " + Session.view() + " body " + Session.body();
        return "tickets " + tickets(User.serverLevel(), User.getUUID()) + " view " + User.getChunkTrackingView() + " session " + Described;
    }

    private static BlockPos mountCamera(ServerLevel World, BlockPos Camera, Direction Facing) {
        World.setBlock(Camera.relative(Facing.getOpposite()), Blocks.STONE.defaultBlockState(), 3);
        World.setBlock(Camera, CameraRegistry.Camera.get().defaultBlockState().setValue(CameraBlock.Face, AttachFace.WALL).setValue(CameraBlock.Facing, Facing), 3);
        return Camera;
    }

    private static Site site(GameTestHelper Helper, BlockPos Offset, boolean Hold) {
        ServerLevel World = Helper.getLevel();
        BlockPos Base = Helper.absolutePos(new BlockPos(4, 1, 4)).offset(Offset);
        ChunkPos Centre = new ChunkPos(Base);
        if (Hold) {
            World.getChunkSource().addRegionTicket(SiteTicket, Centre, SiteRadius, Centre);
        }
        World.getChunk(Base);
        World.getChunk(Base.offset(0, 0, 8));
        for (int X = -3; X <= 3; X++) {
            for (int Z = -1; Z <= 9; Z++) {
                World.setBlock(Base.offset(X, -1, Z), Blocks.STONE.defaultBlockState(), 3);
                for (int Y = 0; Y <= 4; Y++) {
                    World.setBlock(Base.offset(X, Y, Z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        BlockPos Camera = mountCamera(World, Base.offset(0, 2, 0), Direction.SOUTH);
        warm(World, new ChunkPos(Camera), WarmRadius);
        return new Site(World, Base, Camera, Base.offset(0, 0, 6));
    }

    private static void warm(ServerLevel World, ChunkPos Centre, int Radius) {
        for (int X = -Radius; X <= Radius; X++) {
            for (int Z = -Radius; Z <= Radius; Z++) {
                World.getChunk(Centre.x + X, Centre.z + Z);
            }
        }
    }

    private static void release(Site Place) {
        ChunkPos Centre = new ChunkPos(Place.Base());
        Place.World().getChunkSource().removeRegionTicket(SiteTicket, Centre, SiteRadius, Centre);
    }

    private static ItemStack tablet(ServerPlayer User) {
        return User.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static void link(GameTestHelper Helper, ServerPlayer User, ServerLevel World, BlockPos Camera) {
        CameraSessions.link(User, tablet(User), World, Camera);
        CameraLinks Links = CameraLinks.of(tablet(User));
        Helper.assertTrue(Links.indexOf(GlobalPos.of(World.dimension(), Camera)) >= 0, "camera was not linked");
    }

    private static void aimAt(ServerPlayer User, BlockPos Camera, Vec3 Target) {
        CameraBlockEntity Found = (CameraBlockEntity) User.serverLevel().getBlockEntity(Camera);
        CameraMount Mount = Found.mount();
        Vec3 From = Mount.pivot(Camera);
        Vec3 Toward = Target.subtract(From);
        double Flat = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        float Yaw = (float) Math.toDegrees(Math.atan2(-Toward.x, Toward.z));
        float Pitch = (float) -Math.toDegrees(Math.atan2(Toward.y, Flat));
        CameraSessions.look(User, Mth.wrapDegrees(Yaw - Mount.baseYaw()), Pitch - Mount.basePitch());
    }

    private static void open(GameTestHelper Helper, ServerPlayer User, BlockPos Camera) {
        CameraLinks Links = CameraLinks.of(tablet(User));
        int Index = Links.indexOf(GlobalPos.of(User.level().dimension(), Camera));
        CameraSessions.Outcome Result = CameraSessions.open(User, InteractionHand.MAIN_HAND, Index);
        Helper.assertTrue(Result == CameraSessions.Outcome.Opened, "camera did not open: " + Result);
        Helper.assertTrue(CameraViews.isViewing(User), "player is not viewing");
    }

    @SuppressWarnings("unchecked")
    public static boolean holds(ServerLevel World, ChunkPos Pos, TicketType<?> Type, @Nullable Object Key) {
        try {
            DistanceManager Manager = World.getChunkSource().chunkMap.getDistanceManager();
            Field TicketsField = DistanceManager.class.getDeclaredField("tickets");
            TicketsField.setAccessible(true);
            Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> Tickets = (Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>>) TicketsField.get(Manager);
            SortedArraySet<Ticket<?>> Found = Tickets.get(Pos.toLong());
            if (Found == null) {
                return false;
            }
            Field KeyField = Ticket.class.getDeclaredField("key");
            KeyField.setAccessible(true);
            for (Ticket<?> Each : Found) {
                if (Each.getType() == Type && (Key == null || Key.equals(KeyField.get(Each)))) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException Error) {
            throw new IllegalStateException(Error);
        }
    }

    @GameTest(template = Room, timeoutTicks = 100)
    public static void linkingStoresCamera(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = viewer(Helper);
        BlockPos Camera = mountCamera(World, Helper.absolutePos(new BlockPos(8, 3, 8)), Direction.SOUTH);
        CameraSessions.link(User, tablet(User), World, Camera);
        CameraSessions.link(User, tablet(User), World, Camera);
        CameraLinks Links = CameraLinks.of(tablet(User));
        Helper.assertTrue(Links.size() == 1, "expected one link, found " + Links.size());
        CameraLinks.Link Entry = Links.get(0);
        Helper.assertTrue(Entry != null && Entry.pos().equals(Camera) && Entry.dimension() == Level.OVERWORLD, "link stored the wrong place");
        Helper.assertTrue(!Entry.Name().isEmpty(), "link has no name");
        CameraBlockEntity Found = (CameraBlockEntity) World.getBlockEntity(Camera);
        Helper.assertTrue(Found != null && Found.isLinked() && Found.getCameraName().equals(Entry.Name()), "camera does not know it is linked");
        CameraSessions.edit(User, InteractionHand.MAIN_HAND, 0, false, "Gate West");
        Helper.assertTrue("Gate West".equals(CameraLinks.of(tablet(User)).get(0).Name()) && "Gate West".equals(Found.getCameraName()), "rename did not stick");
        leave(User);
        Helper.succeed();
    }

    @GameTest(template = Room, timeoutTicks = 100)
    public static void brokenCameraIsRemoved(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = viewer(Helper);
        BlockPos First = mountCamera(World, Helper.absolutePos(new BlockPos(6, 3, 8)), Direction.SOUTH);
        BlockPos Second = mountCamera(World, Helper.absolutePos(new BlockPos(10, 3, 8)), Direction.SOUTH);
        link(Helper, User, World, First);
        link(Helper, User, World, Second);
        World.setBlock(First, Blocks.AIR.defaultBlockState(), 3);
        World.setBlock(Second, Blocks.STONE.defaultBlockState(), 3);
        CameraSessions.Outcome Result = CameraSessions.open(User, InteractionHand.MAIN_HAND, 0);
        Helper.assertTrue(Result == CameraSessions.Outcome.Missing, "missing camera was not reported: " + Result);
        Helper.assertTrue(CameraLinks.of(tablet(User)).size() == 1, "missing camera was not removed");
        Result = CameraSessions.open(User, InteractionHand.MAIN_HAND, 0);
        Helper.assertTrue(Result == CameraSessions.Outcome.Missing, "replaced camera was not reported: " + Result);
        Helper.assertTrue(CameraLinks.of(tablet(User)).isEmpty(), "replaced camera was not removed");
        Helper.assertTrue(!CameraViews.isViewing(User), "player should not be viewing");
        leave(User);
        Helper.succeed();
    }

    @GameTest(template = Room, timeoutTicks = 1400, batch = "camera_ticketHoldsUnloadedChunk")
    public static void ticketHoldsUnloadedChunk(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, UnloadedSite, false);
        ServerLevel World = Place.World();
        ChunkPos Chunk = new ChunkPos(Place.Camera());
        link(Helper, User, World, Place.Camera());
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(!World.getChunkSource().hasChunk(Chunk.x, Chunk.z), "camera chunk is still loaded"))
                .thenExecute(() -> {
                    Helper.assertTrue(!holds(World, Chunk, CameraSessions.ViewTicket, null), "a camera ticket existed before viewing");
                    open(Helper, User, Place.Camera());
                    Helper.assertTrue(holds(World, Chunk, CameraSessions.ViewTicket, User.getUUID()), "no camera ticket while viewing");
                    Helper.assertTrue(World.getBlockEntity(Place.Camera()) instanceof CameraBlockEntity, "camera chunk did not load");
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    Helper.assertTrue(World.getChunkSource().hasChunk(Chunk.x, Chunk.z), "camera chunk unloaded while viewing");
                    CameraSessions.end(User, CameraSessions.End.Left);
                    Helper.assertTrue(!holds(World, Chunk, CameraSessions.ViewTicket, null), "camera ticket was not removed on exit");
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 300)
    public static void existingTicketIsKept(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, ForcedSite, false);
        ServerLevel World = Place.World();
        ChunkPos Chunk = new ChunkPos(Place.Camera());
        World.setChunkForced(Chunk.x, Chunk.z, true);
        link(Helper, User, World, Place.Camera());
        open(Helper, User, Place.Camera());
        Helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    CameraSessions.end(User, CameraSessions.End.Left);
                    Helper.assertTrue(!holds(World, Chunk, CameraSessions.ViewTicket, null), "camera ticket was not removed on exit");
                    Helper.assertTrue(holds(World, Chunk, TicketType.FORCED, null), "the forced ticket was removed");
                    Helper.assertTrue(World.getForcedChunks().contains(Chunk.toLong()), "the chunk is no longer forced");
                })
                .thenIdle(20)
                .thenExecute(() -> {
                    Helper.assertTrue(World.getChunkSource().hasChunk(Chunk.x, Chunk.z), "the forced chunk unloaded");
                    World.setChunkForced(Chunk.x, Chunk.z, false);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 400)
    public static void cameraFreezes173(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, StatueSite, true);
        ServerLevel World = Place.World();
        Scp173Statue Statue = Scp173Registry.Statue.get().create(World);
        Statue.moveTo(Vec3.atBottomCenterOf(Place.Subject()));
        World.addFreshEntity(Statue);
        link(Helper, User, World, Place.Camera());
        Helper.startSequence()
                .thenIdle(10)
                .thenExecute(() -> {
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "statue is watched before any camera feed");
                    open(Helper, User, Place.Camera());
                    aimAt(User, Place.Camera(), Statue.sightBox().getCenter());
                })
                .thenWaitUntil(() -> Helper.assertTrue(Statue.trackers().contains(User.getUUID()), "the camera viewer never tracked the statue"))
                .thenExecute(() -> {
                    Helper.assertTrue(User.getChunkTrackingView().contains(new ChunkPos(Place.Camera())), "the camera chunks are not sent to the viewer");
                    Helper.assertTrue(User.getChunkTrackingView().contains(User.chunkPosition()), "the body chunks were dropped while viewing");
                })
                .thenExecute(() -> Helper.assertTrue(Scp173Sight.isObserved(World, Statue), "statue is not watched through the camera"))
                .thenExecute(() -> {
                    Scp173Sight.report(User, List.of(Statue.getId()));
                    Helper.assertTrue(Scp173Sight.isObserved(World, Statue), "the feed report of the statue was rejected");
                })
                .thenExecuteFor(Scp173Sight.Window + 2, () -> Scp173Sight.report(User, List.of()))
                .thenExecute(() -> {
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "statue is watched although the feed does not show it");
                    Scp173Sight.report(User, List.of(Statue.getId()));
                    User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "a blindfolded viewer still watches the statue");
                    User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    CameraSessions.end(User, CameraSessions.End.Left);
                    Scp173Sight.forget(User.getUUID());
                    Helper.assertTrue(!Scp173Sight.isObserved(World, Statue), "statue is watched after leaving the feed");
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    Helper.assertTrue(User.getChunkTrackingView().contains(User.chunkPosition()), "the view did not return to the body");
                    Helper.assertTrue(!User.getChunkTrackingView().contains(new ChunkPos(Place.Camera())), "the camera chunks are still sent after leaving");
                    Statue.discard();
                    release(Place);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 300)
    public static void cameraTriggers096(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, ShyGuySite, true);
        ServerLevel World = Place.World();
        Scp096ShyGuy Guy = Scp096Registry.ShyGuy.get().create(World);
        Guy.moveTo(Place.Subject().getX() + 0.5, Place.Subject().getY(), Place.Subject().getZ() + 0.5, 180.0f, 0.0f);
        Guy.setYHeadRot(180.0f);
        Guy.setYBodyRot(180.0f);
        World.addFreshEntity(Guy);
        link(Helper, User, World, Place.Camera());
        Helper.assertTrue(!Guy.reportSighting(World, User), "096 was seen without a camera");
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Guy.tickCount > Scp096ShyGuy.SeenGap, "096 has not started ticking at its site"))
                .thenIdle(6)
                .thenExecute(() -> {
                    open(Helper, User, Place.Camera());
                    aimAt(User, Place.Camera(), Guy.getEyePosition());
                    User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
                    Guy.setYRot(180.0f);
                    Guy.setYHeadRot(180.0f);
                    Helper.assertTrue(!Guy.reportSighting(World, User), "096 was seen by a blindfolded viewer");
                })
                .thenIdle(6)
                .thenExecute(() -> {
                    User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    Guy.setYRot(180.0f);
                    Guy.setYHeadRot(180.0f);
                    Helper.assertTrue(Guy.reportSighting(World, User), "096's face was not seen through the camera");
                    Helper.assertTrue(Guy.targetKeys().contains(Scp096Persons.keyOf(User)), "096 is not after the viewer");
                    CameraSessions.end(User, CameraSessions.End.Left);
                    Guy.discard();
                    release(Place);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 300)
    public static void paintingTriggersOnViewer(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, PaintingSite, true);
        ServerLevel World = Place.World();
        BlockState Painting = Scp151Registry.Painting.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        World.setBlock(Place.Subject(), Painting, 3);
        TallProps.placeUpper(World, Place.Subject(), Painting);
        link(Helper, User, World, Place.Camera());
        Vec3 Canvas = Scp151Gaze.centre(Place.Subject(), Direction.NORTH);
        Helper.startSequence()
                .thenIdle(6)
                .thenExecute(() -> Helper.assertTrue(!Scp151Breath.isGazing(User), "the painting caught the player without a camera"))
                .thenExecute(() -> {
                    open(Helper, User, Place.Camera());
                    aimAt(User, Place.Camera(), Canvas);
                    User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
                })
                .thenIdle(6)
                .thenExecute(() -> {
                    Helper.assertTrue(!Scp151Breath.isGazing(User), "the painting caught a blindfolded viewer");
                    User.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Scp151Breath.isGazing(User), "the painting did not catch the camera viewer"))
                .thenExecute(() -> {
                    CameraSessions.end(User, CameraSessions.End.Left);
                    World.setBlock(Place.Subject().above(), Blocks.AIR.defaultBlockState(), 3);
                    World.setBlock(Place.Subject(), Blocks.AIR.defaultBlockState(), 3);
                    release(Place);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 300)
    public static void lockAndExitPaths(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = viewer(Helper);
        BlockPos Camera = mountCamera(World, Helper.absolutePos(new BlockPos(8, 3, 12)), Direction.NORTH);
        ChunkPos Chunk = new ChunkPos(Camera);
        link(Helper, User, World, Camera);
        Vec3 Start = User.position();
        ChunkPos Body = User.chunkPosition();
        open(Helper, User, Camera);
        Helper.assertTrue(holds(World, Body, CameraSessions.BodyTicket, User.getUUID()), "no body ticket while viewing");
        Helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    Helper.assertTrue(User.position().distanceTo(Start) < 1.0e-3, "the body moved while viewing");
                    Helper.assertTrue(CameraViews.eye(User).distanceTo(User.getEyePosition()) > 1.0, "the view did not move to the camera");
                    BlockPos Floor = Helper.absolutePos(new BlockPos(3, 0, 3));
                    PlayerInteractEvent.RightClickBlock Use = new PlayerInteractEvent.RightClickBlock(User, InteractionHand.MAIN_HAND, Floor,
                            new BlockHitResult(Vec3.atCenterOf(Floor), Direction.UP, Floor, false));
                    Helper.assertTrue(NeoForge.EVENT_BUS.post(Use).isCanceled(), "the body could still use blocks while viewing");
                    User.hurt(World.damageSources().fellOutOfWorld(), 1.0f);
                    Helper.assertTrue(!CameraViews.isViewing(User), "damage did not end the feed");
                    Helper.assertTrue(!holds(World, Chunk, CameraSessions.ViewTicket, User.getUUID()), "damage left the camera ticket");
                    Helper.assertTrue(!holds(World, Body, CameraSessions.BodyTicket, User.getUUID()), "damage left the body ticket");
                    User.setHealth(User.getMaxHealth());
                    open(Helper, User, Camera);
                    User.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    Helper.assertTrue(!CameraViews.isViewing(User), "putting the tablet away did not end the feed");
                    User.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CameraRegistry.Tablet.get()));
                    link(Helper, User, World, Camera);
                    open(Helper, User, Camera);
                    User.setPos(User.getX() + 3.0, User.getY(), User.getZ());
                })
                .thenIdle(2)
                .thenExecute(() -> {
                    Helper.assertTrue(!CameraViews.isViewing(User), "moving the body did not end the feed");
                    User.setPos(Start.x, Start.y, Start.z);
                    open(Helper, User, Camera);
                    ServerLevel Nether = World.getServer().getLevel(Level.NETHER);
                    Helper.assertTrue(Nether != null, "no nether to travel to");
                    User.changeDimension(new DimensionTransition(Nether, new Vec3(0.5, 80.0, 0.5), Vec3.ZERO, 0.0f, 0.0f, DimensionTransition.DO_NOTHING));
                    Helper.assertTrue(!CameraViews.isViewing(User) && CameraSessions.get(User.getUUID()) == null, "changing dimension did not end the feed");
                    Helper.assertTrue(!holds(World, Chunk, CameraSessions.ViewTicket, User.getUUID()), "changing dimension left the camera ticket");
                    Helper.assertTrue(!holds(World, Body, CameraSessions.BodyTicket, User.getUUID()), "changing dimension left the body ticket");
                    leave(User);
                })
                .thenExecute(() -> {
                    ServerPlayer Other = viewer(Helper);
                    link(Helper, Other, World, Camera);
                    open(Helper, Other, Camera);
                    UUID Key = Other.getUUID();
                    ChunkPos OtherBody = Other.chunkPosition();
                    Helper.assertTrue(holds(World, OtherBody, CameraSessions.BodyTicket, Key), "no body ticket for the second viewer");
                    leave(Other);
                    Helper.assertTrue(CameraSessions.get(Key) == null, "logging out did not end the feed");
                    Helper.assertTrue(!holds(World, Chunk, CameraSessions.ViewTicket, Key), "logging out left the camera ticket");
                    Helper.assertTrue(!holds(World, OtherBody, CameraSessions.BodyTicket, Key), "logging out left the body ticket");
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    private static void settled(GameTestHelper Helper, ServerPlayer User) {
        ChunkTrackingView Own = User.getChunkTrackingView();
        boolean Placed = Own instanceof ChunkTrackingView.Positioned Single && Single.center().equals(User.chunkPosition());
        List<ChunkPos> Wanted = new ArrayList<>();
        Own.forEach(Wanted::add);
        boolean Arrived = Placed && chunksIn(tap(User).since(0)).containsAll(Wanted);
        if (!Arrived) {
            linger(User.serverLevel());
        }
        Helper.assertTrue(Placed, "the viewer has not got its own view yet");
        Helper.assertTrue(Arrived, "the body chunks have not all reached the viewer yet");
    }

    private static void linger(ServerLevel World) {
        long Until = System.nanoTime() + LingerNanos;
        while (System.nanoTime() < Until) {
            if (!World.getChunkSource().pollTask()) {
                LockSupport.parkNanos(PauseNanos);
            }
        }
    }

    private static void keepsBody(GameTestHelper Helper, ServerPlayer User, List<Packet<?>> Heard, ChunkTrackingView Home, Vec3 Start, String Phase) {
        Helper.assertTrue(User.position().distanceTo(Start) < 1.0e-6, Phase + ": the body moved");
        Helper.assertTrue(centres(Heard) == 0, Phase + ": the client chunk centre was moved away from the body");
        for (ChunkPos Each : forgotten(Heard)) {
            Helper.assertTrue(!Home.contains(Each), Phase + ": the client was told to forget body chunk " + Each);
        }
        List<ChunkPos> Ever = chunksIn(tap(User).since(0));
        for (ChunkPos Each : Ever) {
            Helper.assertTrue(!Home.contains(Each) || Ever.indexOf(Each) == Ever.lastIndexOf(Each), Phase + ": body chunk " + Each + " was sent again");
        }
    }

    private static void viewing(GameTestHelper Helper, ServerPlayer User, BlockPos Camera, ChunkPos Body) {
        ServerLevel World = User.serverLevel();
        ChunkPos Lens = new ChunkPos(Camera);
        Helper.assertTrue(holds(World, Body, CameraSessions.BodyTicket, User.getUUID()), "no body ticket at the body chunk while viewing");
        Helper.assertTrue(holds(World, Lens, CameraSessions.ViewTicket, User.getUUID()), "no camera ticket while viewing");
        for (int X = -1; X <= 1; X++) {
            for (int Z = -1; Z <= 1; Z++) {
                ChunkPos Near = new ChunkPos(Body.x + X, Body.z + Z);
                Helper.assertTrue(World.getChunkSource().hasChunk(Near.x, Near.z), "body chunk " + Near + " is not loaded while viewing");
                Helper.assertTrue(User.getChunkTrackingView().contains(Near), "body chunk " + Near + " left the tracking view");
                Helper.assertTrue(World.getChunkSource().chunkMap.getPlayers(Near, false).contains(User), "body chunk " + Near + " is not kept on the viewer");
            }
        }
        Helper.assertTrue(User.getChunkTrackingView().contains(Lens), "the camera chunk is not in the tracking view");
    }

    private static GameTestSequence phase(GameTestHelper Helper, GameTestSequence Steps, ServerPlayer User, int[] Mark, ChunkTrackingView[] Home, Vec3[] Start, List<String> Notes, String Name, Runnable Action, BlockPos Target, @Nullable BlockPos Previous) {
        List<ChunkPos> Wanted = core(Target);
        return Steps
                .thenExecute(() -> {
                    Mark[0] = tap(User).mark();
                    Action.run();
                })
                .thenWaitUntil(() -> Helper.assertTrue(chunksIn(tap(User).since(Mark[0])).containsAll(Wanted), Name + ": the camera chunks never reached the viewer"))
                .thenIdle(5)
                .thenExecute(() -> {
                    List<Packet<?>> Heard = tap(User).since(Mark[0]);
                    ChunkPos Lens = new ChunkPos(Target);
                    keepsBody(Helper, User, Heard, Home[0], Start[0], Name);
                    viewing(Helper, User, Target, Home[0] instanceof ChunkTrackingView.Positioned Own ? Own.center() : User.chunkPosition());
                    int Opened = windowAt(Heard, Lens);
                    Helper.assertTrue(Opened >= 0, Name + ": the client never learned where the camera is");
                    Helper.assertTrue(Opened < firstChunkOf(Heard, Wanted), Name + ": camera chunks arrived before the client knew where the camera is");
                    List<ChunkPos> Sent = chunksIn(Heard);
                    Helper.assertTrue(Sent.get(0).equals(Lens), Name + ": the camera's own chunk was not sent first but " + Sent.get(0));
                    if (Previous != null) {
                        List<ChunkPos> Old = core(Previous);
                        Helper.assertTrue(forgotten(Heard).containsAll(Old), Name + ": the old camera chunks were not dropped");
                        Helper.assertTrue(lastForgetOf(Heard, Old) < Opened, Name + ": old camera chunks were dropped after the client moved its camera window");
                    }
                    Notes.add(Name + " " + state(User) + " sent " + sorted(Sent) + " forgot " + sorted(forgotten(Heard)) + " windows " + windows(Heard));
                });
    }

    private static GameTestSequence closing(GameTestHelper Helper, GameTestSequence Steps, ServerPlayer User, int[] Mark, ChunkTrackingView[] Home, Vec3[] Start, List<String> Notes, BlockPos Last) {
        return Steps
                .thenExecute(() -> {
                    Mark[0] = tap(User).mark();
                    CameraSessions.end(User, CameraSessions.End.Left);
                })
                .thenIdle(3)
                .thenExecute(() -> {
                    List<Packet<?>> Heard = tap(User).since(Mark[0]);
                    List<ChunkPos> Old = core(Last);
                    keepsBody(Helper, User, Heard, Home[0], Start[0], "closing");
                    Helper.assertTrue(User.getChunkTrackingView().equals(Home[0]), "the tracking view did not return to the body: " + User.getChunkTrackingView());
                    Helper.assertTrue(tickets(User.serverLevel(), User.getUUID()).equals("[]"), "camera tickets were left behind: " + tickets(User.serverLevel(), User.getUUID()));
                    Helper.assertTrue(CameraSessions.get(User.getUUID()) == null, "the session was not cleared");
                    Helper.assertTrue(forgotten(Heard).containsAll(Old), "the camera chunks were not dropped on leaving");
                    int Closed = windowAt(Heard, null);
                    Helper.assertTrue(Closed > lastForgetOf(Heard, Old), "the client camera window closed before the camera chunks were dropped");
                    Notes.add("closed " + state(User));
                });
    }

    @GameTest(template = Room, timeoutTicks = 1400, batch = "camera_distantViewKeepsBody")
    public static void distantViewKeepsBody(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, FarSite, true);
        ServerLevel World = Place.World();
        link(Helper, User, World, Place.Camera());
        int[] Mark = new int[1];
        ChunkTrackingView[] Home = new ChunkTrackingView[1];
        Vec3[] Start = new Vec3[1];
        List<String> Notes = new ArrayList<>();
        GameTestSequence Steps = Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, User))
                .thenExecute(() -> {
                    Home[0] = User.getChunkTrackingView();
                    Start[0] = User.position();
                    Helper.assertTrue(Home[0] instanceof ChunkTrackingView.Positioned Own && Own.center().equals(User.chunkPosition()), "the viewer did not start with its own view");
                    Helper.assertTrue(!Home[0].contains(new ChunkPos(Place.Camera())), "the test camera is not distant");
                });
        Steps = phase(Helper, Steps, User, Mark, Home, Start, Notes, "open", () -> open(Helper, User, Place.Camera()), Place.Camera(), null);
        Steps = Steps
                .thenExecute(() -> Mark[0] = tap(User).mark())
                .thenIdle(60)
                .thenExecute(() -> {
                    keepsBody(Helper, User, tap(User).since(Mark[0]), Home[0], Start[0], "watching");
                    viewing(Helper, User, Place.Camera(), User.chunkPosition());
                    Helper.assertTrue(CameraViews.isViewing(User), "the feed ended by itself");
                });
        closing(Helper, Steps, User, Mark, Home, Start, Notes, Place.Camera())
                .thenExecute(() -> {
                    release(Place);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 2000, batch = "camera_sessionsRepeatIdentically")
    public static void sessionsRepeatIdentically(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site First = site(Helper, SwitchSiteA, true);
        Site Second = site(Helper, SwitchSiteB, true);
        ServerLevel World = First.World();
        link(Helper, User, World, First.Camera());
        link(Helper, User, World, Second.Camera());
        int[] Mark = new int[1];
        ChunkTrackingView[] Home = new ChunkTrackingView[1];
        Vec3[] Start = new Vec3[1];
        List<List<String>> Rounds = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        GameTestSequence Steps = Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, User))
                .thenExecute(() -> {
                    Home[0] = User.getChunkTrackingView();
                    Start[0] = User.position();
                });
        for (int Round = 0; Round < Rounds.size(); Round++) {
            List<String> Notes = Rounds.get(Round);
            Steps = phase(Helper, Steps, User, Mark, Home, Start, Notes, "open", () -> open(Helper, User, First.Camera()), First.Camera(), null);
            Steps = phase(Helper, Steps, User, Mark, Home, Start, Notes, "switch", () -> CameraSessions.cycle(User, 1), Second.Camera(), First.Camera());
            BlockPos Last = Second.Camera();
            if (Round == 2) {
                Steps = phase(Helper, Steps, User, Mark, Home, Start, Notes, "back", () -> CameraSessions.cycle(User, 1), First.Camera(), Second.Camera());
                Last = First.Camera();
            }
            Steps = closing(Helper, Steps, User, Mark, Home, Start, Notes, Last);
        }
        Steps
                .thenExecute(() -> {
                    List<String> FirstRound = Rounds.get(0);
                    List<String> SecondRound = Rounds.get(1);
                    List<String> ThirdRound = Rounds.get(2);
                    Helper.assertTrue(FirstRound.equals(SecondRound), "the second session differs from the first: " + FirstRound + " vs " + SecondRound);
                    Helper.assertTrue(ThirdRound.get(0).equals(FirstRound.get(0)), "the third session opened differently: " + FirstRound.get(0) + " vs " + ThirdRound.get(0));
                    Helper.assertTrue(ThirdRound.get(1).equals(FirstRound.get(1)), "the third session switched differently: " + FirstRound.get(1) + " vs " + ThirdRound.get(1));
                    Helper.assertTrue(ThirdRound.get(3).equals(FirstRound.get(2)), "the third session closed differently: " + FirstRound.get(2) + " vs " + ThirdRound.get(3));
                    release(First);
                    release(Second);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 1400, batch = "camera_brokenCameraReturnsBody")
    public static void brokenCameraReturnsBody(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, BrokenSite, true);
        ServerLevel World = Place.World();
        link(Helper, User, World, Place.Camera());
        int[] Mark = new int[1];
        ChunkTrackingView[] Home = new ChunkTrackingView[1];
        Vec3[] Start = new Vec3[1];
        List<String> Notes = new ArrayList<>();
        GameTestSequence Steps = Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, User))
                .thenExecute(() -> {
                    Home[0] = User.getChunkTrackingView();
                    Start[0] = User.position();
                });
        phase(Helper, Steps, User, Mark, Home, Start, Notes, "open", () -> open(Helper, User, Place.Camera()), Place.Camera(), null)
                .thenExecute(() -> {
                    Mark[0] = tap(User).mark();
                    World.setBlock(Place.Camera(), Blocks.AIR.defaultBlockState(), 3);
                })
                .thenIdle(4)
                .thenExecute(() -> {
                    List<Packet<?>> Heard = tap(User).since(Mark[0]);
                    Helper.assertTrue(!CameraViews.isViewing(User), "breaking the camera did not end the feed");
                    keepsBody(Helper, User, Heard, Home[0], Start[0], "broken");
                    Helper.assertTrue(User.getChunkTrackingView().equals(Home[0]), "the tracking view did not return to the body");
                    Helper.assertTrue(tickets(World, User.getUUID()).equals("[]"), "camera tickets were left behind: " + tickets(World, User.getUUID()));
                    Helper.assertTrue(windowAt(Heard, null) > lastForgetOf(Heard, core(Place.Camera())), "the client camera window closed before the camera chunks were dropped");
                    release(Place);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 1400, batch = "camera_viewingFromBoat")
    public static void viewingFromBoat(GameTestHelper Helper) {
        ServerPlayer User = viewer(Helper);
        Site Place = site(Helper, RideSite, true);
        ServerLevel World = Place.World();
        Boat Ride = EntityType.BOAT.create(World);
        Helper.assertTrue(Ride != null, "no boat");
        Ride.moveTo(User.getX(), User.getY(), User.getZ(), 0.0f, 0.0f);
        World.addFreshEntity(Ride);
        Helper.assertTrue(User.startRiding(Ride, true), "the viewer could not board the boat");
        link(Helper, User, World, Place.Camera());
        int[] Mark = new int[1];
        ChunkTrackingView[] Home = new ChunkTrackingView[1];
        Vec3[] Start = new Vec3[1];
        List<String> Notes = new ArrayList<>();
        GameTestSequence Steps = Helper.startSequence()
                .thenWaitUntil(() -> settled(Helper, User))
                .thenExecute(() -> {
                    Home[0] = User.getChunkTrackingView();
                    Start[0] = User.position();
                });
        Steps = phase(Helper, Steps, User, Mark, Home, Start, Notes, "open", () -> open(Helper, User, Place.Camera()), Place.Camera(), null)
                .thenIdle(20)
                .thenExecute(() -> {
                    Helper.assertTrue(CameraViews.isViewing(User), "the feed ended while riding");
                    Helper.assertTrue(User.getVehicle() == Ride, "viewing dismounted the body");
                    Helper.assertTrue(World.getChunkSource().chunkMap.getPlayers(Ride.chunkPosition(), false).contains(User), "the boat chunk is not kept on the viewer");
                });
        closing(Helper, Steps, User, Mark, Home, Start, Notes, Place.Camera())
                .thenExecute(() -> {
                    Helper.assertTrue(User.getVehicle() == Ride, "leaving the feed dismounted the body");
                    User.stopRiding();
                    Ride.discard();
                    release(Place);
                    leave(User);
                })
                .thenIdle(SettleTicks)
                .thenSucceed();
    }

    @GameTest(template = Room, timeoutTicks = 20)
    public static void trackingViewJoinsBodyAndLens(GameTestHelper Helper) {
        ChunkTrackingView.Positioned Body = new ChunkTrackingView.Positioned(new ChunkPos(0, 0), 2);
        ChunkTrackingView.Positioned Far = new ChunkTrackingView.Positioned(new ChunkPos(150, -150), 2);
        CameraTrackingView Joined = new CameraTrackingView(Body, Far);
        Helper.assertTrue(Joined.contains(0, 0) && Joined.contains(1, 1) && Joined.contains(150, -150) && Joined.contains(151, -149), "the joined view misses body or camera chunks");
        Helper.assertTrue(!Joined.contains(75, -75), "the joined view covers the space between body and camera");
        Helper.assertTrue(new ChunkPos(0, 0).equals(CameraTrackingView.centre(Joined)), "the client centre left the body");
        Helper.assertTrue(new ChunkPos(150, -150).equals(CameraTrackingView.lensCentre(Joined)), "the camera centre is wrong");
        Helper.assertTrue(CameraTrackingView.leaving(Body, Joined).isEmpty(), "opening the feed drops body chunks");
        List<ChunkPos> Closing = CameraTrackingView.leaving(Joined, Body);
        Helper.assertTrue(!Closing.isEmpty(), "closing the feed keeps camera chunks");
        for (ChunkPos Each : Closing) {
            Helper.assertTrue(!Body.contains(Each) && Far.contains(Each), "closing the feed drops body chunk " + Each);
        }
        ChunkTrackingView.Positioned Near = new ChunkTrackingView.Positioned(new ChunkPos(2, 0), 2);
        List<ChunkPos> Every = new ArrayList<>();
        new CameraTrackingView(Body, Near).forEach(Every::add);
        Helper.assertTrue(Every.size() == new TreeSet<>(Every.stream().map(ChunkPos::toString).toList()).size(), "overlapping views list a chunk twice");
        List<ChunkPos> Switch = CameraTrackingView.leaving(Joined, new CameraTrackingView(Body, Near));
        for (ChunkPos Each : Switch) {
            Helper.assertTrue(!Body.contains(Each), "switching cameras drops body chunk " + Each);
        }
        Helper.succeed();
    }

    @GameTest(template = Room, timeoutTicks = 20)
    public static void switchPacingResetsEachSession(GameTestHelper Helper) {
        CameraCooldown Pace = new CameraCooldown(6);
        for (int Round = 0; Round < 3; Round++) {
            Pace.reset();
            Helper.assertTrue(Pace.use(), "the first switch of session " + (Round + 1) + " was delayed");
            Helper.assertTrue(!Pace.use(), "a second switch went through inside the gap");
            for (int Tick = 0; Tick < 5; Tick++) {
                Pace.tick();
            }
            Helper.assertTrue(!Pace.ready(), "the switch gap ended early");
            Pace.tick();
            Helper.assertTrue(Pace.use(), "the switch gap never ended");
            for (int Tick = 1; Tick <= 600 * (Round + 1); Tick++) {
                Pace.tick();
                if (Tick % 40 == 0) {
                    Helper.assertTrue(Pace.use(), "a spaced switch was refused");
                }
            }
        }
        Helper.succeed();
    }
}
