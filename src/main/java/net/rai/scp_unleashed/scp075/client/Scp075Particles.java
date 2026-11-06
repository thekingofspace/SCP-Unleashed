package net.rai.scp_unleashed.scp075.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class Scp075Particles {
    private Scp075Particles() {
    }

    public static class Drip extends TextureSheetParticle {
        private final float StartSize;

        protected Drip(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.02;
            this.yd = SpeedY + random.nextDouble() * 0.02;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.02;
            this.gravity = 0.35f;
            this.friction = 0.9f;
            this.hasPhysics = true;
            this.lifetime = 18 + random.nextInt(12);
            this.quadSize = 0.06f + random.nextFloat() * 0.04f;
            this.StartSize = quadSize;
            float Shade = 0.85f + random.nextFloat() * 0.15f;
            setColor(Shade, Shade, Shade);
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                float Age = (float) age / lifetime;
                quadSize = StartSize * Math.max(0.25f, Math.round((1.0f - Age * 0.75f) * 4.0f) / 4.0f);
                if (onGround) {
                    xd *= 0.3;
                    zd *= 0.3;
                }
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public record DripProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Drip(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
