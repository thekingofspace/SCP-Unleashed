package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730ThresherBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int HumEvery = 80;
    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation Unstable = RawAnimation.begin().thenLoop("unstable");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private boolean Pulled;

    public Scp1730ThresherBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp1730Registry.ThresherEntity.get(), Pos, State);
    }

    public boolean pulled() {
        return Pulled;
    }

    public void setPulled(boolean Value) {
        Pulled = Value;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp1730ThresherBlockEntity Thresher) {
        if ((World.getGameTime() + Pos.asLong()) % (Thresher.Pulled ? HumEvery / 4 : HumEvery) == 0) {
            World.playSound(null, Pos, Scp1730Registry.ThresherHum.get(), SoundSource.BLOCKS, Thresher.Pulled ? 1.4f : 1.0f, Thresher.Pulled ? 0.6f : 0.8f);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.saveAdditional(Tag, Lookup);
        Tag.putBoolean("Pulled", Pulled);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Lookup) {
        super.loadAdditional(Tag, Lookup);
        Pulled = Tag.getBoolean("Pulled");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Lookup) {
        CompoundTag Tag = super.getUpdateTag(Lookup);
        Tag.putBoolean("Pulled", Pulled);
        return Tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 10, State -> State.setAndContinue(Pulled ? Unstable : Idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
