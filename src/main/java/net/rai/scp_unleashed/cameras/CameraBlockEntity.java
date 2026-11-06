package net.rai.scp_unleashed.cameras;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CameraBlockEntity extends BlockEntity {
    public static final int ServoGap = 6;
    public static final float ServoStep = 0.75f;
    public static final float ShownFollow = 0.45f;

    private String Name = "";
    private float Pan;
    private float Tilt;
    private boolean Linked;
    private int Viewers;
    private long LastServo = Long.MIN_VALUE / 2;
    private float ShownPan;
    private float ShownTilt;
    private float PrevPan;
    private float PrevTilt;
    private boolean Shown;

    public CameraBlockEntity(BlockPos Pos, BlockState State) {
        super(CameraRegistry.CameraEntity.get(), Pos, State);
    }

    public String getCameraName() {
        return Name;
    }

    public boolean hasCameraName() {
        return !Name.isEmpty();
    }

    public void setCameraName(String Wanted) {
        String Cleaned = CameraLinks.clean(Wanted);
        if (!Cleaned.equals(Name)) {
            Name = Cleaned;
            sync();
        }
    }

    public float getPan() {
        return Pan;
    }

    public float getTilt() {
        return Tilt;
    }

    public boolean isLinked() {
        return Linked;
    }

    public void markLinked() {
        if (!Linked) {
            Linked = true;
            sync();
        }
    }

    public int getViewers() {
        return Viewers;
    }

    public void setViewers(int Count) {
        if (Count != Viewers) {
            Viewers = Count;
            sync();
        }
    }

    public CameraMount mount() {
        return CameraMount.of(getBlockState());
    }

    public float yaw() {
        return mount().yaw(Pan);
    }

    public float pitch() {
        return mount().pitch(Tilt);
    }

    public Vec3 eye() {
        return mount().eye(worldPosition, Pan, Tilt);
    }

    public Vec3 look() {
        return mount().look(Pan, Tilt);
    }

    public boolean aim(float WantedPan, float WantedTilt) {
        float NewPan = CameraMount.clampPan(WantedPan);
        float NewTilt = mount().clampTilt(WantedTilt);
        if (Math.abs(NewPan - Pan) < 1.0e-3f && Math.abs(NewTilt - Tilt) < 1.0e-3f) {
            return false;
        }
        float Moved = Math.abs(NewPan - Pan) + Math.abs(NewTilt - Tilt);
        Pan = NewPan;
        Tilt = NewTilt;
        if (level != null && !level.isClientSide && Moved >= ServoStep && level.getGameTime() - LastServo >= ServoGap) {
            LastServo = level.getGameTime();
            Vec3 Centre = Vec3.atCenterOf(worldPosition);
            level.playSound(null, Centre.x, Centre.y, Centre.z, CameraRegistry.Servo.get(), SoundSource.BLOCKS, 0.35f, 0.95f + level.random.nextFloat() * 0.1f);
            level.playSound(null, Centre.x, Centre.y, Centre.z, CameraRegistry.ServoRatchet.get(), SoundSource.BLOCKS, 0.2f, 1.0f);
        }
        sync();
        return true;
    }

    public void clientTick() {
        if (!Shown) {
            ShownPan = Pan;
            ShownTilt = Tilt;
            Shown = true;
        }
        PrevPan = ShownPan;
        PrevTilt = ShownTilt;
        ShownPan += (Pan - ShownPan) * ShownFollow;
        ShownTilt += (Tilt - ShownTilt) * ShownFollow;
    }

    public float shownPan(float Partial) {
        return Shown ? Mth.lerp(Partial, PrevPan, ShownPan) : Pan;
    }

    public float shownTilt(float Partial) {
        return Shown ? Mth.lerp(Partial, PrevTilt, ShownTilt) : Tilt;
    }

    public Component displayName() {
        return Name.isEmpty() ? Component.translatable("block.scp_unleashed.cctv_camera") : Component.literal(Name);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void writeShared(CompoundTag Root) {
        Root.putString("Name", Name);
        Root.putFloat("Pan", Pan);
        Root.putFloat("Tilt", Tilt);
        Root.putBoolean("Linked", Linked);
    }

    private void readShared(CompoundTag Root) {
        Name = CameraLinks.clean(Root.getString("Name"));
        Pan = CameraMount.clampPan(Root.getFloat("Pan"));
        Tilt = Root.getFloat("Tilt");
        Linked = Root.getBoolean("Linked");
    }

    @Override
    protected void saveAdditional(CompoundTag Root, HolderLookup.Provider Registries) {
        super.saveAdditional(Root, Registries);
        writeShared(Root);
    }

    @Override
    protected void loadAdditional(CompoundTag Root, HolderLookup.Provider Registries) {
        super.loadAdditional(Root, Registries);
        readShared(Root);
        if (Root.contains("Viewers")) {
            Viewers = Root.getInt("Viewers");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Root = new CompoundTag();
        writeShared(Root);
        Root.putInt("Viewers", Viewers);
        return Root;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (!Name.isEmpty()) {
            Components.set(DataComponents.CUSTOM_NAME, Component.literal(Name));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Component Custom = Input.get(DataComponents.CUSTOM_NAME);
        if (Custom != null) {
            Name = CameraLinks.clean(Custom.getString());
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Root) {
        super.removeComponentsFromTag(Root);
        Root.remove("Name");
    }

    public AABB renderBox() {
        return new AABB(worldPosition).inflate(0.5);
    }
}
