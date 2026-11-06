package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class TileDrops {
    private record Drop(int X, int Y, int Z, int At) {
    }

    private final List<Drop> Pending = new ArrayList<>();
    private final Set<Long> Marked = new HashSet<>();

    private static long key(int X, int Y, int Z) {
        return BlockPos.asLong(X, Y, Z);
    }

    public boolean marked(int X, int Y, int Z) {
        return Marked.contains(key(X, Y, Z));
    }

    public boolean schedule(Scp024Session Session, int X, int Y, int Z, int Delay, Scp024Mat Warning) {
        if (Session.state(X, Y, Z).isAir() || !Marked.add(key(X, Y, Z))) {
            return false;
        }
        Session.set(X, Y, Z, Warning);
        Pending.add(new Drop(X, Y, Z, Session.ticks() + Delay));
        return true;
    }

    public void tick(Scp024Session Session) {
        if (Session.closed()) {
            return;
        }
        Iterator<Drop> Each = Pending.iterator();
        int Sounds = 0;
        while (Each.hasNext()) {
            Drop Due = Each.next();
            if (Due.At() > Session.ticks()) {
                continue;
            }
            Each.remove();
            BlockPos Cell = Session.at(Due.X(), Due.Y(), Due.Z());
            BlockState Was = Session.World.getBlockState(Cell);
            if (!Was.isAir()) {
                Session.World.levelEvent(2001, Cell, Block.getId(Was));
                Session.clearCell(Due.X(), Due.Y(), Due.Z());
                if (Sounds++ < 2) {
                    Session.sound(Vec3.atCenterOf(Cell), Scp024Registry.Crumble.get(), 0.7f, 0.8f + Session.Random.nextFloat() * 0.3f);
                }
            }
        }
    }
}
