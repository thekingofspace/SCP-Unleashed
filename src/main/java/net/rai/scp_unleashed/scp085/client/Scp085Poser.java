package net.rai.scp_unleashed.scp085.client;

import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp085.Scp085Act;
import net.rai.scp_unleashed.scp085.Scp085Form;
import net.rai.scp_unleashed.scp085.Scp085Pose;
import net.rai.scp_unleashed.scp085.Scp085Scene;
import net.rai.scp_unleashed.scp085.Scp085SheetBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class Scp085Poser {
    public static final float FigureDepth = 0.05f;
    public static final float PropDepth = 0.09f;

    private Scp085Poser() {
    }

    public static List<Placed> pose(Scp085SheetBlockEntity Sheet, long GameTime, float Partial) {
        List<Placed> Parts = new ArrayList<>();
        if (!Sheet.isHere()) {
            return Parts;
        }
        Scp085Scene Scene = Sheet.scene();
        float Time = Math.max(0.0f, GameTime - Sheet.actStart() + Partial);
        int Ticks = (int) Time;
        float Progress = Mth.clamp(Time / Sheet.actLength(), 0.0f, 1.0f);
        boolean Mirror = Sheet.facing() < 0;
        int Facing = Sheet.facing();
        int X = Sheet.toX();
        int Y = Scene.ground(X);
        switch (Sheet.act()) {
            case Walk -> {
                X = Math.round(Mth.lerp(Progress, Sheet.fromX(), Sheet.toX()));
                Y = Scene.ground(X);
                Parts.add(new Placed(Scp085Clip.Walk, Math.abs(X - Sheet.fromX()) / 2, X, Y, Mirror, FigureDepth));
            }
            case Spot -> spot(Parts, Scene, Sheet.param(), Ticks, X, Y, Mirror);
            case Watch -> Parts.add(new Placed(Scp085Clip.Front, Ticks % 70 > 63 ? 1 : 0, X, Y, false, FigureDepth));
            case Wave -> {
                Parts.add(new Placed(Scp085Clip.Wave, Ticks / 4, X, Y, Mirror, FigureDepth));
                if (Sheet.param() == 1 && Ticks >= 6) {
                    int Rise = Math.min(6, (Ticks - 6) / 5);
                    Parts.add(new Placed(Scp085Clip.Heart, 0, X + 4 * Facing, Y - 25 - Rise, false, PropDepth));
                }
            }
            case Note -> {
                if (Ticks < Scp085Act.NoteWriteTicks) {
                    Parts.add(new Placed(Scp085Clip.Write, Ticks / 5, X, Y, Mirror, FigureDepth));
                    Parts.add(new Placed(Scp085Clip.Note, Math.min(3, Ticks / 10), X + 9 * Facing, Y - 10, false, PropDepth));
                } else {
                    int Bob = (Ticks / 12) % 2;
                    Parts.add(new Placed(Scp085Clip.Hold, Bob, X, Y, false, FigureDepth));
                    Parts.add(new Placed(Scp085Clip.Note, 3, X, held(Y - 23 - Bob, 21), false, PropDepth));
                }
            }
            case Sign -> {
                int Bob = Ticks < 8 ? 0 : (Ticks / 12) % 2;
                Parts.add(new Placed(Scp085Clip.Hold, Bob, X, Y, false, FigureDepth));
                Parts.add(new Placed(Scp085Clip.Sign, Sheet.param(), X, held(Y - 24 - Bob, 19), false, PropDepth));
            }
            case LeapOut, LeapIn -> {
                int Frame = Math.min(6, Ticks / 2);
                if (Sheet.act() == Scp085Act.LeapIn) {
                    Frame = 6 - Frame;
                }
                X = Math.round(Mth.lerp(Progress, Sheet.fromX(), Sheet.toX()));
                Y = Scene.ground(Mth.clamp(X, Scene.walkMin(), Scene.walkMax()));
                Parts.add(new Placed(Scp085Clip.Leap, Frame, X, Y, Mirror, FigureDepth));
            }
            case ShiftOut, ShiftBack -> Parts.add(new Placed(Scp085Clip.Scribble, Ticks / 3, X, Y, Mirror, FigureDepth));
            case AnimalIdle -> animalIdle(Parts, Sheet.form(), Ticks, X, Y, Mirror);
            case AnimalMove -> animalMove(Parts, Sheet, Scene, Progress, Ticks, Mirror);
            default -> Parts.add(new Placed(Scp085Clip.Stand, (Ticks / 30) % 2, X, Y, Mirror, FigureDepth));
        }
        return Parts;
    }

    private static int held(int AnchorY, int BoardRow) {
        int Highest = Scp085Scene.SheetTop + 1;
        int BoardTop = AnchorY - 31 + BoardRow;
        return BoardTop < Highest ? AnchorY + (Highest - BoardTop) : AnchorY;
    }

    private static void spot(List<Placed> Parts, Scp085Scene Scene, int Index, int Ticks, int X, int Y, boolean Mirror) {
        if (Index < 0 || Index >= Scene.spots().size()) {
            Parts.add(new Placed(Scp085Clip.Stand, 0, X, Y, Mirror, FigureDepth));
            return;
        }
        Scp085Scene.Spot Where = Scene.spots().get(Index);
        boolean Flip = Where.Facing() < 0;
        int Sx = Where.X();
        int Sy = Where.Y();
        Scp085Pose Kind = Where.Pose();
        switch (Kind) {
            case SitSide -> Parts.add(new Placed(Scp085Clip.SitSide, (Ticks / 60) % 2, Sx, Sy, Flip, FigureDepth));
            case SitFront -> Parts.add(new Placed(Scp085Clip.SitFront, (Ticks / 40) % 2, Sx, Sy, Flip, FigureDepth));
            case SitGround -> Parts.add(new Placed(Scp085Clip.SitGround, (Ticks / 70) % 2, Sx, Sy, Flip, FigureDepth));
            case Lie -> {
                Parts.add(new Placed(Scp085Clip.Lie, (Ticks / 40) % 2, Sx, Sy, Flip, FigureDepth));
                if (Scene == Scp085Scene.Home && Ticks > 40) {
                    Parts.add(new Placed(Scp085Clip.Sleep, (Ticks / 24) % 2, Sx - 8, Sy - 3, false, PropDepth));
                }
            }
            case Crouch -> {
                int Frame = Ticks < 16 ? 0 : Ticks < 32 ? 1 : 2 + (Ticks / 20) % 2;
                Parts.add(new Placed(Scp085Clip.Crouch, Frame, Sx, Sy, Flip, FigureDepth));
            }
            case LookOut -> Parts.add(new Placed(Scp085Clip.LookOut, (Ticks / 40) % 2, Sx, Sy, false, FigureDepth));
            case Twirl -> Parts.add(new Placed(Scp085Clip.Twirl, Ticks / 3, Sx, Sy, false, FigureDepth));
            case Write -> Parts.add(new Placed(Scp085Clip.Write, (Ticks / 6) % 4, Sx, Sy, Flip, FigureDepth));
        }
    }

    private static void animalIdle(List<Placed> Parts, Scp085Form Form, int Ticks, int X, int Y, boolean Mirror) {
        switch (Form) {
            case Cat -> Parts.add(new Placed(Scp085Clip.CatSit, (Ticks / 20) % 2, X, Y, Mirror, FigureDepth));
            case Bird -> Parts.add(new Placed(Ticks % 50 < 30 ? Scp085Clip.BirdPeck : Scp085Clip.BirdHop, Ticks % 50 < 30 ? (Ticks / 6) % 2 : 0, X, Y, Mirror, FigureDepth));
            case Rabbit -> Parts.add(new Placed(Scp085Clip.RabbitSit, (Ticks / 25) % 2, X, Y, Mirror, FigureDepth));
            default -> Parts.add(new Placed(Scp085Clip.Stand, 0, X, Y, Mirror, FigureDepth));
        }
    }

    private static void animalMove(List<Placed> Parts, Scp085SheetBlockEntity Sheet, Scp085Scene Scene, float Progress, int Ticks, boolean Mirror) {
        int X = Math.round(Mth.lerp(Progress, Sheet.fromX(), Sheet.toX()));
        int Y = Scene.ground(X);
        int Travelled = Math.abs(X - Sheet.fromX());
        switch (Sheet.form()) {
            case Cat -> Parts.add(new Placed(Scp085Clip.CatWalk, Travelled / 2, X, Y, Mirror, FigureDepth));
            case Bird -> {
                if (Math.abs(Sheet.toX() - Sheet.fromX()) >= 14) {
                    int Lift = Math.round(Mth.sin(Progress * Mth.PI) * 9.0f);
                    Parts.add(new Placed(Scp085Clip.BirdFly, Ticks / 2, X, Y - Lift, Mirror, FigureDepth));
                } else {
                    Parts.add(new Placed(Scp085Clip.BirdHop, (Travelled / 3) % 2, X, Y, Mirror, FigureDepth));
                }
            }
            case Rabbit -> {
                float Phase = (Travelled % 8) / 8.0f;
                int Lift = Math.round(Mth.sin(Phase * Mth.PI) * 3.0f);
                Parts.add(new Placed(Scp085Clip.RabbitHop, (int) (Phase * 4.0f), X, Y - Lift, Mirror, FigureDepth));
            }
            default -> Parts.add(new Placed(Scp085Clip.Walk, Travelled / 2, X, Y, Mirror, FigureDepth));
        }
    }

    public record Placed(Scp085Clip Clip, int Frame, int AnchorX, int AnchorY, boolean Mirror, float Depth) {
        public int left() {
            return AnchorX - Scp085Clip.Cell / 2;
        }

        public int top() {
            return AnchorY - Clip.anchor();
        }
    }
}
