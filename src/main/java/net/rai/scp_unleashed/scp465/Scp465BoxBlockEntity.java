package net.rai.scp_unleashed.scp465;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class Scp465BoxBlockEntity extends BlockEntity {
    public static final String PartyKey = "Party";
    public static final String MusicKey = "Music";
    public static final String BoothKey = "Booth";
    public static final String LowKey = "Low";
    public static final String HighKey = "High";
    public static final String CelebrantsKey = "Celebrants";
    public static final int StreamerChance = 3;
    public static final int ConfettiChance = 2;
    public static final int BurstPieces = 70;
    public static final int BurstStreamers = 18;
    private static final Set<Scp465BoxBlockEntity> ClientBoxes = Collections.newSetFromMap(new WeakHashMap<>());

    private int PartyId;
    private long MusicStart;
    @Nullable
    private BlockPos Booth;
    @Nullable
    private BlockPos AreaLow;
    @Nullable
    private BlockPos AreaHigh;
    private int Celebrants;
    private boolean Primed;
    private boolean Showing;
    private float From;
    private float Ticks;
    private float LastTicks;

    public Scp465BoxBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp465Registry.BoxEntity.get(), Pos, State);
    }

    public static Set<Scp465BoxBlockEntity> clientBoxes() {
        return ClientBoxes;
    }

    public int partyId() {
        return PartyId;
    }

    public long musicStart() {
        return MusicStart;
    }

    @Nullable
    public BlockPos booth() {
        return Booth;
    }

    @Nullable
    public BlockPos areaLow() {
        return AreaLow;
    }

    @Nullable
    public BlockPos areaHigh() {
        return AreaHigh;
    }

    public int celebrants() {
        return Celebrants;
    }

    public boolean partying() {
        return PartyId > 0 && Scp465BoxBlock.isOpen(getBlockState());
    }

    public void bind(int Id, long Music, BlockPos DeckCentre, BlockPos Low, BlockPos High, int Count) {
        PartyId = Id;
        MusicStart = Music;
        Booth = DeckCentre.immutable();
        AreaLow = Low.immutable();
        AreaHigh = High.immutable();
        Celebrants = Count;
        changed();
    }

    public void unbind() {
        PartyId = 0;
        MusicStart = 0L;
        Booth = null;
        AreaLow = null;
        AreaHigh = null;
        Celebrants = 0;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState State = getBlockState();
            level.sendBlockUpdated(worldPosition, State, State, Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp465BoxBlockEntity Self) {
        ClientBoxes.add(Self);
        boolean Target = Scp465BoxBlock.isOpen(State);
        if (!Self.Primed) {
            Self.Primed = true;
            Self.Showing = Target;
            Self.From = Scp465Lid.settled(Target);
            Self.Ticks = Scp465Lid.duration(Target);
            Self.LastTicks = Self.Ticks;
        } else if (Target != Self.Showing) {
            Self.From = Scp465Lid.angle(Self.Showing, Self.From, Self.Ticks);
            Self.Showing = Target;
            Self.Ticks = 0.0f;
            Self.LastTicks = 0.0f;
            if (Target) {
                burst(World, Pos, World.getRandom());
            }
        } else {
            Self.LastTicks = Self.Ticks;
            Self.Ticks = Math.min(Self.Ticks + 1.0f, Scp465Lid.duration(Target));
        }
        if (Self.partying()) {
            ambience(World, Self, World.getRandom());
        }
    }

    public static void burst(Level World, BlockPos Pos, RandomSource Random) {
        Vec3 Mouth = Vec3.atBottomCenterOf(Pos).add(0.0, 0.7, 0.0);
        for (int Piece = 0; Piece < BurstPieces; Piece++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Spread = 0.05 + Random.nextDouble() * 0.22;
            World.addParticle(Scp465Registry.Confetti.get(), Mouth.x, Mouth.y, Mouth.z, Math.cos(Angle) * Spread, 0.35 + Random.nextDouble() * 0.4, Math.sin(Angle) * Spread);
        }
        for (int Piece = 0; Piece < BurstStreamers; Piece++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Spread = 0.08 + Random.nextDouble() * 0.18;
            World.addParticle(Scp465Registry.Streamer.get(), Mouth.x, Mouth.y, Mouth.z, Math.cos(Angle) * Spread, 0.3 + Random.nextDouble() * 0.35, Math.sin(Angle) * Spread);
        }
    }

    private static void ambience(Level World, Scp465BoxBlockEntity Self, RandomSource Random) {
        BlockPos Low = Self.AreaLow;
        BlockPos High = Self.AreaHigh;
        if (Low == null || High == null) {
            return;
        }
        double Top = High.getY() + 0.9;
        if (Random.nextInt(StreamerChance) == 0) {
            double X = Mth.lerp(Random.nextDouble(), Low.getX() + 0.5, High.getX() + 0.5);
            double Z = Mth.lerp(Random.nextDouble(), Low.getZ() + 0.5, High.getZ() + 0.5);
            World.addParticle(Scp465Registry.Streamer.get(), X, Top, Z, (Random.nextDouble() - 0.5) * 0.12, -0.01, (Random.nextDouble() - 0.5) * 0.12);
        }
        if (Random.nextInt(ConfettiChance) == 0) {
            double X = Mth.lerp(Random.nextDouble(), Low.getX() + 0.5, High.getX() + 0.5);
            double Z = Mth.lerp(Random.nextDouble(), Low.getZ() + 0.5, High.getZ() + 0.5);
            World.addParticle(Scp465Registry.Confetti.get(), X, Top, Z, (Random.nextDouble() - 0.5) * 0.05, -0.02, (Random.nextDouble() - 0.5) * 0.05);
        }
    }

    public float lidAngle(float Partial) {
        if (!Primed) {
            return Scp465Lid.settled(Scp465BoxBlock.isOpen(getBlockState()));
        }
        return Scp465Lid.angle(Showing, From, Mth.lerp(Partial, LastTicks, Ticks));
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ClientBoxes.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.putInt(PartyKey, PartyId);
        if (PartyId > 0) {
            Saved.putLong(MusicKey, MusicStart);
            Saved.putInt(CelebrantsKey, Celebrants);
            if (Booth != null) {
                Saved.put(BoothKey, NbtUtils.writeBlockPos(Booth));
            }
            if (AreaLow != null && AreaHigh != null) {
                Saved.put(LowKey, NbtUtils.writeBlockPos(AreaLow));
                Saved.put(HighKey, NbtUtils.writeBlockPos(AreaHigh));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        PartyId = Saved.getInt(PartyKey);
        MusicStart = Saved.getLong(MusicKey);
        Celebrants = Saved.getInt(CelebrantsKey);
        Booth = NbtUtils.readBlockPos(Saved, BoothKey).orElse(null);
        AreaLow = NbtUtils.readBlockPos(Saved, LowKey).orElse(null);
        AreaHigh = NbtUtils.readBlockPos(Saved, HighKey).orElse(null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        return saveCustomOnly(Registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
