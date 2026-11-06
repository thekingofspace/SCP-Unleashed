package net.rai.scp_unleashed.pressure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum DuctKind implements StringRepresentable {
    Pipe("pipe", 1000, 5, 3, 800, 20, 400.0, 4.0),
    Vent("vent", 250, 3, 3, 200, 10, 150.0, 1.5);

    public static final Codec<DuctKind> KindCodec = StringRepresentable.fromEnum(DuctKind::values);
    public static final int MaxNetworkSize = 4096;

    private final String Name;
    private final int Rating;
    private final int ArmInset;
    private final int CoreInset;
    private final int TargetPressure;
    private final int PumpPower;
    private final double PumpRate;
    private final double LeakRate;

    DuctKind(String Name, int Rating, int ArmInset, int CoreInset, int TargetPressure, int PumpPower, double PumpRate, double LeakRate) {
        this.Name = Name;
        this.Rating = Rating;
        this.ArmInset = ArmInset;
        this.CoreInset = CoreInset;
        this.TargetPressure = TargetPressure;
        this.PumpPower = PumpPower;
        this.PumpRate = PumpRate;
        this.LeakRate = LeakRate;
    }

    public int getRating() {
        return Rating;
    }

    public int getArmInset() {
        return ArmInset;
    }

    public int getCoreInset() {
        return CoreInset;
    }

    public int getTargetPressure() {
        return TargetPressure;
    }

    public int getPumpPower() {
        return PumpPower;
    }

    public double getPumpRate() {
        return PumpRate;
    }

    public double getLeakRate() {
        return LeakRate;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
