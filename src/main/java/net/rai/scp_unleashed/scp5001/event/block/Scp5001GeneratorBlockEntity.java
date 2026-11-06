package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp5001GeneratorBlockEntity extends Scp5001DeviceBlockEntity implements GeoBlockEntity {
    public static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation Overdrive = RawAnimation.begin().thenPlay("spinup").thenLoop("overdrive");
    public static final int SpinTicks = 60;
    public static final double StackReach = 10.0 / 16.0;
    public static final double StackTop = 49.0 / 16.0;
    public static final double CoilReach = 17.0 / 16.0;
    public static final double[] CoilHeights = {18.5 / 16.0, 28.0 / 16.0, 37.5 / 16.0};
    public static final DustColorTransitionOptions Plasma = new DustColorTransitionOptions(new Vector3f(1.0f, 0.66f, 0.28f), new Vector3f(0.61f, 0.55f, 0.94f), 0.9f);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private long SpinStart;
    private int ClientHot;

    public Scp5001GeneratorBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp5001EventRegistry.GeneratorEntity.get(), Pos, State);
    }

    public boolean overdriven() {
        BlockState State = getBlockState();
        return State.hasProperty(Scp5001GeneratorBlock.Overdrive) && State.getValue(Scp5001GeneratorBlock.Overdrive);
    }

    public long spinStart() {
        return SpinStart;
    }

    public void spinStart(long Value) {
        SpinStart = Value;
    }

    public void clientTick() {
        Level World = level;
        if (World == null) {
            return;
        }
        if (!overdriven()) {
            ClientHot = 0;
            return;
        }
        ClientHot++;
        RandomSource Dice = World.random;
        double Cx = worldPosition.getX() + 0.5;
        double Cy = worldPosition.getY();
        double Cz = worldPosition.getZ() + 0.5;
        float Ramp = Math.min(1.0f, ClientHot / (float) SpinTicks);
        for (int Index = 0; Index < 4; Index++) {
            if (Dice.nextFloat() > 0.45f * Ramp) {
                continue;
            }
            double Sx = Index == 0 || Index == 3 ? -1.0 : 1.0;
            double Sz = Index < 2 ? -1.0 : 1.0;
            World.addParticle(Plasma, Cx + Sx * StackReach + (Dice.nextDouble() - 0.5) * 0.12, Cy + StackTop, Cz + Sz * StackReach + (Dice.nextDouble() - 0.5) * 0.12,
                    (Dice.nextDouble() - 0.5) * 0.01, 0.05 + Dice.nextDouble() * 0.05, (Dice.nextDouble() - 0.5) * 0.01);
        }
        if (ClientHot > 20 && Dice.nextInt(5) == 0) {
            double Angle = Dice.nextDouble() * Mth.TWO_PI;
            double Height = CoilHeights[Dice.nextInt(CoilHeights.length)];
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, Cx + Math.cos(Angle) * CoilReach, Cy + Height, Cz + Math.sin(Angle) * CoilReach, 0.0, 0.0, 0.0);
        }
        if (ClientHot < 40 && ClientHot % 4 == 0) {
            Direction Facing = getBlockState().hasProperty(Scp5001DeviceBlock.Facing) ? getBlockState().getValue(Scp5001DeviceBlock.Facing) : Direction.NORTH;
            Direction Side = Facing.getClockWise();
            for (int Sign = -1; Sign <= 1; Sign += 2) {
                double Vx = Side.getStepX() * Sign;
                double Vz = Side.getStepZ() * Sign;
                World.addParticle(ParticleTypes.WHITE_SMOKE, Cx + Vx * 1.25, Cy + 0.7, Cz + Vz * 1.25, Vx * 0.05, 0.02, Vz * 0.05);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "rotor", 6, Animated -> Animated.setAndContinue(overdriven() ? Overdrive : Idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
