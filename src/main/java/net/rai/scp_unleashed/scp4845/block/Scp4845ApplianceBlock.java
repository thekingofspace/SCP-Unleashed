package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Scp4845ApplianceBlock extends Scp4845FurnitureBlock {
    public static final MapCodec<Scp4845ApplianceBlock> ApplianceCodec = simpleCodec(Settings -> new Scp4845ApplianceBlock(Settings, Scp4845Shapes.Grill, Kind.Grill));
    public static final int GrillChance = 3;
    public static final int FryerChance = 4;

    public enum Kind {
        Grill,
        Fryer
    }

    private final Kind Appliance;

    public Scp4845ApplianceBlock(Properties Settings, double[][] North, Kind Appliance) {
        super(Settings, North);
        this.Appliance = Appliance;
    }

    public Kind kind() {
        return Appliance;
    }

    @Override
    protected MapCodec<? extends Scp4845ApplianceBlock> codec() {
        return ApplianceCodec;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        Direction Front = State.getValue(FACING);
        double Forward = Appliance == Kind.Grill ? 0.2 : 0.3;
        double X = Pos.getX() + 0.5 + Front.getStepX() * Forward + (Random.nextDouble() - 0.5) * 0.7;
        double Z = Pos.getZ() + 0.5 + Front.getStepZ() * Forward + (Random.nextDouble() - 0.5) * 0.7;
        double Y = Pos.getY() + 0.92;
        if (Appliance == Kind.Grill) {
            if (Random.nextInt(GrillChance) == 0) {
                World.addParticle(ParticleTypes.SMOKE, X, Y, Z, 0.0, 0.02, 0.0);
            }
            if (Random.nextInt(GrillChance * 4) == 0) {
                World.addParticle(ParticleTypes.WHITE_SMOKE, X, Y, Z, 0.0, 0.03, 0.0);
            }
            return;
        }
        if (Random.nextInt(FryerChance) == 0) {
            World.addParticle(ParticleTypes.BUBBLE_POP, X, Pos.getY() + 0.82, Z, 0.0, 0.01, 0.0);
        }
        if (Random.nextInt(FryerChance * 3) == 0) {
            World.addParticle(ParticleTypes.WHITE_SMOKE, X, Y, Z, 0.0, 0.02, 0.0);
        }
    }
}
