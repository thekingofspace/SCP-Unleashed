package net.rai.scp_unleashed.scp105.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp105Particles {
    public static final int FullBright = 0xF000F0;

    private Scp105Particles() {
    }

    private static float stepped(float Value) {
        return Math.round(Mth.clamp(Value, 0.0f, 1.0f) * 4.0f) / 4.0f;
    }

    public static class Flash extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Start;

        protected Flash(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = 5;
            this.Start = 0.3f;
            this.quadSize = Start;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                setAlpha(stepped(1.3f - (float) age / lifetime));
            }
        }

        @Override
        public float getQuadSize(float Partial) {
            return Start + (age + Partial) / lifetime * 0.5f;
        }

        @Override
        public int getLightColor(float Partial) {
            return FullBright;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Scrap extends TextureSheetParticle {
        private final float Spin;
        private final float Size;

        protected Scrap(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.08;
            this.yd = SpeedY + 0.02 + random.nextDouble() * 0.05;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.08;
            this.gravity = 0.35f;
            this.friction = 0.9f;
            this.hasPhysics = true;
            this.lifetime = 24 + random.nextInt(16);
            this.Size = 0.05f + random.nextFloat() * 0.04f;
            this.quadSize = Size;
            this.Spin = (random.nextFloat() - 0.5f) * 0.4f;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = roll;
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            oRoll = roll;
            if (onGround) {
                xd *= 0.4;
                zd *= 0.4;
                return;
            }
            roll += Spin;
            int Left = lifetime - age;
            quadSize = Left < 6 ? Size * Math.max(0.3f, Left / 6.0f) : Size;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public record FlashProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flash(World, X, Y, Z, Sprites);
        }
    }

    public record ScrapProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scrap(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
