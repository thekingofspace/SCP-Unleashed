package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Palette;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArrowCorridorGame implements Scp024Game {
    public static final int Seconds = 60;
    public static final int Period = 40;
    public static final int Stagger = 9;
    public static final float Speed = 1.8f;
    public static final double Damage = 2.0;
    private final ArrowStorm Arrows = new ArrowStorm();
    private final Map<Integer, List<int[]>> Rows = new LinkedHashMap<>();

    @Override
    public String id() {
        return "arrow_corridor";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.arrowCorridor(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        for (int[] Trap : Session.marks("trap")) {
            Rows.computeIfAbsent(Trap[2], Ignored -> new ArrayList<>()).add(Trap);
        }
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
        int Row = 0;
        for (Map.Entry<Integer, List<int[]>> Entry : Rows.entrySet()) {
            int Phase = (Row * Stagger) % Period;
            if ((Session.ticks() + ArrowStorm.Warn) % Period == Phase) {
                for (int[] Trap : Entry.getValue()) {
                    int StepX = Scp024Palette.side(Trap[3]).getStepX();
                    Vec3 Face = Session.centre(Trap).add(StepX * 0.6, 0.45, 0.0);
                    Vec3 Across = Face.add(StepX * 8.0, 0.0, 0.0);
                    Arrows.queue(Session, Face, Across, ArrowStorm.Warn, Speed, Damage);
                }
            }
            Row++;
        }
        Arrows.tick(Session);
        return Scp024Outcome.Continue;
    }
}
