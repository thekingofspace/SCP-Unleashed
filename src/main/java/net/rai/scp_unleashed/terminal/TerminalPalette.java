package net.rai.scp_unleashed.terminal;

public record TerminalPalette(int Bg, int Panel, int Faint, int Dim, int Main, int Bright, int Warn) {
    public static final TerminalPalette Green = new TerminalPalette(0xFF040B06, 0xFF061109, 0xFF0F2A17, 0xFF2C7443, 0xFF4CDB78, 0xFFA8FFC0, 0xFFE3E07A);
    public static final TerminalPalette Amber = new TerminalPalette(0xFF0C0703, 0xFF120B04, 0xFF2E1D08, 0xFF8C5A14, 0xFFFFB238, 0xFFFFE2A6, 0xFFFF7A4A);

    public static TerminalPalette of(TerminalColour Colour) {
        return Colour == TerminalColour.Amber ? Amber : Green;
    }

    public static int alpha(int Colour, int Alpha) {
        return (Math.max(0, Math.min(255, Alpha)) << 24) | (Colour & 0xFFFFFF);
    }

    public static double luminance(int Colour) {
        return 0.2126 * channel(Colour >> 16) + 0.7152 * channel(Colour >> 8) + 0.0722 * channel(Colour);
    }

    public static double contrast(int Front, int Back) {
        double A = luminance(Front) + 0.05;
        double B = luminance(Back) + 0.05;
        return Math.max(A, B) / Math.min(A, B);
    }

    private static double channel(int Value) {
        double Unit = (Value & 0xFF) / 255.0;
        return Unit <= 0.03928 ? Unit / 12.92 : Math.pow((Unit + 0.055) / 1.055, 2.4);
    }
}
