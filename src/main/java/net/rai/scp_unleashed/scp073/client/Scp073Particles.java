package net.rai.scp_unleashed.scp073.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp073Particles {
    public static final int FullBright = 0xF000F0;

    private Scp073Particles() {
    }

    private static float stepped(float Value) {
        return Math.round(Mth.clamp(Value, 0.0f, 1.0f) * 4.0f) / 4.0f;
    }

    public static class Glint extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Glint(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.friction = 0.82f;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = 8 + random.nextInt(5);
            this.quadSize = 0.09f + random.nextFloat() * 0.05f;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                setAlpha(stepped(1.6f - (float) age / lifetime * 1.6f));
            }
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
            this.lifetime = 6;
            this.Start = 0.35f;
            this.quadSize = Start;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                setAlpha(stepped(1.25f - (float) age / lifetime));
            }
        }

        @Override
        public float getQuadSize(float Partial) {
            return Start + (age + Partial) / lifetime * 0.45f;
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

    public static class Leaf extends TextureSheetParticle {
        private final float Sway;
        private final float Size;

        protected Leaf(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = (random.nextDouble() - 0.5) * 0.02;
            this.yd = -0.01 - random.nextDouble() * 0.01;
            this.zd = (random.nextDouble() - 0.5) * 0.02;
            this.gravity = 0.02f;
            this.friction = 0.92f;
            this.hasPhysics = true;
            this.lifetime = 40 + random.nextInt(30);
            this.Size = 0.075f + random.nextFloat() * 0.035f;
            this.quadSize = Size;
            this.Sway = random.nextFloat() * Mth.TWO_PI;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = roll;
            float Shade = 0.85f + random.nextFloat() * 0.15f;
            setColor(Shade, Shade, Shade);
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
                xd *= 0.3;
                zd *= 0.3;
                return;
            }
            float Phase = Sway + age * 0.25f;
            xd += Mth.sin(Phase) * 0.004;
            zd += Mth.cos(Phase * 0.8f) * 0.004;
            roll += 0.08f * Mth.sin(Phase);
            int Left = lifetime - age;
            quadSize = Left < 8 ? Size * Math.max(0.25f, Left / 8.0f) : Size;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public record GlintProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Glint(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record FlashProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flash(World, X, Y, Z, Sprites);
        }
    }

    public record LeafProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Leaf(World, X, Y, Z, Sprites);
        }
    }
}
