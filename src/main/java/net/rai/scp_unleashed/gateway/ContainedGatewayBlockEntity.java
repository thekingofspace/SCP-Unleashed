package net.rai.scp_unleashed.gateway;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.gateway.network.GatewayPromptPayload;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class ContainedGatewayBlockEntity extends BlockEntity implements WireDevice {
    public static final int StandbyDraw = 12;
    public static final int OpenDraw = 160;
    public static final int PowerGrace = 2;
    public static final int ScanInterval = 2;
    public static final int HumInterval = 70;
    public static final int SyncTicks = 4;
    public static final double EnterReach = 4.5;
    public static final int StatusOff = 0;
    public static final int StatusStandby = 1;
    public static final int StatusOpen = 2;
    public static final int StatusFault = 3;
    public static final String MessagePrefix = "message.scp_unleashed.gateway.";

    private static Consumer<ContainedGatewayBlockEntity> ClientEffects = Gate -> {
    };

    private final Set<UUID> Prompted = new HashSet<>();
    private ItemStack Shard = ItemStack.EMPTY;
    private int Status = StatusOff;
    private int Received;
    private long LastFed = Long.MIN_VALUE / 2;
    private int Ticks;
    private int OpenTicks;
    private boolean NeedsSync;
    private long LastSync = Long.MIN_VALUE / 2;
    private float Bloom;
    private float BloomO;

    public ContainedGatewayBlockEntity(BlockPos Pos, BlockState State) {
        super(GatewayRegistry.GatewayEntity.get(), Pos, State);
    }

    public static void setClientEffects(Consumer<ContainedGatewayBlockEntity> Effects) {
        ClientEffects = Effects;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ContainedGatewayBlockEntity Gate) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof ContainedGatewayBlock) {
            Gate.tickServer(ServerWorld, State);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, ContainedGatewayBlockEntity Gate) {
        Gate.Ticks++;
        Gate.BloomO = Gate.Bloom;
        float Target = Gate.Status == StatusOpen ? 1.0f : 0.0f;
        Gate.Bloom += (Target - Gate.Bloom) * (Target > Gate.Bloom ? 0.12f : 0.2f);
        ClientEffects.accept(Gate);
    }

    private void tickServer(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        Ticks++;
        if (Received >= demand()) {
            LastFed = Now;
        }
        Received = 0;
        boolean Powered = Now - LastFed <= PowerGrace;
        int Next = statusFor(World, Powered);
        if (Next != Status) {
            int Before = Status;
            Status = Next;
            NeedsSync = true;
            if (Next == StatusOpen) {
                OpenTicks = 0;
                play(World, State, GatewayRegistry.Open.get(), 1.0f, 1.0f);
            } else if (Before == StatusOpen) {
                play(World, State, GatewayRegistry.Close.get(), 1.0f, 1.0f);
                Prompted.clear();
            }
        }
        updateGlow(World);
        if (Status == StatusOpen) {
            OpenTicks++;
            if (OpenTicks % HumInterval == 1) {
                play(World, State, GatewayRegistry.Hum.get(), 0.7f, 1.0f);
            }
            if (Ticks % ScanInterval == 0) {
                scan(World, State);
            }
        }
        if (NeedsSync && Now - LastSync >= SyncTicks) {
            NeedsSync = false;
            LastSync = Now;
            World.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int statusFor(ServerLevel World, boolean Powered) {
        if (!Powered) {
            return StatusOff;
        }
        if (Shard.isEmpty()) {
            return StatusStandby;
        }
        GatewayDestination Destination = destination();
        if (Destination == null || World.getServer().getLevel(Destination.Dimension()) == null || World.dimension() == Destination.Dimension()) {
            return StatusFault;
        }
        return StatusOpen;
    }

    private void updateGlow(ServerLevel World) {
        BlockState Current = getBlockState();
        if (!(Current.getBlock() instanceof ContainedGatewayBlock)) {
            return;
        }
        int Wanted = switch (Status) {
            case StatusOpen -> 2;
            case StatusStandby, StatusFault -> 1;
            default -> 0;
        };
        if (Current.getValue(ContainedGatewayBlock.Glow) != Wanted) {
            World.setBlock(worldPosition, Current.setValue(ContainedGatewayBlock.Glow, Wanted), Block.UPDATE_CLIENTS);
        }
    }

    private void scan(ServerLevel World, BlockState State) {
        AABB Zone = threshold(State);
        Set<UUID> Inside = new HashSet<>();
        for (ServerPlayer User : World.getEntitiesOfClass(ServerPlayer.class, Zone.inflate(0.05), Candidate -> Candidate.isAlive() && !Candidate.isSpectator())) {
            if (!User.getBoundingBox().intersects(Zone)) {
                continue;
            }
            Inside.add(User.getUUID());
            if (Prompted.add(User.getUUID())) {
                GatewayDestination Destination = destination();
                if (Destination != null) {
                    PacketDistributor.sendToPlayer(User, new GatewayPromptPayload(worldPosition, Destination.Id()));
                }
            }
        }
        Prompted.retainAll(Inside);
    }

    public AABB threshold(BlockState State) {
        return GatewayGeometry.toWorld(origin(), front(), GatewayGeometry.Threshold);
    }

    public boolean insert(Player User, ItemStack Held) {
        if (!Shard.isEmpty() || !(level instanceof ServerLevel World)) {
            return false;
        }
        if (GatewayDestinations.of(Held) == null) {
            User.displayClientMessage(Component.translatable(MessagePrefix + "blank").withStyle(ChatFormatting.GRAY), true);
            play(World, getBlockState(), GatewayRegistry.Refuse.get(), 0.8f, 1.0f);
            return false;
        }
        Shard = Held.copyWithCount(1);
        if (!User.getAbilities().instabuild) {
            Held.shrink(1);
        }
        play(World, getBlockState(), GatewayRegistry.Insert.get(), 1.0f, 1.0f);
        changed();
        return true;
    }

    public boolean takeOut(Player User) {
        if (Shard.isEmpty() || !(level instanceof ServerLevel World)) {
            return false;
        }
        ItemStack Out = Shard;
        Shard = ItemStack.EMPTY;
        if (!User.getInventory().add(Out)) {
            User.drop(Out, false);
        }
        play(World, getBlockState(), GatewayRegistry.Remove.get(), 1.0f, 1.0f);
        changed();
        return true;
    }

    public void setShard(ItemStack Stack) {
        Shard = Stack.copyWithCount(Stack.isEmpty() ? 0 : 1);
        changed();
    }

    public void dropShard(Level World, BlockPos Pos) {
        if (!Shard.isEmpty()) {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, Shard);
            Shard = ItemStack.EMPTY;
        }
    }

    private void changed() {
        setChanged();
        NeedsSync = true;
        LastSync = Long.MIN_VALUE / 2;
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void play(ServerLevel World, BlockState State, SoundEvent Sound, float Volume, float Pitch) {
        Vec3 Middle = middle();
        World.playSound(null, Middle.x, Middle.y, Middle.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
    }

    public boolean canSend(ServerPlayer User) {
        if (Status != StatusOpen || destination() == null || !User.isAlive()) {
            return false;
        }
        AABB Reach = threshold(getBlockState()).inflate(EnterReach);
        return Reach.contains(User.position());
    }

    public GatewayStash.Spot returnSpot() {
        Direction Front = front();
        Vec3 Out = GatewayGeometry.frontStep(origin(), Front, 1.6);
        return new GatewayStash.Spot(level.dimension(), Out, GatewayGeometry.frontYaw(Front));
    }

    @Nullable
    public GatewayDestination destination() {
        return Shard.isEmpty() ? null : GatewayDestinations.of(Shard);
    }

    @Nullable
    public ResourceLocation destinationId() {
        return Shard.isEmpty() ? null : Shard.get(GatewayRegistry.Destination.get());
    }

    public boolean hasShard() {
        return !Shard.isEmpty();
    }

    public ItemStack shard() {
        return Shard;
    }

    public int status() {
        return Status;
    }

    public boolean isOpen() {
        return Status == StatusOpen;
    }

    public int ticks() {
        return Ticks;
    }

    public float bloom(float Partial) {
        return BloomO + (Bloom - BloomO) * Partial;
    }

    public Direction front() {
        BlockState State = getBlockState();
        return State.hasProperty(ContainedGatewayBlock.Facing) ? State.getValue(ContainedGatewayBlock.Facing) : Direction.NORTH;
    }

    public BlockPos origin() {
        BlockState State = getBlockState();
        return State.getBlock() instanceof ContainedGatewayBlock ? ContainedGatewayBlock.originOf(worldPosition, State) : worldPosition;
    }

    public Vec3 middle() {
        return GatewayGeometry.centre(origin(), front());
    }

    public AABB getRenderBounds() {
        return GatewayGeometry.toWorld(origin(), front(), GatewayGeometry.Cells).inflate(1.0);
    }

    private int demand() {
        return Shard.isEmpty() ? StandbyDraw : OpenDraw;
    }

    @Override
    public int getPowerDemand() {
        return demand();
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Shard.isEmpty()) {
            Tag.put("Shard", Shard.save(Registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Shard = Tag.contains("Shard") ? ItemStack.parse(Registries, Tag.getCompound("Shard")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        if (Tag.contains("Status")) {
            Status = Tag.getByte("Status");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putByte("Status", (byte) Status);
        if (!Shard.isEmpty()) {
            Tag.put("Shard", Shard.save(Registries));
        }
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
