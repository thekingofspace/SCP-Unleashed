package net.rai.scp_unleashed.motion;

public final class StrideGait {
    public static final int Idle = -1;
    public static final int Walk = 0;
    public static final int Run = 1;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final int SwitchTicks = 6;
    public static final double Response = 0.5;
    public static final double SwitchWindow = 0.12;
    public static final double PhaseWrap = 64.0;

    public record Shape(double StillPace, double RunEnter, double RunExit, double WalkDistance, double RunDistance, double MaxStep, int Blend) {
        public double distance(int Stride) {
            return Stride == Run ? RunDistance : WalkDistance;
        }
    }

    private final PaceMeter Meter = new PaceMeter();
    private final Settle<Integer> Pending = new Settle<>();
    private int Stride = Idle;
    private double Pace;
    private double Phase;
    private double PhaseO;
    private double From;
    private double To;
    private int Hold;

    public boolean tick(double Moved, boolean Alive, Shape Form) {
        double Mean = Meter.push(Moved);
        Pace += (Mean - Pace) * Response;
        PhaseO = Phase;
        From = Phase;
        To = Phase;
        int Wanted = wanted(Mean, Alive, Form);
        int Need = Stride == Idle ? StartTicks : Wanted == Idle ? StopTicks : SwitchTicks;
        if (Pending.ready(Stride, Wanted, Need) && (Stride == Idle || Wanted == Idle || nearCycleStart())) {
            Stride = Wanted;
            Pending.clear();
            if (Stride != Idle) {
                Phase = 0.0;
                PhaseO = 0.0;
                From = 0.0;
                To = 0.0;
                Hold = Form.Blend();
            }
        }
        if (Stride == Idle) {
            return false;
        }
        if (Hold > 0) {
            Hold--;
            return false;
        }
        Phase += Math.min(Pace / Form.distance(Stride), Form.MaxStep());
        To = Phase;
        if (Phase > PhaseWrap) {
            Phase -= PhaseWrap;
            PhaseO -= PhaseWrap;
        }
        return true;
    }

    private int wanted(double Mean, boolean Alive, Shape Form) {
        if (!Alive) {
            return Idle;
        }
        if (Stride == Idle) {
            if (Mean <= Form.StillPace()) {
                return Idle;
            }
            return Math.max(Mean, Pace) > Form.RunEnter() ? Run : Walk;
        }
        if (Mean < Form.StillPace()) {
            return Idle;
        }
        if (Stride == Walk) {
            return Pace > Form.RunEnter() ? Run : Walk;
        }
        return Pace < Form.RunExit() ? Walk : Run;
    }

    public boolean nearCycleStart() {
        double Share = Phase - Math.floor(Phase);
        return Share < SwitchWindow || Share > 1.0 - SwitchWindow;
    }

    public int stride() {
        return Stride;
    }

    public double pace() {
        return Pace;
    }

    public double from() {
        return From;
    }

    public double to() {
        return To;
    }

    public double cycle(float Partial) {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Value - Math.floor(Value);
    }
}
