package net.rai.scp_unleashed.scp2317.note;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp2317.note.client.Scp2317RiteNoteClientHooks;

public class Scp2317RiteNoteBlock extends Block {
    public static final MapCodec<Scp2317RiteNoteBlock> NoteCodec = simpleCodec(Scp2317RiteNoteBlock::new);
    private static final VoxelShape Shape = Block.box(2.0, 0.0, 2.0, 14.0, 1.0, 14.0);

    public Scp2317RiteNoteBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return NoteCodec;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            Scp2317RiteNoteClientHooks.read();
        } else if (User instanceof ServerPlayer Reader) {
            Scp2317RiteNote.take(Reader, Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader World, BlockPos Pos, BlockState State) {
        return new ItemStack(Scp2317RiteNoteRegistry.Note.get());
    }
}
