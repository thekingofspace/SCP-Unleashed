package net.rai.scp_unleashed.labware.client;

public record LabHoverArea(int X, int Y, int Width, int Height) {
    public boolean contains(double GuiX, double GuiY) {
        return GuiX >= X && GuiX < X + Width && GuiY >= Y && GuiY < Y + Height;
    }
}
