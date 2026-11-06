package net.rai.scp_unleashed.scp5001.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp5001EngineBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int HumEvery = 70;
    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);

    public Scp5001EngineBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp5001WorldRegistry.EngineEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp5001EngineBlockEntity Engine) {
        long Phase = World.getGameTime() + Math.floorMod(Pos.asLong(), HumEvery);
        if (Phase % HumEvery == 0) {
            float Volume = 1.6f;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp5001WorldRegistry.EngineLayers) {
                World.playSound(null, Pos, Layer.get(), SoundSource.BLOCKS, Volume, 1.0f);
                Volume *= 0.75f;
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 0, Animated -> Animated.setAndContinue(Idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
