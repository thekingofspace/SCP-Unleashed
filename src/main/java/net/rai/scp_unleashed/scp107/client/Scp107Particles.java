package net.rai.scp_unleashed.scp107.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public final class Scp107Particles {
    public static final float CausticWeight = 1.4f;
    public static final int CausticLinger = 5;
    public static final float MilkWeight = 0.9f;
    public static final int MilkLinger = 7;
    public static final float OilWeight = 1.8f;
    public static final int OilLinger = 4;

    private Scp107Particles() {
    }

    public static class Ember extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Ember(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.08;
            this.yd = SpeedY + 0.05 + random.nextDouble() * 0.07;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.08;
            this.gravity = 0.9f;
            this.friction = 0.9f;
            this.lifetime = 6 + random.nextInt(5);
            this.quadSize = 0.05f + random.nextFloat() * 0.04f;
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
        protected int getLightColor(float Partial) {
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Splash extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Splash(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, float Weight, int Linger) {
            super(World, X, Y, Z);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.06;
            this.yd = SpeedY + 0.06 + random.nextDouble() * 0.1;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.06;
            this.gravity = Weight;
            this.friction = 0.92f;
            this.lifetime = Linger + random.nextInt(5);
            this.quadSize = 0.06f + random.nextFloat() * 0.04f;
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
    }

    public record EmberProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Ember(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record SplashProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Splash(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, CausticWeight, CausticLinger);
        }
    }

    public record OilProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Splash(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, OilWeight, OilLinger);
        }
    }

    public record MilkProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Splash(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, MilkWeight, MilkLinger);
        }
    }
}
