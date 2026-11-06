package net.rai.scp_unleashed.processing.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.processing.ElectricFurnaceBlockEntity;
import net.rai.scp_unleashed.processing.ItemPressBlockEntity;
import net.rai.scp_unleashed.processing.ProcessingMachineBlock;
import net.rai.scp_unleashed.processing.ProcessingMachineBlockEntity;
import net.rai.scp_unleashed.processing.ProcessingRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class ProcessingClientEffects {
    public static final float HumLoudness = 0.55f;
    public static final int Crumbs = 6;
    public static final double MouthDepth = 0.02;
    public static final double DieHeight = 5.0 / 16.0;

    private static final Map<ProcessingMachineBlockEntity, MachineLoopSound> Hums = new WeakHashMap<>();

    private ProcessingClientEffects() {
    }

    public static void tick(ProcessingMachineBlockEntity Machine) {
        if (!(Machine instanceof ElectricFurnaceBlockEntity) || !Machine.isWorking()) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        if (Game.level == null || Game.level != Machine.getLevel()) {
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        MachineLoopSound Hum = Hums.get(Machine);
        if (Hum == null || !Sounds.isActive(Hum)) {
            Hum = new MachineLoopSound(Machine, ProcessingRegistry.FurnaceHum.get(), HumLoudness);
            Hums.put(Machine, Hum);
            Sounds.play(Hum);
        }
    }

    public static void completed(ProcessingMachineBlockEntity Machine, ItemStack Result) {
        if (!(Machine instanceof ItemPressBlockEntity Press)) {
            return;
        }
        Level World = Press.getLevel();
        if (World == null) {
            return;
        }
        ItemStack Piece = Press.getShownWorkpiece().isEmpty() ? Result : Press.getShownWorkpiece();
        if (Piece.isEmpty()) {
            return;
        }
        Direction Front = Press.getBlockState().hasProperty(ProcessingMachineBlock.Facing) ? Press.getBlockState().getValue(ProcessingMachineBlock.Facing) : Direction.NORTH;
        BlockPos Pos = Press.getBlockPos();
        RandomSource Random = World.getRandom();
        double MouthX = Pos.getX() + 0.5 + Front.getStepX() * (0.5 + MouthDepth);
        double MouthZ = Pos.getZ() + 0.5 + Front.getStepZ() * (0.5 + MouthDepth);
        double Y = Pos.getY() + DieHeight;
        for (int Index = 0; Index < Crumbs; Index++) {
            double Across = (Random.nextDouble() - 0.5) * 0.45;
            double Speed = 0.05 + Random.nextDouble() * 0.05;
            double SideX = Front.getStepZ() * Across;
            double SideZ = -Front.getStepX() * Across;
            World.addParticle(new ItemParticleOption(ParticleTypes.ITEM, Piece), MouthX + SideX, Y, MouthZ + SideZ,
                    Front.getStepX() * Speed + SideX * 0.15, 0.08 + Random.nextDouble() * 0.06, Front.getStepZ() * Speed + SideZ * 0.15);
        }
        World.addParticle(ParticleTypes.POOF, MouthX, Y + 0.05, MouthZ, Front.getStepX() * 0.02, 0.01, Front.getStepZ() * 0.02);
    }
}
