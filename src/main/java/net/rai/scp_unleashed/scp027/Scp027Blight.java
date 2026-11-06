package net.rai.scp_unleashed.scp027;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.List;

public final class Scp027Blight {
    public static final int WiltInterval = 120;
    public static final int WiltRadius = 5;
    public static final int WiltTries = 6;
    public static final int SpoilInterval = 40;
    public static final double SpoilRadius = 4.0;
    public static final int SpoilAge = 100;
    public static final int SpoilPerPulse = 2;

    private Scp027Blight() {
    }

    public static boolean wilt(ServerLevel World, Scp027Entity Subject) {
        if (!EventHooks.canEntityGrief(World, Subject)) {
            return false;
        }
        RandomSource Random = World.getRandom();
        BlockPos Centre = Subject.blockPosition();
        for (int Try = 0; Try < WiltTries; Try++) {
            BlockPos Pos = Centre.offset(Random.nextInt(WiltRadius * 2 + 1) - WiltRadius, Random.nextInt(3) - 1, Random.nextInt(WiltRadius * 2 + 1) - WiltRadius);
            BlockState State = World.getBlockState(Pos);
            if (!(State.getBlock() instanceof CropBlock Crop)) {
                continue;
            }
            int Age = Crop.getAge(State);
            if (Age <= 0) {
                continue;
            }
            World.setBlock(Pos, Crop.getStateForAge(Age - 1), 2);
            World.sendParticles(ParticleTypes.ASH, Pos.getX() + 0.5, Pos.getY() + 0.4, Pos.getZ() + 0.5, 6, 0.25, 0.2, 0.25, 0.0);
            World.playSound(null, Pos, Scp027Registry.Wilt.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 0.8f + Random.nextFloat() * 0.2f);
            return true;
        }
        return false;
    }

    public static boolean spoils(ItemStack Stack) {
        if (Stack.isEmpty() || Stack.is(Items.ROTTEN_FLESH) || !Stack.has(DataComponents.FOOD)) {
            return false;
        }
        return Stack.getRarity() == Rarity.COMMON;
    }

    public static int spoil(ServerLevel World, Scp027Entity Subject) {
        AABB Area = Subject.getBoundingBox().inflate(SpoilRadius, 2.0, SpoilRadius);
        List<ItemEntity> Found = World.getEntitiesOfClass(ItemEntity.class, Area, Drop -> Drop.isAlive() && Drop.getAge() >= SpoilAge && spoils(Drop.getItem()));
        int Done = 0;
        for (ItemEntity Drop : Found) {
            if (Done >= SpoilPerPulse) {
                break;
            }
            Drop.setItem(new ItemStack(Items.ROTTEN_FLESH, Drop.getItem().getCount()));
            Vec3 At = Drop.position();
            World.sendParticles(ParticleTypes.SMOKE, At.x, At.y + 0.2, At.z, 4, 0.1, 0.1, 0.1, 0.01);
            World.playSound(null, At.x, At.y, At.z, Scp027Registry.Spoil.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 0.85f + World.getRandom().nextFloat() * 0.2f);
            Done++;
        }
        return Done;
    }
}
