package net.rai.scp_unleashed.scp457.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp457Particles {
    public static final int FullBright = 0xF000F0;
    public static final int FlameLife = 10;
    public static final int FlameLifeSpread = 8;
    public static final float FlameSize = 0.11f;
    public static final float FlameSizeSpread = 0.09f;
    public static final float FlameDrag = 0.96f;
    public static final int LickLife = 6;
    public static final int LickLifeSpread = 6;
    public static final float LickSize = 0.06f;
    public static final float LickSizeSpread = 0.06f;
    public static final float LickDrag = 0.93f;

    private Scp457Particles() {
    }

    private static float stepped(float Value) {
        return Math.round(Mth.clamp(Value, 0.0f, 1.0f) * 4.0f) / 4.0f;
    }

    public static class Flame extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Start;

        protected Flame(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, int Life, int LifeSpread,
                        float Size, float SizeSpread, float Drag) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.friction = Drag;
            this.gravity = -0.004f;
            this.hasPhysics = false;
            this.lifetime = Life + random.nextInt(LifeSpread);
            this.Start = Size + random.nextFloat() * SizeSpread;
            this.quadSize = Start;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
            }
        }

        @Override
        public float getQuadSize(float Partial) {
            float Age = (age + Partial) / lifetime;
            return Start * (1.0f - Age * Age * 0.6f);
        }

        @Override
        public int getLightColor(float Partial) {
            return FullBright;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Ember extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Ember(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.friction = 0.94f;
            this.gravity = -0.01f;
            this.hasPhysics = false;
            this.lifetime = 18 + random.nextInt(16);
            this.quadSize = 0.05f + random.nextFloat() * 0.04f;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                xd += (random.nextDouble() - 0.5) * 0.006;
                zd += (random.nextDouble() - 0.5) * 0.006;
                setAlpha(stepped(1.5f - (float) age / lifetime * 1.5f));
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

    public static class Ash extends TextureSheetParticle {
        private final float Sway;

        protected Ash(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.03;
            this.yd = SpeedY + random.nextDouble() * 0.02;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.03;
            this.gravity = 0.04f;
            this.friction = 0.9f;
            this.hasPhysics = true;
            this.lifetime = 30 + random.nextInt(30);
            this.quadSize = 0.06f + random.nextFloat() * 0.05f;
            this.Sway = random.nextFloat() * Mth.TWO_PI;
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
                xd *= 0.2;
                zd *= 0.2;
                return;
            }
            float Phase = Sway + age * 0.3f;
            xd += Mth.sin(Phase) * 0.003;
            zd += Mth.cos(Phase) * 0.003;
            roll += 0.06f * Mth.sin(Phase);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Steam extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Start;

        protected Steam(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.02;
            this.yd = Math.max(0.03, SpeedY) + random.nextDouble() * 0.03;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.02;
            this.friction = 0.9f;
            this.gravity = -0.02f;
            this.hasPhysics = false;
            this.lifetime = 16 + random.nextInt(14);
            this.Start = 0.18f + random.nextFloat() * 0.12f;
            this.quadSize = Start;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed) {
                setSpriteFromAge(Sprites);
                setAlpha(stepped(1.2f - (float) age / lifetime));
            }
        }

        @Override
        public float getQuadSize(float Partial) {
            return Start * (1.0f + (age + Partial) / lifetime * 1.2f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record FlameProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flame(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, FlameLife, FlameLifeSpread, FlameSize, FlameSizeSpread, FlameDrag);
        }
    }

    public record LickProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flame(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, LickLife, LickLifeSpread, LickSize, LickSizeSpread, LickDrag);
        }
    }

    public record EmberProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Ember(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record AshProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Ash(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record SteamProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Steam(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
