package net.rai.scp_unleashed.daevitegod.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class DaeviteGodParticles {
    private DaeviteGodParticles() {
    }

    public static class Ember extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Ember(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.02;
            this.yd = SpeedY + random.nextDouble() * 0.02;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.02;
            this.gravity = -0.02f;
            this.friction = 0.92f;
            this.hasPhysics = false;
            this.lifetime = 20 + random.nextInt(24);
            this.quadSize = 0.07f + random.nextFloat() * 0.07f;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                float Age = (float) age / lifetime;
                setAlpha(Math.round(Mth.clamp(1.4f - Age * 1.4f, 0.0f, 1.0f) * 4.0f) / 4.0f);
            }
        }

        @Override
        protected int getLightColor(float Partial) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Ash extends TextureSheetParticle {
        protected Ash(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.03;
            this.yd = SpeedY + random.nextDouble() * 0.02;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.03;
            this.gravity = 0.06f;
            this.friction = 0.9f;
            this.hasPhysics = true;
            this.lifetime = 30 + random.nextInt(30);
            this.quadSize = 0.08f + random.nextFloat() * 0.06f;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = roll;
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                oRoll = roll;
                if (!onGround) {
                    roll += 0.08f;
                }
                float Age = (float) age / lifetime;
                setAlpha(Math.round(Mth.clamp(1.6f - Age * 1.6f, 0.0f, 1.0f) * 4.0f) / 4.0f);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record EmberProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Ember(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record AshProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Ash(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
