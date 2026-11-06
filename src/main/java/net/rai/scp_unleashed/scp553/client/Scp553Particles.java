package net.rai.scp_unleashed.scp553.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp553Particles {
    private Scp553Particles() {
    }

    public static class Glint extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Glint(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.01;
            this.yd = SpeedY + (random.nextDouble() - 0.5) * 0.01;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.01;
            this.gravity = 0.05f;
            this.friction = 0.9f;
            this.hasPhysics = false;
            this.lifetime = 10 + random.nextInt(12);
            this.quadSize = 0.04f + random.nextFloat() * 0.04f;
            float Shade = 0.85f + random.nextFloat() * 0.15f;
            setColor(Shade, Shade, Shade);
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        @Override
        public float getQuadSize(float Partial) {
            return Mth.clamp(quadSize, 0.0f, 0.15f);
        }

        @Override
        protected int getLightColor(float Partial) {
            return 0xF000F0;
        }
    }

    public record GlintProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Glint(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
