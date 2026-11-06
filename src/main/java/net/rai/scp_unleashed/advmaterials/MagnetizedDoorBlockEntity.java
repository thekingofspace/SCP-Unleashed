package net.rai.scp_unleashed.advmaterials;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.door.DoorGeometry;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.DoorLatch;
import net.rai.scp_unleashed.door.DoorVoice;

import java.util.function.Predicate;

public class MagnetizedDoorBlockEntity extends BlockEntity {
    private static final Predicate<Entity> Blocking = Target -> Target.isAlive() && !Target.isSpectator() && (Target instanceof LivingEntity || Target instanceof VehicleEntity);

    private float Progress;
    private float Previous;
    private final DoorLatch Latch = new DoorLatch();
    private boolean Checked;

    public MagnetizedDoorBlockEntity(BlockPos Pos, BlockState State) {
        super(AdvancedMaterialsRegistry.DoorEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, MagnetizedDoorBlockEntity Door) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof MagnetizedDoorBlock Frame) {
            Door.tickServer(ServerWorld, State, Frame);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, MagnetizedDoorBlockEntity Door) {
        Door.Previous = Door.Progress;
        Door.Progress = Mth.approach(Door.Progress, State.getValue(MagnetizedDoorBlock.Open) ? 1.0f : 0.0f, 1.0f / MagnetizedDoorBlock.TravelTicks);
    }

    private void tickServer(ServerLevel World, BlockState State, MagnetizedDoorBlock Frame) {
        if (!Checked) {
            Checked = true;
            refreshPower(World);
        }
        Direction Front = State.getValue(MagnetizedDoorBlock.Facing);
        boolean Obstructed = Latch.needsClearance(Progress) && isObstructed(World, Front);
        boolean Wanted = Latch.wanted(World.getGameTime(), Progress, Obstructed);
        if (Wanted != State.getValue(MagnetizedDoorBlock.Open)) {
            Frame.setOpen(World, worldPosition, Front, Wanted);
            Vec3 Centre = centre(Front);
            DoorVoice.Heavy.start(World, Centre, Wanted);
            if (Wanted) {
                World.playSound(null, Centre.x, Centre.y, Centre.z, AdvancedMaterialsRegistry.DoorRelease.get(), SoundSource.BLOCKS, 0.9f, 0.95f + World.getRandom().nextFloat() * 0.1f);
            }
            World.gameEvent(null, Wanted ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, worldPosition);
        }
        Previous = Progress;
        float Target = Wanted ? 1.0f : 0.0f;
        if (Progress != Target) {
            Progress = Mth.approach(Progress, Target, 1.0f / MagnetizedDoorBlock.TravelTicks);
            setChanged();
            if (Progress == Target) {
                Vec3 Centre = centre(Front);
                DoorVoice.Heavy.stop(World, Centre, Wanted);
                if (!Wanted) {
                    World.playSound(null, Centre.x, Centre.y, Centre.z, AdvancedMaterialsRegistry.DoorClamp.get(), SoundSource.BLOCKS, 1.0f, 0.95f + World.getRandom().nextFloat() * 0.1f);
                }
            }
        }
    }

    public DoorKind layout() {
        return getBlockState().getBlock() instanceof MagnetizedDoorBlock Frame ? Frame.getLayout() : MagnetizedDoorBlock.Layout;
    }

    private Vec3 centre(Direction Front) {
        return DoorGeometry.centre(layout(), worldPosition, Front);
    }

    private boolean isObstructed(Level World, Direction Front) {
        AABB Doorway = DoorGeometry.worldBox(worldPosition, Front, DoorGeometry.clearance(layout()));
        return !World.getEntitiesOfClass(Entity.class, Doorway, Blocking).isEmpty();
    }

    public void refreshPower(Level World) {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof MagnetizedDoorBlock Frame) {
            boolean Signal = Frame.anyCellPowered(World, worldPosition, State.getValue(MagnetizedDoorBlock.Facing));
            if (Latch.power(Signal, true)) {
                setChanged();
            }
        }
    }

    public boolean toggle() {
        if (Latch.toggle(true)) {
            setChanged();
            return true;
        }
        return false;
    }

    public boolean closeByHand() {
        if (Latch.isPowered() || !Latch.isLatched() && !Latch.isHeld()) {
            return false;
        }
        return toggle();
    }

    public void hold(boolean Holding) {
        if (Latch.hold(Holding)) {
            setChanged();
        }
    }

    public boolean isHeld() {
        return Latch.isHeld();
    }

    public boolean isPowered() {
        return Latch.isPowered();
    }

    public boolean isSealed() {
        return Progress <= 0.0f && !getBlockState().getValue(MagnetizedDoorBlock.Open);
    }

    public float getProgress(float PartialTick) {
        return Mth.lerp(PartialTick, Previous, Progress);
    }

    public float getEased() {
        return DoorGeometry.ease(Progress);
    }

    public AABB getRenderBounds() {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof MagnetizedDoorBlock Frame) {
            return DoorGeometry.renderBounds(Frame.getLayout(), worldPosition, State.getValue(MagnetizedDoorBlock.Facing));
        }
        return new AABB(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putFloat("Progress", Progress);
        Latch.save(Tag);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Progress = Mth.clamp(Tag.getFloat("Progress"), 0.0f, 1.0f);
        Previous = Progress;
        Latch.load(Tag);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putFloat("Progress", Progress);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
