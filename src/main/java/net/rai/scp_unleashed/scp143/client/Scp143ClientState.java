package net.rai.scp_unleashed.scp143.client;

import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp143.Scp143Trees;

import java.util.HashMap;
import java.util.Map;

public final class Scp143ClientState {
    public static final int MaxTracked = 512;
    private static final Map<Integer, Scp143PetalParticle> Live = new HashMap<>();
    private static SpriteSet PetalSprites;
    private static BlockPos Site;

    private Scp143ClientState() {
    }

    public static void sprites(SpriteSet Sprites) {
        PetalSprites = Sprites;
    }

    public static SpriteSet sprites() {
        return PetalSprites;
    }

    public static void track(int Id, Scp143PetalParticle Petal) {
        if (Live.size() > MaxTracked) {
            Live.values().removeIf(Found -> !Found.isAlive());
        }
        Scp143PetalParticle Old = Live.put(Id, Petal);
        if (Old != null && Old != Petal) {
            Old.remove();
        }
    }

    public static boolean has(int Id) {
        Scp143PetalParticle Found = Live.get(Id);
        return Found != null && Found.isAlive();
    }

    public static void forget(int Id, Scp143PetalParticle Petal) {
        Live.remove(Id, Petal);
    }

    public static Scp143PetalParticle find(int Id) {
        return Live.get(Id);
    }

    public static void site(boolean Known, BlockPos Base) {
        Site = Known ? Base : null;
    }

    public static boolean isOriginal(Level World, BlockPos Pos) {
        return Site != null && World.dimension() == Level.OVERWORLD && Scp143Trees.withinOriginal(Site, Pos);
    }

    public static void clear() {
        Live.clear();
        Site = null;
    }
}
