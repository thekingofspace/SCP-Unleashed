package net.rai.scp_unleashed.scp076;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Scp076CoffinBlockEntity extends BlockEntity {
    public static final int LidTicks = 30;
    public static final double FloorHeight = 4.0 / 16.0;

    private boolean LidOpen;
    private long LidStart = -10000L;

    public Scp076CoffinBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp076Registry.CoffinEntity.get(), Pos, State);
    }

    public boolean lidOpen() {
        return LidOpen;
    }

    public Direction axis() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp076CoffinBlock.Facing) ? State.getValue(Scp076CoffinBlock.Facing) : Direction.SOUTH;
    }

    public Vec3 restingPoint() {
        return Vec3.atBottomCenterOf(worldPosition).add(0.0, FloorHeight, 0.0);
    }

    public float lid(float Partial) {
        if (level == null) {
            return LidOpen ? 1.0f : 0.0f;
        }
        float Share = (float) Mth.clamp((level.getGameTime() - LidStart + Partial) / LidTicks, 0.0, 1.0);
        return LidOpen ? Share : 1.0f - Share;
    }

    public AABB renderBounds() {
        return new AABB(worldPosition).inflate(2.5, 1.0, 2.5);
    }

    public void openLid() {
        move(true);
    }

    public void closeLid() {
        move(false);
    }

    private void move(boolean Opening) {
        if (level == null || LidOpen == Opening) {
            return;
        }
        LidOpen = Opening;
        LidStart = level.getGameTime();
        Scp076CoffinBlock.setOpen(level, worldPosition, axis(), Opening);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        if (level instanceof ServerLevel World) {
            Vec3 Middle = Vec3.atCenterOf(worldPosition);
            World.playSound(null, Middle.x, Middle.y, Middle.z, Scp076Registry.LidScrape.get(), SoundSource.BLOCKS, 1.2f, Opening ? 0.6f : 0.55f);
        }
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel World)) {
            return;
        }
        long Time = World.getGameTime() - LidStart;
        if (Time == (LidOpen ? LidTicks - 4 : LidTicks - 2)) {
            Vec3 Middle = Vec3.atCenterOf(worldPosition);
            World.playSound(null, Middle.x, Middle.y, Middle.z, Scp076Registry.LidDrop.get(), SoundSource.BLOCKS, 1.3f, LidOpen ? 0.6f : 0.7f);
            Direction Side = axis().getCounterClockWise();
            Vec3 Edge = Middle.add(Side.getStepX() * (LidOpen ? 1.0 : 0.4), -0.4, Side.getStepZ() * (LidOpen ? 1.0 : 0.4));
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp076Registry.VaultStone.get().defaultBlockState()), Edge.x, Edge.y, Edge.z, 14, 0.3, 0.1, 0.9, 0.05);
            World.sendParticles(Scp076DoorBlockEntity.Ash, Edge.x, Edge.y + 0.2, Edge.z, 10, 0.4, 0.2, 1.0, 0.01);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.saveAdditional(Saved, Registries);
        Saved.putBoolean("LidOpen", LidOpen);
        Saved.putLong("LidStart", LidStart);
    }

    @Override
    protected void loadAdditional(CompoundTag Saved, HolderLookup.Provider Registries) {
        super.loadAdditional(Saved, Registries);
        LidOpen = Saved.getBoolean("LidOpen");
        LidStart = Saved.contains("LidStart") ? Saved.getLong("LidStart") : -10000L;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Saved = new CompoundTag();
        Saved.putBoolean("LidOpen", LidOpen);
        Saved.putLong("LidStart", LidStart);
        return Saved;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
