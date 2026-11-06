package net.rai.scp_unleashed.scp024;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

public class Scp024ExitDoorBlock extends Scp024DoorBlock {
    public static final MapCodec<Scp024ExitDoorBlock> ExitCodec = simpleCodec(Scp024ExitDoorBlock::new);

    public Scp024ExitDoorBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<? extends Scp024ExitDoorBlock> codec() {
        return ExitCodec;
    }

    @Override
    protected void used(ServerPlayer Contestant, BlockPos Origin, Direction Front) {
        Scp024Runs.leaveByDoor(Contestant);
    }
}
