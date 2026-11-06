package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.BreakerBlock;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.rai.scp_unleashed.wire.WireNetworkManager;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class BreakerBlockEntity extends BlockEntity implements Upgradeable {
    public static final int AutoResetDelay = 40;
    public static final int AutoResetInterval = 10;
    public static final double AutoResetMargin = 0.9;

    private final Set<UpgradeType> Upgrades = EnumSet.noneOf(UpgradeType.class);
    private boolean OverloadTrip;
    private long OpenedAt;

    public BreakerBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.Breaker.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, BreakerBlockEntity Breaker) {
        if (World instanceof ServerLevel ServerWorld) {
            Breaker.tickServer(ServerWorld);
        }
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        if (!OverloadTrip || !isTripped() || !Upgrades.contains(UpgradeType.AutoReset) || Now - OpenedAt < AutoResetDelay || Now % AutoResetInterval != 0) {
            return;
        }
        double[] Estimate = WireNetworkManager.get(World).openSwitchEstimate(World, worldPosition);
        if (Estimate != null && Estimate[0] <= Estimate[1] * AutoResetMargin) {
            reset(World);
        }
    }

    @Override
    public boolean acceptsUpgrade(UpgradeType Type) {
        return Type == UpgradeType.AutoReset;
    }

    @Override
    public boolean hasUpgrade(UpgradeType Type) {
        return Upgrades.contains(Type);
    }

    @Override
    public void installUpgrade(UpgradeType Type) {
        if (Upgrades.add(Type)) {
            setChanged();
        }
    }

    @Override
    public Collection<UpgradeType> getUpgrades() {
        return Collections.unmodifiableSet(Upgrades);
    }

    public void dropUpgrades(Level World, BlockPos Pos) {
        for (UpgradeType Type : Upgrades) {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, new ItemStack(Type.getItem()));
        }
        Upgrades.clear();
    }

    public boolean isTripped() {
        BlockState State = getBlockState();
        return State.hasProperty(BreakerBlock.Open) && State.getValue(BreakerBlock.Open);
    }

    public void trip(ServerLevel World) {
        setOpen(World, true, false);
    }

    public void overloadTrip(ServerLevel World) {
        setOpen(World, true, true);
    }

    public void reset(ServerLevel World) {
        setOpen(World, false, false);
    }

    private void setOpen(ServerLevel World, boolean Opening, boolean Overload) {
        BlockState State = getBlockState();
        if (!State.hasProperty(BreakerBlock.Open) || State.getValue(BreakerBlock.Open) == Opening) {
            return;
        }
        OverloadTrip = Opening && Overload;
        OpenedAt = World.getGameTime();
        setChanged();
        World.setBlock(worldPosition, State.setValue(BreakerBlock.Open, Opening), Block.UPDATE_ALL);
        Vec3 Centre = Vec3.atCenterOf(worldPosition);
        if (Opening) {
            World.playSound(null, worldPosition, ModSounds.BreakerTrip.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, Centre.x, Centre.y, Centre.z, 12, 0.35, 0.35, 0.35, 0.15);
        } else {
            World.playSound(null, worldPosition, ModSounds.BreakerReset.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        ListTag Installed = new ListTag();
        for (UpgradeType Type : Upgrades) {
            Installed.add(StringTag.valueOf(Type.getSerializedName()));
        }
        Tag.put("Upgrades", Installed);
        Tag.putBoolean("OverloadTrip", OverloadTrip);
        Tag.putLong("OpenedAt", OpenedAt);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Upgrades.clear();
        ListTag Installed = Tag.getList("Upgrades", net.minecraft.nbt.Tag.TAG_STRING);
        for (int Index = 0; Index < Installed.size(); Index++) {
            String Name = Installed.getString(Index);
            for (UpgradeType Type : UpgradeType.values()) {
                if (Type.getSerializedName().equals(Name)) {
                    Upgrades.add(Type);
                }
            }
        }
        OverloadTrip = Tag.getBoolean("OverloadTrip");
        OpenedAt = Tag.getLong("OpenedAt");
    }
}
