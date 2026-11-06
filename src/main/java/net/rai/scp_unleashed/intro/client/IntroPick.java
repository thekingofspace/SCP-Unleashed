package net.rai.scp_unleashed.intro.client;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public final class IntroPick implements RandomSource {
    private final RandomSource Base;
    private int Forced;

    public IntroPick(RandomSource Base, int Forced) {
        this.Base = Base;
        this.Forced = Forced;
    }

    @Override
    public RandomSource fork() {
        return Base.fork();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return Base.forkPositional();
    }

    @Override
    public void setSeed(long Seed) {
        Base.setSeed(Seed);
    }

    @Override
    public int nextInt() {
        return Base.nextInt();
    }

    @Override
    public int nextInt(int Bound) {
        if (Forced >= 0 && Bound > 0) {
            int Chosen = Forced % Bound;
            Forced = -1;
            return Chosen;
        }
        return Base.nextInt(Bound);
    }

    @Override
    public long nextLong() {
        return Base.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return Base.nextBoolean();
    }

    @Override
    public float nextFloat() {
        return Base.nextFloat();
    }

    @Override
    public double nextDouble() {
        return Base.nextDouble();
    }

    @Override
    public double nextGaussian() {
        return Base.nextGaussian();
    }
}
