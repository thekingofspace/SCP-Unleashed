package net.rai.scp_unleashed.scp354.beasts.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class Scp354BeastParticles {
    private Scp354BeastParticles() {
    }

    public static class Drip extends TextureSheetParticle {
        private int Pooled;

        protected Drip(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX * 0.6 + (random.nextDouble() - 0.5) * 0.02;
            this.yd = SpeedY * 0.6 - random.nextDouble() * 0.02;
            this.zd = SpeedZ * 0.6 + (random.nextDouble() - 0.5) * 0.02;
            this.gravity = 0.9f;
            this.friction = 0.96f;
            this.hasPhysics = true;
            this.lifetime = 30 + random.nextInt(20);
            this.quadSize = 0.07f + random.nextFloat() * 0.05f;
            float Shade = 0.85f + random.nextFloat() * 0.15f;
            setColor(Shade, Shade, Shade);
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            if (onGround) {
                xd = 0.0;
                zd = 0.0;
                if (++Pooled > 8) {
                    quadSize *= 0.85f;
                }
                if (quadSize < 0.02f) {
                    remove();
                }
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Splash extends TextureSheetParticle {
        private final SpriteSet Frames;

        protected Splash(ClientLevel World, double X, double Y, double Z, SpriteSet Frames) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Frames = Frames;
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = 12 + random.nextInt(6);
            this.quadSize = 0.35f + random.nextFloat() * 0.3f;
            setSpriteFromAge(Frames);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Frames);
                quadSize *= 1.03f;
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

    public record SplashProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Splash(World, X, Y, Z, Sprites);
        }
    }
}
