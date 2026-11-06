package net.rai.scp_unleashed.scp354.beasts;

public final class Scp354Gaits {
    public static final Scp354Stride BruteWalk = new Scp354Stride("walk", 1.875, new float[]{0.22f, 0.72f, 0f, 0.5f});
    public static final Scp354Stride BruteRun = new Scp354Stride("run", 4.25, new float[]{0f, 0.1f, 0.5f, 0.6f});
    public static final Scp354Stride CrawlerWalk = new Scp354Stride("walk", 1.5625, new float[]{0.8571f, 0.7143f, 0.5714f, 0.4286f, 0.2857f, 0.1429f, 0f, 0.3571f, 0.2143f, 0.0714f, 0.9286f, 0.7857f, 0.6429f, 0.5f});
    public static final Scp354Stride CrawlerRun = new Scp354Stride("run", 2.5, new float[]{0.8571f, 0.7143f, 0.5714f, 0.4286f, 0.2857f, 0.1429f, 0f, 0.3571f, 0.2143f, 0.0714f, 0.9286f, 0.7857f, 0.6429f, 0.5f});
    public static final Scp354Stride LeaperWalk = new Scp354Stride("walk", 1, new float[]{0.25f, 0.75f, 0f, 0.5f});
    public static final Scp354Stride LeaperRun = new Scp354Stride("run", 2.75, new float[]{0.6f, 0.62f, 0f, 0.02f});
    public static final Scp354Stride ChargerWalk = new Scp354Stride("walk", 1.75, new float[]{0.25f, 0.75f, 0f, 0.5f});
    public static final Scp354Stride ChargerRun = new Scp354Stride("run", 3.25, new float[]{0.45f, 0.55f, 0f, 0.1f});
    public static final Scp354Stride ChargerCharge = new Scp354Stride("charge", 3.25, new float[]{0.45f, 0.55f, 0f, 0.1f});
    public static final Scp354Stride WingbeastAir = new Scp354Stride("drift", 4.0, new float[]{});

    private Scp354Gaits() {
    }
}
