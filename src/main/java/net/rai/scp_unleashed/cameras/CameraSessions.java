package net.rai.scp_unleashed.cameras;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.cameras.network.CameraFeedPayload;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CameraSessions {
    public static final TicketType<UUID> ViewTicket = TicketType.create("scp_unleashed_camera_view", UUID::compareTo);
    public static final TicketType<UUID> BodyTicket = TicketType.create("scp_unleashed_camera_body", UUID::compareTo);
    public static final int MaxView = 6;
    public static final int BodyRadius = 3;
    public static final int RefreshTicks = 5;
    public static final double LockSlack = 0.5;
    private static final Map<UUID, CameraSession> Sessions = new ConcurrentHashMap<>();

    public enum Outcome {
        Opened,
        Missing,
        NoSignal,
        Refused,
        NoEntry
    }

    public enum End {
        Left,
        Hurt,
        Tablet,
        Lost,
        Moved,
        Quiet
    }

    private CameraSessions() {
    }

    @Nullable
    public static CameraSession get(UUID Viewer) {
        return Sessions.get(Viewer);
    }

    public static Collection<CameraSession> all() {
        return Sessions.values();
    }

    public static boolean isViewing(ServerPlayer User) {
        return Sessions.containsKey(User.getUUID());
    }

    public static int viewRadius(ServerPlayer User) {
        MinecraftServer Server = User.getServer();
        int Limit = Server == null ? MaxView : Math.max(2, Server.getPlayerList().getViewDistance());
        return Mth.clamp(User.requestedViewDistance(), 2, Math.min(MaxView, Limit));
    }

    public static void hold(ServerLevel World, ChunkPos Centre, int Radius, UUID Key) {
        World.getChunkSource().addRegionTicket(ViewTicket, Centre, Radius, Key);
    }

    public static void release(ServerLevel World, ChunkPos Centre, int Radius, UUID Key) {
        World.getChunkSource().removeRegionTicket(ViewTicket, Centre, Radius, Key);
    }

    public static void holdBody(ServerLevel World, CameraSession Session) {
        World.getChunkSource().addRegionTicket(BodyTicket, Session.body(), BodyRadius, Session.viewer());
    }

    public static void releaseBody(ServerLevel World, CameraSession Session) {
        World.getChunkSource().removeRegionTicket(BodyTicket, Session.body(), BodyRadius, Session.viewer());
    }

    @Nullable
    public static CameraBlockEntity cameraAt(ServerLevel World, BlockPos Pos, boolean Load) {
        if (!World.isInWorldBounds(Pos)) {
            return null;
        }
        if (!Load && !World.isLoaded(Pos)) {
            return null;
        }
        World.getChunk(Pos);
        return World.getBlockEntity(Pos) instanceof CameraBlockEntity Found ? Found : null;
    }

    public static Component label(CameraLinks.Link Entry, int Index) {
        return Component.literal(Entry.Name().isEmpty() ? CameraLinks.fallbackName(Index + 1) : Entry.Name());
    }

    private static void notify(ServerPlayer User, Component Text) {
        User.displayClientMessage(Text, true);
    }

    public static Outcome open(ServerPlayer User, InteractionHand Hand, int Index) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!Stack.is(CameraRegistry.Tablet.get()) || !User.isAlive() || User.isSpectator() || User.isSleeping()) {
            return Outcome.Refused;
        }
        CameraLinks Links = CameraLinks.of(Stack);
        CameraLinks.Link Entry = Links.get(Index);
        if (Entry == null) {
            return Outcome.NoEntry;
        }
        if (Senses.refuseSight(User)) {
            return Outcome.Refused;
        }
        ServerLevel World = User.serverLevel();
        if (!Entry.dimension().equals(World.dimension())) {
            notify(User, Component.translatable("message.scp_unleashed.camera_tablet.no_signal", label(Entry, Index)).withStyle(ChatFormatting.GRAY));
            return Outcome.NoSignal;
        }
        BlockPos Pos = Entry.pos();
        UUID Key = User.getUUID();
        CameraSession Old = Sessions.get(Key);
        ChunkPos Centre = new ChunkPos(Pos);
        int View = viewRadius(User);
        boolean Same = Old != null && Old.dimension().equals(World.dimension()) && Old.centre().equals(Centre) && Old.view() == View;
        if (!Same && World.isInWorldBounds(Pos)) {
            hold(World, Centre, View + 1, Key);
        }
        CameraBlockEntity Camera = cameraAt(World, Pos, true);
        if (Camera == null) {
            if (!Same && World.isInWorldBounds(Pos)) {
                release(World, Centre, View + 1, Key);
            }
            CameraLinks.store(Stack, Links.without(Index));
            notify(User, Component.translatable("message.scp_unleashed.camera_tablet.removed_broken", label(Entry, Index)).withStyle(ChatFormatting.RED));
            return Outcome.Missing;
        }
        BlockPos Previous = null;
        ResourceKey<Level> PreviousDimension = null;
        if (Old != null) {
            Previous = Old.camera();
            PreviousDimension = Old.dimension();
            if (!Same) {
                ServerLevel OldWorld = User.server.getLevel(Old.dimension());
                if (OldWorld != null) {
                    release(OldWorld, Old.centre(), Old.ticketRadius(), Key);
                }
            }
        }
        String Name = Camera.getCameraName();
        if (Name.isEmpty()) {
            Name = Entry.Name().isEmpty() ? CameraLinks.fallbackName(Index + 1) : Entry.Name();
            Camera.setCameraName(Name);
        }
        if (!Name.equals(Entry.Name())) {
            Links = Links.replaced(Index, Entry.named(Name));
            CameraLinks.store(Stack, Links);
        }
        Camera.markLinked();
        CameraSession Session = Old;
        if (Session == null) {
            Session = new CameraSession(Key, User.position(), Hand, World.dimension(), Pos, Centre, View, World.getGameTime());
            Sessions.put(Key, Session);
            holdBody(World, Session);
        } else {
            Session.retarget(World.dimension(), Pos, Centre, View);
            Session.setHand(Hand);
        }
        Session.aimFrom(Camera);
        if (Previous != null && PreviousDimension != null && !(Previous.equals(Pos) && PreviousDimension.equals(World.dimension()))) {
            recount(User.server, PreviousDimension, Previous);
        }
        recount(User.server, World.dimension(), Pos);
        Vec3 Centred = Vec3.atCenterOf(Pos);
        World.playSound(null, Centred.x, Centred.y, Centred.z, CameraRegistry.RecordBeep.get(), SoundSource.BLOCKS, 0.25f, 1.0f);
        send(User, feed(Camera, Name, Index, Links.size()));
        World.getChunkSource().move(User);
        return Outcome.Opened;
    }

    public static CameraFeedPayload feed(CameraBlockEntity Camera, String Name, int Index, int Count) {
        CameraMount Mount = Camera.mount();
        return new CameraFeedPayload(true, Camera.getBlockPos(), Mount.Ceiling(), Mount.Facing(), Camera.getPan(), Camera.getTilt(), Name, Index, Count);
    }

    private static void send(ServerPlayer User, CameraFeedPayload Payload) {
        if (User.connection != null && User.connection.hasChannel(CameraFeedPayload.PayloadType)) {
            PacketDistributor.sendToPlayer(User, Payload);
        }
    }

    public static void cycle(ServerPlayer User, int Step) {
        CameraSession Session = Sessions.get(User.getUUID());
        if (Session == null) {
            return;
        }
        ItemStack Stack = User.getItemInHand(Session.hand());
        if (!Stack.is(CameraRegistry.Tablet.get())) {
            end(User, End.Tablet);
            return;
        }
        int Sign = Step < 0 ? -1 : 1;
        int Tries = CameraLinks.MaxLinks + 1;
        while (Tries-- > 0) {
            CameraLinks Links = CameraLinks.of(Stack);
            int Count = Links.size();
            if (Count <= 1) {
                notify(User, Component.translatable("message.scp_unleashed.camera_tablet.no_other").withStyle(ChatFormatting.GRAY));
                return;
            }
            int Current = Links.indexOf(GlobalPos.of(Session.dimension(), Session.camera()));
            int Next = Current < 0 ? 0 : Math.floorMod(Current + Sign, Count);
            for (int Offset = 0; Offset < Count; Offset++) {
                int Candidate = Math.floorMod(Next + Offset * Sign, Count);
                CameraLinks.Link Entry = Links.get(Candidate);
                if (Candidate != Current && Entry != null && Entry.dimension().equals(User.level().dimension())) {
                    Next = Candidate;
                    break;
                }
            }
            if (Next == Current) {
                notify(User, Component.translatable("message.scp_unleashed.camera_tablet.no_other").withStyle(ChatFormatting.GRAY));
                return;
            }
            Outcome Result = open(User, Session.hand(), Next);
            if (Result != Outcome.Missing) {
                return;
            }
        }
    }

    public static void look(ServerPlayer User, float Pan, float Tilt) {
        CameraSession Session = Sessions.get(User.getUUID());
        if (Session == null || !Float.isFinite(Pan) || !Float.isFinite(Tilt)) {
            return;
        }
        ServerLevel World = User.serverLevel();
        if (!World.dimension().equals(Session.dimension()) || !World.isLoaded(Session.camera())) {
            return;
        }
        if (World.getBlockEntity(Session.camera()) instanceof CameraBlockEntity Camera) {
            Camera.aim(Pan, Tilt);
            Session.aimFrom(Camera);
            User.resetLastActionTime();
        }
    }

    public static void end(ServerPlayer User, End Reason) {
        CameraSession Session = Sessions.remove(User.getUUID());
        if (Session == null) {
            return;
        }
        cleanup(User.server, Session);
        send(User, CameraFeedPayload.Closed);
        String Key = switch (Reason) {
            case Hurt -> "message.scp_unleashed.camera_tablet.cut_hurt";
            case Tablet -> "message.scp_unleashed.camera_tablet.cut_tablet";
            case Lost -> "message.scp_unleashed.camera_tablet.cut_lost";
            case Moved -> "message.scp_unleashed.camera_tablet.cut_moved";
            default -> null;
        };
        if (Key != null) {
            notify(User, Component.translatable(Key).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void cleanup(MinecraftServer Server, CameraSession Session) {
        ServerLevel World = Server.getLevel(Session.dimension());
        if (World != null) {
            release(World, Session.centre(), Session.ticketRadius(), Session.viewer());
        }
        ServerLevel Home = Server.getLevel(Session.home());
        if (Home != null) {
            releaseBody(Home, Session);
        }
        recount(Server, Session.dimension(), Session.camera());
    }

    public static int viewersOf(ResourceKey<Level> Dimension, BlockPos Pos) {
        int Count = 0;
        for (CameraSession Session : Sessions.values()) {
            if (Session.dimension().equals(Dimension) && Session.camera().equals(Pos)) {
                Count++;
            }
        }
        return Count;
    }

    private static void recount(MinecraftServer Server, ResourceKey<Level> Dimension, BlockPos Pos) {
        ServerLevel World = Server.getLevel(Dimension);
        if (World != null && World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof CameraBlockEntity Camera) {
            Camera.setViewers(viewersOf(Dimension, Pos));
        }
    }

    @Nullable
    private static End check(ServerPlayer User, CameraSession Session) {
        if (!User.isAlive() || User.isRemoved() || User.isSpectator() || User.isSleeping() || !User.level().dimension().equals(Session.dimension())) {
            return End.Quiet;
        }
        if (!User.getItemInHand(Session.hand()).is(CameraRegistry.Tablet.get())) {
            return End.Tablet;
        }
        if (User.position().distanceToSqr(Session.lock()) > LockSlack * LockSlack) {
            return End.Moved;
        }
        ServerLevel World = User.serverLevel();
        if (World.isLoaded(Session.camera())) {
            if (!(World.getBlockEntity(Session.camera()) instanceof CameraBlockEntity Camera)) {
                return End.Lost;
            }
            Session.aimFrom(Camera);
        }
        return null;
    }

    public static void tick(MinecraftServer Server) {
        if (Sessions.isEmpty()) {
            return;
        }
        int Now = Server.getTickCount();
        for (CameraSession Session : List.copyOf(Sessions.values())) {
            ServerPlayer User = Server.getPlayerList().getPlayer(Session.viewer());
            if (User == null) {
                if (Sessions.remove(Session.viewer(), Session)) {
                    cleanup(Server, Session);
                }
                continue;
            }
            End Reason = check(User, Session);
            if (Reason != null) {
                end(User, Reason);
                continue;
            }
            if ((Now + Session.viewer().hashCode()) % RefreshTicks == 0) {
                User.serverLevel().getChunkSource().move(User);
            }
        }
    }

    public static void clear(MinecraftServer Server) {
        for (CameraSession Session : List.copyOf(Sessions.values())) {
            Sessions.remove(Session.viewer());
            cleanup(Server, Session);
        }
    }

    public static void edit(ServerPlayer User, InteractionHand Hand, int Index, boolean Remove, String Wanted) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!Stack.is(CameraRegistry.Tablet.get())) {
            return;
        }
        CameraLinks Links = CameraLinks.of(Stack);
        CameraLinks.Link Entry = Links.get(Index);
        if (Entry == null) {
            return;
        }
        if (Remove) {
            CameraLinks.store(Stack, Links.without(Index));
            notify(User, Component.translatable("message.scp_unleashed.camera_tablet.unlinked", label(Entry, Index)).withStyle(ChatFormatting.GRAY));
            return;
        }
        String Name = CameraLinks.clean(Wanted);
        if (Name.isEmpty()) {
            return;
        }
        CameraLinks.store(Stack, Links.replaced(Index, Entry.named(Name)));
        ServerLevel World = User.server.getLevel(Entry.dimension());
        if (World != null && World.isLoaded(Entry.pos()) && World.getBlockEntity(Entry.pos()) instanceof CameraBlockEntity Camera) {
            Camera.setCameraName(Name);
            CameraSession Session = Sessions.get(User.getUUID());
            if (Session != null && Session.camera().equals(Entry.pos()) && Session.dimension().equals(Entry.dimension())) {
                send(User, feed(Camera, Name, Index, Links.size()));
            }
        }
    }

    public static int nextNumber(CameraLinks Links) {
        for (int Number = 1; Number <= CameraLinks.MaxLinks * 4; Number++) {
            String Candidate = CameraLinks.fallbackName(Number);
            boolean Taken = false;
            for (CameraLinks.Link Entry : Links.Entries()) {
                if (Entry.Name().equals(Candidate)) {
                    Taken = true;
                    break;
                }
            }
            if (!Taken) {
                return Number;
            }
        }
        return Links.size() + 1;
    }

    public static InteractionResult link(ServerPlayer User, ItemStack Stack, ServerLevel World, BlockPos Pos) {
        if (!(World.getBlockEntity(Pos) instanceof CameraBlockEntity Camera)) {
            return InteractionResult.PASS;
        }
        GlobalPos Where = GlobalPos.of(World.dimension(), Pos.immutable());
        CameraLinks Links = CameraLinks.of(Stack);
        int Existing = Links.indexOf(Where);
        if (Existing >= 0) {
            CameraLinks.Link Entry = Links.get(Existing);
            notify(User, Component.translatable("message.scp_unleashed.camera_tablet.already", label(Entry, Existing)).withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        if (Links.isFull()) {
            notify(User, Component.translatable("message.scp_unleashed.camera_tablet.full", CameraLinks.MaxLinks).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        String Name = Camera.hasCameraName() ? Camera.getCameraName() : CameraLinks.fallbackName(nextNumber(Links));
        Camera.setCameraName(Name);
        Camera.markLinked();
        CameraLinks Next = Links.with(new CameraLinks.Link(Where, Name));
        CameraLinks.store(Stack, Next);
        notify(User, Component.translatable("message.scp_unleashed.camera_tablet.linked", Name, Next.size(), CameraLinks.MaxLinks).withStyle(ChatFormatting.GREEN));
        World.playSound(null, User.getX(), User.getY(), User.getZ(), CameraRegistry.TabletLink.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
        World.playSound(null, User.getX(), User.getY(), User.getZ(), CameraRegistry.RecordBeep.get(), SoundSource.PLAYERS, 0.35f, 1.2f);
        return InteractionResult.SUCCESS;
    }
}
