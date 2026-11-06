package net.rai.scp_unleashed.scp098.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp098Particles {
    private Scp098Particles() {
    }

    public static class Spore extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Peak;

        protected Spore(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.gravity = -0.002f;
            this.friction = 0.96f;
            this.hasPhysics = false;
            this.lifetime = 40 + random.nextInt(36);
            this.quadSize = 0.12f + random.nextFloat() * 0.14f;
            this.Peak = 0.75f + random.nextFloat() * 0.25f;
            float Shade = 0.88f + random.nextFloat() * 0.12f;
            setColor(Shade, Shade, Shade);
            setAlpha(0.0f);
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            float Fade = Mth.clamp(Math.min(Age * 6.0f, (1.0f - Age) * 2.5f), 0.0f, 1.0f) * Peak;
            setAlpha(Math.round(Fade * 4.0f) / 4.0f);
            xd += (random.nextDouble() - 0.5) * 0.002;
            zd += (random.nextDouble() - 0.5) * 0.002;
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
}
