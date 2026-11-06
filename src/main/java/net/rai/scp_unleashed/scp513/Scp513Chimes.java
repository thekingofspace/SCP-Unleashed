package net.rai.scp_unleashed.scp513;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.gel.GelPacked;
import net.rai.scp_unleashed.storage.gel.GelStorageRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp513Chimes {
    public static final int CheckEvery = 20;
    public static final int MinGap = 3600;
    public static final int MaxGap = 9600;
    public static final int TapeLife = 36000;
    public static final int ChunksPerTick = 16;
    public static final int FollowDepth = 3;
    public static final int FollowLimit = 12;
    public static final float ThunkVolume = 0.7f;
    public static final float TapeVolume = 0.8f;
    private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> Known = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> PendingChunks = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> PendingSpots = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<UUID>> Carts = new HashMap<>();

    public enum Pulse {
        Quiet,
        Taped,
        Loosened,
        Ring
    }

    private Scp513Chimes() {
    }

    public static int gap(RandomSource Random) {
        return MinGap + Random.nextInt(MaxGap - MinGap + 1);
    }

    public static Pulse pulse(ServerLevel World, ItemStack Stack) {
        if (!Scp513BellItem.is(Stack)) {
            return Pulse.Quiet;
        }
        long Now = World.getGameTime();
        Long Tape = Stack.get(Scp513Registry.Tape.get());
        if (Tape != null) {
            if (Tape <= 0L) {
                Stack.set(Scp513Registry.Tape.get(), Now + TapeLife);
                return Pulse.Taped;
            }
            if (Now < Tape) {
                return Pulse.Taped;
            }
            Stack.remove(Scp513Registry.Tape.get());
            Stack.set(Scp513Registry.NextRing.get(), Now + gap(World.random));
            return Pulse.Loosened;
        }
        Long Next = Stack.get(Scp513Registry.NextRing.get());
        if (Next == null) {
            Stack.set(Scp513Registry.NextRing.get(), Now + gap(World.random));
            return Pulse.Quiet;
        }
        if (Now < Next) {
            return Pulse.Quiet;
        }
        Stack.set(Scp513Registry.NextRing.get(), Now + gap(World.random));
        return Pulse.Ring;
    }

    public static boolean hold(ServerLevel World, ItemStack Stack) {
        if (!Scp513BellItem.is(Stack)) {
            return false;
        }
        Long Next = Stack.get(Scp513Registry.NextRing.get());
        if (Next != null && Next > World.getGameTime()) {
            return false;
        }
        Stack.set(Scp513Registry.NextRing.get(), World.getGameTime() + gap(World.random));
        return true;
    }

    public static boolean isBound(ServerLevel World, ItemStack Stack) {
        Long Tape = Stack.get(Scp513Registry.Tape.get());
        return Tape != null && (Tape <= 0L || World.getGameTime() < Tape);
    }

    public static void strike(ServerLevel World, Player User, ItemStack Stack) {
        Vec3 Source = User.getEyePosition();
        if (isBound(World, Stack)) {
            if (Stack.getOrDefault(Scp513Registry.Tape.get(), 0L) <= 0L) {
                Stack.set(Scp513Registry.Tape.get(), World.getGameTime() + TapeLife);
            }
            Scp513Registry.playLayers(World, Source, Scp513Registry.ThunkLayers, SoundSource.PLAYERS, ThunkVolume, 0.9f + World.random.nextFloat() * 0.15f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp513.taped"), true);
            return;
        }
        if (Stack.has(Scp513Registry.Tape.get())) {
            Stack.remove(Scp513Registry.Tape.get());
            Scp513Registry.playLayers(World, Source, Scp513Registry.TapeLayers, SoundSource.PLAYERS, TapeVolume, 1.0f);
        }
        Stack.set(Scp513Registry.NextRing.get(), World.getGameTime() + gap(World.random));
        Scp513Ring.ring(World, Source, User);
    }

    public static void answer(ServerLevel World, Pulse Result, Vec3 Source, @Nullable Entity Holder) {
        if (Result == Pulse.Ring) {
            Scp513Ring.ring(World, Source, Holder);
        } else if (Result == Pulse.Loosened) {
            Scp513Registry.playLayers(World, Source, Scp513Registry.TapeLayers, SoundSource.PLAYERS, TapeVolume, 0.95f + World.random.nextFloat() * 0.1f);
            if (Holder instanceof Player User) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.scp513.tape_gives"), true);
            }
        }
    }

    public static boolean holdsBell(ItemStack Stack) {
        if (Scp513BellItem.is(Stack)) {
            return true;
        }
        GelPacked Packed = Stack.get(GelStorageRegistry.Packed.get());
        if (Packed != null && Scp513BellItem.is(Packed.Contents())) {
            return true;
        }
        ItemContainerContents Contents = Stack.get(DataComponents.CONTAINER);
        return Contents != null && Contents.nonEmptyStream().anyMatch(Scp513BellItem::is);
    }

    private static boolean visit(ServerLevel World, ItemStack Stack, Vec3 Source, @Nullable Entity Holder) {
        if (Scp513BellItem.is(Stack)) {
            answer(World, pulse(World, Stack), Source, Holder);
            return true;
        }
        GelPacked Packed = Stack.get(GelStorageRegistry.Packed.get());
        if (Packed != null && Scp513BellItem.is(Packed.Contents())) {
            GelPacked Aged = Packed.aged(CheckEvery);
            ItemStack Inner = Aged.Contents();
            if (Aged.sealed()) {
                hold(World, Inner);
            } else {
                answer(World, pulse(World, Inner), Source, Holder);
            }
            Stack.set(GelStorageRegistry.Packed.get(), Aged.withContents(Inner));
            return true;
        }
        ItemContainerContents Contents = Stack.get(DataComponents.CONTAINER);
        if (Contents == null || Contents.nonEmptyStream().noneMatch(Scp513BellItem::is)) {
            return false;
        }
        NonNullList<ItemStack> Inner = NonNullList.withSize(Contents.getSlots(), ItemStack.EMPTY);
        Contents.copyInto(Inner);
        for (ItemStack Nested : Inner) {
            if (Scp513BellItem.is(Nested)) {
                answer(World, pulse(World, Nested), Source, Holder);
            }
        }
        Stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(Inner));
        return true;
    }

    public static boolean tickCarrier(ServerPlayer User) {
        if (!(User.level() instanceof ServerLevel World) || User.isSpectator()) {
            return false;
        }
        Vec3 Source = User.position().add(0.0, User.getBbHeight() * 0.55, 0.0);
        boolean Found = false;
        for (int Index = 0; Index < User.getInventory().getContainerSize(); Index++) {
            Found |= visit(World, User.getInventory().getItem(Index), Source, User);
        }
        if (User.containerMenu != null && !User.containerMenu.getCarried().isEmpty()) {
            Found |= visit(World, User.containerMenu.getCarried(), Source, User);
        }
        for (int Index = 0; Index < User.getEnderChestInventory().getContainerSize(); Index++) {
            Found |= visit(World, User.getEnderChestInventory().getItem(Index), Source, User);
        }
        return Found;
    }

    public static void tickLoose(ServerLevel World, ItemEntity Dropped) {
        ItemStack Stack = Dropped.getItem();
        Vec3 Source = Dropped.position().add(0.0, 0.25, 0.0);
        if (visit(World, Stack, Source, null)) {
            Dropped.setItem(Stack.copy());
        }
    }

    public static boolean tickContainer(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || !(World.getBlockEntity(Pos) instanceof Container Box) || !isHolder(World.getBlockEntity(Pos))) {
            return false;
        }
        Vec3 Source = Vec3.atCenterOf(Pos);
        boolean Found = false;
        for (int Index = 0; Index < Box.getContainerSize(); Index++) {
            Found |= visit(World, Box.getItem(Index), Source, null);
        }
        if (Found) {
            Box.setChanged();
        }
        return Found;
    }

    public static boolean tickCart(ServerLevel World, Entity Cart) {
        if (!(Cart instanceof ContainerEntity Box) || !Cart.isAlive() || Box.getLootTable() != null) {
            return false;
        }
        Vec3 Source = Cart.position().add(0.0, 0.5, 0.0);
        boolean Found = false;
        for (int Index = 0; Index < Box.getContainerSize(); Index++) {
            Found |= visit(World, Box.getItem(Index), Source, null);
        }
        if (Found) {
            Box.setChanged();
        }
        return Found;
    }

    public static boolean isHolder(@Nullable BlockEntity Tile) {
        if (!(Tile instanceof Container Box) || Tile instanceof StorageContainerBlockEntity || Tile.isRemoved()) {
            return false;
        }
        if (Tile instanceof RandomizableContainer Loot && Loot.getLootTable() != null) {
            return false;
        }
        for (int Index = 0; Index < Box.getContainerSize(); Index++) {
            if (holdsBell(Box.getItem(Index))) {
                return true;
            }
        }
        return false;
    }

    private static Map<Long, Set<BlockPos>> known(ServerLevel World) {
        return Known.computeIfAbsent(World.dimension(), Key -> new HashMap<>());
    }

    public static boolean isTracked(ServerLevel World, BlockPos Pos) {
        Set<BlockPos> Spots = known(World).get(ChunkPos.asLong(Pos));
        return Spots != null && Spots.contains(Pos);
    }

    public static boolean isTrackedCart(ServerLevel World, Entity Cart) {
        Set<UUID> Tracked = Carts.get(World.dimension());
        return Tracked != null && Tracked.contains(Cart.getUUID());
    }

    public static int trackedCount(ServerLevel World) {
        return known(World).values().stream().mapToInt(Set::size).sum();
    }

    private static void track(ServerLevel World, BlockPos Pos) {
        known(World).computeIfAbsent(ChunkPos.asLong(Pos), Key -> new HashSet<>()).add(Pos.immutable());
    }

    private static void forget(ServerLevel World, BlockPos Pos) {
        Map<Long, Set<BlockPos>> Spots = known(World);
        long Key = ChunkPos.asLong(Pos);
        Set<BlockPos> Inside = Spots.get(Key);
        if (Inside != null) {
            Inside.remove(Pos);
            if (Inside.isEmpty()) {
                Spots.remove(Key);
            }
        }
    }

    public static boolean refresh(ServerLevel World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return false;
        }
        if (isHolder(World.getBlockEntity(Pos))) {
            track(World, Pos);
            return true;
        }
        forget(World, Pos);
        return false;
    }

    public static int scanChunk(ServerLevel World, LevelChunk Chunk) {
        int Found = 0;
        for (BlockEntity Tile : List.copyOf(Chunk.getBlockEntities().values())) {
            if (isHolder(Tile)) {
                track(World, Tile.getBlockPos());
                Found++;
            }
        }
        return Found;
    }

    public static void trackCart(ServerLevel World, Entity Cart) {
        Carts.computeIfAbsent(World.dimension(), Key -> new HashSet<>()).add(Cart.getUUID());
    }

    private static void follow(ServerLevel World, BlockPos Start) {
        ArrayDeque<BlockPos> Queue = new ArrayDeque<>();
        Set<BlockPos> Seen = new HashSet<>();
        Queue.add(Start);
        Seen.add(Start);
        int Depth = 0;
        while (!Queue.isEmpty() && Depth < FollowDepth && Seen.size() < FollowLimit) {
            int Width = Queue.size();
            for (int Step = 0; Step < Width; Step++) {
                BlockPos Here = Queue.poll();
                for (Direction Side : Direction.values()) {
                    BlockPos Next = Here.relative(Side);
                    if (!Seen.add(Next) || !World.isLoaded(Next) || !(World.getBlockEntity(Next) instanceof Container)) {
                        continue;
                    }
                    if (!refresh(World, Next)) {
                        Queue.add(Next);
                    }
                }
            }
            Depth++;
        }
    }

    public static void sweep(ServerLevel World) {
        List<BlockPos> Spots = new ArrayList<>();
        for (Set<BlockPos> Inside : known(World).values()) {
            Spots.addAll(Inside);
        }
        for (BlockPos Pos : Spots) {
            if (!World.isLoaded(Pos)) {
                continue;
            }
            if (!tickContainer(World, Pos)) {
                forget(World, Pos);
                follow(World, Pos);
            }
        }
        Set<UUID> Tracked = Carts.get(World.dimension());
        if (Tracked != null) {
            Iterator<UUID> Each = Tracked.iterator();
            while (Each.hasNext()) {
                Entity Cart = World.getEntity(Each.next());
                if (Cart == null || !tickCart(World, Cart)) {
                    Each.remove();
                }
            }
        }
    }

    private static void drain(ServerLevel World) {
        Set<Long> Chunks = PendingChunks.get(World.dimension());
        if (Chunks != null && !Chunks.isEmpty()) {
            Iterator<Long> Each = Chunks.iterator();
            int Done = 0;
            while (Each.hasNext() && Done < ChunksPerTick) {
                long Key = Each.next();
                Each.remove();
                Done++;
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkPos.getX(Key), ChunkPos.getZ(Key));
                if (Chunk != null) {
                    scanChunk(World, Chunk);
                }
            }
        }
        Set<BlockPos> Spots = PendingSpots.remove(World.dimension());
        if (Spots != null) {
            for (BlockPos Pos : Spots) {
                refresh(World, Pos);
            }
        }
    }

    private static void queueSpot(ServerLevel World, BlockPos Pos) {
        PendingSpots.computeIfAbsent(World.dimension(), Key -> new LinkedHashSet<>()).add(Pos.immutable());
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getChunk() instanceof LevelChunk Chunk && !Event.isNewChunk()) {
            PendingChunks.computeIfAbsent(World.dimension(), Key -> new LinkedHashSet<>()).add(Chunk.getPos().toLong());
        }
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            long Key = Event.getChunk().getPos().toLong();
            known(World).remove(Key);
            Set<Long> Chunks = PendingChunks.get(World.dimension());
            if (Chunks != null) {
                Chunks.remove(Key);
            }
        }
    }

    @SubscribeEvent
    public static void levelUnloaded(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Known.remove(World.dimension());
            PendingChunks.remove(World.dimension());
            PendingSpots.remove(World.dimension());
            Carts.remove(World.dimension());
        }
    }

    @SubscribeEvent
    public static void containerClosed(PlayerContainerEvent.Close Event) {
        if (!(Event.getEntity().level() instanceof ServerLevel World)) {
            return;
        }
        Set<Object> Seen = new HashSet<>();
        for (Slot Each : Event.getContainer().slots) {
            Container Box = Each.container;
            if (Box == null || Box == Event.getEntity().getInventory() || !Seen.add(Box)) {
                continue;
            }
            if (Box instanceof BlockEntity Tile && Tile.getLevel() == World) {
                queueSpot(World, Tile.getBlockPos());
            } else if (Box instanceof ContainerEntity && Box instanceof Entity Cart && Cart.level() == World) {
                if (tickCart(World, Cart)) {
                    trackCart(World, Cart);
                }
            }
        }
    }

    @SubscribeEvent
    public static void blockPlaced(BlockEvent.EntityPlaceEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            queueSpot(World, Event.getPos());
        }
    }

    @SubscribeEvent
    public static void entityLeft(EntityLeaveLevelEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getEntity() instanceof ItemEntity Dropped && holdsBell(Dropped.getItem())
                && Dropped.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
            BlockPos Pos = Dropped.blockPosition();
            queueSpot(World, Pos);
            queueSpot(World, Pos.below());
        }
    }

    @SubscribeEvent
    public static void entityJoined(EntityJoinLevelEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getEntity() instanceof ContainerEntity Box && Box.getLootTable() == null) {
            for (int Index = 0; Index < Box.getContainerSize(); Index++) {
                if (holdsBell(Box.getItem(Index))) {
                    trackCart(World, Event.getEntity());
                    return;
                }
            }
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        drain(World);
        if (World.getGameTime() % CheckEvery == 0L) {
            sweep(World);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && (User.tickCount + User.getId()) % CheckEvery == 0) {
            tickCarrier(User);
        }
    }
}
