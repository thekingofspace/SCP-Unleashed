package net.rai.scp_unleashed.scp610.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp610Particles {
    private Scp610Particles() {
    }

    public static class Spore extends TextureSheetParticle {
        protected Spore(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX * 0.6 + (random.nextDouble() - 0.5) * 0.03;
            this.yd = SpeedY * 0.6 + random.nextDouble() * 0.015;
            this.zd = SpeedZ * 0.6 + (random.nextDouble() - 0.5) * 0.03;
            this.gravity = 0.015f;
            this.friction = 0.92f;
            this.hasPhysics = true;
            this.lifetime = 40 + random.nextInt(40);
            this.quadSize = 0.07f + random.nextFloat() * 0.09f;
            this.roll = random.nextInt(4) * Mth.HALF_PI;
            this.oRoll = roll;
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                float Age = (float) age / lifetime;
                setAlpha(Math.round(Mth.clamp(1.6f - Age * 1.6f, 0.0f, 1.0f) * 4.0f) / 4.0f);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Maw extends TextureSheetParticle {
        private final SpriteSet Frames;

        protected Maw(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Frames = Sprites;
            this.xd = 0.0;
            this.yd = 0.01;
            this.zd = 0.0;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = 14;
            this.quadSize = 0.75f;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Frames);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record SporeProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Spore(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record MawProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Maw(World, X, Y, Z, Sprites);
        }
    }
}
