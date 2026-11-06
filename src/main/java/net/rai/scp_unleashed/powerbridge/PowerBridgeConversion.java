package net.rai.scp_unleashed.powerbridge;

public final class PowerBridgeConversion {
    public static final int RatioFe = 1;
    public static final int RatioUnits = 1;
    public static final int LossPercent = 5;
    public static final int KeepPercent = 100 - LossPercent;
    public static final long Milli = 1000L;
    public static final int Sample = 100;

    private PowerBridgeConversion() {
    }

    public static long milli(long Fe) {
        return Fe * Milli;
    }

    public static long wholeFe(long BufferMilli) {
        return Math.max(0L, BufferMilli) / Milli;
    }

    public static long unitsAvailable(long BufferMilli) {
        return Math.max(0L, BufferMilli) * RatioUnits * KeepPercent / ((long) RatioFe * 100L * Milli);
    }

    public static long costMilli(long Units) {
        long Top = Math.max(0L, Units) * RatioFe * 100L * Milli;
        long Bottom = (long) RatioUnits * KeepPercent;
        return (Top + Bottom - 1) / Bottom;
    }

    public static long gainMilli(long Units) {
        return Math.max(0L, Units) * RatioFe * KeepPercent * Milli / ((long) RatioUnits * 100L);
    }

    public static long unitsToFill(long RoomMilli) {
        return Math.max(0L, RoomMilli) * RatioUnits * 100L / ((long) RatioFe * KeepPercent * Milli);
    }

    public static long unitsFromFe(long Fe) {
        return unitsAvailable(milli(Fe));
    }

    public static long feFromUnits(long Units) {
        return wholeFe(gainMilli(Units));
    }

    public static long feForUnits(long Units) {
        return (long) Math.ceil((double) Units * RatioFe / RatioUnits);
    }

    public static long unitsForFe(long Fe) {
        return (long) Math.ceil((double) Fe * RatioUnits / RatioFe);
    }

    public static long sampleOut(PowerBridgeKind Kind) {
        return Kind.takesFe() ? unitsFromFe(Sample) : feFromUnits(Sample);
    }
}
