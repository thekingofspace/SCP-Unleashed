package net.rai.scp_unleashed.scp107;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class Scp107ShellBlockEntity extends BlockEntity {
    public static final int PourTicks = 50;

    private int Poured = -1;
    private long PouredAt;

    public Scp107ShellBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp107Registry.ShellEntity.get(), Pos, State);
    }

    public void poured(Scp107RainKind Kind) {
        if (level == null) {
            return;
        }
        Poured = Kind.ordinal();
        PouredAt = level.getGameTime();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public Optional<Scp107RainKind> pouredKind() {
        return Scp107RainKind.byOrdinal(Poured);
    }

    public float pourProgress(float Partial) {
        if (level == null || Poured < 0) {
            return -1.0f;
        }
        float Age = level.getGameTime() - PouredAt + Partial;
        if (Age < 0.0f || Age >= PourTicks) {
            return -1.0f;
        }
        return Age / PourTicks;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Poured", Poured);
        Tag.putLong("PouredAt", PouredAt);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Poured = Tag.contains("Poured") ? Tag.getInt("Poured") : -1;
        PouredAt = Tag.getLong("PouredAt");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putInt("Poured", Poured);
        Tag.putLong("PouredAt", PouredAt);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
