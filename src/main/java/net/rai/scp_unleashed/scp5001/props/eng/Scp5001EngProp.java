package net.rai.scp_unleashed.scp5001.props.eng;

public final class Scp5001EngProp {
    public enum Kind { Floor, FloorTall, Wall, Ceiling }

    private final String Id;
    private final Kind Form;
    private final int Light;
    private final double[][] Lower;
    private final double[][] Upper;

    public Scp5001EngProp(String Id, Kind Form, int Light, double[][] Lower, double[][] Upper) {
        this.Id = Id;
        this.Form = Form;
        this.Light = Light;
        this.Lower = Lower;
        this.Upper = Upper;
    }

    public String id() {
        return Id;
    }

    public Kind kind() {
        return Form;
    }

    public int light() {
        return Light;
    }

    public double[][] lower() {
        return Lower;
    }

    public double[][] upper() {
        return Upper;
    }

    public boolean tall() {
        return Form == Kind.FloorTall;
    }
}
