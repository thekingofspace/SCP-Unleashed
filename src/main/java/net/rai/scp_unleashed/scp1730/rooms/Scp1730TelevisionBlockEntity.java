package net.rai.scp_unleashed.scp1730.rooms;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730Symbols;
import net.rai.scp_unleashed.scp1730.block.Scp1730SealBlock;
import net.rai.scp_unleashed.scp1730.rooms.client.Scp1730TelevisionClient;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp1730TelevisionBlockEntity extends BlockEntity {
    public static final int Idle = 0;
    public static final int Warming = 1;
    public static final int Showing = 2;
    public static final int StaticTicks = 20;
    public static final int ShowTicks = 560;
    public static final int WindTicks = 70;
    public static final int GraceTicks = 30;
    public static final int LethalTicks = 8;
    public static final int CooldownTicks = 12000;
    public static final int CheckEvery = 4;
    public static final double ScreenInset = 0.44;
    public static final double ScreenHeight = 0.5;
    public static final double ScreenHalf = 0.36;
    public static final double MinCone = 6.0;
    public static final double LookRange = 24.0;
    private int Phase = Idle;
    private long PhaseStart;
    private long ReadyAt;
    private final List<BlockPos> Sealed = new ArrayList<>();
    private final Map<UUID, Integer> Watched = new HashMap<>();
    @Nullable
    private Scp1730SpecialRoom Override;
    @Nullable
    private Scp1730SpecialRoom Synced;

    public Scp1730TelevisionBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp1730RoomsRegistry.TelevisionEntity.get(), Pos, State);
    }

    public void override(@Nullable Scp1730SpecialRoom Room) {
        Override = Room;
        setChanged();
    }

    @Nullable
    public Scp1730SpecialRoom room() {
        if (Override != null) {
            return Override;
        }
        if (level == null) {
            return null;
        }
        if (level.isClientSide) {
            return Synced;
        }
        return Scp1730SpecialRooms.at(level, Vec3.atCenterOf(worldPosition), Scp1730RoomKind.Television);
    }

    public int phase() {
        return Phase;
    }

    public long phaseStart() {
        return PhaseStart;
    }

    public long readyAt() {
        return ReadyAt;
    }

    public int watched(Player User) {
        return Watched.getOrDefault(User.getUUID(), 0);
    }

    public List<BlockPos> sealed() {
        return Sealed;
    }

    public Direction facing() {
        return getBlockState().getValue(Scp1730TelevisionBlock.Facing);
    }

    public Vec3 screen() {
        Direction Front = facing();
        return new Vec3(worldPosition.getX() + 0.5 + Front.getStepX() * ScreenInset, worldPosition.getY() + ScreenHeight, worldPosition.getZ() + 0.5 + Front.getStepZ() * ScreenInset);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp1730TelevisionBlockEntity Entity) {
        if (World instanceof ServerLevel Server) {
            Entity.tick(Server);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp1730TelevisionBlockEntity Entity) {
        Scp1730TelevisionClient.tick(Entity);
    }

    private void tick(ServerLevel World) {
        long Now = World.getGameTime();
        if (Phase == Idle) {
            if (Now < ReadyAt || (Now + worldPosition.hashCode()) % CheckEvery != 0) {
                return;
            }
            Scp1730SpecialRoom Room = room();
            if (Room == null) {
                return;
            }
            for (ServerPlayer User : World.players()) {
                if (eligible(User) && Room.within(User.getBoundingBox())) {
                    begin(World, Room, Now);
                    return;
                }
            }
            return;
        }
        Scp1730SpecialRoom Room = room();
        if (Room == null || occupants(World, Room).isEmpty()) {
            finish(World, Now);
            return;
        }
        seal(World, Room);
        long Age = Now - PhaseStart;
        if (Phase == Warming) {
            if (Age >= StaticTicks) {
                Phase = Showing;
                PhaseStart = Now;
                screen(World, Scp1730TelevisionBlock.Screen.Show);
                sync(World);
            }
            return;
        }
        if (Age >= ShowTicks) {
            finish(World, Now);
            return;
        }
        if (Age < GraceTicks) {
            return;
        }
        Vec3 Screen = screen();
        for (ServerPlayer User : occupants(World, Room)) {
            if (!eligible(User) || protectedFrom(User) || !watching(World, User, Screen)) {
                continue;
            }
            int Count = Watched.merge(User.getUUID(), 1, Integer::sum);
            if (Count >= LethalTicks) {
                Watched.remove(User.getUUID());
                User.hurt(source(World), Float.MAX_VALUE);
            }
        }
    }

    public static boolean eligible(Player User) {
        return User.isAlive() && !User.isSpectator() && !User.isCreative();
    }

    public static boolean protectedFrom(Player User) {
        return Senses.isBlindfolded(User) || AntiMemetic.isProtected(User);
    }

    public boolean watching(Level World, Player User, Vec3 Screen) {
        Direction Front = facing();
        Vec3 Normal = new Vec3(Front.getStepX(), 0.0, Front.getStepZ());
        Vec3 Eye = User.getEyePosition();
        if (Eye.subtract(Screen).dot(Normal) <= 0.05) {
            return false;
        }
        double Distance = Eye.distanceTo(Screen);
        if (Distance > LookRange) {
            return false;
        }
        double Cone = Math.max(MinCone, Math.toDegrees(Math.atan2(ScreenHalf, Math.max(0.1, Distance))));
        if (Scp1730Symbols.angleTo(Eye, User.getViewVector(1.0f), Screen) > Cone) {
            return false;
        }
        return Scp1730Symbols.clearSight(World, User, Eye, Screen.add(Normal.scale(0.06)), worldPosition, null);
    }

    public List<ServerPlayer> occupants(ServerLevel World, Scp1730SpecialRoom Room) {
        List<ServerPlayer> Found = new ArrayList<>();
        AABB Inner = Room.inner();
        for (ServerPlayer User : World.players()) {
            if (User.isAlive() && !User.isSpectator() && User.getBoundingBox().intersects(Inner)) {
                Found.add(User);
            }
        }
        return Found;
    }

    private void begin(ServerLevel World, Scp1730SpecialRoom Room, long Now) {
        Phase = Warming;
        PhaseStart = Now;
        Watched.clear();
        seal(World, Room);
        Vec3 At = Vec3.atCenterOf(worldPosition);
        World.playSound(null, At.x, At.y, At.z, Scp1730RoomsRegistry.TelevisionClick.get(), SoundSource.BLOCKS, 1.0f, 0.9f);
        World.playSound(null, At.x, At.y, At.z, Scp1730RoomsRegistry.TelevisionStatic.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        screen(World, Scp1730TelevisionBlock.Screen.Noise);
        sync(World);
    }

    private void finish(ServerLevel World, long Now) {
        boolean Ran = Phase != Idle;
        Phase = Idle;
        PhaseStart = Now;
        ReadyAt = Now + CooldownTicks;
        Watched.clear();
        unseal(World);
        if (Ran) {
            Vec3 At = Vec3.atCenterOf(worldPosition);
            World.playSound(null, At.x, At.y, At.z, Scp1730RoomsRegistry.TelevisionClick.get(), SoundSource.BLOCKS, 0.8f, 0.6f);
        }
        screen(World, Scp1730TelevisionBlock.Screen.Off);
        sync(World);
    }

    public void forceFinish(ServerLevel World) {
        finish(World, World.getGameTime());
        ReadyAt = 0L;
    }

    private void seal(ServerLevel World, Scp1730SpecialRoom Room) {
        boolean Slammed = false;
        for (Scp1730SpecialRoom.Door Door : Room.Doors()) {
            Direction.Axis Axis = Door.Dir() % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z;
            for (int Side = -1; Side <= 1; Side++) {
                for (int Level = 1; Level <= 3; Level++) {
                    BlockPos Pos = new BlockPos(Door.X() + Door.sideX() * Side, Room.Floor() + Level, Door.Z() + Door.sideZ() * Side);
                    if (Sealed.contains(Pos) || !World.isLoaded(Pos)) {
                        continue;
                    }
                    BlockState Here = World.getBlockState(Pos);
                    if (!Here.isAir() && !Here.canBeReplaced()) {
                        continue;
                    }
                    if (!World.getEntitiesOfClass(LivingEntity.class, new AABB(Pos)).isEmpty()) {
                        continue;
                    }
                    World.setBlock(Pos, Scp1730Registry.Seal.get().defaultBlockState().setValue(Scp1730SealBlock.Axis, Axis), Block.UPDATE_ALL);
                    Sealed.add(Pos.immutable());
                    Slammed = true;
                }
            }
            if (Slammed) {
                Vec3 At = new Vec3(Door.X() + 0.5, Room.Floor() + 2, Door.Z() + 0.5);
                World.playSound(null, At.x, At.y, At.z, Scp1730RoomsRegistry.RoomSeal.get(), SoundSource.BLOCKS, 1.2f, 0.8f);
                World.playSound(null, At.x, At.y, At.z, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35f, 0.5f);
                Slammed = false;
            }
        }
        setChanged();
    }

    private void unseal(ServerLevel World) {
        for (BlockPos Pos : Sealed) {
            if (World.isLoaded(Pos) && World.getBlockState(Pos).is(Scp1730Registry.Seal.get())) {
                World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        if (!Sealed.isEmpty()) {
            BlockPos First = Sealed.getFirst();
            World.playSound(null, First, Scp1730Registry.SealOpen.get(), SoundSource.BLOCKS, 0.9f, 1.1f);
        }
        Sealed.clear();
        setChanged();
    }

    private void screen(ServerLevel World, Scp1730TelevisionBlock.Screen Wanted) {
        BlockState State = World.getBlockState(worldPosition);
        if (State.hasProperty(Scp1730TelevisionBlock.ScreenState) && State.getValue(Scp1730TelevisionBlock.ScreenState) != Wanted) {
            World.setBlock(worldPosition, State.setValue(Scp1730TelevisionBlock.ScreenState, Wanted), Block.UPDATE_ALL);
        }
    }

    private void sync(ServerLevel World) {
        setChanged();
        World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public static DamageSource source(ServerLevel World) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730RoomsRegistry.BroadcastDamage));
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.saveAdditional(Tag, Lookup);
        Tag.putInt("Phase", Phase);
        Tag.putLong("PhaseStart", PhaseStart);
        Tag.putLong("ReadyAt", ReadyAt);
        long[] Seals = new long[Sealed.size()];
        for (int Index = 0; Index < Seals.length; Index++) {
            Seals[Index] = Sealed.get(Index).asLong();
        }
        Tag.putLongArray("Sealed", Seals);
        if (Override != null) {
            Tag.put("Room", writeRoom(Override));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.loadAdditional(Tag, Lookup);
        Phase = Tag.getInt("Phase");
        PhaseStart = Tag.getLong("PhaseStart");
        ReadyAt = Tag.getLong("ReadyAt");
        Sealed.clear();
        for (long Each : Tag.getLongArray("Sealed")) {
            Sealed.add(BlockPos.of(Each));
        }
        Override = Tag.contains("Room") ? readRoom(Tag.getCompound("Room")) : null;
        if (Tag.contains("Synced")) {
            Synced = readRoom(Tag.getCompound("Synced"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Phase", Phase);
        Tag.putLong("PhaseStart", PhaseStart);
        Tag.putLong("ReadyAt", ReadyAt);
        Scp1730SpecialRoom Room = room();
        if (Room != null) {
            Tag.put("Synced", writeRoom(Room));
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static CompoundTag writeRoom(Scp1730SpecialRoom Room) {
        CompoundTag Tag = new CompoundTag();
        Tag.putIntArray("Box", new int[]{Room.MinX(), Room.MinZ(), Room.MaxX(), Room.MaxZ(), Room.Floor(), Room.Height()});
        int[] Doors = new int[Room.Doors().size() * 3];
        for (int Index = 0; Index < Room.Doors().size(); Index++) {
            Scp1730SpecialRoom.Door Door = Room.Doors().get(Index);
            Doors[Index * 3] = Door.X();
            Doors[Index * 3 + 1] = Door.Z();
            Doors[Index * 3 + 2] = Door.Dir();
        }
        Tag.putIntArray("Doors", Doors);
        return Tag;
    }

    @Nullable
    public static Scp1730SpecialRoom readRoom(CompoundTag Tag) {
        int[] Box = Tag.getIntArray("Box");
        if (Box.length < 6) {
            return null;
        }
        int[] Doors = Tag.getIntArray("Doors");
        List<Scp1730SpecialRoom.Door> Made = new ArrayList<>();
        for (int Index = 0; Index + 2 < Doors.length; Index += 3) {
            Made.add(new Scp1730SpecialRoom.Door(Doors[Index], Doors[Index + 1], Doors[Index + 2] & 3));
        }
        return new Scp1730SpecialRoom(Scp1730RoomKind.Television, Box[0], Box[1], Box[2], Box[3], Box[4], Box[5], List.copyOf(Made));
    }
}
