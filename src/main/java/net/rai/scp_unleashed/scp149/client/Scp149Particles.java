package net.rai.scp_unleashed.scp149.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp149Particles {
    private Scp149Particles() {
    }

    public static class Mote extends TextureSheetParticle {
        protected Mote(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.03;
            this.yd = SpeedY + random.nextDouble() * 0.03;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.03;
            this.gravity = 0.4f;
            this.friction = 0.88f;
            this.hasPhysics = true;
            this.lifetime = 14 + random.nextInt(14);
            this.quadSize = 0.05f + random.nextFloat() * 0.05f;
            float Shade = 0.82f + random.nextFloat() * 0.18f;
            setColor(Shade, Shade, Shade);
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed && onGround) {
                xd *= 0.4;
                zd *= 0.4;
            }
            if (!removed && age > lifetime * 2 / 3) {
                quadSize *= 0.9f;
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }

        @Override
        public float getQuadSize(float Partial) {
            return Mth.clamp(quadSize, 0.0f, 0.2f);
        }
    }

    public record MoteProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Mote(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
