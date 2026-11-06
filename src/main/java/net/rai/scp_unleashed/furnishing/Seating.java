package net.rai.scp_unleashed.furnishing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class Seating {
    private Seating() {
    }

    public static InteractionResult sit(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!(State.getBlock() instanceof SeatHost Host) || User.isSecondaryUseActive() || User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (User.getVehicle() instanceof FurnishingSeat Current && Current.blockPosition().equals(Pos)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        List<FurnishingSeat> Taken = World.getEntitiesOfClass(FurnishingSeat.class, new AABB(Pos));
        for (FurnishingSeat Seat : Taken) {
            if (Seat.isVehicle()) {
                User.displayClientMessage(Component.translatable("block.scp_unleashed.furnishing.seat_taken"), true);
                return InteractionResult.CONSUME;
            }
            Seat.discard();
        }
        Vec3 Spot = new Vec3(Pos.getX() + 0.5, Pos.getY() + Host.seatHeight(State), Pos.getZ() + 0.5);
        FurnishingSeat Seat = new FurnishingSeat(World, Spot, Host.seatYaw(State, Pos, User));
        World.addFreshEntity(Seat);
        if (!User.startRiding(Seat, true)) {
            Seat.discard();
            return InteractionResult.CONSUME;
        }
        World.playSound(null, Pos, FurnishingSounds.CushionSit.get(), SoundSource.BLOCKS, 0.9f, 0.9f + World.random.nextFloat() * 0.2f);
        return InteractionResult.CONSUME;
    }
}
