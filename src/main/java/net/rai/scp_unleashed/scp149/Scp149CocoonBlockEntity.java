package net.rai.scp_unleashed.scp149;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp149CocoonBlockEntity extends BlockEntity {
    public static final int HatchMin = 1200;
    public static final int HatchMax = 1800;
    public static final int Crowded = 4;
    public static final float[] StageShares = {0.3f, 0.6f, 0.85f};
    public static final int[] BeatTicks = {72, 52, 36, 22};
    public static final int HeldBeat = 64;
    public static final float SwellTicks = 4.0f;
    public static final float SettleTicks = 9.0f;
    public static final int SyncTicks = 200;
    public static final int RoomCheck = 100;
    public static final double PulseHearing = 16.0;
    @Nullable
    private UUID Brood;
    private int Strands;
    private int Released;
    private int Age;
    private int Hatch;
    private boolean Held;
    private boolean Burst;
    private boolean NeedsSync;
    private long LastSync;
    private double Clock;
    private double ClockO;

    public Scp149CocoonBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp149Registry.CocoonEntity.get(), Pos, State);
    }

    @Nullable
    public UUID brood() {
        return Brood;
    }

    public int strands() {
        return Strands;
    }

    public int broodSize() {
        return Math.max(1, Strands);
    }

    public int released() {
        return Released;
    }

    public int waiting() {
        return Math.max(0, Math.max(1, Strands) - Released);
    }

    public void release(int Count) {
        Released += Count;
        NeedsSync = true;
        setChanged();
    }

    public int age() {
        return Age;
    }

    public int hatchAt() {
        return Hatch;
    }

    public boolean held() {
        return Held;
    }

    public boolean burst() {
        return Burst;
    }

    public void markBurst() {
        Burst = true;
    }

    public void spin(UUID Owner) {
        Brood = Owner;
        setChanged();
    }

    public void addStrand() {
        Strands++;
        NeedsSync = true;
        setChanged();
    }

    public void ripen() {
        Age = Math.max(Age, Hatch);
        NeedsSync = true;
        setChanged();
    }

    public int stage() {
        if (Hatch <= 0) {
            return 0;
        }
        float Share = Age / (float) Hatch;
        int Found = 0;
        for (float Threshold : StageShares) {
            if (Share >= Threshold) {
                Found++;
            }
        }
        return Found;
    }

    private void settle(Level World) {
        if (Hatch <= 0) {
            Hatch = HatchMin + World.getRandom().nextInt(HatchMax - HatchMin + 1);
            setChanged();
        }
        if (Brood == null) {
            Brood = UUID.randomUUID();
            setChanged();
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp149CocoonBlockEntity Cocoon) {
        if (!(World instanceof ServerLevel Server) || Cocoon.Burst) {
            return;
        }
        Cocoon.settle(World);
        if (Cocoon.Age < Cocoon.Hatch) {
            Cocoon.Age++;
            if (Cocoon.Age % 20 == 0) {
                Cocoon.setChanged();
            }
        }
        int Stage = Cocoon.stage();
        if (State.getValue(Scp149CocoonBlock.Stage) != Stage) {
            World.setBlock(Pos, State.setValue(Scp149CocoonBlock.Stage, Stage), Block.UPDATE_CLIENTS);
            Cocoon.NeedsSync = true;
        }
        if (Cocoon.Age >= Cocoon.Hatch && (Cocoon.Age == Cocoon.Hatch && !Cocoon.Held || World.getGameTime() % RoomCheck == 0)) {
            Scp149Brood.Split Result = Scp149Brood.hatch(Server, Pos);
            if (Result.Opened()) {
                return;
            }
            if (!Cocoon.Held) {
                Cocoon.Held = true;
                Cocoon.NeedsSync = true;
                Cocoon.setChanged();
            }
        }
        if (World.getGameTime() - Cocoon.LastSync >= SyncTicks) {
            Cocoon.NeedsSync = true;
        }
        if (Cocoon.NeedsSync) {
            Cocoon.NeedsSync = false;
            Cocoon.LastSync = World.getGameTime();
            BlockState Current = World.getBlockState(Pos);
            World.sendBlockUpdated(Pos, Current, Current, Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp149CocoonBlockEntity Cocoon) {
        if (Cocoon.Hatch > 0 && Cocoon.Age < Cocoon.Hatch) {
            Cocoon.Age++;
        }
        int Period = Cocoon.period(State);
        Cocoon.ClockO = Cocoon.Clock;
        Cocoon.Clock += 1.0;
        if (Cocoon.Clock >= Period) {
            Cocoon.Clock -= Period;
            Cocoon.ClockO -= Period;
            Cocoon.beat(World, Pos, State);
        }
    }

    public int period(BlockState State) {
        if (Held) {
            return HeldBeat;
        }
        return BeatTicks[Mth.clamp(State.getValue(Scp149CocoonBlock.Stage), 0, BeatTicks.length - 1)];
    }

    private void beat(Level World, BlockPos Pos, BlockState State) {
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + 0.4;
        double Z = Pos.getZ() + 0.5;
        if (!World.hasNearbyAlivePlayer(X, Y, Z, PulseHearing)) {
            return;
        }
        int Stage = State.getValue(Scp149CocoonBlock.Stage);
        float Pitch = 0.85f + Stage * 0.08f;
        float Volume = 0.35f + Stage * 0.12f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp149Registry.PulseLayers) {
            World.playLocalSound(X, Y, Z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch, false);
        }
        if (Stage >= 2) {
            World.addParticle(Scp149Registry.Mote.get(), X + (World.getRandom().nextDouble() - 0.5) * 0.4, Y + 0.3, Z + (World.getRandom().nextDouble() - 0.5) * 0.4, 0.0, 0.02, 0.0);
        }
    }

    public float pulse(float Partial) {
        float Ticks = (float) (ClockO + (Clock - ClockO) * Partial);
        if (Ticks < SwellTicks) {
            return Mth.sin(Ticks / SwellTicks * Mth.HALF_PI);
        }
        float Settle = (Ticks - SwellTicks) / SettleTicks;
        if (Settle >= 1.0f) {
            return 0.0f;
        }
        return 0.5f + 0.5f * Mth.cos(Settle * Mth.PI);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (Brood != null) {
            Tag.putUUID("Brood", Brood);
        }
        Tag.putInt("Strands", Strands);
        Tag.putInt("Released", Released);
        Tag.putInt("Age", Age);
        Tag.putInt("Hatch", Hatch);
        Tag.putBoolean("Held", Held);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Brood = Tag.hasUUID("Brood") ? Tag.getUUID("Brood") : null;
        Strands = Tag.getInt("Strands");
        Released = Tag.getInt("Released");
        Age = Tag.getInt("Age");
        Hatch = Tag.getInt("Hatch");
        Held = Tag.getBoolean("Held");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Strands", Strands);
        Tag.putInt("Released", Released);
        Tag.putInt("Age", Age);
        Tag.putInt("Hatch", Hatch);
        Tag.putBoolean("Held", Held);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
