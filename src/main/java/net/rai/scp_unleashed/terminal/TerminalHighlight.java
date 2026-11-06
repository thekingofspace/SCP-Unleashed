package net.rai.scp_unleashed.terminal;

public final class TerminalHighlight {
    private TerminalHighlight() {
    }

    public static boolean lit(boolean Hovered, boolean Focused, boolean AnyHovered, boolean AnyFocused, boolean MouseMode) {
        if (MouseMode) {
            return Hovered || (!AnyHovered && Focused);
        }
        return Focused || (!AnyFocused && Hovered);
    }

    public static boolean mouseMode(boolean Previous, boolean Moved, boolean KeyNavigated) {
        if (KeyNavigated) {
            return false;
        }
        return Moved || Previous;
    }

    public static boolean within(double MouseX, double MouseY, int X, int Y, int Width, int Height) {
        return MouseX >= X && MouseY >= Y && MouseX < X + Width && MouseY < Y + Height;
    }
}
