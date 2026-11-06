package net.rai.scp_unleashed.daevite.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;

public class DaeviteDrumBlock extends DaeviteDecorBlock {
    public static final MapCodec<DaeviteDrumBlock> DrumCodec = simpleCodec(DaeviteDrumBlock::new);

    public DaeviteDrumBlock(Properties Settings) {
        super(Settings, DaeviteDecor.WarDrum);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return DrumCodec;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel Server) {
            float Pitch = 0.85f + World.random.nextFloat() * 0.2f;
            World.playSound(null, Pos, DaeviteRegistry.Drum.get(), SoundSource.BLOCKS, 1.6f, Pitch);
            Server.sendParticles(ParticleTypes.POOF, Pos.getX() + 0.5, Pos.getY() + 0.85, Pos.getZ() + 0.5, 4, 0.2, 0.02, 0.2, 0.01);
            World.gameEvent(User, GameEvent.BLOCK_ACTIVATE, Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
