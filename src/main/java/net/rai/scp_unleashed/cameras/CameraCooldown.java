package net.rai.scp_unleashed.cameras;

public final class CameraCooldown {
    private final int Gap;
    private int Left;

    public CameraCooldown(int Gap) {
        this.Gap = Gap;
    }

    public void tick() {
        if (Left > 0) {
            Left--;
        }
    }

    public boolean ready() {
        return Left <= 0;
    }

    public boolean use() {
        if (Left > 0) {
            return false;
        }
        Left = Gap;
        return true;
    }

    public void reset() {
        Left = 0;
    }
}
