package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp1730Particles {
    private Scp1730Particles() {
    }

    public static class Glyph extends TextureSheetParticle {
        private final float Size;

        protected Glyph(ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz, SpriteSet Sprites, float Size, int Life) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = Dx;
            this.yd = Dy;
            this.zd = Dz;
            this.Size = Size;
            this.quadSize = Size;
            this.lifetime = Life;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.alpha = 0.0f;
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            float Share = age / (float) Math.max(1, lifetime);
            alpha = Mth.clamp(Math.min(Share * 6.0f, (1.0f - Share) * 3.0f), 0.0f, 0.9f);
            if (random.nextInt(9) == 0) {
                alpha *= 0.4f;
            }
            quadSize = Size * (0.85f + 0.15f * Mth.sin(age * 0.4f));
            xd *= 0.95;
            yd *= 0.95;
            zd *= 0.95;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        protected int getLightColor(float Partial) {
            return 0xF000F0;
        }
    }

    public static final class GlyphProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public GlyphProvider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz) {
            return new Glyph(World, X, Y, Z, Dx, Dy, Dz, Sprites, 0.32f + World.random.nextFloat() * 0.18f, 28 + World.random.nextInt(24));
        }
    }

    public static final class AnchorProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public AnchorProvider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz) {
            return new Glyph(World, X, Y, Z, Dx, Dy, Dz, Sprites, 0.08f + World.random.nextFloat() * 0.06f, 18 + World.random.nextInt(14));
        }
    }
}
