package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.List;

public class IceBridgeGame implements Scp024Game {
    public static final int Seconds = 40;
    public static final int Grace = 120;
    public static final int CollapseEvery = 5;
    public static final int Warning = 14;
    private final TileDrops Drops = new TileDrops();
    private int Collapsed;

    @Override
    public String id() {
        return "ice_bridge";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.iceBridge(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public double killDepth() {
        return -3.0;
    }

    @Override
    public void start(Scp024Session Session) {
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (User == null) {
            return Scp024Outcome.Continue;
        }
        if (Session.onFinish(User)) {
            return Scp024Outcome.Win;
        }
        List<int[]> Path = Session.marks("path");
        if (Session.ticks() >= Grace && (Session.ticks() - Grace) % CollapseEvery == 0 && Collapsed < Path.size()) {
            int[] Cell = Path.get(Collapsed++);
            Drops.schedule(Session, Cell[0], Cell[1], Cell[2], Warning, Scp024Mat.WhiteConcrete);
        }
        Drops.tick(Session);
        if (!User.onGround() && User.getDeltaMovement().y < -0.4 && Session.height(User) > -1.0) {
            Session.taunt("near");
        }
        return Scp024Outcome.Continue;
    }
}
