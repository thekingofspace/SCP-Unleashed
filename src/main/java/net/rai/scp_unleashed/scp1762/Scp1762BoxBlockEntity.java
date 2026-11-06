package net.rai.scp_unleashed.scp1762;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class Scp1762BoxBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final byte Closed = 0;
    public static final byte Open = 1;
    public static final byte Returning = 2;
    public static final byte Recall = 3;
    public static final int LockMin = 1200;
    public static final int LockMax = 2400;
    public static final int PourTicks = 40;
    public static final int NoteDelay = 24;
    public static final int ReturnSpacing = 5;
    public static final int ReturnTimeout = 900;
    public static final int RecallSpacing = 2;
    public static final int RecallClose = 34;
    public static final int RecallTimeout = 600;
    public static final int RecallGrace = 12;
    public static final int PoofSpread = 6;
    public static final int ArmTicks = 600;
    public static final double ArmReach = 8.0;
    public static final double SeamHeight = 0.29;
    public static final int PruneDelay = 60;
    public static final double Radius = 10.0;
    public static final double Ceiling = 6.0;
    public static final double Floor = 1.0;
    public static final double MouthHeight = 0.42;
    public static final int[] KindWeights = {30, 20, 25, 25};
    public static final Vector3f SmokeColour = new Vector3f(0.78f, 0.1f, 0.12f);
    public static final Vector3f SmokeDeep = new Vector3f(0.5f, 0.04f, 0.07f);
    public static final String StageKey = "Stage";
    private static final Set<Scp1762BoxBlockEntity> Active = Collections.newSetFromMap(new WeakHashMap<>());
    private static final RawAnimation ClosedLoop = RawAnimation.begin().thenLoop("closed");
    private static final RawAnimation OpenedLoop = RawAnimation.begin().thenLoop("opened");
    private static final RawAnimation OpenPlay = RawAnimation.begin().thenPlayAndHold("open");
    private static final RawAnimation ClosePlay = RawAnimation.begin().thenPlayAndHold("close");
    private static final RawAnimation RattlePlay = RawAnimation.begin().thenPlay("rattle");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> Out = new LinkedHashSet<>();
    private final Set<UUID> Called = new LinkedHashSet<>();
    private int Stage;
    private byte Phase = Closed;
    private int Timer;
    private int Planned;
    private int Poured;
    private int Session;
    private int Age;
    private boolean NoteGiven;
    @Nullable
    private UUID Opener;
    @Nullable
    private Boolean SeenOpen;
    private boolean Moved;
    @Nullable
    private UUID ArmedBy;
    private long ArmedUntil;

    public Scp1762BoxBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp1762Registry.BoxEntity.get(), Pos, State);
    }

    public static List<Scp1762BoxBlockEntity> active() {
        synchronized (Active) {
            return new ArrayList<>(Active);
        }
    }

    public int stage() {
        return Stage;
    }

    public void setStage(int Value) {
        Stage = Math.max(0, Math.min(Scp1762Stage.Last, Value));
        changed();
    }

    public byte phase() {
        return Phase;
    }

    public int timer() {
        return Timer;
    }

    public void setTimer(int Ticks) {
        Timer = Ticks;
    }

    public int planned() {
        return Planned;
    }

    public int session() {
        return Session;
    }

    public List<UUID> out() {
        return List.copyOf(Out);
    }

    public boolean guarded() {
        return Phase != Closed && !Scp1762Stage.free(Stage);
    }

    public boolean listening() {
        return Phase == Open && Stage == Scp1762Stage.First && !Out.isEmpty();
    }

    public boolean dormant() {
        return Phase == Closed && Scp1762Stage.free(Stage) && !Scp1762BoxBlock.isOpen(getBlockState());
    }

    public boolean armed(UUID Player, long Now) {
        return dormant() && Player.equals(ArmedBy) && Now <= ArmedUntil;
    }

    public void setArmedUntil(long Time) {
        ArmedUntil = Time;
        setChanged();
    }

    public void arm(ServerLevel World, Player User) {
        ArmedBy = User.getUUID();
        ArmedUntil = World.getGameTime() + ArmTicks;
        Vec3 Seam = centre().add(0.0, SeamHeight, 0.0);
        World.sendParticles(new DustParticleOptions(SmokeColour, 0.7f), Seam.x, Seam.y, Seam.z, 5, 0.3, 0.02, 0.22, 0.004);
        World.playSound(null, Seam.x, Seam.y, Seam.z, Scp1762Registry.Rustle.get(), SoundSource.BLOCKS, 0.3f, 0.8f + World.getRandom().nextFloat() * 0.1f);
        setChanged();
    }

    public boolean revive(ServerLevel World, Player User) {
        if (!armed(User.getUUID(), World.getGameTime())) {
            return false;
        }
        ArmedBy = null;
        ArmedUntil = 0L;
        Stage = Scp1762Stage.First;
        Vec3 Mouth = mouth();
        smoke(World, Mouth.add(0.0, 0.15, 0.0), 0.5);
        layers(World, Scp1762Registry.NoteLayers, 0.6f, 0.85f);
        open(World, User);
        return true;
    }

    public boolean owns(UUID Dragon, int Code) {
        return Phase != Closed && Code == Session && Out.contains(Dragon);
    }

    public Vec3 mouth() {
        return Vec3.atBottomCenterOf(worldPosition).add(0.0, MouthHeight, 0.0);
    }

    public Vec3 centre() {
        return Vec3.atBottomCenterOf(worldPosition);
    }

    private void track(boolean On) {
        synchronized (Active) {
            if (On) {
                Active.add(this);
            } else {
                Active.remove(this);
            }
        }
    }

    public void interact(ServerLevel World, Player User) {
        if (Phase == Closed) {
            open(World, User);
            return;
        }
        if (Scp1762Stage.free(Stage) && Phase == Open) {
            finish(World, Stage);
            return;
        }
        triggerAnim("rattle", "rattle");
        layers(World, Scp1762Registry.RattleLayers, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        if (User instanceof ServerPlayer Viewer) {
            Viewer.displayClientMessage(Component.translatable("message.scp_unleashed.scp1762.stuck"), true);
        }
    }

    public void open(ServerLevel World, @Nullable Player User) {
        RandomSource Random = World.getRandom();
        Session = Random.nextInt() | 1;
        Phase = Open;
        Age = 0;
        Out.clear();
        Called.clear();
        Planned = Scp1762Stage.count(Stage, Random);
        Poured = 0;
        NoteGiven = Scp1762Stage.note(Stage) < 0;
        Opener = User != null ? User.getUUID() : null;
        Timer = Scp1762Stage.free(Stage) ? 0 : LockMin + Random.nextInt(LockMax - LockMin + 1);
        setOpen(World, true);
        layers(World, Scp1762Registry.OpenLayers, 0.8f, 0.95f + Random.nextFloat() * 0.1f);
        Vec3 Mouth = mouth();
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), Mouth.x, Mouth.y + 0.1, Mouth.z, 10, 0.2, 0.05, 0.15, 0.06);
        track(true);
        changed();
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp1762BoxBlockEntity Self) {
        if (Self.Phase == Closed || !(World instanceof ServerLevel Server)) {
            return;
        }
        Self.track(true);
        Self.Age++;
        switch (Self.Phase) {
            case Open -> Self.openTick(Server);
            case Returning -> Self.returnTick(Server);
            case Recall -> Self.recallTick(Server);
            default -> {
            }
        }
        if (Self.Age % 20 == 0) {
            Self.setChanged();
        }
    }

    private void openTick(ServerLevel World) {
        int Target = Planned * Math.min(Age, PourTicks) / PourTicks;
        while (Poured < Target) {
            pour(World, Poured);
            Poured++;
        }
        if (!NoteGiven && Age >= NoteDelay) {
            NoteGiven = true;
            note(World);
        }
        if (Scp1762Stage.free(Stage)) {
            return;
        }
        if (Timer > 0) {
            Timer--;
        }
        if (Age > PruneDelay) {
            prune(World);
        }
        if (Timer <= 0 && Poured >= Planned) {
            if (Scp1762Stage.recallsAtEnd(Stage)) {
                recall(World);
            } else {
                Phase = Returning;
                Age = 0;
                layers(World, List.of(Scp1762Registry.Rustle), 0.5f, 1.0f);
                changed();
            }
        }
    }

    private void returnTick(ServerLevel World) {
        callHome(World, ReturnSpacing);
        if (Age > PruneDelay) {
            prune(World);
        }
        if (Out.isEmpty() || Age > ReturnTimeout) {
            finish(World, Scp1762Stage.afterReturn(Stage));
        }
    }

    private void callHome(ServerLevel World, int Spacing) {
        if (Age % Spacing == 0) {
            Scp1762Dragon Next = null;
            double Nearest = -1.0;
            for (UUID Id : Out) {
                if (Called.contains(Id) || !(World.getEntity(Id) instanceof Scp1762Dragon Found)) {
                    continue;
                }
                double Distance = Found.distanceToSqr(mouth());
                if (Next == null || Distance < Nearest) {
                    Next = Found;
                    Nearest = Distance;
                }
            }
            if (Next != null) {
                Called.add(Next.getUUID());
                Next.comeHome();
            }
        }
    }

    private void recallTick(ServerLevel World) {
        callHome(World, RecallSpacing);
        if (Age > PruneDelay) {
            prune(World);
        }
        if (Age == RecallTimeout) {
            RandomSource Random = World.getRandom();
            for (UUID Id : List.copyOf(Out)) {
                if (World.getEntity(Id) instanceof Scp1762Dragon Found) {
                    Found.poof(Random.nextInt(PoofSpread));
                }
            }
        }
        if (Age >= RecallClose && Out.isEmpty() || Age > RecallTimeout + RecallGrace) {
            Vec3 Mouth = mouth();
            smoke(World, Mouth.add(0.0, 0.25, 0.0), 0.6);
            layers(World, Scp1762Registry.SmokeLayers, 0.6f, 1.05f + World.getRandom().nextFloat() * 0.1f);
            finish(World, Scp1762Stage.afterRecall(Stage));
        }
    }

    public void recall(ServerLevel World) {
        if (Phase == Closed || Phase == Recall) {
            return;
        }
        Phase = Recall;
        Age = 0;
        Poured = Planned;
        Vec3 Mouth = mouth();
        smoke(World, Mouth.add(0.0, 0.2, 0.0), 1.0);
        layers(World, Scp1762Registry.SmokeLayers, 1.0f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        changed();
    }

    public static void smoke(ServerLevel World, Vec3 At, double Size) {
        int Count = (int) Math.round(36 * Size);
        World.sendParticles(new DustParticleOptions(SmokeColour, (float) (2.6 * Size)), At.x, At.y, At.z, Count, 0.35 * Size, 0.3 * Size, 0.35 * Size, 0.02);
        World.sendParticles(new DustParticleOptions(SmokeDeep, (float) (1.8 * Size)), At.x, At.y + 0.2, At.z, Count / 2, 0.45 * Size, 0.4 * Size, 0.45 * Size, 0.02);
        World.sendParticles(ParticleTypes.POOF, At.x, At.y, At.z, Math.max(2, Count / 4), 0.25 * Size, 0.2 * Size, 0.25 * Size, 0.03);
    }

    private void finish(ServerLevel World, int NewStage) {
        vanishAll(World);
        Stage = NewStage;
        Phase = Closed;
        Age = 0;
        Timer = 0;
        Planned = 0;
        Poured = 0;
        Called.clear();
        Opener = null;
        setOpen(World, false);
        layers(World, Scp1762Registry.CloseLayers, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        track(false);
        changed();
    }

    public void vanishAll(ServerLevel World) {
        for (UUID Id : List.copyOf(Out)) {
            Entity Found = World.getEntity(Id);
            if (Found != null) {
                Vec3 At = Found.position();
                World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), At.x, At.y + 0.15, At.z, 3, 0.1, 0.1, 0.1, 0.03);
                Found.discard();
            }
        }
        Out.clear();
        track(false);
    }

    private void prune(ServerLevel World) {
        Out.removeIf(Id -> {
            Entity Found = World.getEntity(Id);
            return Found == null && World.isLoaded(worldPosition) || Found != null && !Found.isAlive();
        });
    }

    public void arrived(ServerLevel World, Scp1762Dragon Dragon) {
        if (Out.remove(Dragon.getUUID())) {
            Vec3 Mouth = mouth();
            if (Phase == Recall) {
                smoke(World, Mouth.add(0.0, 0.12, 0.0), 0.25);
            } else {
                World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), Mouth.x, Mouth.y, Mouth.z, 2, 0.08, 0.04, 0.08, 0.02);
            }
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1762Registry.Home.get(), SoundSource.BLOCKS, 0.35f, 0.9f + World.getRandom().nextFloat() * 0.3f);
            setChanged();
        }
    }

    public void forget(UUID Dragon) {
        if (Out.remove(Dragon)) {
            setChanged();
        }
    }

    private Scp1762Kind pick(RandomSource Random, int Index) {
        Scp1762Kind[] Kinds = Scp1762Kind.values();
        if (Planned >= Kinds.length && Index < Kinds.length) {
            return Kinds[Math.floorMod(Index + Session, Kinds.length)];
        }
        int Total = 0;
        for (int Weight : KindWeights) {
            Total += Weight;
        }
        int Roll = Random.nextInt(Total);
        for (int Kind = 0; Kind < Kinds.length; Kind++) {
            Roll -= KindWeights[Kind];
            if (Roll < 0) {
                return Kinds[Kind];
            }
        }
        return Kinds[0];
    }

    private void pour(ServerLevel World, int Index) {
        RandomSource Random = World.getRandom();
        Scp1762Kind Kind = pick(Random, Index);
        Scp1762Dragon Made = Scp1762Registry.dragon(Kind).create(World);
        if (Made == null) {
            return;
        }
        Vec3 Mouth = mouth();
        BlockState State = getBlockState();
        Direction Facing = State.getBlock() instanceof Scp1762BoxBlock ? State.getValue(Scp1762BoxBlock.Facing) : Direction.NORTH;
        Made.bind(worldPosition, Session, Scp1762Palette.pick(Random));
        Made.emerge(Random, Facing);
        World.addFreshEntity(Made);
        Out.add(Made.getUUID());
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), Mouth.x, Mouth.y + 0.05, Mouth.z, 2, 0.1, 0.02, 0.1, 0.04);
        if (Index % 3 == 0) {
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1762Registry.Flap.get(), SoundSource.NEUTRAL, 0.5f, 1.1f + Random.nextFloat() * 0.3f);
        }
        if (Index % 5 == 2) {
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1762Registry.Roar.get(), SoundSource.NEUTRAL, 0.35f, 1.0f + Random.nextFloat() * 0.2f);
        }
    }

    private void note(ServerLevel World) {
        int Line = Scp1762Stage.note(Stage);
        if (Line < 0) {
            return;
        }
        ItemStack Stack = new ItemStack(Scp1762Registry.NoteItem.get());
        Stack.set(Scp1762Registry.NoteLine.get(), Line);
        Vec3 Mouth = mouth();
        ItemEntity Dropped = new ItemEntity(World, Mouth.x, Mouth.y + 0.2, Mouth.z, Stack);
        Player Reader = Opener != null ? World.getPlayerByUUID(Opener) : null;
        Vec3 Toward = Reader != null && Reader.distanceToSqr(Mouth) < 256.0 ? Reader.getEyePosition().subtract(Mouth).multiply(1.0, 0.0, 1.0) : Vec3.ZERO;
        Vec3 Push = Toward.lengthSqr() > 1.0e-4 ? Toward.normalize().scale(0.16) : Vec3.ZERO;
        Dropped.setDeltaMovement(Push.x, 0.3, Push.z);
        Dropped.setPickUpDelay(10);
        Dropped.setUnlimitedLifetime();
        World.addFreshEntity(Dropped);
        layers(World, Scp1762Registry.NoteLayers, 0.8f, 1.0f);
        World.sendParticles(ParticleTypes.END_ROD, Mouth.x, Mouth.y + 0.3, Mouth.z, 4, 0.15, 0.1, 0.15, 0.01);
    }

    private void layers(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        Vec3 At = mouth();
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.BLOCKS, Volume, Pitch);
        }
    }

    private void setOpen(ServerLevel World, boolean Value) {
        BlockState State = getBlockState();
        if (State.getBlock() instanceof Scp1762BoxBlock && State.getValue(Scp1762BoxBlock.Open) != Value) {
            World.setBlock(worldPosition, State.setValue(Scp1762BoxBlock.Open, Value), Block.UPDATE_ALL);
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState State = getBlockState();
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        track(false);
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.putInt(StageKey, Stage);
        Saved.putByte("Phase", Phase);
        if (ArmedBy != null) {
            Saved.putUUID("ArmedBy", ArmedBy);
            Saved.putLong("ArmedUntil", ArmedUntil);
        }
        if (Phase != Closed) {
            Saved.putInt("Timer", Timer);
            Saved.putInt("Planned", Planned);
            Saved.putInt("Poured", Poured);
            Saved.putInt("Session", Session);
            Saved.putInt("Age", Age);
            Saved.putBoolean("NoteGiven", NoteGiven);
            ListTag Ids = new ListTag();
            for (UUID Id : Out) {
                Ids.add(NbtUtils.createUUID(Id));
            }
            Saved.put("Out", Ids);
            if (Opener != null) {
                Saved.putUUID("Opener", Opener);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        Stage = Math.max(0, Math.min(Scp1762Stage.Last, Saved.getInt(StageKey)));
        Phase = Saved.getByte("Phase");
        Timer = Saved.getInt("Timer");
        Planned = Saved.getInt("Planned");
        Poured = Saved.getInt("Poured");
        Session = Saved.getInt("Session");
        Age = Saved.getInt("Age");
        NoteGiven = Saved.getBoolean("NoteGiven");
        Out.clear();
        for (Tag Id : Saved.getList("Out", Tag.TAG_INT_ARRAY)) {
            Out.add(NbtUtils.loadUUID(Id));
        }
        Opener = Saved.hasUUID("Opener") ? Saved.getUUID("Opener") : null;
        ArmedBy = Saved.hasUUID("ArmedBy") ? Saved.getUUID("ArmedBy") : null;
        ArmedUntil = Saved.getLong("ArmedUntil");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag0 = new CompoundTag();
        Tag0.putInt(StageKey, Stage);
        Tag0.putByte("Phase", Phase);
        return Tag0;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        Components.set(Scp1762Registry.Stage.get(), Stage);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        Integer Carried = Input.get(Scp1762Registry.Stage.get());
        if (Carried != null) {
            Stage = Math.max(0, Math.min(Scp1762Stage.Last, Carried));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag Saved) {
        super.removeComponentsFromTag(Saved);
        Saved.remove(StageKey);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "lid", 0, State -> {
            boolean IsOpen = Scp1762BoxBlock.isOpen(getBlockState());
            if (SeenOpen == null) {
                SeenOpen = IsOpen;
            } else if (SeenOpen != IsOpen) {
                SeenOpen = IsOpen;
                Moved = true;
            }
            if (IsOpen) {
                return State.setAndContinue(Moved ? OpenPlay : OpenedLoop);
            }
            return State.setAndContinue(Moved ? ClosePlay : ClosedLoop);
        }));
        Controllers.add(new AnimationController<>(this, "rattle", 0, State -> PlayState.STOP).triggerableAnim("rattle", RattlePlay));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
