package net.rai.scp_unleashed.door;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class PanelDoorBlockEntity extends BlockEntity {
    private static final Predicate<Entity> Blocking = Target -> Target.isAlive() && !Target.isSpectator() && (Target instanceof LivingEntity || Target instanceof VehicleEntity);
    private static BiConsumer<PanelDoorBlockEntity, DoorCue> ClientEffects = (Door, Cue) -> {
    };

    private float Progress;
    private float Previous;
    private final DoorLatch Latch = new DoorLatch();
    private boolean Checked;
    private int Heading;

    public PanelDoorBlockEntity(BlockPos Pos, BlockState State) {
        super(DoorRegistry.PanelDoor.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PanelDoorBlockEntity Door) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof PanelDoorBlock Frame) {
            Door.tickServer(ServerWorld, State, Frame);
        }
    }

    public static void setClientEffects(BiConsumer<PanelDoorBlockEntity, DoorCue> Effects) {
        ClientEffects = Effects;
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, PanelDoorBlockEntity Door) {
        if (State.getBlock() instanceof PanelDoorBlock Frame) {
            Door.tickClient(State.getValue(PanelDoorBlock.Open) ? 1.0f : 0.0f, Frame.getKind());
        }
    }

    private void tickClient(float Target, DoorKind Kind) {
        int Was = Heading;
        int Now = Progress < Target ? 1 : Progress > Target ? -1 : 0;
        Previous = Progress;
        Progress = Mth.approach(Progress, Target, 1.0f / Kind.getTravelTicks());
        Heading = Now != 0 && Progress != Target ? Now : 0;
        if (!Kind.getVoice().isLayered()) {
            return;
        }
        if (Now != 0 && Now != Was) {
            ClientEffects.accept(this, Was == 0 ? DoorCue.Start : DoorCue.Reverse);
        }
        if (Now != 0) {
            ClientEffects.accept(this, DoorCue.Travel);
        }
        if (Heading == 0 && (Was != 0 || Now != 0)) {
            ClientEffects.accept(this, DoorCue.Arrive);
        }
    }

    private void tickServer(ServerLevel World, BlockState State, PanelDoorBlock Frame) {
        if (!Checked) {
            Checked = true;
            refreshPower(World);
        }
        DoorKind Kind = Frame.getKind();
        Direction Front = State.getValue(PanelDoorBlock.Facing);
        boolean Obstructed = Latch.needsClearance(Progress) && isObstructed(World, Kind, Front);
        boolean Wanted = Latch.wanted(World.getGameTime(), Progress, Obstructed);
        if (Wanted != State.getValue(PanelDoorBlock.Open)) {
            Frame.setOpen(World, worldPosition, Front, Wanted);
            playStart(World, Kind, Front, Wanted);
            World.gameEvent(null, Wanted ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, worldPosition);
        }
        Previous = Progress;
        float Target = Wanted ? 1.0f : 0.0f;
        if (Progress != Target) {
            Progress = Mth.approach(Progress, Target, 1.0f / Kind.getTravelTicks());
            setChanged();
            if (Progress == Target) {
                playStop(World, Kind, Front, Wanted);
            }
        }
    }

    private boolean isObstructed(Level World, DoorKind Kind, Direction Front) {
        AABB Doorway = DoorGeometry.worldBox(worldPosition, Front, DoorGeometry.clearance(Kind));
        return !World.getEntitiesOfClass(Entity.class, Doorway, Blocking).isEmpty();
    }

    private void playStart(ServerLevel World, DoorKind Kind, Direction Front, boolean Opening) {
        Kind.getVoice().start(World, DoorGeometry.centre(Kind, worldPosition, Front), Opening);
    }

    private void playStop(ServerLevel World, DoorKind Kind, Direction Front, boolean Opened) {
        Kind.getVoice().stop(World, DoorGeometry.centre(Kind, worldPosition, Front), Opened);
    }

    public void refreshPower(Level World) {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof PanelDoorBlock Frame) {
            boolean Signal = Frame.anyCellPowered(World, worldPosition, State.getValue(PanelDoorBlock.Facing));
            if (Latch.power(Signal, Frame.getKind().isManual())) {
                setChanged();
            }
        }
    }

    public boolean toggle() {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof PanelDoorBlock Frame && Latch.toggle(Frame.getKind().isManual())) {
            setChanged();
            return true;
        }
        return false;
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

    public boolean isLatched() {
        return Latch.isLatched();
    }

    public float getProgress(float PartialTick) {
        return Mth.lerp(PartialTick, Previous, Progress);
    }

    public float getEased() {
        return DoorGeometry.ease(Progress);
    }

    public boolean isTravelling() {
        return Heading != 0;
    }

    public float getSpeed() {
        return 4.0f * Progress * (1.0f - Progress);
    }

    public AABB getRenderBounds() {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof PanelDoorBlock Frame) {
            return DoorGeometry.renderBounds(Frame.getKind(), worldPosition, State.getValue(PanelDoorBlock.Facing));
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
