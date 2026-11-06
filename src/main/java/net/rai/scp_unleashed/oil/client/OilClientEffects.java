package net.rai.scp_unleashed.oil.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.oil.ChemicalProcessorBlockEntity;
import net.rai.scp_unleashed.oil.CombustionGeneratorBlock;
import net.rai.scp_unleashed.oil.CombustionGeneratorBlockEntity;
import net.rai.scp_unleashed.oil.DistillationTowerBlockEntity;
import net.rai.scp_unleashed.oil.OilExtractorBlockEntity;
import net.rai.scp_unleashed.oil.OilLayout;
import net.rai.scp_unleashed.oil.OilMachineBlockEntity;
import net.rai.scp_unleashed.oil.OilRefineryBlockEntity;
import net.rai.scp_unleashed.oil.OilRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class OilClientEffects {
    public static final float SoundStart = 0.03f;
    public static final float ThumpPhase = 0.25f;
    public static final float CreakPhase = 0.75f;
    public static final float GlugChance = 0.35f;
    public static final int KnockTicks = 4;

    private static final Map<OilMachineBlockEntity, OilLoopSound> Loops = new WeakHashMap<>();

    private OilClientEffects() {
    }

    public static void tick(OilMachineBlockEntity Machine) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Machine.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        float Churn = Machine.getChurn();
        if (Churn <= SoundStart) {
            return;
        }
        RandomSource Random = World.getRandom();
        if (Machine instanceof OilExtractorBlockEntity Extractor) {
            extractor(World, Extractor, Churn, Random);
        } else if (Machine instanceof DistillationTowerBlockEntity) {
            tower(World, Machine, Churn, Random);
        } else if (Machine instanceof ChemicalProcessorBlockEntity) {
            processor(World, Machine, Churn, Random);
        } else if (Machine instanceof CombustionGeneratorBlockEntity) {
            generator(Game, World, Machine, Churn, Random);
        } else if (Machine instanceof OilRefineryBlockEntity) {
            refinery(World, Machine, Churn, Random);
        }
    }

    private static boolean crossed(float Before, float After, float Mark) {
        if (After >= Before) {
            return Before < Mark && After >= Mark;
        }
        return Before < Mark || After >= Mark;
    }

    private static void play(Level World, Vec3 Spot, SoundEvent Sound, float Volume, float Pitch) {
        World.playLocalSound(Spot.x, Spot.y, Spot.z, Sound, SoundSource.BLOCKS, Volume, Pitch, false);
    }

    private static Vec3 point(OilMachineBlockEntity Machine, double X, double Y, double Z) {
        return OilLayout.toWorld(Machine.getBlockPos(), Machine.getFront(), X, Y, Z);
    }

    private static void extractor(Level World, OilExtractorBlockEntity Machine, float Churn, RandomSource Random) {
        float Before = Machine.getPreviousPhase();
        float After = Machine.getCurrentPhase();
        Vec3 Well = point(Machine, 24.0, 20.0, 8.0);
        Vec3 Crank = point(Machine, 24.0, 20.0, 72.0);
        if (crossed(Before, After, ThumpPhase)) {
            float Loud = 0.35f + 0.45f * Churn;
            play(World, Well, OilRegistry.ExtractorThump.get(), Loud, 0.9f + Random.nextFloat() * 0.15f);
            play(World, Crank, OilRegistry.ExtractorClank.get(), Loud * 0.6f, 0.95f + Random.nextFloat() * 0.1f);
            if (Random.nextFloat() < GlugChance) {
                play(World, Well, OilRegistry.ExtractorGlug.get(), Loud * 0.7f, 0.8f + Random.nextFloat() * 0.2f);
            }
        }
        if (crossed(Before, After, CreakPhase)) {
            Vec3 Pivot = point(Machine, 24.0, 50.0, 40.0);
            play(World, Pivot, OilRegistry.ExtractorCreak.get(), 0.2f + 0.25f * Churn, 0.85f + Random.nextFloat() * 0.2f);
        }
    }

    private static void tower(Level World, OilMachineBlockEntity Machine, float Churn, RandomSource Random) {
        if (Random.nextFloat() < 0.04f * Churn) {
            play(World, point(Machine, 24.0, 80.0, 24.0), OilRegistry.TowerBoil.get(), 0.35f + 0.3f * Churn, 0.85f + Random.nextFloat() * 0.2f);
        }
        if (Random.nextFloat() < 0.03f * Churn) {
            play(World, point(Machine, 24.0, 12.0, 4.0), OilRegistry.TowerCrackle.get(), 0.25f + 0.2f * Churn, 0.8f + Random.nextFloat() * 0.2f);
        }
        if (Random.nextFloat() < 0.18f * Churn) {
            Vec3 Top = point(Machine, 24.0, 161.0, 24.0);
            World.addParticle(ParticleTypes.WHITE_SMOKE, Top.x + (Random.nextDouble() - 0.5) * 0.15, Top.y, Top.z + (Random.nextDouble() - 0.5) * 0.15,
                    0.0, 0.03 + Random.nextDouble() * 0.02, 0.0);
        }
    }

    private static void processor(Level World, OilMachineBlockEntity Machine, float Churn, RandomSource Random) {
        if (Random.nextFloat() < 0.05f * Churn) {
            play(World, point(Machine, 24.0, 20.0, 26.0), OilRegistry.ProcessorBubble.get(), 0.35f + 0.25f * Churn, 0.85f + Random.nextFloat() * 0.25f);
        }
        if (Random.nextFloat() < 0.015f * Churn) {
            play(World, point(Machine, 24.0, 44.0, 26.0), OilRegistry.ProcessorHiss.get(), 0.15f + 0.1f * Churn, 0.7f + Random.nextFloat() * 0.2f);
        }
    }

    private static void generator(Minecraft Game, Level World, OilMachineBlockEntity Machine, float Churn, RandomSource Random) {
        SoundManager Sounds = Game.getSoundManager();
        OilLoopSound Current = Loops.get(Machine);
        if (Current == null || !Sounds.isActive(Current)) {
            Current = new OilLoopSound(Machine, OilRegistry.GeneratorEngine.get(), 0.55f, 0.55f, 0.25f);
            Loops.put(Machine, Current);
            Sounds.play(Current);
        }
        if (World.getGameTime() % KnockTicks == 0) {
            play(World, point(Machine, 24.0, 14.0, 30.0), OilRegistry.GeneratorKnock.get(), 0.12f + 0.16f * Churn, 0.9f + Random.nextFloat() * 0.15f);
        }
        BlockPos Exhaust = CombustionGeneratorBlock.Layout.cellPos(Machine.getBlockPos(), Machine.getFront(), CombustionGeneratorBlock.ExhaustPart).above();
        if (Random.nextFloat() < 0.05f * Churn && World.getBlockState(Exhaust).isAir()) {
            Vec3 Stack = point(Machine, 8.0, 33.0, 40.0);
            World.addParticle(ParticleTypes.SMOKE, Stack.x, Stack.y, Stack.z, 0.0, 0.01, 0.0);
        }
    }

    private static void refinery(Level World, OilMachineBlockEntity Machine, float Churn, RandomSource Random) {
        if (Random.nextFloat() < 0.025f * Churn) {
            play(World, point(Machine, 24.0, 40.0, 22.0), OilRegistry.RefineryHiss.get(), 0.2f + 0.15f * Churn, 0.75f + Random.nextFloat() * 0.25f);
        }
        if (Random.nextFloat() < 0.012f * Churn) {
            play(World, point(Machine, 36.0, 6.0, 2.0), OilRegistry.RefineryDrop.get(), 0.3f, 0.9f + Random.nextFloat() * 0.2f);
        }
        if (Random.nextFloat() < 0.15f * Churn) {
            Vec3 Top = point(Machine, 24.0, 65.0, 22.0);
            World.addParticle(ParticleTypes.CLOUD, Top.x + (Random.nextDouble() - 0.5) * 0.1, Top.y, Top.z + (Random.nextDouble() - 0.5) * 0.1, 0.0, 0.025, 0.0);
        }
    }
}
