package net.rai.scp_unleashed.scp261;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Scp261VendingBlockEntity extends BlockEntity {
    public static final int WhirrTick = 3;
    public static final int SpinStart = 6;
    public static final int SpinEnd = 30;
    public static final int FallEnd = 38;
    public static final int SwallowTick = 32;
    public static final int PopTick = 44;
    public static final int CycleEnd = 96;
    public static final int VendEvent = 1;
    public static final int HumChance = 900;
    public static final double PanelHeight = 1.3;
    public static final double TrayHeight = 0.25;
    public static final double PopReach = 0.62;
    public static final double PopSpeed = 0.14;

    private boolean Busy;
    private int Timer;
    private boolean Gold;
    private Scp261Odds.Outcome Result = Scp261Odds.Outcome.Single;
    private int First = -1;
    private int Second = -1;
    private int Vends;
    private int ShownFirst = -1;
    private int ShownSecond = -1;
    private int ShownOutcome;
    private long ShownStart = Long.MIN_VALUE / 2;

    public Scp261VendingBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp261Registry.VendingEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp261VendingBlockEntity Machine) {
        Machine.tickServer(World);
    }

    private void tickServer(Level World) {
        if (!Busy) {
            if (World.getRandom().nextInt(HumChance) == 0) {
                play(Scp261Registry.Hum.get(), PanelHeight, 0.25f, 0.8f + World.getRandom().nextFloat() * 0.3f);
            }
            return;
        }
        Timer++;
        if (Timer == WhirrTick) {
            cue(Scp261Registry.WhirrCue, 1.0, 0.6f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
        if (Result == Scp261Odds.Outcome.Swallowed && Timer == SwallowTick) {
            cue(Scp261Registry.SwallowCue, 0.9, 0.6f, 0.9f + World.getRandom().nextFloat() * 0.1f);
        }
        if (Result != Scp261Odds.Outcome.Swallowed && Timer == FallEnd) {
            cue(Scp261Registry.ThunkCue, TrayHeight, 0.7f, 0.92f + World.getRandom().nextFloat() * 0.16f);
            if (World instanceof ServerLevel Server) {
                Vec3 Tray = frontSpot(TrayHeight + 0.1, 0.45);
                Server.sendParticles(ParticleTypes.PORTAL, Tray.x, Tray.y, Tray.z, 10, 0.2, 0.1, 0.2, 0.25);
            }
        }
        if (Result != Scp261Odds.Outcome.Swallowed && Timer == PopTick) {
            pop(World);
        }
        if (Timer >= CycleEnd) {
            Busy = false;
            Timer = 0;
            First = -1;
            Second = -1;
            setChanged();
            sync();
        }
    }

    public boolean isIdle() {
        return !Busy;
    }

    public int getTimer() {
        return Timer;
    }

    public int getVends() {
        return Vends;
    }

    public Scp261Odds.Outcome getResult() {
        return Result;
    }

    public void pay(Player User, ItemStack Stack) {
        if (level == null) {
            return;
        }
        if (Busy) {
            play(Scp261Registry.Refuse.get(), PanelHeight, 0.5f, 1.1f);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp261.busy").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean Golden = Scp261VendingBlock.isGoldCoin(Stack);
        if (!User.hasInfiniteMaterials()) {
            Stack.shrink(1);
        }
        start(Golden, Scp261Odds.roll(level.getRandom(), Golden));
    }

    public boolean start(boolean Golden, Scp261Odds.Roll Rolled) {
        if (level == null || Busy) {
            return false;
        }
        Busy = true;
        Timer = 0;
        Gold = Golden;
        Result = Rolled.Result();
        First = Rolled.First().ordinal();
        Second = Rolled.Second() == null ? -1 : Rolled.Second().ordinal();
        Vends++;
        setChanged();
        sync();
        cue(Scp261Registry.CoinCue, PanelHeight, 0.7f, (Golden ? 1.15f : 0.95f) + level.getRandom().nextFloat() * 0.1f);
        level.blockEvent(worldPosition, getBlockState().getBlock(), VendEvent, pack(First, Second, Result));
        return true;
    }

    public static int pack(int FirstSlot, int SecondSlot, Scp261Odds.Outcome Ending) {
        return (FirstSlot & 31) | ((SecondSlot + 1) & 31) << 5 | Ending.ordinal() << 10;
    }

    private void pop(Level World) {
        Scp261Snack Main = Scp261Snack.byIndex(First);
        if (Main == null) {
            return;
        }
        List<ItemStack> Out = new ArrayList<>();
        Out.add(new ItemStack(Scp261Registry.snack(Main)));
        if (Result == Scp261Odds.Outcome.Pair) {
            Scp261Snack Extra = Scp261Snack.byIndex(Second);
            Out.add(new ItemStack(Scp261Registry.snack(Extra == null ? Main : Extra)));
        }
        Direction Front = getFront();
        for (int Index = 0; Index < Out.size(); Index++) {
            Vec3 Spot = frontSpot(TrayHeight, PopReach);
            ItemEntity Dropped = new ItemEntity(World, Spot.x, Spot.y, Spot.z, Out.get(Index));
            double Side = (Index - (Out.size() - 1) * 0.5) * 0.06;
            Dropped.setDeltaMovement(Front.getStepX() * PopSpeed + Front.getStepZ() * Side, 0.12, Front.getStepZ() * PopSpeed - Front.getStepX() * Side);
            Dropped.setPickUpDelay(10);
            World.addFreshEntity(Dropped);
        }
        play(Scp261Registry.Eject.get(), TrayHeight, 0.6f, 1.0f + World.getRandom().nextFloat() * 0.15f);
        if (Result == Scp261Odds.Outcome.Pair) {
            play(Scp261Registry.Bonus.get(), PanelHeight, 0.5f, 1.3f);
        }
    }

    private void cue(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, double Height, float Volume, float Pitch) {
        if (level == null) {
            return;
        }
        Scp261Registry.playCue(level, frontSpot(Height, 0.5), Layers, SoundSource.BLOCKS, Volume, Pitch);
    }

    private void play(SoundEvent Sound, double Height, float Volume, float Pitch) {
        if (level == null) {
            return;
        }
        Vec3 Spot = frontSpot(Height, 0.5);
        level.playSound(null, Spot.x, Spot.y, Spot.z, Sound, SoundSource.BLOCKS, Volume, Pitch);
    }

    public Vec3 frontSpot(double Height, double Reach) {
        Direction Front = getFront();
        return Vec3.atBottomCenterOf(worldPosition).add(Front.getStepX() * Reach, Height, Front.getStepZ() * Reach);
    }

    public Direction getFront() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp261VendingBlock.Facing) ? State.getValue(Scp261VendingBlock.Facing) : Direction.NORTH;
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getShownFirst() {
        return ShownFirst;
    }

    public int getShownSecond() {
        return ShownSecond;
    }

    public Scp261Odds.Outcome getShownOutcome() {
        Scp261Odds.Outcome[] All = Scp261Odds.Outcome.values();
        return All[Math.max(0, Math.min(All.length - 1, ShownOutcome))];
    }

    public float getShownAge(float PartialTick) {
        return level == null ? Float.MAX_VALUE : level.getGameTime() - ShownStart + PartialTick;
    }

    public AABB getRenderBounds() {
        return new AABB(worldPosition).expandTowards(0.0, 1.0, 0.0).inflate(0.1);
    }

    @Override
    public boolean triggerEvent(int Id, int Param) {
        if (Id == VendEvent) {
            if (level != null && level.isClientSide) {
                ShownFirst = Param & 31;
                ShownSecond = ((Param >> 5) & 31) - 1;
                ShownOutcome = (Param >> 10) & 3;
                ShownStart = level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(Id, Param);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putBoolean("Busy", Busy);
        Tag.putInt("Timer", Timer);
        Tag.putBoolean("Gold", Gold);
        Tag.putInt("Outcome", Result.ordinal());
        Tag.putInt("First", First);
        Tag.putInt("Second", Second);
        Tag.putInt("Vends", Vends);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Busy = Tag.getBoolean("Busy");
        Timer = Math.max(0, Math.min(CycleEnd, Tag.getInt("Timer")));
        Gold = Tag.getBoolean("Gold");
        Scp261Odds.Outcome[] All = Scp261Odds.Outcome.values();
        Result = All[Math.max(0, Math.min(All.length - 1, Tag.getInt("Outcome")))];
        First = Tag.contains("First") ? Tag.getInt("First") : -1;
        Second = Tag.contains("Second") ? Tag.getInt("Second") : -1;
        Vends = Tag.getInt("Vends");
        if (Busy && Scp261Snack.byIndex(First) == null) {
            Busy = false;
            Timer = 0;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        return saveWithoutMetadata(Registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
