package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Prizes;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.List;

public class DoorRouletteGame implements Scp024Game {
    public static final int Seconds = 75;
    public static final int Rounds = 3;
    public static final int DoorWidth = 3;
    public static final int DoorHeight = 3;
    public static final double ZonkRadius = 3.0;
    public static final float ZonkDamage = 7.0f;
    public static final int ResetDelay = 40;
    private final boolean[] Sealed = new boolean[3];
    private int Round;
    private int Winner;
    private int Reset = -1;

    @Override
    public String id() {
        return "door_roulette";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.doorRoulette(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        Winner = Session.Random.nextInt(3);
    }

    private void fill(Scp024Session Session, int Door, Scp024Mat Mat) {
        int[] Corner = Session.marks("door").get(Door);
        for (int X = 0; X < DoorWidth; X++) {
            for (int Y = 0; Y < DoorHeight; Y++) {
                if (Mat == null) {
                    Session.restore(Corner[0] + X, Corner[1] + Y, Corner[2]);
                } else {
                    Session.set(Corner[0] + X, Corner[1] + Y, Corner[2], Mat);
                }
            }
        }
    }

    @Override
    public boolean use(Scp024Session Session, BlockPos Local) {
        if (Reset >= 0) {
            return true;
        }
        List<int[]> Doors = Session.marks("door");
        for (int Door = 0; Door < Doors.size(); Door++) {
            int[] Corner = Doors.get(Door);
            boolean Inside = Local.getX() >= Corner[0] && Local.getX() < Corner[0] + DoorWidth && Local.getY() >= Corner[1] && Local.getY() < Corner[1] + DoorHeight
                    && Local.getZ() == Corner[2];
            if (!Inside || Sealed[Door]) {
                continue;
            }
            pick(Session, Door);
            return true;
        }
        return false;
    }

    private void pick(Scp024Session Session, int Door) {
        int[] Corner = Session.marks("door").get(Door);
        Vec3 Front = Session.centre(Corner[0] + 1, 1, Corner[2] + 1);
        if (Door == Winner) {
            fill(Session, Door, Scp024Mat.Air);
            Round++;
            Session.cheer("correct");
            Scp024Prizes.confetti(Session.World, Front.x, Front.y + 2.0, Front.z, 30);
            Reset = Session.ticks() + ResetDelay;
            return;
        }
        Sealed[Door] = true;
        fill(Session, Door, Scp024Mat.Air);
        Session.blast(Front.add(0.0, 0.0, -1.0), ZonkRadius, ZonkDamage);
        fill(Session, Door, Scp024Mat.IronBars);
        Session.cue(Scp024Cue.Buzzer);
        Session.taunt("zonk");
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        if (Reset >= 0 && Session.ticks() >= Reset) {
            Reset = -1;
            if (Round >= Rounds) {
                return Scp024Outcome.Win;
            }
            for (int Door = 0; Door < 3; Door++) {
                Sealed[Door] = false;
                fill(Session, Door, null);
            }
            Winner = Session.Random.nextInt(3);
            Session.cue(Scp024Cue.Drumroll);
            ServerPlayer User = Session.player();
            if (User != null && Session.standing(User).getZ() <= 3) {
                Session.toStart();
            }
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.doors", Math.min(Rounds, Round + 1), Rounds));
        return Scp024Outcome.Continue;
    }
}
