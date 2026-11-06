package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.labware.LabExposure;

public class Scp610PustuleBlock extends Scp610FleshBlock {
    public static final double BurstRadius = 2.0;

    public Scp610PustuleBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
    }

    @Override
    public void stepOn(Level World, BlockPos Pos, BlockState State, Entity Walker) {
        if (World instanceof ServerLevel Server && Walker instanceof LivingEntity Living && !(Living instanceof Scp610Creature) && !Walker.isSteppingCarefully()) {
            Scp610Hive Owner = Scp610Hives.ownerOf(Server, Pos);
            Server.setBlock(Pos, Scp610Registry.Flesh.get().defaultBlockState().setValue(Wild, State.getValue(Wild)), Block.UPDATE_ALL);
            if (Owner != null) {
                Scp610Hives.own(Server, Owner, Pos);
            }
        }
        super.stepOn(World, Pos, State, Walker);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moving) {
        if (!State.is(Replaced.getBlock()) && World instanceof ServerLevel Server) {
            burst(Server, Pos);
        }
        super.onRemove(State, World, Pos, Replaced, Moving);
    }

    public static void burst(ServerLevel World, BlockPos Pos) {
        Vec3 Centre = Vec3.atCenterOf(Pos).add(0.0, 0.6, 0.0);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp610Sounds.BurstLayers) {
            World.playSound(null, Centre.x, Centre.y, Centre.z, Layer.get(), SoundSource.BLOCKS, 1.0f, 0.9f + World.random.nextFloat() * 0.2f);
        }
        Scp610Infection.cloud(World, Centre, BurstRadius, 24);
        Scp610Infection.exposeAround(World, Centre, Found -> Found.equals(Pos), BurstRadius);
    }
}
