package net.rai.scp_unleashed.scp372;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp372.network.Scp372PulsePayload;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp372AnchorBlockEntity extends BlockEntity implements WireDevice {
    public static final int PowerDraw = 45;
    public static final int PowerGrace = 10;
    public static final int LoadGrace = 40;
    public static final int ChargeTicks = 48;
    public static final int PulseTick = 30;
    public static final int SnapTick = 44;
    public static final double PulseReach = 16.0;
    public static final float PulseStrength = 1.0f;
    public static final int HumMin = 120;
    public static final int HumMax = 160;
    public static final double HumReach = 12.0;
    public static final float HumVolume = 0.5f;
    public static final float SwitchVolume = 0.8f;
    public static final float ChargeVolume = 1.0f;
    public static final float SnapVolume = 1.0f;
    public static final int CheckInterval = 10;
    public static final int LostGrace = 200;
    public static final double LensHeight = 12.5 / 16.0;
    public static final double LensReach = 5.0 / 16.0;
    public static final String Prefix = "block.scp_unleashed." + Scp372Registry.AnchorId + ".";

    public enum Stage {
        Idle,
        Charging,
        Holding
    }

    public enum Release {
        Power,
        Broken,
        Died,
        Lost,
        Command
    }

    private int Received;
    private int LastReceived;
    private long LastFed = Long.MIN_VALUE / 2;
    private boolean Loaded;
    private Stage Phase = Stage.Idle;
    private int Charge;
    @Nullable
    private UUID Held;
    @Nullable
    private Vec3 Target;
    private int Missing;
    private int HumTimer = HumMin;

    public Scp372AnchorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp372Registry.AnchorEntity.get(), Pos, State);
    }

    public static void tick(Level World, BlockPos Pos, BlockState State, Scp372AnchorBlockEntity Anchor) {
        if (World instanceof ServerLevel Server) {
            Anchor.tickServer(Server, Pos, State);
        }
    }

    public Stage stage() {
        return Phase;
    }

    public boolean isHolding() {
        return Phase == Stage.Holding;
    }

    public boolean isCharging() {
        return Phase == Stage.Charging;
    }

    public boolean holds(UUID Creature) {
        return Phase == Stage.Holding && Creature.equals(Held);
    }

    @Nullable
    public UUID held() {
        return Held;
    }

    public int getLastReceived() {
        return LastReceived;
    }

    public boolean isPowered() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp372AnchorBlock.Light) && State.getValue(Scp372AnchorBlock.Light) != Scp372AnchorLight.Off;
    }

    public Direction facing() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp372AnchorBlock.Facing) ? State.getValue(Scp372AnchorBlock.Facing) : Direction.NORTH;
    }

    public Vec3 lens() {
        Direction Front = facing();
        return Vec3.atLowerCornerOf(worldPosition).add(0.5 + Front.getStepX() * LensReach, LensHeight, 0.5 + Front.getStepZ() * LensReach);
    }

    private void tickServer(ServerLevel World, BlockPos Pos, BlockState State) {
        long Now = World.getGameTime();
        boolean WasPowered = State.getValue(Scp372AnchorBlock.Light) != Scp372AnchorLight.Off;
        if (!Loaded) {
            Loaded = true;
            if (WasPowered) {
                LastFed = Now + LoadGrace;
            }
            validate(World, Pos);
        }
        LastReceived = Received;
        if (Received >= PowerDraw) {
            LastFed = Math.max(LastFed, Now);
        }
        Received = 0;
        boolean Powered = Now - LastFed <= PowerGrace;
        if (Powered != WasPowered) {
            play(World, Powered ? List.of(Scp372Registry.Start) : List.of(Scp372Registry.Stop), SwitchVolume, 1.0f);
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
        }
        if (!Powered) {
            if (Phase == Stage.Charging) {
                abort(World);
            } else if (Phase == Stage.Holding) {
                release(Release.Power);
            }
        }
        if (Phase == Stage.Charging) {
            charge(World);
        } else if (Phase == Stage.Holding) {
            watch(World, Pos);
        }
        if (Powered && Phase != Stage.Charging && --HumTimer <= 0) {
            HumTimer = Mth.nextInt(World.getRandom(), HumMin, HumMax);
            if (World.hasNearbyAlivePlayer(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, HumReach)) {
                play(World, Scp372Registry.HumLayers, Phase == Stage.Holding ? HumVolume : HumVolume * 0.6f, Phase == Stage.Holding ? 1.0f : 0.9f);
            }
        }
        Scp372AnchorLight Wanted = !Powered ? Scp372AnchorLight.Off : Phase == Stage.Holding ? Scp372AnchorLight.Holding : Scp372AnchorLight.Powered;
        BlockState Current = World.getBlockState(Pos);
        if (Current.is(Scp372Registry.Anchor.get()) && Current.getValue(Scp372AnchorBlock.Light) != Wanted) {
            World.setBlock(Pos, Current.setValue(Scp372AnchorBlock.Light, Wanted), Block.UPDATE_CLIENTS);
        }
    }

    private void validate(ServerLevel World, BlockPos Pos) {
        Scp372State State = Scp372State.get(World.getServer());
        if (Phase == Stage.Charging) {
            Phase = Stage.Idle;
            setChanged();
        }
        if (Phase == Stage.Holding && (Held == null || !State.isHeldAt(World, Pos) || !State.isHeldBy(Held))) {
            Phase = Stage.Idle;
            Held = null;
            setChanged();
        }
    }

    public void use(ServerPlayer User) {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        if (!isPowered()) {
            tell(User, "dark_lens", ChatFormatting.GRAY);
            play(World, List.of(Scp372Registry.Refuse), 0.6f, 0.8f);
            return;
        }
        if (Phase == Stage.Charging) {
            return;
        }
        if (Phase == Stage.Holding) {
            tell(User, "steady", ChatFormatting.DARK_AQUA);
            return;
        }
        if (!Scp372Unlock.unlocked(User)) {
            tell(User, "refuse", ChatFormatting.GRAY);
            play(World, List.of(Scp372Registry.Refuse), 0.7f, 1.0f);
            return;
        }
        if (!begin(World)) {
            tell(User, "elsewhere", ChatFormatting.GRAY);
            play(World, List.of(Scp372Registry.Refuse), 0.7f, 0.9f);
        }
    }

    private static void tell(ServerPlayer User, String Key, ChatFormatting Style) {
        User.displayClientMessage(Component.translatable(Prefix + Key).withStyle(Style, ChatFormatting.ITALIC), true);
    }

    public boolean begin(ServerLevel World) {
        if (!isPowered() || Phase != Stage.Idle) {
            return false;
        }
        Scp372State State = Scp372State.get(World.getServer());
        if (State.isHeld()) {
            return false;
        }
        Optional<Vec3> Spot = Scp372Anchoring.spawnSpot(World, worldPosition, facing(), Scp372Registry.Insect.get().getDimensions());
        if (Spot.isEmpty() || !State.beginCharging(World, worldPosition)) {
            return false;
        }
        Target = Spot.get();
        Phase = Stage.Charging;
        Charge = 0;
        setChanged();
        play(World, Scp372Registry.ChargeLayers, ChargeVolume, 1.0f);
        return true;
    }

    private void charge(ServerLevel World) {
        Charge++;
        Vec3 Lens = lens();
        Vec3 Spot = Target != null ? Target.add(0.0, 0.24, 0.0) : Lens;
        float Share = Math.min(1.0f, Charge / (float) ChargeTicks);
        int Count = 1 + Math.round(Share * 4.0f);
        for (int Index = 0; Index < Count; Index++) {
            double Along = World.getRandom().nextDouble();
            Vec3 Point = Lens.lerp(Spot, Along * Share);
            Vec3 Toward = Spot.subtract(Point);
            World.sendParticles(Scp372Registry.Flare.get(), Point.x, Point.y, Point.z, 0, Toward.x, Toward.y, Toward.z, 0.06);
        }
        if (Charge % 6 == 0) {
            World.sendParticles(Scp372Registry.Flare.get(), Lens.x, Lens.y, Lens.z, 3, 0.08, 0.08, 0.08, 0.02);
        }
        if (Charge == PulseTick) {
            PacketDistributor.sendToPlayersNear(World, null, Lens.x, Lens.y, Lens.z, PulseReach, new Scp372PulsePayload(PulseStrength));
        }
        if (Charge == SnapTick) {
            play(World, Scp372Registry.SnapLayers, SnapVolume, 1.0f);
            World.sendParticles(Scp372Registry.Flare.get(), Spot.x, Spot.y, Spot.z, 30, 0.36, 0.18, 0.36, 0.072);
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Spot.x, Spot.y, Spot.z, 12, 0.3, 0.18, 0.3, 0.12);
        }
        if (Charge >= ChargeTicks) {
            materialize(World);
        }
    }

    private void materialize(ServerLevel World) {
        Scp372State State = Scp372State.get(World.getServer());
        Optional<Vec3> Spot = Scp372Anchoring.spawnSpot(World, worldPosition, facing(), Scp372Registry.Insect.get().getDimensions());
        Scp372Insect Creature = Scp372Registry.Insect.get().create(World);
        if (Spot.isEmpty() || Creature == null || State.isHeld()) {
            abort(World);
            return;
        }
        Vec3 Where = Spot.get();
        Vec3 Away = Where.subtract(Vec3.atBottomCenterOf(worldPosition));
        float Yaw = (float) (Mth.atan2(Away.z, Away.x) * Mth.RAD_TO_DEG) - 90.0f;
        Creature.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        Creature.setYHeadRot(Yaw);
        Creature.setYBodyRot(Yaw);
        Creature.bindTo(worldPosition);
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Where)), MobSpawnType.EVENT, null);
        Creature.materialize();
        if (!World.addFreshEntity(Creature)) {
            abort(World);
            return;
        }
        Phase = Stage.Holding;
        Held = Creature.getUUID();
        Target = null;
        Missing = 0;
        Charge = 0;
        State.hold(World, worldPosition, Held);
        Scp372Glimpses.hushAll(World.getServer(), true);
        setChanged();
    }

    private void abort(ServerLevel World) {
        Phase = Stage.Idle;
        Charge = 0;
        Target = null;
        Scp372State.get(World.getServer()).endCharging(World, worldPosition);
        Vec3 Lens = lens();
        World.sendParticles(Scp372Registry.Flare.get(), Lens.x, Lens.y, Lens.z, 8, 0.15, 0.15, 0.15, 0.04);
        play(World, Scp372Registry.FlickerLayers, 0.6f, 1.3f);
        setChanged();
    }

    private void watch(ServerLevel World, BlockPos Pos) {
        if (World.getGameTime() % CheckInterval != 0 || Held == null) {
            return;
        }
        Scp372State State = Scp372State.get(World.getServer());
        if (!State.isHeldAt(World, Pos) || !State.isHeldBy(Held)) {
            Phase = Stage.Idle;
            Held = null;
            setChanged();
            return;
        }
        Entity Found = World.getEntity(Held);
        if (Found instanceof Scp372Insect Creature && Creature.isAlive() && !Creature.isFlickering()) {
            Missing = 0;
            return;
        }
        Missing += CheckInterval;
        if (Missing >= LostGrace) {
            release(Release.Lost);
        }
    }

    public void release(Release Why) {
        if (Phase != Stage.Holding || !(level instanceof ServerLevel World)) {
            return;
        }
        UUID Was = Held;
        Phase = Stage.Idle;
        Held = null;
        Missing = 0;
        setChanged();
        if (Was != null && World.getEntity(Was) instanceof Scp372Insect Creature && Creature.isAlive()) {
            Creature.flickerOut();
        }
        Vec3 Lens = lens();
        World.sendParticles(Scp372Registry.Flare.get(), Lens.x, Lens.y, Lens.z, 18, 0.25, 0.2, 0.25, 0.1);
        play(World, Scp372Registry.FlickerLayers, Why == Release.Broken ? 1.0f : 0.8f, 0.85f);
        Scp372Anchoring.freed(World.getServer(), World.getGameTime());
    }

    public void broken() {
        if (level instanceof ServerLevel World) {
            if (Phase == Stage.Charging) {
                abort(World);
            }
            release(Release.Broken);
        }
    }

    private void play(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Shifted = Pitch * (0.96f + World.getRandom().nextFloat() * 0.08f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, worldPosition, Layer.get(), SoundSource.BLOCKS, Volume, Shifted);
        }
    }

    @Override
    public int getPowerDemand() {
        return PowerDraw;
    }

    @Override
    public void insertPower(int Amount) {
        Received += Amount;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.saveAdditional(Tag, Lookup);
        Tag.putString("Stage", Phase == Stage.Holding ? Stage.Holding.name() : Stage.Idle.name());
        if (Phase == Stage.Holding && Held != null) {
            Tag.putUUID("Held", Held);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.loadAdditional(Tag, Lookup);
        Phase = Stage.Holding.name().equals(Tag.getString("Stage")) && Tag.hasUUID("Held") ? Stage.Holding : Stage.Idle;
        Held = Phase == Stage.Holding ? Tag.getUUID("Held") : null;
        Loaded = false;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Synced = super.getUpdateTag(Lookup);
        Synced.putString("Stage", Phase.name());
        return Synced;
    }
}
