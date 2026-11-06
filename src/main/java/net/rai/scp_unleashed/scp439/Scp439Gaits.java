package net.rai.scp_unleashed.scp439;

public final class Scp439Gaits {
    public static final Scp439Stride QueenWalk = new Scp439Stride("walk", 0.2875, new float[]{0.6667f, 0.3333f, 0f, 0.1667f, 0.8333f, 0.5f});
    public static final Scp439Stride QueenRun = new Scp439Stride("run", 0.3875, new float[]{0.6667f, 0.3333f, 0f, 0.1667f, 0.8333f, 0.5f});
    public static final Scp439Stride WorkerWalk = new Scp439Stride("walk", 0.35, new float[]{0f, 0.5f, 0f, 0.5f, 0f, 0.5f});
    public static final Scp439Stride WorkerRun = new Scp439Stride("run", 0.4625, new float[]{0f, 0.5f, 0f, 0.5f, 0f, 0.5f});
    public static final Scp439Stride BruteWalk = new Scp439Stride("walk", 0.5625, new float[]{0f, 0.5f, 0f, 0.5f, 0f, 0.5f});
    public static final Scp439Stride BruteRun = new Scp439Stride("run", 0.7188, new float[]{0f, 0.5f, 0f, 0.5f, 0f, 0.5f});

    private Scp439Gaits() {
    }
}
