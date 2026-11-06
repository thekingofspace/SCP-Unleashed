package net.rai.scp_unleashed.assembler.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.assembler.AdvancedAssemblerBlockEntity;
import net.rai.scp_unleashed.assembler.AssemblerRegistry;

public final class AssemblerClientEffects {
    private AssemblerClientEffects() {
    }

    public static void tick(AdvancedAssemblerBlockEntity Assembler) {
        Level World = Assembler.getLevel();
        if (World == null || Minecraft.getInstance().level != World) {
            return;
        }
        RandomSource Random = World.getRandom();
        boolean Engaged = Assembler.getEngage(1.0f) > 0.98f;
        float Before = Assembler.getPreviousClock() / AdvancedAssemblerBlockEntity.CycleTicks;
        float After = Assembler.getCurrentClock() / AdvancedAssemblerBlockEntity.CycleTicks;
        if (Engaged && Before != After) {
            for (float Mark : AssemblerArms.GripperMoves) {
                if (crossed(Before, After, Mark)) {
                    play(Assembler, World, AssemblerArms.gripperTarget(After).Tip(), AssemblerRegistry.Servo.get(), 0.22f, 1.05f + Random.nextFloat() * 0.15f);
                }
            }
            for (float Mark : AssemblerArms.WelderMoves) {
                if (crossed(Before, After, Mark)) {
                    play(Assembler, World, AssemblerArms.welderTarget(After).Tip(), AssemblerRegistry.Servo.get(), 0.2f, 0.88f + Random.nextFloat() * 0.12f);
                }
            }
            for (float[] Window : AssemblerArms.WeldWindows) {
                if (crossed(Before, After, Window[0])) {
                    play(Assembler, World, AssemblerArms.welderTarget(After).Tip(), AssemblerRegistry.Weld.get(), 0.4f, 0.95f + Random.nextFloat() * 0.1f);
                }
            }
        }
        if (Engaged && AssemblerArms.welding(After)) {
            Vec3 Tip = toWorld(Assembler, AssemblerArms.welderTarget(After).Tip());
            int Count = Random.nextFloat() < 0.35f ? 2 : 1;
            for (int Index = 0; Index < Count; Index++) {
                World.addParticle(AssemblerRegistry.Spark.get(), Tip.x, Tip.y, Tip.z, (Random.nextDouble() - 0.5) * 0.09, 0.02 + Random.nextDouble() * 0.06,
                        (Random.nextDouble() - 0.5) * 0.09);
            }
        }
        if (Assembler.takeFinish()) {
            finish(Assembler, World, Random);
        }
    }

    private static boolean crossed(float Before, float After, float Mark) {
        if (After >= Before) {
            return Mark > Before && Mark <= After;
        }
        return Mark > Before || Mark <= After;
    }

    private static void play(AdvancedAssemblerBlockEntity Assembler, Level World, Vec3 Local, SoundEvent Sound, float Volume, float Pitch) {
        Vec3 At = toWorld(Assembler, Local);
        World.playLocalSound(At.x, At.y, At.z, Sound, SoundSource.BLOCKS, Volume, Pitch, false);
    }

    private static void finish(AdvancedAssemblerBlockEntity Assembler, Level World, RandomSource Random) {
        Vec3 Centre = toWorld(Assembler, AssemblerArms.WorkCentre.add(0.0, 0.6, 0.0));
        for (int Index = 0; Index < 10; Index++) {
            World.addParticle(AssemblerRegistry.Spark.get(), Centre.x, Centre.y, Centre.z, (Random.nextDouble() - 0.5) * 0.14, 0.03 + Random.nextDouble() * 0.08,
                    (Random.nextDouble() - 0.5) * 0.14);
        }
        Vec3 Vent = toWorld(Assembler, new Vec3(8.0, 16.2, 8.0));
        for (int Index = 0; Index < 3; Index++) {
            World.addParticle(ParticleTypes.SMOKE, Vent.x + (Random.nextDouble() - 0.5) * 0.2, Vent.y, Vent.z + (Random.nextDouble() - 0.5) * 0.2, 0.0, 0.04, 0.0);
        }
    }

    public static Vec3 toWorld(AdvancedAssemblerBlockEntity Assembler, Vec3 Local) {
        BlockPos Pos = Assembler.getBlockPos();
        Direction Front = Assembler.getFront();
        double X = Local.x / 16.0 - 0.5;
        double Z = Local.z / 16.0 - 0.5;
        double TurnedX;
        double TurnedZ;
        switch (Front) {
            case EAST -> {
                TurnedX = -Z;
                TurnedZ = X;
            }
            case SOUTH -> {
                TurnedX = -X;
                TurnedZ = -Z;
            }
            case WEST -> {
                TurnedX = Z;
                TurnedZ = -X;
            }
            default -> {
                TurnedX = X;
                TurnedZ = Z;
            }
        }
        return new Vec3(Pos.getX() + 0.5 + TurnedX, Pos.getY() + 1.0 + Local.y / 16.0, Pos.getZ() + 0.5 + TurnedZ);
    }
}
