package net.rai.scp_unleashed.scp3008.gear.carry;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearTags;
import net.rai.scp_unleashed.scp3008.gear.network.Scp3008CarryActionPayload;
import net.rai.scp_unleashed.scp3008.gear.network.Scp3008CarrySyncPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class Scp3008Carry {
    public static final double SlowAmount = -0.35;
    public static final int SettleTicks = 6;
    public static final int ReleaseReach = 2;
    public static final ResourceLocation SlowId = Scp3008GearRegistry.location("scp3008_gear_carry_slow");
    public static final String MessagePrefix = "message.scp_unleashed.scp3008_gear_";
    private static final int RemoveFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final int PlaceFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Map<UUID, Long> LastAction = new HashMap<>();

    public enum Outcome {
        Lifted, Placed, Turned, Dropped, Ignored, Refused;

        public boolean worked() {
            return this == Lifted || this == Placed || this == Turned || this == Dropped;
        }
    }

    private Scp3008Carry() {
    }

    public static Scp3008Carried carried(Player User) {
        return User.hasData(Scp3008GearRegistry.Carry) ? User.getData(Scp3008GearRegistry.Carry) : Scp3008Carried.Empty;
    }

    public static boolean carrying(Player User) {
        return !carried(User).isEmpty();
    }

    public static boolean liftable(BlockState State) {
        return State.is(Scp3008GearTags.Liftable);
    }

    public static boolean fixed(Level World, BlockPos Pos, BlockState State) {
        return State.is(Scp3008GearTags.Unbreakable) || State.getDestroySpeed(World, Pos) < 0.0f;
    }

    public static List<BlockPos> partsOf(Level World, BlockPos Pos, BlockState State) {
        return Scp3008CarryParts.parts(World, Pos, State, State.is(Scp3008GearTags.Multiblock));
    }

    public static Outcome lift(Player User, BlockPos Pos) {
        Level World = User.level();
        if (World.isClientSide || carrying(User) || User.isSpectator() || !User.isAlive()) {
            return Outcome.Ignored;
        }
        BlockState State = World.getBlockState(Pos);
        if (!liftable(State)) {
            return Outcome.Ignored;
        }
        if (!User.mayBuild() || !World.mayInteract(User, Pos) || fixed(World, Pos, State)) {
            return refuse(User, "fixed");
        }
        List<BlockPos> Positions = partsOf(World, Pos, State);
        if (Positions.size() > Scp3008CarryParts.MaxParts) {
            return refuse(User, "too_big");
        }
        for (BlockPos At : Positions) {
            BlockState PartState = World.getBlockState(At);
            if (fixed(World, At, PartState) || !World.mayInteract(User, At)) {
                return refuse(User, "fixed");
            }
        }
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(World, Pos, State, User)).isCanceled()) {
            return refuse(User, "fixed");
        }
        BlockPos Anchor = Positions.get(0);
        List<Scp3008Carried.Part> Parts = new ArrayList<>();
        for (BlockPos At : Positions) {
            BlockState PartState = World.getBlockState(At);
            BlockEntity Entity = World.getBlockEntity(At);
            CompoundTag Data = Entity == null ? new CompoundTag() : Entity.saveWithId(World.registryAccess());
            Parts.add(new Scp3008Carried.Part(At.subtract(Anchor), PartState, Data));
        }
        for (BlockPos At : Positions) {
            BlockEntity Entity = World.getBlockEntity(At);
            if (Entity != null) {
                Clearable.tryClear(Entity);
                World.removeBlockEntity(At);
            }
        }
        for (BlockPos At : Positions) {
            World.setBlock(At, Blocks.AIR.defaultBlockState(), RemoveFlags);
        }
        for (BlockPos At : Positions) {
            World.blockUpdated(At, Blocks.AIR);
            Blocks.AIR.defaultBlockState().updateNeighbourShapes(World, At, Block.UPDATE_ALL);
        }
        Scp3008Carried Held = new Scp3008Carried(List.copyOf(Parts), Scp3008CarryParts.facingOf(partState(Parts)), 0);
        User.stopUsingItem();
        User.setData(Scp3008GearRegistry.Carry, Held);
        stamp(User);
        sync(User);
        Vec3 Centre = Vec3.atCenterOf(Pos);
        play(World, Centre, Scp3008GearRegistry.CrowbarPry.get(), 0.9f);
        play(World, Centre, Scp3008GearRegistry.CrowbarClank.get(), 0.7f);
        return Outcome.Lifted;
    }

    private static BlockState partState(List<Scp3008Carried.Part> Parts) {
        for (Scp3008Carried.Part Piece : Parts) {
            if (Piece.Offset().equals(BlockPos.ZERO)) {
                return Piece.State();
            }
        }
        return Parts.get(0).State();
    }

    public static Direction wantedFacing(Player User, Scp3008Carried Held) {
        return Scp3008CarryParts.turned(User.getDirection(), Held.Turns());
    }

    public static Outcome place(Player User, BlockPos Target) {
        Level World = User.level();
        Scp3008Carried Held = carried(User);
        if (World.isClientSide || Held.isEmpty()) {
            return Outcome.Ignored;
        }
        Outcome Result = put(User, World, Target, Held, wantedFacing(User, Held));
        if (Result == Outcome.Placed) {
            finish(User);
            play(World, Vec3.atBottomCenterOf(Target), Scp3008GearRegistry.CarrySetDown.get(), 0.9f);
        }
        return Result;
    }

    private static Outcome put(Player User, Level World, BlockPos Target, Scp3008Carried Held, Direction Wanted) {
        Rotation Turn = Scp3008CarryParts.rotationFor(Held.Facing(), Wanted);
        List<BlockPos> Spots = new ArrayList<>();
        List<BlockState> States = new ArrayList<>();
        for (Scp3008Carried.Part Piece : Held.Parts()) {
            BlockPos At = Target.offset(Scp3008CarryParts.rotated(Piece.Offset(), Turn));
            BlockState State = Scp3008CarryParts.rotated(Piece.State(), Turn);
            if (!World.isInWorldBounds(At) || !World.getBlockState(At).canBeReplaced() || !World.mayInteract(User, At)
                    || !World.isUnobstructed(State, At, CollisionContext.empty())) {
                return Outcome.Refused;
            }
            Spots.add(At);
            States.add(State);
        }
        List<BlockSnapshot> Snapshots = new ArrayList<>();
        List<BlockState> Before = new ArrayList<>();
        for (BlockPos At : Spots) {
            Snapshots.add(BlockSnapshot.create(World.dimension(), World, At));
            Before.add(World.getBlockState(At));
        }
        World.setBlock(Spots.get(0), States.get(0), PlaceFlags);
        if (missing(World, Spots, States) && World.getBlockState(Spots.get(0)).is(States.get(0).getBlock())) {
            Scp3008CarryParts.placeWhole(World, Spots.get(0), World.getBlockState(Spots.get(0)));
        }
        for (int Index = 1; Index < Spots.size(); Index++) {
            if (!World.getBlockState(Spots.get(Index)).is(States.get(Index).getBlock())) {
                World.setBlock(Spots.get(Index), States.get(Index), PlaceFlags);
            }
        }
        boolean Survives = !missing(World, Spots, States);
        for (int Index = 0; Index < Spots.size(); Index++) {
            if (!World.getBlockState(Spots.get(Index)).canSurvive(World, Spots.get(Index))) {
                Survives = false;
            }
        }
        boolean Stopped = Survives && (Snapshots.size() == 1 ? EventHooks.onBlockPlace(User, Snapshots.get(0), Direction.UP)
                : EventHooks.onMultiBlockPlace(User, Snapshots, Direction.UP));
        if (!Survives || Stopped) {
            for (int Index = 0; Index < Spots.size(); Index++) {
                World.removeBlockEntity(Spots.get(Index));
                World.setBlock(Spots.get(Index), Before.get(Index), PlaceFlags | Block.UPDATE_SUPPRESS_DROPS);
            }
            return Outcome.Refused;
        }
        for (int Index = 0; Index < Spots.size(); Index++) {
            CompoundTag Data = Held.Parts().get(Index).Data();
            BlockEntity Entity = World.getBlockEntity(Spots.get(Index));
            if (Entity != null && !Data.isEmpty()) {
                Entity.loadWithComponents(Data, World.registryAccess());
                Entity.setChanged();
            }
        }
        for (int Index = 0; Index < Spots.size(); Index++) {
            BlockPos At = Spots.get(Index);
            BlockState Placed = World.getBlockState(At);
            World.blockUpdated(At, Placed.getBlock());
            Placed.updateNeighbourShapes(World, At, Block.UPDATE_ALL);
            World.sendBlockUpdated(At, Before.get(Index), Placed, Block.UPDATE_ALL);
        }
        return Outcome.Placed;
    }

    private static boolean missing(Level World, List<BlockPos> Spots, List<BlockState> States) {
        for (int Index = 0; Index < Spots.size(); Index++) {
            if (!World.getBlockState(Spots.get(Index)).is(States.get(Index).getBlock())) {
                return true;
            }
        }
        return false;
    }

    public static Outcome placeAhead(Player User) {
        Level World = User.level();
        HitResult Hit = User.pick(User.blockInteractionRange(), 1.0f, false);
        if (Hit instanceof BlockHitResult Aimed && Hit.getType() == HitResult.Type.BLOCK) {
            BlockPos Clicked = Aimed.getBlockPos();
            BlockPos Target = World.getBlockState(Clicked).canBeReplaced() ? Clicked : Clicked.relative(Aimed.getDirection());
            return place(User, Target);
        }
        return place(User, User.blockPosition().relative(User.getDirection()));
    }

    public static Outcome turn(Player User) {
        Scp3008Carried Held = carried(User);
        if (User.level().isClientSide || Held.isEmpty()) {
            return Outcome.Ignored;
        }
        User.setData(Scp3008GearRegistry.Carry, Held.turned());
        sync(User);
        play(User.level(), User.position(), Scp3008GearRegistry.CarryTurn.get(), 0.6f);
        return Outcome.Turned;
    }

    public static Outcome act(Player User, int Action) {
        if (!carrying(User) || !User.isAlive()) {
            return Outcome.Ignored;
        }
        if (User.level().getGameTime() - LastAction.getOrDefault(User.getUUID(), Long.MIN_VALUE / 2) < SettleTicks) {
            return Outcome.Ignored;
        }
        stamp(User);
        if (Action == Scp3008CarryActionPayload.Drop) {
            Outcome Result = place(User, User.blockPosition().relative(User.getDirection()));
            return Result == Outcome.Placed ? Result : release(User);
        }
        if (User.isShiftKeyDown()) {
            return turn(User);
        }
        Outcome Result = placeAhead(User);
        if (Result == Outcome.Refused) {
            refuse(User, "no_room");
        }
        return Result;
    }

    public static Outcome release(Player User) {
        Level World = User.level();
        Scp3008Carried Held = carried(User);
        if (World.isClientSide || Held.isEmpty()) {
            return Outcome.Ignored;
        }
        Direction Wanted = wantedFacing(User, Held);
        BlockPos Feet = User.blockPosition();
        for (int Radius = 0; Radius <= ReleaseReach; Radius++) {
            for (int Up = 0; Up <= 1; Up++) {
                for (int Dx = -Radius; Dx <= Radius; Dx++) {
                    for (int Dz = -Radius; Dz <= Radius; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                            continue;
                        }
                        BlockPos Spot = Feet.offset(Dx, Up, Dz);
                        if (put(User, World, Spot, Held, Wanted) == Outcome.Placed) {
                            finish(User);
                            play(World, Vec3.atBottomCenterOf(Spot), Scp3008GearRegistry.CarrySetDown.get(), 0.9f);
                            return Outcome.Placed;
                        }
                    }
                }
            }
        }
        drop(World, User.position(), Held);
        finish(User);
        play(World, User.position(), Scp3008GearRegistry.CarrySetDown.get(), 0.9f);
        return Outcome.Dropped;
    }

    public static void drop(Level World, Vec3 Where, Scp3008Carried Held) {
        BlockPos Pos = BlockPos.containing(Where);
        Set<Item> Kinds = new LinkedHashSet<>();
        for (Scp3008Carried.Part Piece : Held.Parts()) {
            Item Kind = Piece.State().getBlock().asItem();
            if (Kind != Items.AIR) {
                Kinds.add(Kind);
            }
            if (Piece.Data().isEmpty()) {
                continue;
            }
            BlockEntity Loaded = BlockEntity.loadStatic(Pos, Piece.State(), Piece.Data(), World.registryAccess());
            if (Loaded == null) {
                continue;
            }
            Loaded.setLevel(World);
            if (Loaded instanceof RandomizableContainer Loot && Loot.getLootTable() != null) {
                Loot.unpackLootTable(null);
            }
            if (Loaded instanceof Container Box) {
                Containers.dropContents(World, Pos, Box);
            }
        }
        for (Item Kind : Kinds) {
            Containers.dropItemStack(World, Where.x, Where.y + 0.5, Where.z, new ItemStack(Kind));
        }
    }

    private static void finish(Player User) {
        User.removeData(Scp3008GearRegistry.Carry);
        stamp(User);
        sync(User);
        slow(User, false);
    }

    public static void tick(Player User) {
        if (User.level().isClientSide) {
            return;
        }
        boolean Carrying = carrying(User);
        slow(User, Carrying);
        if (Carrying && (User.isSpectator() || User.isPassenger() || User.isSleeping())) {
            release(User);
        }
    }

    private static void slow(Player User, boolean Carrying) {
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed == null) {
            return;
        }
        boolean Present = Speed.hasModifier(SlowId);
        if (Carrying && !Present) {
            Speed.addTransientModifier(new AttributeModifier(SlowId, SlowAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!Carrying && Present) {
            Speed.removeModifier(SlowId);
        }
    }

    public static void forget(Player User) {
        LastAction.remove(User.getUUID());
    }

    private static void stamp(Player User) {
        LastAction.put(User.getUUID(), User.level().getGameTime());
    }

    private static Outcome refuse(Player User, String Reason) {
        User.displayClientMessage(Component.translatable(MessagePrefix + Reason).withStyle(ChatFormatting.GRAY), true);
        play(User.level(), User.position(), Scp3008GearRegistry.CarryRefuse.get(), 0.5f);
        return Outcome.Refused;
    }

    private static void play(Level World, Vec3 Where, SoundEvent Sound, float Volume) {
        World.playSound(null, Where.x, Where.y, Where.z, Sound, SoundSource.PLAYERS, Volume, 0.92f + World.random.nextFloat() * 0.16f);
    }

    public static Scp3008CarrySyncPayload payloadFor(Player User) {
        Scp3008Carried Held = carried(User);
        List<Scp3008CarrySyncPayload.Shown> Shown = new ArrayList<>();
        for (Scp3008Carried.Part Piece : Held.Parts()) {
            Shown.add(new Scp3008CarrySyncPayload.Shown(Piece.Offset(), Piece.State()));
        }
        int Facing = Held.Facing().map(Direction::get2DDataValue).orElse(-1);
        return new Scp3008CarrySyncPayload(User.getId(), Shown, Facing, Held.Turns());
    }

    public static void sync(Player User) {
        if (User instanceof ServerPlayer Server && Server.connection != null && User.level() instanceof ServerLevel) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(Server, payloadFor(User));
        }
    }

    public static void sendTo(ServerPlayer Watcher, Player Target) {
        if (carrying(Target) && Watcher.connection != null) {
            PacketDistributor.sendToPlayer(Watcher, payloadFor(Target));
        }
    }

    public static Optional<Direction> facingOfPayload(int Facing) {
        return Facing < 0 ? Optional.empty() : Optional.of(Direction.from2DDataValue(Facing));
    }
}
