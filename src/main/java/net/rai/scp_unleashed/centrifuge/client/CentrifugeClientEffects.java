package net.rai.scp_unleashed.centrifuge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.centrifuge.CentrifugeBlockEntity;
import net.rai.scp_unleashed.centrifuge.CentrifugeRegistry;

import java.util.Map;
import java.util.WeakHashMap;

public final class CentrifugeClientEffects {
    public static final float SoundStart = 0.03f;
    public static final float GritStart = 0.3f;
    public static final float GritRate = 0.35f;
    public static final float LidShare = 0.3f;

    private static final Map<CentrifugeBlockEntity, CentrifugeLoopSound> Whirs = new WeakHashMap<>();

    private CentrifugeClientEffects() {
    }

    public static void tick(CentrifugeBlockEntity Machine) {
        Minecraft Game = Minecraft.getInstance();
        Level World = Machine.getLevel();
        if (World == null || Game.level != World) {
            return;
        }
        float Spin = Machine.getSpin();
        if (Spin > SoundStart) {
            SoundManager Sounds = Game.getSoundManager();
            CentrifugeLoopSound Whir = Whirs.get(Machine);
            if (Whir == null || !Sounds.isActive(Whir)) {
                Whir = new CentrifugeLoopSound(Machine);
                Whirs.put(Machine, Whir);
                Sounds.play(Whir);
            }
        }
        grit(Machine, World, Spin);
    }

    private static void grit(CentrifugeBlockEntity Machine, Level World, float Spin) {
        if (Spin < GritStart) {
            return;
        }
        RandomSource Random = World.getRandom();
        float Strength = (Spin - GritStart) / (1.0f - GritStart);
        if (Random.nextFloat() > GritRate * Strength) {
            return;
        }
        BlockPos Pos = Machine.getBlockPos();
        Direction Side = Direction.Plane.HORIZONTAL.getRandomDirection(Random);
        double Along = 0.08 + Random.nextDouble() * 0.84;
        boolean Lid = Random.nextFloat() < LidShare;
        double Out = Lid ? 0.42 : 0.53;
        double X = Pos.getX() + 0.5 + Side.getStepX() * Out + (Side.getStepX() == 0 ? Along - 0.5 : 0.0);
        double Z = Pos.getZ() + 0.5 + Side.getStepZ() * Out + (Side.getStepZ() == 0 ? Along - 0.5 : 0.0);
        double Y = Pos.getY() + (Lid ? 1.02 : 0.03 + Random.nextDouble() * 0.12);
        double Kick = 0.012 + Random.nextDouble() * 0.02 * Strength;
        World.addParticle(CentrifugeRegistry.Grit.get(), X, Y, Z, Side.getStepX() * Kick, 0.03 + Random.nextDouble() * 0.04 * Strength, Side.getStepZ() * Kick);
    }
}
