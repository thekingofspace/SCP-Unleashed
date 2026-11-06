package net.rai.scp_unleashed.scp2317.note;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp2317.Scp2317KingBlockEntity;
import net.rai.scp_unleashed.scp2317.Scp2317Sounds;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp2317RiteNote {
    public static final String Prefix = "gui.scp_unleashed.scp001_scarlet_rite_note.";
    public static final String WordsKey = Prefix + "words";
    public static final String SignKey = Prefix + "sign";
    public static final String LabelKey = Prefix + "label";
    public static final List<String> Body = List.of(Prefix + "0", Prefix + "1", Prefix + "2", Prefix + "3", Prefix + "4", WordsKey, Prefix + "5");
    public static final int[][] Spots = {{1, -6}, {-1, -6}, {2, -6}, {0, -7}, {-2, -6}, {1, -7}, {-1, -7}, {3, -6}, {-3, -6}};
    public static final float TakeVolume = 0.8f;
    public static final float TakePitch = 1.0f;
    public static final float ReadVolume = 0.5f;
    public static final float ReadPitch = 1.0f;

    private Scp2317RiteNote() {
    }

    public static boolean due(int Broken) {
        return Broken >= ScarletState.Chains;
    }

    public static void tick(ServerLevel World, Scp2317KingBlockEntity King) {
        tend(World, King.getBlockPos(), King.broken());
    }

    public static boolean tend(ServerLevel World, BlockPos King, int Broken) {
        if (!due(Broken)) {
            return false;
        }
        Scp2317RiteNoteState Notes = Scp2317RiteNoteState.get(World);
        if (Notes.placed(King)) {
            return false;
        }
        BlockPos Spot = spot(World, King);
        if (Spot == null) {
            return false;
        }
        World.setBlock(Spot, Scp2317RiteNoteRegistry.FloorNote.get().defaultBlockState(), Block.UPDATE_ALL);
        Notes.mark(King);
        return true;
    }

    @Nullable
    public static BlockPos spot(ServerLevel World, BlockPos King) {
        for (int[] Offset : Spots) {
            BlockPos At = King.offset(Offset[0], 0, Offset[1]);
            if (!World.isLoaded(At) || !World.isLoaded(At.below())) {
                return null;
            }
            if (fits(World, At)) {
                return At;
            }
        }
        return null;
    }

    public static boolean fits(ServerLevel World, BlockPos At) {
        BlockState Here = World.getBlockState(At);
        BlockPos Below = At.below();
        return Here.canBeReplaced() && Here.getFluidState().isEmpty() && World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    public static boolean holds(Player User) {
        return User.getInventory().hasAnyMatching(Stack -> Stack.is(Scp2317RiteNoteRegistry.Note.get())) || User.containerMenu.getCarried().is(Scp2317RiteNoteRegistry.Note.get());
    }

    public static boolean take(ServerPlayer User, BlockPos Pos) {
        ServerLevel World = User.serverLevel();
        if (holds(User)) {
            World.playSound(null, Pos, Scp2317RiteNoteRegistry.Read.get(), SoundSource.BLOCKS, ReadVolume, ReadPitch);
            return false;
        }
        ItemStack Copy = new ItemStack(Scp2317RiteNoteRegistry.Note.get());
        User.getInventory().add(Copy);
        if (!Copy.isEmpty()) {
            User.drop(Copy, false);
        }
        Scp2317Sounds.play(World, Vec3.atCenterOf(Pos), Scp2317RiteNoteRegistry.TakeLayers, SoundSource.BLOCKS, TakeVolume, TakePitch);
        return true;
    }
}
