package net.rai.scp_unleashed.scp5001.mekhane.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;

public class Scp5001EmergencyConsoleBlockEntity extends MekhaneStatusBlockEntity {
    public Scp5001EmergencyConsoleBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp5001MekhaneRegistry.ConsoleEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp5001EmergencyConsoleBlockEntity Console) {
        Console.refresh(Scp5001EmergencyConsoleBlock.Awake);
    }

    @Override
    protected void claim(MekhaneProtocol Protocol) {
        Protocol.console(worldPosition);
    }
}
