package net.rai.scp_unleashed.scp4666.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp4666.Scp4666Cues;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

public class Scp4666GramophoneBlockEntity extends BlockEntity {
    public static final int TicksPerBeat = 5;
    public static final int WrongTicksPerBeat = 9;
    public static final int WrongShift = -5;
    public static final int CrackleEvery = 26;
    public static final float Volume = 0.9f;
    private boolean Playing;
    private boolean Wrong;
    private int Song;
    private int Cursor;
    private int Wait;
    private int Age;

    public Scp4666GramophoneBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp4666Registry.GramophoneEntity.get(), Pos, State);
    }

    public boolean playing() {
        return Playing;
    }

    public boolean wrong() {
        return Wrong;
    }

    public void start(boolean Odd) {
        Playing = true;
        Wrong = Odd;
        Cursor = 0;
        Wait = 10;
        if (Odd && level != null) {
            Song = level.getRandom().nextInt(Scp4666Cues.Carols.length);
        }
        changed();
    }

    public void stop() {
        Playing = false;
        Wrong = false;
        Cursor = 0;
        changed();
        if (level != null) {
            level.playSound(null, worldPosition, Scp4666Registry.Crackle.get(), SoundSource.BLOCKS, 0.4f, 0.6f);
        }
    }

    private void changed() {
        setChanged();
        if (level != null) {
            BlockState State = getBlockState();
            if (State.hasProperty(Scp4666GramophoneBlock.Playing) && State.getValue(Scp4666GramophoneBlock.Playing) != Playing) {
                level.setBlock(worldPosition, State.setValue(Scp4666GramophoneBlock.Playing, Playing), Block.UPDATE_ALL);
            }
        }
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp4666GramophoneBlockEntity Player) {
        if (World instanceof ServerLevel Server) {
            Player.tickServer(Server);
        }
    }

    private void tickServer(ServerLevel World) {
        if (!Playing) {
            BlockState State = getBlockState();
            if (State.hasProperty(Scp4666GramophoneBlock.Playing) && State.getValue(Scp4666GramophoneBlock.Playing)) {
                changed();
            }
            return;
        }
        Age++;
        if (Age % CrackleEvery == 0) {
            World.playSound(null, worldPosition, Scp4666Registry.Crackle.get(), SoundSource.RECORDS, 0.35f, 0.9f + World.getRandom().nextFloat() * 0.2f);
        }
        if (Wait > 0) {
            Wait--;
            return;
        }
        int[][] Melody = Scp4666Cues.Carols[Math.floorMod(Song, Scp4666Cues.Carols.length)];
        if (Cursor >= Melody.length) {
            if (!Wrong) {
                Song = (Song + 1) % Scp4666Cues.Carols.length;
            }
            stop();
            return;
        }
        int[] Note = Melody[Cursor];
        int Pitch = Math.max(0, Math.min(24, Note[0] + (Wrong ? WrongShift : 0)));
        float Base = Scp4666Cues.pitchOf(Pitch) * (Wrong ? 0.97f + World.getRandom().nextFloat() * 0.04f : 1.0f);
        World.playSound(null, worldPosition, Scp4666Registry.CarolNote.get(), SoundSource.RECORDS, Volume, Math.max(0.5f, Math.min(2.0f, Base)));
        World.playSound(null, worldPosition, Scp4666Registry.CarolBell.get(), SoundSource.RECORDS, Volume * 0.35f, Math.max(0.5f, Math.min(2.0f, Base)));
        Wait = Note[1] * (Wrong ? WrongTicksPerBeat : TicksPerBeat);
        Cursor++;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Song", Song);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Song = Tag.getInt("Song");
        Playing = false;
    }
}
