package net.rai.scp_unleashed.scp372.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp372Particles {
    private Scp372Particles() {
    }

    public static class Flare extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Flare(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.01;
            this.yd = SpeedY + (random.nextDouble() - 0.5) * 0.01;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.01;
            this.gravity = 0.0f;
            this.friction = 0.86f;
            this.hasPhysics = false;
            this.lifetime = 10 + random.nextInt(9);
            this.quadSize = 0.05f + random.nextFloat() * 0.06f;
            float Shade = 0.85f + random.nextFloat() * 0.15f;
            setColor(Shade, Shade, Shade);
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
            return LightTexture.FULL_BRIGHT;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record FlareProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flare(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
