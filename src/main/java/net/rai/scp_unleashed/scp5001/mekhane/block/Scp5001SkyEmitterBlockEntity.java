package net.rai.scp_unleashed.scp5001.mekhane.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;

public class Scp5001SkyEmitterBlockEntity extends BlockEntity {
    public static final int RefreshEvery = 20;

    public Scp5001SkyEmitterBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp5001MekhaneRegistry.EmitterEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp5001SkyEmitterBlockEntity Emitter) {
        if (World.getGameTime() % RefreshEvery != 0) {
            return;
        }
        MinecraftServer Server = World.getServer();
        if (Server == null || World != Server.overworld()) {
            return;
        }
        MekhaneProtocol Protocol = MekhaneProtocol.get(Server);
        Protocol.emitter(Pos);
        boolean Firing = Protocol.stage() == MekhaneProtocol.Stage.Summon || Protocol.stage() == MekhaneProtocol.Stage.Duel;
        if (State.getValue(Scp5001SkyEmitterBlock.Lit) != Firing) {
            World.setBlock(Pos, State.setValue(Scp5001SkyEmitterBlock.Lit, Firing), Block.UPDATE_ALL);
        }
    }

}
