package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class HotTiles {
    public static final int WarmTicks = 22;
    public static final int HotTicks = 120;
    public static final int BurnEvery = 10;
    public static final float BurnDamage = 2.0f;
    public static final float SpreadChance = 0.18f;

    private static final class Tile {
        int Stage;
        int Until;
    }

    private final Map<Long, Tile> Tiles = new HashMap<>();
    private final int MinX;
    private final int MinZ;
    private final int MaxX;
    private final int MaxZ;
    private final double MaxShare;

    public HotTiles(int MinX, int MinZ, int MaxX, int MaxZ, double MaxShare) {
        this.MinX = MinX;
        this.MinZ = MinZ;
        this.MaxX = MaxX;
        this.MaxZ = MaxZ;
        this.MaxShare = MaxShare;
    }

    private boolean inside(int X, int Z) {
        return X >= MinX && X <= MaxX && Z >= MinZ && Z <= MaxZ;
    }

    public boolean heat(Scp024Session Session, int X, int Z) {
        if (!inside(X, Z) || Tiles.containsKey(BlockPos.asLong(X, 0, Z))) {
            return false;
        }
        int Area = (MaxX - MinX + 1) * (MaxZ - MinZ + 1);
        if (Tiles.size() >= Area * MaxShare) {
            return false;
        }
        Tile Fresh = new Tile();
        Fresh.Stage = 0;
        Fresh.Until = Session.ticks() + WarmTicks;
        Tiles.put(BlockPos.asLong(X, 0, Z), Fresh);
        Session.set(X, 0, Z, Scp024Mat.OrangeTerracotta);
        return true;
    }

    public void heatNear(Scp024Session Session, int X, int Z, int Reach) {
        heat(Session, X + Session.Random.nextInt(Reach * 2 + 1) - Reach, Z + Session.Random.nextInt(Reach * 2 + 1) - Reach);
    }

    public void heatAnywhere(Scp024Session Session) {
        heat(Session, MinX + Session.Random.nextInt(MaxX - MinX + 1), MinZ + Session.Random.nextInt(MaxZ - MinZ + 1));
    }

    public void tick(Scp024Session Session) {
        if (Session.closed()) {
            return;
        }
        Map<Long, Tile> Spread = new HashMap<>();
        Iterator<Map.Entry<Long, Tile>> Each = Tiles.entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<Long, Tile> Entry = Each.next();
            Tile Cell = Entry.getValue();
            int X = BlockPos.getX(Entry.getKey());
            int Z = BlockPos.getZ(Entry.getKey());
            if (Cell.Stage == 1 && Session.ticks() % 6 == 0) {
                Vec3 Spot = Session.centre(X, 1, Z);
                Session.World.sendParticles(ParticleTypes.LAVA, Spot.x, Spot.y, Spot.z, 1, 0.2, 0.0, 0.2, 0.0);
            }
            if (Cell.Stage == 1 && Session.ticks() % BurnEvery == 0 && Session.Random.nextFloat() < SpreadChance) {
                int[][] Steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                int[] Step = Steps[Session.Random.nextInt(4)];
                Spread.put(BlockPos.asLong(X + Step[0], 0, Z + Step[1]), null);
            }
            if (Session.ticks() < Cell.Until) {
                continue;
            }
            if (Cell.Stage == 0) {
                Cell.Stage = 1;
                Cell.Until = Session.ticks() + HotTicks;
                Session.set(X, 0, Z, Scp024Mat.Magma);
            } else {
                Session.restore(X, 0, Z);
                Each.remove();
            }
        }
        for (Long Key : Spread.keySet()) {
            heat(Session, BlockPos.getX(Key), BlockPos.getZ(Key));
        }
        ServerPlayer User = Session.player();
        if (User != null && Session.ticks() % BurnEvery == 0 && User.onGround()) {
            BlockPos Cell = Session.standing(User);
            Tile Under = Tiles.get(BlockPos.asLong(Cell.getX(), 0, Cell.getZ()));
            if (Cell.getY() == 0 && Under != null && Under.Stage == 1) {
                User.hurt(Session.World.damageSources().hotFloor(), BurnDamage);
            }
        }
    }

    public int hot() {
        int Count = 0;
        for (Tile Cell : Tiles.values()) {
            if (Cell.Stage == 1) {
                Count++;
            }
        }
        return Count;
    }
}
