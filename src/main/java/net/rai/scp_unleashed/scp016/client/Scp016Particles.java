package net.rai.scp_unleashed.scp016.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp016Particles {
    public static final int GlowLight = 11;

    private Scp016Particles() {
    }

    public static class Fleck extends TextureSheetParticle {
        protected Fleck(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.02;
            this.yd = SpeedY + random.nextDouble() * 0.02;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.02;
            this.gravity = 0.35f;
            this.friction = 0.9f;
            this.hasPhysics = true;
            this.lifetime = 14 + random.nextInt(14);
            this.quadSize = 0.05f + random.nextFloat() * 0.05f;
            float Shade = 0.85f + random.nextFloat() * 0.15f;
            setColor(Shade, Shade, Shade);
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                float Age = (float) age / lifetime;
                setAlpha(Math.round(Mth.clamp(1.4f - Age * 1.4f, 0.0f, 1.0f) * 4.0f) / 4.0f);
                if (onGround) {
                    xd *= 0.5;
                    zd *= 0.5;
                }
            }
        }

        @Override
        protected int getLightColor(float PartialTick) {
            int Packed = super.getLightColor(PartialTick);
            int Block = Math.max(Packed & 0xFF, GlowLight << 4);
            return Packed & 0xFFFF0000 | Block;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record FleckProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Fleck(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
