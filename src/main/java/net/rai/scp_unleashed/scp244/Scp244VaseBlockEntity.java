package net.rai.scp_unleashed.scp244;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class Scp244VaseBlockEntity extends BlockEntity {
    public static final String FrontKey = "Front";
    public static final String IntensityKey = "Intensity";
    public static final String ClosedKey = "Closed";
    public static final int RebuildTicks = 40;
    public static final int FirstBudget = 2500;
    public static final int Budget = 900;
    public static final int SyncTicks = 40;
    public static final float LidStep = 1.0f / 14.0f;

    private static Consumer<Scp244VaseBlockEntity> ClientTicker = Vase -> {
    };

    private Scp244Cloud.State Cloud = Scp244Cloud.State.Still;
    @Nullable
    private Scp244Field Field;
    @Nullable
    private Scp244Flood Flood;
    private long FieldBuilt;
    private long LastSync;
    private float Lid;
    private float LidO;
    private boolean LidReady;
    @Nullable
    private Object ClientState;

    public Scp244VaseBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp244Registry.VaseEntity.get(), Pos, State);
    }

    public static void setClientTicker(Consumer<Scp244VaseBlockEntity> Ticker) {
        ClientTicker = Ticker;
    }

    public Scp244Style style() {
        return getBlockState().getBlock() instanceof Scp244VaseBlock Vase ? Vase.style() : Scp244Style.Amphora;
    }

    public boolean isOpen() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp244VaseBlock.Open) && State.getValue(Scp244VaseBlock.Open);
    }

    public boolean bound() {
        return isOpen() || Cloud.active();
    }

    public float front() {
        return Cloud.Front();
    }

    public float intensity() {
        return Cloud.Intensity();
    }

    public int closedTicks() {
        return Cloud.ClosedTicks();
    }

    public float density(float Distance) {
        return Scp244Cloud.density(Distance, Cloud.Front(), Cloud.Intensity());
    }

    @Nullable
    public Scp244Field field() {
        return Field;
    }

    public float densityAt(BlockPos Pos) {
        if (Field == null) {
            return 0.0f;
        }
        float Distance = Field.distanceAt(Pos);
        return Distance < 0.0f ? 0.0f : density(Distance);
    }

    public float lid(float Partial) {
        return Mth.lerp(Partial, LidO, Lid);
    }

    public Vec3 mouth() {
        Scp244Style Style = style();
        return Vec3.atBottomCenterOf(worldPosition).add(0.0, Style.mouthHeight(), 0.0);
    }

    @Nullable
    public Object getClientState() {
        return ClientState;
    }

    public void setClientState(@Nullable Object State) {
        ClientState = State;
    }

    public void toggle(@Nullable Player User) {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    public void open() {
        if (!(level instanceof ServerLevel World) || isOpen()) {
            return;
        }
        Scp244VaseBlock.setOpen(World, worldPosition, true);
        Vec3 Mouth = mouth();
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp244Registry.LidLift.get(), SoundSource.BLOCKS, 0.9f, Pitch);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp244Registry.LidScrape.get(), SoundSource.BLOCKS, 0.7f, Pitch);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp244Registry.Release.get(), SoundSource.BLOCKS, 0.9f, 0.9f + World.random.nextFloat() * 0.1f);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp244Registry.ReleaseGust.get(), SoundSource.BLOCKS, 0.8f, 0.9f + World.random.nextFloat() * 0.1f);
        setChanged();
        sync(true);
    }

    public void close() {
        if (!(level instanceof ServerLevel World) || !isOpen()) {
            return;
        }
        Scp244VaseBlock.setOpen(World, worldPosition, false);
        Vec3 Mouth = mouth();
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp244Registry.LidClink.get(), SoundSource.BLOCKS, 0.9f, Pitch);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp244Registry.LidSeat.get(), SoundSource.BLOCKS, 0.8f, Pitch);
        setChanged();
        sync(true);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp244VaseBlockEntity Vase) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        boolean Open = State.getValue(Scp244VaseBlock.Open);
        Scp244Cloud.State Before = Vase.Cloud;
        Vase.Cloud = Before.advance(Open);
        if (!Vase.Cloud.active() && !Open) {
            Vase.Field = null;
            Vase.Flood = null;
            if (Before.active()) {
                Vase.setChanged();
                Vase.sync(true);
            }
            return;
        }
        Vase.upkeep(Server);
        if (Vase.Field != null) {
            Vase.chill(Server);
            Scp244Frost.tick(Server, Vase, Vase.Field);
        }
        if (Server.getGameTime() % 20L == 0L) {
            Vase.setChanged();
        }
        Vase.sync(false);
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp244VaseBlockEntity Vase) {
        boolean Open = State.getValue(Scp244VaseBlock.Open);
        float Target = Open ? 1.0f : 0.0f;
        if (!Vase.LidReady) {
            Vase.Lid = Target;
            Vase.LidO = Target;
            Vase.LidReady = true;
        }
        Vase.LidO = Vase.Lid;
        if (Vase.Lid < Target) {
            Vase.Lid = Math.min(Target, Vase.Lid + LidStep);
        } else if (Vase.Lid > Target) {
            Vase.Lid = Math.max(Target, Vase.Lid - LidStep * 1.5f);
        }
        Vase.Cloud = Vase.Cloud.advance(Open);
        ClientTicker.accept(Vase);
    }

    private void upkeep(ServerLevel World) {
        long Now = World.getGameTime();
        if (Flood == null && (Field == null || Now - FieldBuilt >= RebuildTicks)) {
            Flood = new Scp244Flood(World, worldPosition);
        }
        if (Flood != null && Flood.step(Field == null ? FirstBudget : Budget)) {
            Field = Flood.result();
            FieldBuilt = Now;
            Flood = null;
        }
    }

    private void chill(ServerLevel World) {
        Scp244Field Fog = Field;
        if (Fog == null) {
            return;
        }
        long Now = World.getGameTime();
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Fog.bounds(), Scp244Chill::susceptible)) {
            float Feet = densityAt(Target.blockPosition());
            float Eyes = densityAt(BlockPos.containing(Target.getEyePosition()));
            float Felt = Math.max(Feet, Eyes);
            if (Felt >= Scp244Chill.MinDensity) {
                Scp244Chill.expose(Target, Felt, Now);
            }
        }
    }

    public void rebuildNow() {
        if (level != null) {
            Field = Scp244Flood.fill(level, worldPosition);
            FieldBuilt = level.getGameTime();
            Flood = null;
        }
    }

    private void sync(boolean Force) {
        if (level == null || level.isClientSide) {
            return;
        }
        long Now = level.getGameTime();
        if (!Force && Now - LastSync < SyncTicks) {
            return;
        }
        LastSync = Now;
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void writeCloud(CompoundTag Tag) {
        Tag.putFloat(FrontKey, Cloud.Front());
        Tag.putFloat(IntensityKey, Cloud.Intensity());
        Tag.putInt(ClosedKey, Cloud.ClosedTicks());
    }

    private void readCloud(CompoundTag Tag) {
        if (Tag.contains(IntensityKey)) {
            Cloud = new Scp244Cloud.State(Tag.getFloat(FrontKey), Tag.getFloat(IntensityKey), Tag.getInt(ClosedKey));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        writeCloud(Tag);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        readCloud(Tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = super.getUpdateTag(Registries);
        writeCloud(Tag);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
