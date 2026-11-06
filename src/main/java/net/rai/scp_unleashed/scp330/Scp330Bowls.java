package net.rai.scp_unleashed.scp330;

import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Scp330Bowls {
    private static final Set<Scp330BowlBlockEntity> Loaded = new LinkedHashSet<>();

    private Scp330Bowls() {
    }

    public static synchronized void add(Scp330BowlBlockEntity Bowl) {
        Level World = Bowl.getLevel();
        if (World != null && !World.isClientSide) {
            Loaded.add(Bowl);
        }
    }

    public static synchronized void remove(Scp330BowlBlockEntity Bowl) {
        Loaded.remove(Bowl);
    }

    public static synchronized List<Scp330BowlBlockEntity> all() {
        List<Scp330BowlBlockEntity> Found = new ArrayList<>();
        for (Scp330BowlBlockEntity Bowl : Loaded) {
            if (!Bowl.isRemoved()) {
                Found.add(Bowl);
            }
        }
        return Found;
    }

    public static synchronized void clear() {
        Loaded.clear();
    }
}
