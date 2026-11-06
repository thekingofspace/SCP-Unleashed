package net.rai.scp_unleashed.scp024.games;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Hazards;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.List;

public class SilverfishEscapeGame implements Scp024Game {
    public static final int Seconds = 40;
    public static final int FirstSwarm = 40;
    public static final int SwarmEvery = 16;
    public static final int MostAlive = 24;
    public static final int AmbushAt = 300;
    public static final int AmbushSize = 2;
    private boolean Ambushed;

    @Override
    public String id() {
        return "silverfish_escape";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.silverfishEscape(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
    }

    private void swarm(Scp024Session Session, int[] Mark, ServerPlayer User) {
        Silverfish Pest = new Silverfish(EntityType.SILVERFISH, Session.World);
        Vec3 At = Session.centre(Mark);
        Pest.moveTo(At.x + Session.Random.nextDouble() * 0.4 - 0.2, At.y, At.z + Session.Random.nextDouble() * 0.4 - 0.2, Session.Random.nextFloat() * 360.0f, 0.0f);
        Scp024Hazards.contestantFoe(Pest, 8.0, 1.0, 0.3);
        Session.spawn(Pest);
        Scp024Hazards.hunt(Pest, User);
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
        if (Session.ticks() >= FirstSwarm && Session.ticks() % SwarmEvery == 0 && Session.alive(Silverfish.class) < MostAlive) {
            List<int[]> Vents = Session.marks("vent");
            swarm(Session, Vents.get(Session.Random.nextInt(Vents.size())), User);
            if (Session.Random.nextBoolean()) {
                swarm(Session, Vents.get(Session.Random.nextInt(Vents.size())), User);
            }
        }
        if (!Ambushed && Session.ticks() >= AmbushAt) {
            Ambushed = true;
            for (int[] Hole : Session.marks("ambush")) {
                for (int Count = 0; Count < AmbushSize; Count++) {
                    swarm(Session, Hole, User);
                }
            }
            Session.cue(Scp024Cue.Ooh);
        }
        return Scp024Outcome.Continue;
    }
}
