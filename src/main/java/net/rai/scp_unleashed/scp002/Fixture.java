package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum Fixture implements StringRepresentable {
    Desk("desk", new double[][]{{0, 12, 1, 16, 15, 16}, {1, 0, 2, 3, 12, 4}, {13, 0, 2, 15, 12, 4}, {1, 0, 13, 3, 12, 15}, {13, 0, 13, 15, 12, 15}, {3, 8, 2, 13, 12, 14}}),
    Chair("chair", new double[][]{{2, 0, 2, 14, 9, 14}, {3, 9, 11.5, 13, 16, 13.5}}),
    Dresser("dresser", new double[][]{{1, 0, 2.5, 15, 15, 15.5}, {0.5, 15, 2, 15.5, 16, 16}}),
    Bed("bed", new double[][]{{0, 0, 0, 16, 9, 16}}),
    Lamp("lamp", new double[][]{{5, 0, 5, 11, 2, 11}, {7, 2, 7, 9, 9, 9}, {3.5, 9, 3.5, 12.5, 16, 12.5}}),
    Rug("rug", new double[][]{{0, 0, 0, 16, 1, 16}});

    public static final Codec<Fixture> FixtureCodec = StringRepresentable.fromEnum(Fixture::values);

    private final String SerializedName;
    private final double[][] Boxes;

    Fixture(String SerializedName, double[][] Boxes) {
        this.SerializedName = SerializedName;
        this.Boxes = Boxes;
    }

    @Override
    public String getSerializedName() {
        return SerializedName;
    }

    public double[][] getBoxes() {
        return Boxes;
    }

    public String flavourKey() {
        return "block.scp_unleashed.scp002_" + SerializedName + ".touch";
    }
}
