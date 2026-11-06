package net.rai.scp_unleashed.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StorageContainerBlockEntity extends BlockEntity implements StorageView {
    private ItemStack Contents = ItemStack.EMPTY;
    private ItemStack Glass = ItemStack.EMPTY;
    private final DoorSwing Swing = new DoorSwing();
    private final List<PendingSound> Pending = new ArrayList<>();
    @Nullable
    private ContainedBehaviour Behaviour;
    private boolean BehaviourKnown;

    public StorageContainerBlockEntity(BlockPos Pos, BlockState State) {
        super(StorageRegistry.ContainerEntity.get(), Pos, State);
    }

    protected StorageContainerBlockEntity(BlockEntityType<?> Type, BlockPos Pos, BlockState State) {
        super(Type, Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, StorageContainerBlockEntity Locker) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        DoorSwing.Event Happened = Locker.Swing.step(State.getValue(StorageContainerBlock.Open));
        switch (Happened) {
            case Stopped -> Locker.cue(StorageSounds.Opened, false);
            case Slammed -> Locker.cue(StorageSounds.Slammed, false);
            case Settled -> Locker.cue(StorageSounds.Settled, false);
            default -> {
            }
        }
        Locker.tickSounds(World);
        if (!Locker.Contents.isEmpty()) {
            ContainedBehaviour Found = Locker.behaviour();
            if (Found != null) {
                Found.tick(Server, Locker);
            }
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, StorageContainerBlockEntity Locker) {
        Locker.Swing.step(State.getValue(StorageContainerBlock.Open));
        StorageAmbience.tick(World, Pos, State, Locker);
    }

    public float getDoorAngle(float PartialTick) {
        return Swing.angle(PartialTick);
    }

    public float getDoorAngle() {
        return Swing.getAngle();
    }

    public boolean isInteriorShown() {
        return Swing.getAngle() > 0.0f || hasGlassFront() || !Swing.isPrimed() && isOpen();
    }

    @Override
    public BlockPos getStoragePos() {
        return worldPosition;
    }

    @Override
    public Direction getFacing() {
        BlockState State = getBlockState();
        return State.hasProperty(StorageContainerBlock.Facing) ? State.getValue(StorageContainerBlock.Facing) : Direction.NORTH;
    }

    @Override
    public boolean isTall() {
        return getBlockState().getBlock() instanceof StorageContainerBlock Owner && Owner.isTall();
    }

    @Override
    public ItemStack getContents() {
        return Contents.copy();
    }

    public ItemStack peekContents() {
        return Contents;
    }

    @Override
    public boolean hasContents() {
        return !Contents.isEmpty();
    }

    @Override
    public boolean isOpen() {
        BlockState State = getBlockState();
        return State.hasProperty(StorageContainerBlock.Open) && State.getValue(StorageContainerBlock.Open);
    }

    @Override
    public boolean hasGlassFront() {
        return !Glass.isEmpty();
    }

    @Override
    public ItemStack getGlassFront() {
        return Glass.copy();
    }

    public ItemStack peekGlass() {
        return Glass;
    }

    @Override
    public StorageEnvironment getEnvironment() {
        BlockState State = getBlockState();
        return State.hasProperty(StorageContainerBlock.Environment) ? State.getValue(StorageContainerBlock.Environment) : StorageEnvironment.Normal;
    }

    public void setContents(ItemStack Stack) {
        Contents = Stack.copy();
        BehaviourKnown = false;
        contentsChanged();
    }

    @Nullable
    public Component refusalFor(ItemStack One) {
        if (!StorageTags.isStorable(One)) {
            return Component.translatable("message.scp_unleashed.storage.not_storable", One.getHoverName());
        }
        if (!isTall() && One.is(StorageTags.TallOnly)) {
            return Component.translatable("message.scp_unleashed.storage.too_big", One.getHoverName());
        }
        return StorageBehaviours.find(One).map(Found -> Found.refuse(this, One)).orElse(null);
    }

    public void store(Player User, ItemStack Held) {
        ItemStack One = Held.copyWithCount(1);
        Component Refusal = refusalFor(One);
        if (Refusal != null) {
            User.displayClientMessage(Refusal, true);
            refuse();
            return;
        }
        ItemStack Absorbed = StorageBehaviours.find(One).map(Found -> Found.absorb(this, ItemStack.EMPTY, Held, User)).orElse(null);
        if (Absorbed != null) {
            Contents = Absorbed;
        } else {
            Contents = One;
            Held.consume(1, User);
        }
        BehaviourKnown = false;
        contentsChanged();
        cue(StorageSounds.Stored, false);
        notifyChange(StorageChange.Inserted, Contents);
    }

    public boolean topUp(Player User, ItemStack Held) {
        ContainedBehaviour Found = behaviour();
        ItemStack Absorbed = Found == null || Held.isEmpty() ? null : Found.absorb(this, Contents, Held, User);
        if (Absorbed == null) {
            return false;
        }
        Contents = Absorbed;
        BehaviourKnown = false;
        contentsChanged();
        cue(StorageSounds.Stored, false);
        notifyChange(StorageChange.Inserted, Contents);
        return true;
    }

    public void take(Player User, InteractionHand Hand) {
        ItemStack Taken = Contents;
        Contents = ItemStack.EMPTY;
        BehaviourKnown = false;
        contentsChanged();
        List<ItemStack> Parts = unpacked(Taken);
        User.setItemInHand(Hand, Parts.get(0));
        for (ItemStack Part : Parts.subList(1, Parts.size())) {
            StorageContainerBlock.give(level, worldPosition, User, Part);
        }
        cue(StorageSounds.Taken, false);
        notifyChange(StorageChange.Removed, Taken);
    }

    public boolean takeOne(Player User, InteractionHand Hand) {
        ContainedBehaviour Found = behaviour();
        ContainedBehaviour.Split Part = Found == null ? null : Found.takeOne(Contents);
        if (Part == null || Part.Taken().isEmpty()) {
            return false;
        }
        Contents = Part.Left();
        BehaviourKnown = false;
        contentsChanged();
        User.setItemInHand(Hand, Part.Taken());
        cue(StorageSounds.Taken, false);
        notifyChange(StorageChange.Removed, Part.Taken());
        return true;
    }

    @Nullable
    public Component occupiedMessage(ItemStack Held) {
        ContainedBehaviour Found = behaviour();
        return Found == null ? null : Found.occupied(this, Contents, Held);
    }

    private static List<ItemStack> unpacked(ItemStack Stored) {
        List<ItemStack> Parts = StorageBehaviours.find(Stored).map(Found -> Found.unpack(Stored)).orElse(List.of(Stored));
        return Parts.isEmpty() ? List.of(ItemStack.EMPTY) : Parts;
    }

    public void receive(ItemStack One) {
        Contents = One.copyWithCount(1);
        BehaviourKnown = false;
        contentsChanged();
        cue(StorageSounds.Stored, false);
        notifyChange(StorageChange.Inserted, Contents);
    }

    public ItemStack release() {
        ItemStack Taken = Contents;
        Contents = ItemStack.EMPTY;
        BehaviourKnown = false;
        contentsChanged();
        cue(StorageSounds.Taken, false);
        notifyChange(StorageChange.Removed, Taken);
        return Taken;
    }

    public void fitGlass(Player User, ItemStack Held) {
        Glass = Held.copyWithCount(1);
        Held.consume(1, User);
        contentsChanged();
        cue(StorageSounds.Glazed, false);
        notifyChange(StorageChange.GlassFitted, Contents);
    }

    public ItemStack removeGlass() {
        ItemStack Removed = Glass;
        Glass = ItemStack.EMPTY;
        contentsChanged();
        notifyChange(StorageChange.GlassRemoved, Contents);
        return Removed;
    }

    public void refuse() {
        cue(StorageSounds.Refused, false);
    }

    public void upgraded() {
        cue(StorageSounds.Upgraded, false);
        environmentChanged();
    }

    public void environmentChanged() {
        notifyChange(StorageChange.EnvironmentChanged, Contents);
    }

    public void doorToggled(boolean Opening) {
        cue(Opening ? StorageSounds.Opening : StorageSounds.Closing, true);
        notifyChange(Opening ? StorageChange.Opened : StorageChange.Closed, Contents);
    }

    public void dropAll(Level World, BlockPos Pos) {
        if (!Contents.isEmpty()) {
            for (ItemStack Part : unpacked(Contents)) {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Part);
            }
            Contents = ItemStack.EMPTY;
        }
        if (!Glass.isEmpty()) {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Glass);
            Glass = ItemStack.EMPTY;
        }
    }

    @Nullable
    private ContainedBehaviour behaviour() {
        if (!BehaviourKnown) {
            Behaviour = StorageBehaviours.find(Contents).orElse(null);
            BehaviourKnown = true;
        }
        return Behaviour;
    }

    private void notifyChange(StorageChange Change, ItemStack Subject) {
        if (level instanceof ServerLevel World && !Subject.isEmpty()) {
            StorageBehaviours.find(Subject).ifPresent(Found -> Found.changed(World, this, Change, Subject));
        }
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState State = getBlockState();
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
            if (isTall()) {
                level.updateNeighbourForOutputSignal(worldPosition.above(), State.getBlock());
            }
        }
    }

    private void cue(List<StorageCue> Cues, boolean Fresh) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (Fresh) {
            Pending.clear();
        }
        float Pitch = 0.95f + level.random.nextFloat() * 0.1f;
        for (StorageCue Cue : Cues) {
            if (Cue.Delay() > 0) {
                Pending.add(new PendingSound(Cue.Event().get(), Cue.Delay(), Pitch));
            } else {
                play(level, Cue.Event().get(), Pitch);
            }
        }
    }

    private void tickSounds(Level World) {
        if (Pending.isEmpty()) {
            return;
        }
        Iterator<PendingSound> Queue = Pending.iterator();
        while (Queue.hasNext()) {
            PendingSound Entry = Queue.next();
            Entry.Remaining--;
            if (Entry.Remaining <= 0) {
                play(World, Entry.Event, Entry.Pitch);
                Queue.remove();
            }
        }
    }

    private void play(Level World, SoundEvent Event, float Pitch) {
        Vec3 Where = soundCentre();
        World.playSound(null, Where.x, Where.y, Where.z, Event, SoundSource.BLOCKS, 1.0f, Pitch);
    }

    public Vec3 soundCentre() {
        return StorageGeometry.point(worldPosition, getFacing(), StorageGeometry.CentreX, isTall() ? 16.0 : 8.0, StorageGeometry.FrontFace + 2.0);
    }

    public AABB getRenderBounds() {
        Direction Front = getFacing();
        AABB Box = new AABB(worldPosition).expandTowards(Front.getStepX(), 0.0, Front.getStepZ());
        return isTall() ? Box.expandTowards(0.0, 1.0, 0.0) : Box;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (!Contents.isEmpty()) {
            Tag.put("Contents", Contents.save(Registries));
        }
        if (!Glass.isEmpty()) {
            Tag.put("Glass", Glass.save(Registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Contents = Tag.contains("Contents") ? ItemStack.parseOptional(Registries, Tag.getCompound("Contents")) : ItemStack.EMPTY;
        Glass = Tag.contains("Glass") ? ItemStack.parseOptional(Registries, Tag.getCompound("Glass")) : ItemStack.EMPTY;
        BehaviourKnown = false;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = saveCustomOnly(Registries);
        Tag.putBoolean("Filled", !Contents.isEmpty());
        Tag.putBoolean("Glazed", !Glass.isEmpty());
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static final class PendingSound {
        private final SoundEvent Event;
        private final float Pitch;
        private int Remaining;

        private PendingSound(SoundEvent Event, int Remaining, float Pitch) {
            this.Event = Event;
            this.Remaining = Remaining;
            this.Pitch = Pitch;
        }
    }
}
