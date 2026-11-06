package net.rai.scp_unleashed.scp111.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp111.Scp111Breath;

public final class Scp111Particles {
    public static final int FlameFirst = 0;
    public static final int SmokeFirst = 4;
    public static final int SparkFirst = 8;
    public static final int EmberFirst = 10;
    public static final int FrameCount = 12;
    public static final int FullBright = 0xF000F0;

    private Scp111Particles() {
    }

    public static class Breath extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final int Style;
        private final float CoreRed;
        private final float CoreGreen;
        private final float CoreBlue;
        private final float EdgeRed;
        private final float EdgeGreen;
        private final float EdgeBlue;
        private final float StartSize;

        protected Breath(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, Scp111Breath Options, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.Style = Options.Style();
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.CoreRed = (Options.Core() >> 16 & 255) / 255.0f;
            this.CoreGreen = (Options.Core() >> 8 & 255) / 255.0f;
            this.CoreBlue = (Options.Core() & 255) / 255.0f;
            this.EdgeRed = (Options.Edge() >> 16 & 255) / 255.0f;
            this.EdgeGreen = (Options.Edge() >> 8 & 255) / 255.0f;
            this.EdgeBlue = (Options.Edge() & 255) / 255.0f;
            this.hasPhysics = Style == Scp111Breath.Ember;
            this.friction = (float) Scp111Breath.JetFriction;
            switch (Style) {
                case Scp111Breath.Smoke -> {
                    this.lifetime = 16 + random.nextInt(10);
                    this.gravity = -0.02f;
                }
                case Scp111Breath.Spark -> {
                    this.lifetime = 5 + random.nextInt(4);
                    this.gravity = 0.0f;
                }
                case Scp111Breath.Ember -> {
                    this.lifetime = 12 + random.nextInt(8);
                    this.gravity = 0.35f;
                    this.friction = 0.94f;
                }
                default -> {
                    this.lifetime = 8 + random.nextInt(6);
                    this.gravity = -0.015f;
                }
            }
            this.quadSize = Options.Size() * (0.85f + random.nextFloat() * 0.3f);
            this.StartSize = quadSize;
            this.roll = random.nextInt(4) * Mth.HALF_PI;
            this.oRoll = roll;
            shade(0.0f);
            pick();
        }

        private int first() {
            return switch (Style) {
                case Scp111Breath.Smoke -> SmokeFirst;
                case Scp111Breath.Spark -> SparkFirst;
                case Scp111Breath.Ember -> EmberFirst;
                default -> FlameFirst;
            };
        }

        private int frames() {
            return Style == Scp111Breath.Spark || Style == Scp111Breath.Ember ? 2 : 4;
        }

        private void pick() {
            int Frames = frames();
            int Frame = Math.min(Frames - 1, age * Frames / Math.max(1, lifetime));
            setSprite(Sprites.get(first() + Frame, FrameCount - 1));
        }

        private void shade(float Life) {
            float Blend = Style == Scp111Breath.Smoke ? Life : Mth.clamp(Life * 1.8f, 0.0f, 1.0f);
            rCol = Mth.lerp(Blend, CoreRed, EdgeRed);
            gCol = Mth.lerp(Blend, CoreGreen, EdgeGreen);
            bCol = Mth.lerp(Blend, CoreBlue, EdgeBlue);
            float Fade = Style == Scp111Breath.Smoke ? 0.75f : 1.0f;
            alpha = Life > 0.6f ? Fade * Mth.clamp((1.0f - Life) / 0.4f, 0.0f, 1.0f) : Fade;
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            float Life = (float) age / lifetime;
            shade(Life);
            float Grow = switch (Style) {
                case Scp111Breath.Smoke -> 1.0f + Life * 1.2f;
                case Scp111Breath.Spark, Scp111Breath.Ember -> 1.0f - Life * 0.5f;
                default -> 1.0f + Life * 0.35f;
            };
            quadSize = StartSize * Math.round(Grow * 4.0f) / 4.0f;
            pick();
        }

        @Override
        public int getLightColor(float PartialTick) {
            return Style == Scp111Breath.Smoke ? super.getLightColor(PartialTick) : FullBright;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record BreathProvider(SpriteSet Sprites) implements ParticleProvider<Scp111Breath> {
        @Override
        public Particle createParticle(Scp111Breath Options, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Breath(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Options, Sprites);
        }
    }
}
