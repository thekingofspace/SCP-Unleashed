package net.rai.scp_unleashed.scp049;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Scp049TrayBlockEntity extends BlockEntity {
    public static final long GuardWindow = 24000L;
    public static final int CheckTicks = 20;

    @Nullable
    private UUID Owner;
    private long LastPing;
    private int Operations;
    private boolean Linked;

    public Scp049TrayBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp049Registry.TrayEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp049TrayBlockEntity Tray) {
        if ((World.getGameTime() + Pos.asLong()) % CheckTicks != 0) {
            return;
        }
        boolean Now = Tray.Owner != null && World.getGameTime() - Tray.LastPing < GuardWindow;
        if (Tray.Owner != null && !Now) {
            Tray.Owner = null;
            Tray.setChanged();
        }
        if (Now != Tray.Linked) {
            Tray.Linked = Now;
            Tray.setChanged();
            Tray.sync();
        }
    }

    public boolean canBind(UUID Doctor) {
        return Owner == null || Owner.equals(Doctor);
    }

    public boolean owns(UUID Doctor) {
        return Doctor.equals(Owner);
    }

    @Nullable
    public UUID owner() {
        return Owner;
    }

    public void bind(Scp049Doctor Doctor) {
        boolean Fresh = !Doctor.getUUID().equals(Owner) || !Linked;
        Owner = Doctor.getUUID();
        LastPing = Doctor.level().getGameTime();
        Linked = true;
        setChanged();
        if (Fresh) {
            sync();
        }
    }

    public void release(UUID Doctor) {
        if (!Doctor.equals(Owner)) {
            return;
        }
        Owner = null;
        Linked = false;
        setChanged();
        sync();
    }

    public void operated() {
        Operations++;
        setChanged();
        sync();
    }

    public int operations() {
        return Operations;
    }

    public boolean isLinked() {
        return Linked;
    }

    public Vec3 centre() {
        return Vec3.atBottomCenterOf(worldPosition).add(0.0, 0.75, 0.0);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
        Tag.putLong("LastPing", LastPing);
        Tag.putInt("Operations", Operations);
        Tag.putBoolean("Linked", Linked);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Owner = Tag.hasUUID("Owner") ? Tag.getUUID("Owner") : null;
        LastPing = Tag.getLong("LastPing");
        Operations = Tag.getInt("Operations");
        Linked = Tag.getBoolean("Linked");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Linked", Linked);
        Tag.putInt("Operations", Operations);
        return Tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
