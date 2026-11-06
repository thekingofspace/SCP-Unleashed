package net.rai.scp_unleashed.scp205;

import net.minecraft.util.StringRepresentable;

public enum Scp205Tilt implements StringRepresentable {
    Level("level", 0.0f),
    Up("up", 22.5f),
    Down("down", -22.5f);

    private final String Name;
    private final float Degrees;

    Scp205Tilt(String Name, float Degrees) {
        this.Name = Name;
        this.Degrees = Degrees;
    }

    public float degrees() {
        return Degrees;
    }

    public Scp205Tilt next() {
        return switch (this) {
            case Level -> Up;
            case Up -> Down;
            case Down -> Level;
        };
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
