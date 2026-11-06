package net.rai.scp_unleashed.steam.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.steam.CoolingTowerBlockEntity;
import net.rai.scp_unleashed.steam.MachineLayout;
import net.rai.scp_unleashed.steam.MachineLight;
import net.rai.scp_unleashed.steam.SteamMachineBlock;
import net.rai.scp_unleashed.steam.SteamRegistry;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;
import net.rai.scp_unleashed.steam.WaterBoilerBlock;
import net.rai.scp_unleashed.steam.WaterBoilerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class SteamClientEffects {
    public static final float FlameChance = 0.14f;
    public static final float CrackleChance = 0.025f;
    public static final float BoilChance = 0.035f;
    public static final float WeepChance = 0.06f;
    public static final float ValveChance = 0.5f;
    public static final int ValveSoundTicks = 28;
    public static final float SoundStart = 0.02f;
    public static final float RainChance = 0.07f;
    public static final double MouthSpread = 0.8;
    public static final double ValveX = 12.5;
    public static final double ValveY = 64.5;
    public static final double ValveZ = 13.5;

    private static final Map<SteamTurbineBlockEntity, List<TurbineLoopSound>> Loops = new WeakHashMap<>();

    private SteamClientEffects() {
    }

    private static boolean isLocal(Level World) {
        return World != null && Minecraft.getInstance().level == World;
    }

    private static List<TurbineLoopSound.Layer> layers() {
        return List.of(
                new TurbineLoopSound.Layer(SteamRegistry.TurbineRumble.get(), 0.55f, 0.0f, 0.55f, 0.6f),
                new TurbineLoopSound.Layer(SteamRegistry.TurbineRush.get(), 0.45f, 0.08f, 0.7f, 0.7f),
                new TurbineLoopSound.Layer(SteamRegistry.TurbineWhine.get(), 0.4f, 0.2f, 0.75f, 1.15f));
    }

    public static void boilerTick(WaterBoilerBlockEntity Machine) {
        Level World = Machine.getLevel();
        if (!isLocal(World)) {
            return;
        }
        BlockState State = Machine.getBlockState();
        if (!(State.getBlock() instanceof WaterBoilerBlock)) {
            return;
        }
        BlockPos Pos = Machine.getBlockPos();
        Direction Front = State.getValue(WaterBoilerBlock.Facing);
        MachineLayout Layout = WaterBoilerBlock.Layout;
        int Master = Layout.Master();
        RandomSource Random = World.getRandom();
        BlockState Door = World.getBlockState(Layout.cellPos(Layout.originOf(Pos, Master, Front), Front, WaterBoilerBlock.DoorPart));
        if (Door.getBlock() instanceof WaterBoilerBlock && Door.getValue(SteamMachineBlock.Light) == MachineLight.Working) {
            if (Random.nextFloat() < FlameChance) {
                Vec3 Spot = Layout.point(Pos, Master, Front, 24.0 + (Random.nextDouble() - 0.5) * 10.0, 5.0 + Random.nextDouble() * 5.0, -0.5);
                World.addParticle(ParticleTypes.SMOKE, Spot.x, Spot.y, Spot.z, 0.0, 0.0, 0.0);
                World.addParticle(ParticleTypes.FLAME, Spot.x, Spot.y, Spot.z, 0.0, 0.0, 0.0);
            }
            if (Random.nextFloat() < CrackleChance) {
                Vec3 Spot = Layout.point(Pos, Master, Front, 24.0, 8.0, 0.0);
                World.playLocalSound(Spot.x, Spot.y, Spot.z, SteamRegistry.BoilerFire.get(), SoundSource.BLOCKS, 0.8f, 0.85f + Random.nextFloat() * 0.2f, false);
            }
        }
        int Heat = Machine.getShownHeat();
        Vec3 Valve = Layout.point(Pos, Master, Front, ValveX, ValveY, ValveZ);
        if (Machine.isShownBlocked()) {
            if (Random.nextFloat() < ValveChance) {
                World.addParticle(SteamRegistry.SteamPlume.get(), Valve.x, Valve.y, Valve.z, (Random.nextDouble() - 0.5) * 0.04, 0.12 + Random.nextDouble() * 0.05,
                        (Random.nextDouble() - 0.5) * 0.04);
            }
            if (Math.floorMod(World.getGameTime() + Pos.asLong(), ValveSoundTicks) == 0) {
                World.playLocalSound(Valve.x, Valve.y, Valve.z, SteamRegistry.BoilerValve.get(), SoundSource.BLOCKS, 0.7f, 0.9f + Random.nextFloat() * 0.2f, false);
            }
        }
        if (Heat <= 0) {
            return;
        }
        float Strength = Heat / (float) WaterBoilerBlockEntity.HeatSteps;
        if (Random.nextFloat() < BoilChance * (0.5f + Strength)) {
            Vec3 Drum = Layout.point(Pos, Master, Front, 24.0, 36.0, 24.0);
            World.playLocalSound(Drum.x, Drum.y, Drum.z, SteamRegistry.BoilerBoil.get(), SoundSource.BLOCKS, 0.45f + 0.35f * Strength, 0.75f + Random.nextFloat() * 0.15f, false);
        }
        if (!Machine.isShownBlocked() && Random.nextFloat() < WeepChance * Strength) {
            World.addParticle(SteamRegistry.SteamPlume.get(), Valve.x, Valve.y, Valve.z, 0.0, 0.03, 0.0);
        }
    }

    public static void turbineTick(SteamTurbineBlockEntity Machine) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Machine.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        if (Machine.getSpin() <= SoundStart) {
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        List<TurbineLoopSound> Playing = Loops.get(Machine);
        boolean Alive = Playing != null && !Playing.isEmpty();
        if (Alive) {
            for (TurbineLoopSound Loop : Playing) {
                Alive &= Sounds.isActive(Loop);
            }
        }
        if (Alive) {
            return;
        }
        if (Playing != null) {
            for (TurbineLoopSound Loop : Playing) {
                Sounds.stop(Loop);
            }
        }
        List<TurbineLoopSound> Started = new ArrayList<>();
        for (TurbineLoopSound.Layer Kind : layers()) {
            TurbineLoopSound Loop = new TurbineLoopSound(Machine, Kind);
            Started.add(Loop);
            Sounds.play(Loop);
        }
        Loops.put(Machine, Started);
    }

    public static void towerTick(CoolingTowerBlockEntity Machine) {
        Level World = Machine.getLevel();
        if (!isLocal(World)) {
            return;
        }
        int Plume = Machine.getPlume();
        if (Plume <= 0) {
            return;
        }
        RandomSource Random = World.getRandom();
        BlockPos Mouth = Machine.getMouth();
        float Strength = Plume / (float) CoolingTowerBlockEntity.PlumeSteps;
        float Puffs = 0.6f + Strength * 2.2f;
        while (Puffs > 0.0f) {
            if (Puffs >= 1.0f || Random.nextFloat() < Puffs) {
                double X = Mouth.getX() + 0.5 + (Random.nextDouble() * 2.0 - 1.0) * MouthSpread;
                double Z = Mouth.getZ() + 0.5 + (Random.nextDouble() * 2.0 - 1.0) * MouthSpread;
                double Y = Mouth.getY() + 0.65 + Random.nextDouble() * 0.25;
                World.addParticle(SteamRegistry.SteamPlume.get(), X, Y, Z, (Random.nextDouble() - 0.5) * 0.03, 0.05 + Strength * 0.07 + Random.nextDouble() * 0.03,
                        (Random.nextDouble() - 0.5) * 0.03);
            }
            Puffs -= 1.0f;
        }
        if (Random.nextFloat() < RainChance * (0.4f + Strength)) {
            BlockPos Base = Machine.getBlockPos();
            World.playLocalSound(Base.getX() + 0.5, Base.getY() + 0.6, Base.getZ() + 0.5, SteamRegistry.TowerRain.get(), SoundSource.BLOCKS, 0.3f + 0.35f * Strength,
                    0.9f + Random.nextFloat() * 0.15f, false);
        }
    }
}
