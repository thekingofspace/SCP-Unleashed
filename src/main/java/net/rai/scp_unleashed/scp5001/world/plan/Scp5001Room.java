package net.rai.scp_unleashed.scp5001.world.plan;

import java.util.ArrayList;
import java.util.List;

public final class Scp5001Room {
    public final int Id;
    public final String Kind;
    public String Type;
    public final int Level;
    public final int Levels;
    public int X0;
    public int Z0;
    public int X1;
    public int Z1;
    public int Cells;
    public final List<Scp5001Door> Doors = new ArrayList<>();
    public boolean Dark;
    public boolean Terminal;
    public int Inward;

    public Scp5001Room(int Id, String Kind, String Type, int Level, int Levels, int X0, int Z0, int X1, int Z1) {
        this.Id = Id;
        this.Kind = Kind;
        this.Type = Type;
        this.Level = Level;
        this.Levels = Levels;
        this.X0 = Math.min(X0, X1);
        this.Z0 = Math.min(Z0, Z1);
        this.X1 = Math.max(X0, X1);
        this.Z1 = Math.max(Z0, Z1);
    }

    public void setBounds(int MinX, int MinZ, int MaxX, int MaxZ) {
        X0 = MinX;
        Z0 = MinZ;
        X1 = MaxX;
        Z1 = MaxZ;
    }

    public boolean rectangular() {
        return Kind.equals(Scp5001Plan.KindStair);
    }

    public boolean corridor() {
        return Kind.equals(Scp5001Plan.KindCorridor) || Kind.equals(Scp5001Plan.KindGallery);
    }

    public int width() {
        return X1 - X0 + 1;
    }

    public int depth() {
        return Z1 - Z0 + 1;
    }

    public boolean contains(int X, int Z) {
        return X >= X0 && X <= X1 && Z >= Z0 && Z <= Z1;
    }
}
