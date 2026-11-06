package net.rai.scp_unleashed.scp003.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp003Particles {
    private Scp003Particles() {
    }

    public static class Flesh extends TextureSheetParticle {
        protected Flesh(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.05;
            this.yd = SpeedY + random.nextDouble() * 0.08;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.05;
            this.gravity = 0.9f;
            this.friction = 0.96f;
            this.hasPhysics = true;
            this.lifetime = 18 + random.nextInt(18);
            this.quadSize = 0.06f + random.nextFloat() * 0.06f;
            float Shade = 0.82f + random.nextFloat() * 0.18f;
            setColor(Shade, Shade, Shade);
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (onGround) {
                xd *= 0.6;
                zd *= 0.6;
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Heal extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Heal(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY + 0.01;
            this.zd = SpeedZ;
            this.gravity = -0.01f;
            this.friction = 0.9f;
            this.hasPhysics = false;
            this.lifetime = 10 + random.nextInt(8);
            this.quadSize = 0.07f + random.nextFloat() * 0.05f;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                float Age = (float) age / lifetime;
                setAlpha(Math.round(Mth.clamp(1.2f - Age, 0.0f, 1.0f) * 4.0f) / 4.0f);
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

    public static class Spore extends TextureSheetParticle {
        private final float Phase;

        protected Spore(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.gravity = -0.003f;
            this.friction = 0.92f;
            this.hasPhysics = false;
            this.lifetime = 24 + random.nextInt(20);
            this.quadSize = 0.035f + random.nextFloat() * 0.03f;
            this.Phase = random.nextFloat() * Mth.TWO_PI;
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                float Age = (float) age / lifetime;
                setAlpha(Math.round(Mth.clamp(Math.min(Age * 5.0f, (1.0f - Age) * 2.5f), 0.0f, 1.0f) * 4.0f) / 4.0f);
                xd += Mth.sin(age * 0.3f + Phase) * 0.0015;
                zd += Mth.cos(age * 0.27f + Phase) * 0.0015;
            }
        }

        @Override
        protected int getLightColor(float PartialTick) {
            int Packed = super.getLightColor(PartialTick);
            int Block = Math.max(Packed & 0xFF, 6 << 4);
            return Packed & 0xFFFF0000 | Block;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record FleshProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flesh(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record HealProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Heal(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record SporeProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Spore(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
