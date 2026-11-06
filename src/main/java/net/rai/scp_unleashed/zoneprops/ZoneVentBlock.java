package net.rai.scp_unleashed.zoneprops;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.props.PropWater;

public class ZoneVentBlock extends ZoneStackPropBlock {
    public static final MapCodec<ZoneVentBlock> VentCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ZonePropKind.KindCodec.fieldOf("kind").forGetter(ZonePropBlock::getKind), propertiesCodec()).apply(Instance, ZoneVentBlock::new));
    private static final double PuffHeight = 12.5 / 16.0;

    public ZoneVentBlock(ZonePropKind Kind, Properties Properties) {
        super(Kind, Properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return VentCodec;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        PillarPart Piece = State.getValue(Part);
        if (Piece != PillarPart.Top && Piece != PillarPart.Single || State.getValue(PropWater.Waterlogged) || Random.nextInt(3) != 0) {
            return;
        }
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Out = 0.3;
        double X = Pos.getX() + 0.5 + Math.cos(Angle) * Out;
        double Z = Pos.getZ() + 0.5 + Math.sin(Angle) * Out;
        double Y = Pos.getY() + PuffHeight;
        World.addParticle(ParticleTypes.WHITE_SMOKE, X, Y, Z, Math.cos(Angle) * 0.02, 0.03 + Random.nextDouble() * 0.02, Math.sin(Angle) * 0.02);
    }
}
