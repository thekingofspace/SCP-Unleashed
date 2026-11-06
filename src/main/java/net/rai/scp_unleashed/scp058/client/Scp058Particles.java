package net.rai.scp_unleashed.scp058.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp058Particles {
    private Scp058Particles() {
    }

    public static class Mote extends TextureSheetParticle {
        protected Mote(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.03;
            this.yd = SpeedY + random.nextDouble() * 0.04;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.03;
            this.gravity = 0.45f;
            this.friction = 0.88f;
            this.hasPhysics = true;
            this.lifetime = 16 + random.nextInt(16);
            this.quadSize = 0.06f + random.nextFloat() * 0.06f;
            float Shade = 0.8f + random.nextFloat() * 0.2f;
            setColor(Shade, Shade, Shade);
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                float Age = (float) age / lifetime;
                setAlpha(Math.round(Mth.clamp(1.5f - Age * 1.5f, 0.0f, 1.0f) * 4.0f) / 4.0f);
                if (onGround) {
                    xd *= 0.4;
                    zd *= 0.4;
                }
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record MoteProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Mote(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
