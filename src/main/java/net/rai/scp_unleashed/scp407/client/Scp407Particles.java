package net.rai.scp_unleashed.scp407.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp407Particles {
    public static final int FullBright = 0xF000F0;

    private Scp407Particles() {
    }

    public static class Spore extends TextureSheetParticle {
        private final float Phase;
        private final SpriteSet Sprites;

        protected Spore(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.006;
            this.yd = SpeedY + random.nextDouble() * 0.004;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.006;
            this.gravity = -0.0015f;
            this.friction = 0.96f;
            this.hasPhysics = false;
            this.lifetime = 70 + random.nextInt(70);
            this.quadSize = 0.035f + random.nextFloat() * 0.035f;
            this.Phase = random.nextFloat() * Mth.TWO_PI;
            float Warm = random.nextFloat();
            setColor(0.86f + Warm * 0.12f, 0.92f + Warm * 0.06f, 0.52f + Warm * 0.2f);
            setAlpha(0.0f);
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            xd += Mth.sin((age + Phase * 20.0f) * 0.11f) * 0.0006;
            zd += Mth.cos((age + Phase * 20.0f) * 0.09f) * 0.0006;
            float Age = (float) age / lifetime;
            float Fade = Age < 0.15f ? Age / 0.15f : Age > 0.7f ? (1.0f - Age) / 0.3f : 1.0f;
            setAlpha(Math.round(Mth.clamp(Fade, 0.0f, 1.0f) * 4.0f) / 4.0f * 0.9f);
            setSpriteFromAge(Sprites);
        }

        @Override
        protected int getLightColor(float Partial) {
            return FullBright;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Flake extends TextureSheetParticle {
        private final float Spin;

        protected Flake(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, boolean Withered) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.04;
            this.yd = SpeedY + random.nextDouble() * 0.03;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.04;
            this.gravity = Withered ? 0.08f : 0.12f;
            this.friction = 0.9f;
            this.hasPhysics = true;
            this.lifetime = 30 + random.nextInt(30);
            this.quadSize = 0.07f + random.nextFloat() * 0.05f;
            this.Spin = (random.nextFloat() - 0.5f) * 0.3f;
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
            if (!onGround) {
                roll += Spin;
                xd += Mth.sin(age * 0.3f) * 0.002;
            } else {
                xd *= 0.5;
                zd *= 0.5;
            }
            float Age = (float) age / lifetime;
            if (Age > 0.75f) {
                setAlpha(Math.round((1.0f - Age) / 0.25f * 4.0f) / 4.0f);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Mote extends TextureSheetParticle {
        private final float Phase;
        private final float Peak;

        protected Mote(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.004;
            this.yd = (random.nextDouble() - 0.45) * 0.003;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.004;
            this.gravity = 0.0f;
            this.friction = 0.985f;
            this.hasPhysics = false;
            this.lifetime = 120 + random.nextInt(120);
            this.quadSize = 0.02f + random.nextFloat() * 0.025f;
            this.Phase = random.nextFloat() * Mth.TWO_PI;
            this.Peak = 0.45f + random.nextFloat() * 0.35f;
            float Tint = random.nextFloat();
            setColor(0.72f + Tint * 0.2f, 0.82f + Tint * 0.12f, 0.58f + Tint * 0.16f);
            setAlpha(0.0f);
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            xd += Mth.sin((age + Phase * 30.0f) * 0.05f) * 0.0003;
            yd += Mth.cos((age + Phase * 30.0f) * 0.04f) * 0.0002;
            zd += Mth.cos((age + Phase * 30.0f) * 0.06f) * 0.0003;
            float Age = (float) age / lifetime;
            float Fade = Age < 0.2f ? Age / 0.2f : Age > 0.75f ? (1.0f - Age) / 0.25f : 1.0f;
            setAlpha(Math.round(Mth.clamp(Fade, 0.0f, 1.0f) * 4.0f) / 4.0f * Peak);
        }

        @Override
        protected int getLightColor(float Partial) {
            return FullBright;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Glint extends TextureSheetParticle {
        private final float Phase;
        private final SpriteSet Sprites;

        protected Glint(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX * 0.5;
            this.yd = Math.max(0.004, SpeedY);
            this.zd = SpeedZ * 0.5;
            this.gravity = -0.0004f;
            this.friction = 0.97f;
            this.hasPhysics = false;
            this.lifetime = 60 + random.nextInt(70);
            this.quadSize = 0.03f + random.nextFloat() * 0.03f;
            this.Phase = random.nextFloat() * Mth.TWO_PI;
            float Warm = random.nextFloat();
            setColor(1.0f, 0.86f + Warm * 0.12f, 0.46f + Warm * 0.3f);
            setAlpha(0.0f);
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            xd += Mth.sin((age + Phase * 20.0f) * 0.08f) * 0.0004;
            float Age = (float) age / lifetime;
            float Fade = Age < 0.15f ? Age / 0.15f : Age > 0.7f ? (1.0f - Age) / 0.3f : 1.0f;
            float Twinkle = 0.65f + 0.35f * Mth.sin(age * 0.45f + Phase);
            setAlpha(Math.round(Mth.clamp(Fade * Twinkle, 0.0f, 1.0f) * 4.0f) / 4.0f);
            setSpriteFromAge(Sprites);
        }

        @Override
        protected int getLightColor(float Partial) {
            return FullBright;
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

    public record GlintProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Glint(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record SporeProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Spore(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record LeafProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flake(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, false);
        }
    }

    public record AshProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flake(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, true);
        }
    }
}
