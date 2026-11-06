package net.rai.scp_unleashed.furnishing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SeatBlock extends FurnishingBlock implements SeatHost {
    public static final MapCodec<SeatBlock> SeatCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            FurnishingKind.KindCodec.fieldOf("kind").forGetter(SeatBlock::getKind), propertiesCodec()).apply(Instance, SeatBlock::new));

    public SeatBlock(FurnishingKind Kind, Properties Properties) {
        super(Kind, Properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return SeatCodec;
    }

    @Override
    protected String[] details() {
        return new String[]{"sit"};
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        return Seating.sit(World, Pos, State, User);
    }

    @Override
    public double seatHeight(BlockState State) {
        return getKind().getSeatHeight();
    }

    @Override
    public float seatYaw(BlockState State, BlockPos Pos, Player User) {
        if (getKind() == FurnishingKind.LabStool) {
            return User.getYRot();
        }
        return State.getValue(FACING).toYRot();
    }
}
