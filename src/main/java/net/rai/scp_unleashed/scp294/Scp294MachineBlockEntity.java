package net.rai.scp_unleashed.scp294;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class Scp294MachineBlockEntity extends BlockEntity {
    public static final int WhirrTick = 4;
    public static final int DropStart = 16;
    public static final int DropEnd = 22;
    public static final int PourStart = 26;
    public static final int PourEnd = 56;
    public static final int PopTick = 64;
    public static final int CycleEnd = 74;
    public static final int NoticeTicks = 60;
    public static final int HumChance = 1400;
    public static final int NoticeNone = 0;
    public static final int NoticeOutOfRange = 1;
    public static final int NoticeNoCoin = 2;
    public static final int NoticeBusy = 3;
    public static final int NoticeEnjoy = 4;
    public static final double AlcoveSide = 2.5 / 16.0;
    public static final double AlcoveDepth = 4.0 / 16.0;
    public static final double AlcoveFloor = 4.0 / 16.0;
    public static final double PanelHeight = 1.2;
    public static final double PopReach = 0.55;
    public static final double PopSpeed = 0.11;

    public enum Outcome {
        Started, Busy, Refused, NoCoin
    }

    private boolean Busy;
    private int Timer;
    private long StartTime = Long.MIN_VALUE / 2;
    private int Colour = 0xFFFFFF;
    @Nullable
    private Scp294Drink Pending;
    @Nullable
    private UUID Orderer;
    private String LastOrder = "";
    private int Orders;
    private int Notice;
    private long NoticeTime = Long.MIN_VALUE / 2;

    public Scp294MachineBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp294Registry.MachineEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp294MachineBlockEntity Machine) {
        Machine.tickServer(World);
    }

    private void tickServer(Level World) {
        if (!Busy) {
            if (World.getRandom().nextInt(HumChance) == 0) {
                play(Scp294Registry.Hum.get(), PanelHeight, 0.25f, 0.8f + World.getRandom().nextFloat() * 0.3f);
            }
            return;
        }
        Timer++;
        if (Timer == WhirrTick) {
            cue(Scp294Registry.WhirrCue, PanelHeight, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            play(Scp294Registry.Hum.get(), PanelHeight, 0.45f, 0.9f);
        }
        if (Timer == DropEnd) {
            cue(Scp294Registry.DropCue, AlcoveFloor + 0.1, 0.6f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
        if (Timer == PourStart || Timer == PourStart + 14) {
            cue(Scp294Registry.PourCue, AlcoveFloor + 0.3, 0.6f, (Timer == PourStart ? 0.95f : 1.1f) + World.getRandom().nextFloat() * 0.1f);
        }
        if (Timer == PopTick) {
            pop(World);
        }
        if (Timer >= CycleEnd) {
            Busy = false;
            Timer = 0;
            Pending = null;
            Orderer = null;
            setChanged();
            sync();
        }
    }

    public Outcome order(ServerPlayer User, String Raw) {
        if (level == null || level.getServer() == null) {
            return Outcome.Refused;
        }
        if (Busy) {
            notice(NoticeBusy);
            cue(Scp294Registry.RefuseCue, PanelHeight, 0.5f, 1.2f);
            return Outcome.Busy;
        }
        Scp294Resolver.Result Resolved = Scp294Resolver.resolve(level.getServer(), Raw);
        if (Resolved.refused()) {
            notice(NoticeOutOfRange);
            cue(Scp294Registry.RefuseCue, PanelHeight, 0.9f, 1.0f);
            return Outcome.Refused;
        }
        if (!User.hasInfiniteMaterials() && !takeNugget(User)) {
            notice(NoticeNoCoin);
            cue(Scp294Registry.RefuseCue, PanelHeight, 0.6f, 1.35f);
            return Outcome.NoCoin;
        }
        start(Resolved.Drink(), Resolved.Typed(), User.getUUID());
        return Outcome.Started;
    }

    public boolean start(Scp294Drink Drink, String Typed, @Nullable UUID Who) {
        if (level == null || Busy) {
            return false;
        }
        Busy = true;
        Timer = 0;
        StartTime = level.getGameTime();
        Colour = Drink.Colour();
        Pending = Drink;
        Orderer = Who;
        LastOrder = Typed;
        Orders++;
        Notice = NoticeNone;
        setChanged();
        sync();
        cue(Scp294Registry.CoinCue, PanelHeight - 0.2, 0.7f, 0.95f + level.getRandom().nextFloat() * 0.1f);
        return true;
    }

    public static int countNuggets(Player User) {
        Inventory Pockets = User.getInventory();
        int Count = 0;
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!Stack.isEmpty() && Stack.is(Tags.Items.NUGGETS_IRON)) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    public static boolean takeNugget(Player User) {
        Inventory Pockets = User.getInventory();
        ItemStack Held = User.getMainHandItem();
        if (!Held.isEmpty() && Held.is(Tags.Items.NUGGETS_IRON)) {
            Held.shrink(1);
            Pockets.setChanged();
            return true;
        }
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (!Stack.isEmpty() && Stack.is(Tags.Items.NUGGETS_IRON)) {
                Stack.shrink(1);
                Pockets.setChanged();
                return true;
            }
        }
        return false;
    }

    private void pop(Level World) {
        if (Pending == null) {
            return;
        }
        ItemStack Cup = Scp294CupItem.filled(Pending);
        Direction Front = getFront();
        Vec3 Spot = alcoveSpot(PopReach, AlcoveFloor + 0.05);
        ItemEntity Dropped = new ItemEntity(World, Spot.x, Spot.y, Spot.z, Cup);
        Dropped.setDeltaMovement(Front.getStepX() * PopSpeed, 0.1, Front.getStepZ() * PopSpeed);
        Dropped.setPickUpDelay(8);
        World.addFreshEntity(Dropped);
        cue(Scp294Registry.PopCue, AlcoveFloor + 0.1, 0.6f, 1.0f + World.getRandom().nextFloat() * 0.1f);
        if (World instanceof ServerLevel Server) {
            Vec3 Steam = alcoveSpot(0.45, AlcoveFloor + 0.35);
            Server.sendParticles(ParticleTypes.WHITE_SMOKE, Steam.x, Steam.y, Steam.z, 3, 0.05, 0.05, 0.05, 0.01);
        }
        notice(NoticeEnjoy);
    }

    public void greet() {
        if (level != null && !Busy) {
            play(Scp294Registry.Hum.get(), PanelHeight, 0.3f, 1.0f + level.getRandom().nextFloat() * 0.1f);
        }
    }

    private void notice(int Code) {
        if (level == null) {
            return;
        }
        Notice = Code;
        NoticeTime = level.getGameTime();
        setChanged();
        sync();
    }

    private void cue(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, double Height, float Volume, float Pitch) {
        if (level != null) {
            Scp294Registry.playCue(level, alcoveSpot(0.5, Height), Layers, SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    private void play(SoundEvent Sound, double Height, float Volume, float Pitch) {
        if (level != null) {
            Vec3 Spot = alcoveSpot(0.5, Height);
            level.playSound(null, Spot.x, Spot.y, Spot.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    public Vec3 alcoveSpot(double Reach, double Height) {
        Direction Front = getFront();
        Direction Side = Front.getClockWise();
        return Vec3.atBottomCenterOf(worldPosition).add(Front.getStepX() * Reach + Side.getStepX() * AlcoveSide, Height, Front.getStepZ() * Reach + Side.getStepZ() * AlcoveSide);
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp294MachineBlock.Facing) ? State.getValue(Scp294MachineBlock.Facing) : Direction.NORTH;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isBusy() {
        return Busy;
    }

    public int getTimer() {
        return Timer;
    }

    public int getColour() {
        return Colour;
    }

    public String getLastOrder() {
        return LastOrder;
    }

    public int getOrders() {
        return Orders;
    }

    @Nullable
    public Scp294Drink getPending() {
        return Pending;
    }

    public float getAge(float PartialTick) {
        return level == null ? Float.MAX_VALUE : level.getGameTime() - StartTime + PartialTick;
    }

    public int getNotice() {
        if (level == null || Notice == NoticeNone || level.getGameTime() - NoticeTime > NoticeTicks) {
            return NoticeNone;
        }
        return Notice;
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).inflate(0.1);
    }

    @Override
    protected void saveAdditional(CompoundTag Data, HolderLookup.Provider Registries) {
        super.saveAdditional(Data, Registries);
        writeShared(Data);
        if (Pending != null) {
            Scp294Drink.DrinkCodec.encodeStart(RegistryOps.create(NbtOps.INSTANCE, Registries), Pending).result().ifPresent(Saved -> Data.put("Pending", Saved));
        }
        if (Orderer != null) {
            Data.putUUID("Orderer", Orderer);
        }
    }

    private void writeShared(CompoundTag Data) {
        Data.putBoolean("Busy", Busy);
        Data.putInt("Timer", Timer);
        Data.putLong("Start", StartTime);
        Data.putInt("Colour", Colour);
        Data.putString("LastOrder", LastOrder);
        Data.putInt("Orders", Orders);
        Data.putInt("Notice", Notice);
        Data.putLong("NoticeTime", NoticeTime);
    }

    @Override
    protected void loadAdditional(CompoundTag Data, HolderLookup.Provider Registries) {
        super.loadAdditional(Data, Registries);
        Busy = Data.getBoolean("Busy");
        Timer = Math.max(0, Math.min(CycleEnd, Data.getInt("Timer")));
        StartTime = Data.contains("Start") ? Data.getLong("Start") : Long.MIN_VALUE / 2;
        Colour = Data.getInt("Colour");
        LastOrder = Data.getString("LastOrder");
        Orders = Data.getInt("Orders");
        Notice = Data.getInt("Notice");
        NoticeTime = Data.contains("NoticeTime") ? Data.getLong("NoticeTime") : Long.MIN_VALUE / 2;
        if (Data.contains("Pending", CompoundTag.TAG_COMPOUND)) {
            Pending = Scp294Drink.DrinkCodec.parse(RegistryOps.create(NbtOps.INSTANCE, Registries), Data.get("Pending")).result().orElse(null);
        }
        Orderer = Data.hasUUID("Orderer") ? Data.getUUID("Orderer") : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Data = new CompoundTag();
        writeShared(Data);
        return Data;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
