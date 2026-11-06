package net.rai.scp_unleashed.facilitysign;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;

import java.util.ArrayList;
import java.util.List;

public final class FacilitySignSupport {
    public static final String Key = "block.scp_unleashed.facility_sign.";

    private FacilitySignSupport() {
    }

    public static Direction rightOf(Direction Facing) {
        return Facing.getCounterClockWise();
    }

    public static boolean onWall(LevelReader World, BlockPos Pos, Direction Facing) {
        BlockPos Support = Pos.relative(Facing.getOpposite());
        return World.getBlockState(Support).isFaceSturdy(World, Support, Facing);
    }

    public static boolean underCeiling(LevelReader World, BlockPos Pos) {
        BlockPos Support = Pos.above();
        return World.getBlockState(Support).isFaceSturdy(World, Support, Direction.DOWN);
    }

    public static List<Direction> facingChoices(BlockPlaceContext Context) {
        List<Direction> Found = new ArrayList<>();
        Direction Clicked = Context.getClickedFace();
        if (Clicked.getAxis().isHorizontal()) {
            Found.add(Clicked);
        }
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (Side.getAxis().isHorizontal() && !Found.contains(Side.getOpposite())) {
                Found.add(Side.getOpposite());
            }
        }
        return Found;
    }

    public static void refuse(BlockPlaceContext Context, String Reason) {
        Player User = Context.getPlayer();
        if (User != null && !Context.getLevel().isClientSide) {
            User.displayClientMessage(Component.translatable(Key + Reason).withStyle(ChatFormatting.GRAY), true);
        }
    }
}
