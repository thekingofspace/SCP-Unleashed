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

import java.util.ArrayDeque;
import java.util.Deque;

public class MinefieldGame implements Scp024Game {
    public static final int Seconds = 90;
    public static final float Density = 0.17f;
    public static final double Radius = 2.5;
    public static final float Damage = 9.0f;
    public static final Scp024Mat[] Counts = {Scp024Mat.WhiteConcrete, Scp024Mat.LimeConcrete, Scp024Mat.YellowConcrete, Scp024Mat.OrangeConcrete, Scp024Mat.RedConcrete};
    private int MinX;
    private int MinZ;
    private int Width;
    private int Depth;
    private boolean[][] Mines;
    private boolean[][] Shown;
    private int LastX = Integer.MIN_VALUE;
    private int LastZ = Integer.MIN_VALUE;
    private int Blasts;

    @Override
    public String id() {
        return "minefield";
    }

    @Override
    public Scp024Plan plan(long Seed) {
        return Scp024Rooms.minefield(Seed);
    }

    @Override
    public int seconds() {
        return Seconds;
    }

    @Override
    public void start(Scp024Session Session) {
        int[] Low = Session.marks("field").get(0);
        int[] High = Session.marks("field").get(1);
        MinX = Low[0];
        MinZ = Low[2];
        Width = High[0] - Low[0] + 1;
        Depth = High[2] - Low[2] + 1;
        Mines = new boolean[Width][Depth];
        Shown = new boolean[Width][Depth];
        boolean[][] Path = new boolean[Width][Depth];
        int X = Session.Random.nextInt(Width);
        for (int Z = 0; Z < Depth; Z++) {
            Path[X][Z] = true;
            int Drift = Session.Random.nextInt(3) - 1;
            int Next = Math.max(0, Math.min(Width - 1, X + Drift));
            Path[Next][Z] = true;
            X = Next;
        }
        for (int Column = 0; Column < Width; Column++) {
            for (int Row = 0; Row < Depth; Row++) {
                Mines[Column][Row] = !Path[Column][Row] && Row > 0 && Session.Random.nextFloat() < Density;
            }
        }
    }

    private int around(int Column, int Row) {
        int Count = 0;
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                int X = Column + Dx;
                int Z = Row + Dz;
                if ((Dx != 0 || Dz != 0) && X >= 0 && Z >= 0 && X < Width && Z < Depth && Mines[X][Z]) {
                    Count++;
                }
            }
        }
        return Count;
    }

    private void reveal(Scp024Session Session, int Column, int Row) {
        Deque<int[]> Queue = new ArrayDeque<>();
        Queue.add(new int[]{Column, Row});
        while (!Queue.isEmpty()) {
            int[] Cell = Queue.poll();
            int X = Cell[0];
            int Z = Cell[1];
            if (X < 0 || Z < 0 || X >= Width || Z >= Depth || Shown[X][Z] || Mines[X][Z]) {
                continue;
            }
            Shown[X][Z] = true;
            int Count = around(X, Z);
            Session.set(MinX + X, 0, MinZ + Z, Counts[Math.min(Counts.length - 1, Count)]);
            Session.clearCell(MinX + X, 1, MinZ + Z);
            if (Count == 0) {
                for (int Dx = -1; Dx <= 1; Dx++) {
                    for (int Dz = -1; Dz <= 1; Dz++) {
                        Queue.add(new int[]{X + Dx, Z + Dz});
                    }
                }
            }
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
        if (!User.onGround()) {
            return Scp024Outcome.Continue;
        }
        BlockPos Cell = Session.standing(User);
        int Column = Cell.getX() - MinX;
        int Row = Cell.getZ() - MinZ;
        if (Cell.getY() != 0 || Column < 0 || Row < 0 || Column >= Width || Row >= Depth || Column == LastX && Row == LastZ) {
            return Scp024Outcome.Continue;
        }
        LastX = Column;
        LastZ = Row;
        if (Mines[Column][Row]) {
            Mines[Column][Row] = false;
            Shown[Column][Row] = true;
            Session.set(Cell.getX(), 0, Cell.getZ(), Scp024Mat.BlackConcrete);
            Session.clearCell(Cell.getX(), 1, Cell.getZ());
            Vec3 Spot = Session.centre(Cell.getX(), 1, Cell.getZ());
            Session.blast(Spot, Radius, Damage);
            Blasts++;
            Session.taunt("mine");
            Session.cue(Scp024Cue.Laugh);
        } else if (!Shown[Column][Row]) {
            reveal(Session, Column, Row);
            Session.sound(User.position(), Scp024Registry.Click.get(), 0.6f, 1.4f);
        }
        Session.status(Component.translatable("status.scp_unleashed.scp024.mines", Blasts));
        return Scp024Outcome.Continue;
    }
}
