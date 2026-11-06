package net.rai.scp_unleashed.tesla.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.tesla.TeslaGateBlockEntity;
import net.rai.scp_unleashed.tesla.TeslaGeometry;
import net.rai.scp_unleashed.tesla.TeslaPhase;
import net.rai.scp_unleashed.tesla.TeslaRegistry;
import net.rai.scp_unleashed.tesla.TeslaStatus;

import java.util.Map;
import java.util.WeakHashMap;

public final class TeslaClientEffects {
    private static final Map<TeslaGateBlockEntity, TeslaLoopSound> Hums = new WeakHashMap<>();
    private static final Map<TeslaGateBlockEntity, TeslaLoopSound> Buzzes = new WeakHashMap<>();
    private static final double SparkLift = 0.3;

    private TeslaClientEffects() {
    }

    public static void tick(TeslaGateBlockEntity Gate) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Gate.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        SoundManager Sounds = Game.getSoundManager();
        if (humLevel(Gate) > 0.0) {
            TeslaLoopSound Hum = Hums.get(Gate);
            if (Hum == null || !Sounds.isActive(Hum)) {
                Hum = new TeslaLoopSound(Gate, TeslaRegistry.Hum.get(), 1.0f, TeslaClientEffects::humLevel, TeslaClientEffects::humPitch);
                Hums.put(Gate, Hum);
                Sounds.play(Hum);
            }
        }
        if (buzzLevel(Gate) > 0.0) {
            TeslaLoopSound Buzz = Buzzes.get(Gate);
            if (Buzz == null || !Sounds.isActive(Buzz)) {
                Buzz = new TeslaLoopSound(Gate, TeslaRegistry.Buzz.get(), 0.4f, TeslaClientEffects::buzzLevel, Target -> 1.0);
                Buzzes.put(Gate, Buzz);
                Sounds.play(Buzz);
            }
        }
        sparks(Gate, World);
    }

    public static double humLevel(TeslaGateBlockEntity Gate) {
        return switch (Gate.getPhase()) {
            case Charged -> 0.7;
            case WindUp, Discharge -> 1.0;
            case Cooldown -> Math.max(0.0, 0.7 - Gate.getPhaseTicks() / (double) TeslaGateBlockEntity.CooldownTicks);
            default -> 0.0;
        };
    }

    public static double humPitch(TeslaGateBlockEntity Gate) {
        return 0.94 + 0.22 * Gate.windUpProgress(0.0f);
    }

    public static double buzzLevel(TeslaGateBlockEntity Gate) {
        return Gate.getPhase() == TeslaPhase.Idle && Gate.getStatus() == TeslaStatus.Ready ? 1.0 : 0.0;
    }

    private static void sparks(TeslaGateBlockEntity Gate, Level World) {
        RandomSource Random = World.getRandom();
        TeslaPhase Phase = Gate.getPhase();
        int Count = switch (Phase) {
            case Charged -> Random.nextFloat() < 0.2f ? 1 : 0;
            case WindUp -> 1 + Random.nextInt(2);
            case Discharge -> 3 + Random.nextInt(3);
            case Cooldown -> Random.nextFloat() < 0.1f ? 1 : 0;
            default -> 0;
        };
        double Speed = Phase == TeslaPhase.Discharge ? 0.12 : 0.05;
        for (int Index = 0; Index < Count; Index++) {
            TeslaGeometry.Emitter Bar = TeslaGeometry.BarPoints.get(Random.nextInt(TeslaGeometry.BarPoints.size()));
            Vec3 Outward = Bar.Surface().normal();
            Vec3 Tip = Bar.Start().add(Outward.scale(SparkLift));
            Vec3 Scatter = new Vec3((Random.nextDouble() - 0.5) * 1.2, (Random.nextDouble() - 0.3) * 0.8, (Random.nextDouble() - 0.5) * 1.2);
            Vec3 Push = Scatter.subtract(Outward.scale(Scatter.dot(Outward))).add(Outward.scale(0.35 + Random.nextDouble() * 0.5));
            Vec3 Local = Tip.add(Push.scale(Speed * 16.0));
            Vec3 At = TeslaGeometry.toWorld(Gate.getOrigin(), Gate.getFront(), Tip);
            Vec3 Velocity = TeslaGeometry.toWorld(Gate.getOrigin(), Gate.getFront(), Local).subtract(At);
            World.addParticle(TeslaRegistry.Spark.get(), At.x, At.y, At.z, Velocity.x, Velocity.y, Velocity.z);
        }
    }
}
