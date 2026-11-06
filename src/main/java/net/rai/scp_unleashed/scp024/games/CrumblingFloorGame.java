package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

public class CrumblingFloorGame implements Scp024Game {
    public static final int Seconds = 45;
    public static final int Size = 15;
    public static final int StepDelay = 12;
    public static final int RandomStart = 300;
    public static final int RandomEvery = 10;
    public static final int RandomDelay = 24;
    private final TileDrops Drops = new TileDrops();

    @Override
    public String id() {
        return "crumbling_floor";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.crumblingFloor(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public boolean survival() {
        return true;
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
        if (User != null && User.onGround()) {
            double Half = User.getBbWidth() * 0.5 - 0.05;
            for (double Dx : new double[]{-Half, Half}) {
                for (double Dz : new double[]{-Half, Half}) {
                    BlockPos Cell = Session.local(BlockPos.containing(User.getX() + Dx, User.getY() - 0.2, User.getZ() + Dz));
                    if (Cell.getY() == 0 && Cell.getX() >= 0 && Cell.getX() < Size && Cell.getZ() >= 0 && Cell.getZ() < Size) {
                        Drops.schedule(Session, Cell.getX(), 0, Cell.getZ(), StepDelay, Scp024Mat.RedConcrete);
                    }
                }
            }
        }
        if (Session.ticks() >= RandomStart && Session.ticks() % RandomEvery == 0) {
            for (int Try = 0; Try < 8; Try++) {
                if (Drops.schedule(Session, Session.Random.nextInt(Size), 0, Session.Random.nextInt(Size), RandomDelay, Scp024Mat.OrangeConcrete)) {
                    break;
                }
            }
        }
        Drops.tick(Session);
        if (User != null && !User.onGround() && User.getDeltaMovement().y < -0.4 && Session.height(User) > -1.0) {
            Session.taunt("near");
        }
        return Scp024Outcome.Continue;
    }
}
