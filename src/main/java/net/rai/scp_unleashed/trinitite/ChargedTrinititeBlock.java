package net.rai.scp_unleashed.trinitite;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.materials.MaterialTooltips;

import java.util.List;

public class ChargedTrinititeBlock extends Block {
    public static final MapCodec<ChargedTrinititeBlock> BlockCodec = simpleCodec(ChargedTrinititeBlock::new);
    public static final int SparkChance = 3;
    public static final int MaxSparks = 3;
    private static final double SparkOffset = 0.52;
    private static final double SparkSpread = 0.8;
    private static final double SparkSpeed = 0.06;

    public ChargedTrinititeBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(SparkChance) != 0) {
            return;
        }
        Direction Side = Direction.getRandom(Random);
        BlockPos Beside = Pos.relative(Side);
        if (World.getBlockState(Beside).isSolidRender(World, Beside)) {
            return;
        }
        double X = Pos.getX() + 0.5 + Side.getStepX() * SparkOffset + (Side.getStepX() == 0 ? Random.nextDouble() - 0.5 : 0.0) * SparkSpread;
        double Y = Pos.getY() + 0.5 + Side.getStepY() * SparkOffset + (Side.getStepY() == 0 ? Random.nextDouble() - 0.5 : 0.0) * SparkSpread;
        double Z = Pos.getZ() + 0.5 + Side.getStepZ() * SparkOffset + (Side.getStepZ() == 0 ? Random.nextDouble() - 0.5 : 0.0) * SparkSpread;
        int Count = 1 + Random.nextInt(MaxSparks);
        for (int Index = 0; Index < Count; Index++) {
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, X, Y, Z,
                    Side.getStepX() * SparkSpeed + (Random.nextDouble() - 0.5) * SparkSpeed,
                    Side.getStepY() * SparkSpeed + (Random.nextDouble() - 0.5) * SparkSpeed,
                    Side.getStepZ() * SparkSpeed + (Random.nextDouble() - 0.5) * SparkSpeed);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        MaterialTooltips.append(getDescriptionId(), Lines);
    }
}
