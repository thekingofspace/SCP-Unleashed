package net.rai.scp_unleashed.scp024.games;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.Scp024Cue;
import net.rai.scp_unleashed.scp024.Scp024Game;
import net.rai.scp_unleashed.scp024.Scp024Outcome;
import net.rai.scp_unleashed.scp024.Scp024Registry;
import net.rai.scp_unleashed.scp024.Scp024Session;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import net.rai.scp_unleashed.scp024.plan.Scp024Rooms;

import java.util.ArrayList;
import java.util.List;

public class MemoryTilesGame implements Scp024Game {
    public static final int Seconds = 120;
    public static final int[] Lengths = {3, 4, 5};
    public static final int LitTicks = 14;
    public static final int GapTicks = 6;
    public static final int RoundPause = 30;
    public static final int Strikes = 1;
    public static final int TileSize = 3;
    private final List<Integer> Sequence = new ArrayList<>();
    private int Round;
    private int Step;
    private int Mistakes;
    private boolean Showing;
    private int PhaseStart;
    private int Lit = -1;
    private int LitUntil;
    private int Last = -1;

    @Override
    public String id() {
        return "memory_tiles";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.memoryTiles(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        Round = 0;
        newRound(Session);
    }

    private void newRound(Scp024Session Session) {
        Sequence.clear();
        for (int Index = 0; Index < Lengths[Round]; Index++) {
            int Pick = Session.Random.nextInt(9);
            while (!Sequence.isEmpty() && Sequence.get(Sequence.size() - 1) == Pick) {
                Pick = Session.Random.nextInt(9);
            }
            Sequence.add(Pick);
        }
        replay(Session);
    }

    private void replay(Scp024Session Session) {
        Showing = true;
        PhaseStart = Session.ticks() + RoundPause;
        Step = 0;
    }

    private void paint(Scp024Session Session, int Tile, Scp024Mat Mat) {
        int[] Corner = Session.marks("tile").get(Tile);
        for (int X = 0; X < TileSize; X++) {
            for (int Z = 0; Z < TileSize; Z++) {
                if (Mat == null) {
                    Session.restore(Corner[0] + X, 0, Corner[2] + Z);
                } else {
                    Session.set(Corner[0] + X, 0, Corner[2] + Z, Mat);
                }
            }
        }
    }

    private void flash(Scp024Session Session, int Tile, int Ticks, boolean Wrong) {
        if (Lit >= 0) {
            paint(Session, Lit, null);
        }
        Lit = Tile;
        LitUntil = Session.ticks() + Ticks;
        paint(Session, Tile, Wrong ? Scp024Mat.RedConcrete : Scp024Mat.SeaLantern);
        int[] Corner = Session.marks("tile").get(Tile);
        Vec3 Middle = Session.centre(Corner[0] + 1, 1, Corner[2] + 1);
        Session.sound(Middle, Wrong ? Scp024Registry.NoteBuzzer.get() : Scp024Registry.NoteBell.get(), 1.0f, Wrong ? 0.5f : 0.6f + Tile * 0.15f);
    }

    private int tileUnder(Scp024Session Session, ServerPlayer User) {
        if (!User.onGround()) {
            return Last;
        }
        BlockPos Cell = Session.standing(User);
        if (Cell.getY() != 0) {
            return -1;
        }
        List<int[]> Tiles = Session.marks("tile");
        for (int Index = 0; Index < Tiles.size(); Index++) {
            int[] Corner = Tiles.get(Index);
            if (Cell.getX() >= Corner[0] && Cell.getX() < Corner[0] + TileSize && Cell.getZ() >= Corner[2] && Cell.getZ() < Corner[2] + TileSize) {
                return Index;
            }
        }
        return -1;
    }

    @Override
    public Scp024Outcome tick(Scp024Session Session) {
        ServerPlayer User = Session.player();
        if (Lit >= 0 && Session.ticks() >= LitUntil) {
            paint(Session, Lit, null);
            Lit = -1;
        }
        if (User == null) {
            return Scp024Outcome.Continue;
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.memory", Round + 1, Lengths.length, Strikes - Mistakes));
        if (Showing) {
            int Elapsed = Session.ticks() - PhaseStart;
            if (Elapsed >= 0 && Elapsed % (LitTicks + GapTicks) == 0) {
                int Index = Elapsed / (LitTicks + GapTicks);
                if (Index < Sequence.size()) {
                    flash(Session, Sequence.get(Index), LitTicks, false);
                } else {
                    Showing = false;
                    Step = 0;
                    Last = tileUnder(Session, User);
                    Session.objective(Component.translatable("objective.scp_unleashed.scp024.memory"));
                    Session.cue(Scp024Cue.Tick);
                }
            }
            return Scp024Outcome.Continue;
        }
        int Under = tileUnder(Session, User);
        if (Under != Last) {
            Last = Under;
            if (Under >= 0) {
                if (Under == Sequence.get(Step)) {
                    flash(Session, Under, 8, false);
                    Step++;
                    if (Step >= Sequence.size()) {
                        Round++;
                        if (Round >= Lengths.length) {
                            return Scp024Outcome.Win;
                        }
                        Session.cheer("correct");
                        newRound(Session);
                    }
                } else {
                    flash(Session, Under, 12, true);
                    Mistakes++;
                    Session.cue(Scp024Cue.Buzzer);
                    if (Mistakes > Strikes) {
                        return Scp024Outcome.Lose;
                    }
                    Session.taunt("wrong");
                    replay(Session);
                }
            }
        }
        return Scp024Outcome.Continue;
    }
}
