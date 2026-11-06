package net.rai.scp_unleashed.scp2317;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class Scp2317VaseBlock extends Block {
    public static final IntegerProperty Glow = IntegerProperty.create("glow", 0, 2);
    public static final int Dark = 0;
    public static final int Idle = 1;
    public static final int Awake = 2;
    public static final MapCodec<Scp2317VaseBlock> VaseCodec = simpleCodec(Scp2317VaseBlock::new);
    private static final VoxelShape Shape = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    private static final DustParticleOptions Blood = new DustParticleOptions(new Vector3f(0.42f, 0.04f, 0.05f), 0.8f);

    public Scp2317VaseBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Glow, Idle));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return VaseCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Glow);
    }

    public static int lightOf(BlockState State) {
        return switch (State.getValue(Glow)) {
            case Awake -> 9;
            case Idle -> 3;
            default -> 0;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Level0 = State.getValue(Glow);
        if (Level0 == Awake && Random.nextInt(3) == 0) {
            double Side = Random.nextInt(4);
            double X = Pos.getX() + 0.5 + (Side == 0 ? 0.4 : Side == 1 ? -0.4 : (Random.nextDouble() - 0.5) * 0.6);
            double Z = Pos.getZ() + 0.5 + (Side == 2 ? 0.4 : Side == 3 ? -0.4 : (Random.nextDouble() - 0.5) * 0.6);
            World.addParticle(Blood, X, Pos.getY() + 0.9 + Random.nextDouble() * 0.5, Z, 0.0, -0.05, 0.0);
        }
        if (Level0 != Dark && Random.nextInt(Level0 == Awake ? 2 : 8) == 0) {
            World.addParticle(new DustParticleOptions(new Vector3f(0.7f, 0.12f, 0.08f), 0.6f), Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.2, Pos.getY() + 1.55,
                    Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.2, 0.0, 0.02, 0.0);
        }
    }
}
