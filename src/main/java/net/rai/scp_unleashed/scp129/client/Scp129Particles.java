package net.rai.scp_unleashed.scp129.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;

public final class Scp129Particles {
    public static final int ShortLife = 26;
    public static final int LifeJitter = 18;
    public static final float StartSize = 0.07f;
    public static final float SizeJitter = 0.04f;
    public static final float Growth = 1.6f;
    public static final float Drag = 0.86f;
    public static final float Sink = 0.0012f;
    public static final int AlphaSteps = 4;
    public static final float PeakAlpha = 0.85f;
    public static final float FadeFrom = 0.35f;

    private Scp129Particles() {
    }

    public static class Spore extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float BaseSize;

        protected Spore(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, ColorParticleOption Tint) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.gravity = Sink;
            this.friction = Drag;
            this.hasPhysics = true;
            this.lifetime = ShortLife + random.nextInt(LifeJitter);
            this.BaseSize = StartSize + random.nextFloat() * SizeJitter;
            this.quadSize = BaseSize;
            float Shade = 0.9f + random.nextFloat() * 0.12f;
            setColor(Math.min(1.0f, Tint.getRed() * Shade), Math.min(1.0f, Tint.getGreen() * Shade), Math.min(1.0f, Tint.getBlue() * Shade));
            setAlpha(PeakAlpha);
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            float Fade = Age < FadeFrom ? 1.0f : 1.0f - (Age - FadeFrom) / (1.0f - FadeFrom);
            setAlpha(Math.round(Mth.clamp(Fade, 0.0f, 1.0f) * AlphaSteps) / (float) AlphaSteps * PeakAlpha);
            quadSize = BaseSize * (1.0f + Age * Growth);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record SporeProvider(SpriteSet Sprites) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption Tint, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Spore(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, Tint);
        }
    }
}
