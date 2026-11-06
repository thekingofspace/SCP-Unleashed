package net.rai.scp_unleashed.scp163.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp163Particles {
    private Scp163Particles() {
    }

    public static class Breath extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Breath(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.gravity = 0.0f;
            this.friction = 0.92f;
            this.hasPhysics = false;
            this.lifetime = 18 + random.nextInt(14);
            this.quadSize = 0.05f + random.nextFloat() * 0.04f;
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
        protected int getLightColor(float PartialTick) {
            return 0xF000F0;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record BreathProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Breath(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
