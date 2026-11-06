package net.rai.scp_unleashed.scp001day.sun;

public final class Scp001DaySunUpload {
    public static final int FarSpan = 24;
    public static final Unpack Clean = new Unpack(0, 0, 0, 0, 0, 1);
    public static final Shape Grid = new Shape(Scp001DayGridStore.Span, Scp001DayGridStore.Span, Scp001DayGridStore.Span, 2, 1, true);
    public static final Shape Coarse = new Shape(Scp001DayGridStore.CoarseSpan, Scp001DayGridStore.CoarseSpan, Scp001DayGridStore.CoarseSpan, 1, 1, true);
    public static final Shape Far = new Shape(FarSpan * 2, FarSpan, 1, 16, 4, false);

    private Scp001DaySunUpload() {
    }

    public record Unpack(int RowLength, int ImageHeight, int SkipPixels, int SkipRows, int SkipImages, int Alignment) {
    }

    public record Shape(int Width, int Height, int Depth, int TexelBytes, int ComponentBytes, boolean Volume) {
        public long bytes() {
            return (long) Width * Height * Depth * TexelBytes;
        }
    }

    public static long rowStride(Shape Texture, Unpack State) {
        long Pixels = State.RowLength() > 0 ? State.RowLength() : Texture.Width();
        long Row = Pixels * Texture.TexelBytes();
        int Alignment = Math.max(1, State.Alignment());
        if (Texture.ComponentBytes() >= Alignment) {
            return Row;
        }
        return (Row + Alignment - 1) / Alignment * Alignment;
    }

    public static long span(Shape Texture, Unpack State) {
        long Row = rowStride(Texture, State);
        long Image = (Texture.Volume() && State.ImageHeight() > 0 ? State.ImageHeight() : Texture.Height()) * Row;
        long Skipped = Texture.Volume() ? State.SkipImages() : 0L;
        long Start = Skipped * Image + State.SkipRows() * Row + (long) State.SkipPixels() * Texture.TexelBytes();
        return Start + (Texture.Depth() - 1L) * Image + (Texture.Height() - 1L) * Row + (long) Texture.Width() * Texture.TexelBytes();
    }

    public static boolean fits(Shape Texture, Unpack State, long Capacity) {
        return span(Texture, State) <= Capacity;
    }
}
