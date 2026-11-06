package net.rai.scp_unleashed.scpsign;

public enum ScpSignKind {
    Wide(174.0f, 78.0f, 14.5f),
    Small(78.0f, 48.0f, 12.0f);

    public static final float Unit = 1.0f / 96.0f;
    public static final float Left = 1.5f;

    private final float Width;
    private final float Height;
    private final float Top;

    ScpSignKind(float Width, float Height, float Top) {
        this.Width = Width;
        this.Height = Height;
        this.Top = Top;
    }

    public float getWidth() {
        return Width;
    }

    public float getHeight() {
        return Height;
    }

    public float getTop() {
        return Top;
    }
}
