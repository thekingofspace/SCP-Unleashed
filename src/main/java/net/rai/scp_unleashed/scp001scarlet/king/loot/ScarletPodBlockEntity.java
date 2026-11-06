package net.rai.scp_unleashed.scp001scarlet.king.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingConfig;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Registry;
import org.joml.Vector3f;

public class ScarletPodBlockEntity extends BlockEntity {
    public static final int BeatEvery = 60;
    public static final long Unset = Long.MIN_VALUE;
    private long Start = Unset;

    public ScarletPodBlockEntity(BlockPos Pos, BlockState State) {
        super(ScarletKingRegistry.PodEntity.get(), Pos, State);
    }

    public long start() {
        return Start;
    }

    public void start(long Value) {
        Start = Value;
        setChanged();
    }

    public float progress(long Now) {
        return Start == Unset ? 0.0f : Math.min(1.0f, (Now - Start) / (float) ScarletKingConfig.podTicks());
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, ScarletPodBlockEntity Pod) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        long Now = World.getGameTime();
        if (Pod.Start == Unset) {
            Pod.start(Now);
        }
        float Grown = Pod.progress(Now);
        int Stage = Math.min(3, (int) (Grown * 4.0f));
        if (State.getValue(ScarletPodBlock.Stage) != Stage) {
            World.setBlock(Pos, State.setValue(ScarletPodBlock.Stage, Stage), Block.UPDATE_ALL);
        }
        if (Now % BeatEvery == 0L) {
            World.playSound(null, Pos, ScarletKingRegistry.PodBeat.get(), SoundSource.BLOCKS, 0.4f + Stage * 0.15f, 0.8f + Stage * 0.1f);
        }
        if (Grown >= 1.0f) {
            hatch(Server, Pos);
        }
    }

    public static void hatch(ServerLevel World, BlockPos Pos) {
        World.removeBlock(Pos, false);
        Scp999Blob Born = Scp999Registry.Blob.get().spawn(World, Pos, MobSpawnType.BREEDING);
        if (Born != null) {
            Born.setPersistenceRequired();
        }
        World.sendParticles(new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.4f), 1.6f), Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.05);
        World.sendParticles(ParticleTypes.HEART, Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 6, 0.4, 0.3, 0.4, 0.05);
        World.playSound(null, Pos, ScarletKingRegistry.PodHatch.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putLong("Start", Start);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Start = Tag.contains("Start") ? Tag.getLong("Start") : Unset;
    }
}
