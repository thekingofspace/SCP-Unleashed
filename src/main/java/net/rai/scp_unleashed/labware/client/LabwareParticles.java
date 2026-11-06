package net.rai.scp_unleashed.labware.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;

public final class LabwareParticles {
    private LabwareParticles() {
    }

    public static class VentPlume extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float StartSize;
        private final float Swirl;

        protected VentPlume(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, ColorParticleOption Tint) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.012;
            this.yd = SpeedY + 0.03 + random.nextDouble() * 0.02;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.012;
            this.gravity = -0.012f;
            this.friction = 0.96f;
            this.hasPhysics = true;
            this.lifetime = 60 + random.nextInt(40);
            this.StartSize = 0.18f + random.nextFloat() * 0.12f;
            this.Swirl = random.nextFloat() * Mth.TWO_PI;
            this.quadSize = StartSize;
            float Shade = 0.93f + random.nextFloat() * 0.07f;
            setColor(Shade * Tint.getRed(), Shade * Tint.getGreen(), Math.min(1.0f, Shade + 0.02f) * Tint.getBlue());
            setAlpha(0.0f);
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                float Age = (float) age / lifetime;
                float Envelope = Mth.clamp(Age * 8.0f, 0.0f, 1.0f) * Mth.clamp((1.0f - Age) * 1.6f, 0.0f, 1.0f);
                setAlpha(Math.round(0.62f * Envelope * 8.0f) / 8.0f);
                quadSize = StartSize * (1.0f + Age * 2.6f);
                xd += Mth.sin(age * 0.11f + Swirl) * 0.0014;
                zd += Mth.cos(age * 0.097f + Swirl) * 0.0014;
            }
        }

        @Override
        protected int getLightColor(float PartialTick) {
            int Packed = super.getLightColor(PartialTick);
            int Glow = Math.max(Packed & 0xFF, 5 << 4);
            return Packed & 0xFFFF0000 | Glow;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record VentPlumeProvider(SpriteSet Sprites) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption Tint, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new VentPlume(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, Tint);
        }
    }
}
