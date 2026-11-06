package net.rai.scp_unleashed.scp4666.christmas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp4666DoorNote extends Entity {
    public static final double NoteLift = 1.2;
    public static final double FaceGap = 0.02;
    public static final double RenderReach = 64.0;
    private static final EntityDataAccessor<BlockPos> DoorData = SynchedEntityData.defineId(Scp4666DoorNote.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Direction> SideData = SynchedEntityData.defineId(Scp4666DoorNote.class, EntityDataSerializers.DIRECTION);
    public static Runnable Reader = () -> {
    };
    @Nullable
    private UUID Owner;

    public record Placement(Vec3 Spot, float Yaw, Direction Normal) {
    }

    public Scp4666DoorNote(EntityType<? extends Scp4666DoorNote> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    public static Scp4666DoorNote attach(ServerLevel World, BlockPos Door, Direction Side, @Nullable UUID Owner) {
        Placement Spot = placement(World, Door, Side);
        if (Spot == null) {
            return null;
        }
        Scp4666DoorNote Note = new Scp4666DoorNote(Scp4666Registry.DoorNote.get(), World);
        Note.entityData.set(DoorData, Door.immutable());
        Note.entityData.set(SideData, Side);
        Note.Owner = Owner;
        Note.moveTo(Spot.Spot().x, Spot.Spot().y, Spot.Spot().z, Spot.Yaw(), 0.0f);
        return World.addFreshEntity(Note) ? Note : null;
    }

    public static Direction normal(BlockState State, Direction Side) {
        if (!State.getValue(DoorBlock.OPEN)) {
            return Side;
        }
        return State.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT ? Side.getClockWise() : Side.getCounterClockWise();
    }

    @Nullable
    public static Placement placement(BlockGetter World, BlockPos Door, Direction Side) {
        BlockState State = World.getBlockState(Door);
        if (!Scp4666NoteEvent.lowerDoor(State)) {
            return null;
        }
        Direction Normal = normal(State, Side);
        VoxelShape Shape = State.getShape(World, Door);
        AABB Box = Shape.isEmpty() ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : Shape.bounds();
        double X = (Box.minX + Box.maxX) / 2.0;
        double Z = (Box.minZ + Box.maxZ) / 2.0;
        boolean Positive = Normal.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        if (Normal.getAxis() == Direction.Axis.X) {
            X = (Positive ? Box.maxX : Box.minX) + Normal.getStepX() * FaceGap;
        } else {
            Z = (Positive ? Box.maxZ : Box.minZ) + Normal.getStepZ() * FaceGap;
        }
        return new Placement(new Vec3(Door.getX() + X, Door.getY() + NoteLift, Door.getZ() + Z), Normal.toYRot(), Normal);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DoorData, BlockPos.ZERO);
        Builder.define(SideData, Direction.NORTH);
    }

    public BlockPos door() {
        return entityData.get(DoorData);
    }

    public Direction side() {
        return entityData.get(SideData);
    }

    @Nullable
    public UUID owner() {
        return Owner;
    }

    @Nullable
    public Placement placement() {
        return placement(level(), door(), side());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !level().isLoaded(door())) {
            return;
        }
        Placement Spot = placement();
        if (Spot == null) {
            drop();
            return;
        }
        if (position().distanceToSqr(Spot.Spot()) > 1.0E-6 || getYRot() != Spot.Yaw()) {
            setPos(Spot.Spot());
            setYRot(Spot.Yaw());
        }
    }

    public void drop() {
        if (isRemoved()) {
            return;
        }
        spawnAtLocation(new ItemStack(Scp4666Registry.SaintNote.get()));
        discard();
    }

    @Override
    public InteractionResult interact(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            Reader.run();
            return InteractionResult.SUCCESS;
        }
        if (isRemoved()) {
            return InteractionResult.CONSUME;
        }
        ItemStack Stack = new ItemStack(Scp4666Registry.SaintNote.get());
        if (!User.getInventory().add(Stack)) {
            User.drop(Stack, false);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.1f);
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (isInvulnerableTo(Source)) {
            return false;
        }
        if (!level().isClientSide) {
            drop();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Scp4666Registry.SaintNote.get());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderReach * RenderReach;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        NbtUtils.readBlockPos(Tag, "Door").ifPresent(Pos -> entityData.set(DoorData, Pos));
        Direction Side = Direction.byName(Tag.getString("Side"));
        entityData.set(SideData, Side != null && Side.getAxis().isHorizontal() ? Side : Direction.NORTH);
        Owner = Tag.hasUUID("Owner") ? Tag.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.put("Door", NbtUtils.writeBlockPos(door()));
        Tag.putString("Side", side().getName());
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
    }
}
