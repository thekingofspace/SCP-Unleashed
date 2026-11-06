package net.rai.scp_unleashed.z0002.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public final class Z0002Particles {
    public static final int VapourFrames = 4;
    public static final int SparkFrames = 4;
    public static final int ShardFrames = 4;
    public static final int MoteFrames = 4;

    private Z0002Particles() {
    }

    public static class Vapour extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float FullSize;

        protected Vapour(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.006;
            this.yd = SpeedY;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.006;
            this.gravity = -0.002f;
            this.friction = 0.97f;
            this.hasPhysics = false;
            this.lifetime = 60 + random.nextInt(30);
            this.FullSize = 0.45f + random.nextFloat() * 0.3f;
            this.quadSize = FullSize * 0.4f;
            this.alpha = 0.0f;
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            float Life = (float) age / lifetime;
            quadSize = FullSize * (0.4f + 0.9f * Life);
            alpha = Life < 0.15f ? Life / 0.15f * 0.7f : 0.7f * (1.0f - (Life - 0.15f) / 0.85f);
            setSpriteFromAge(Sprites);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Spark extends TextureSheetParticle {
        protected Spark(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.05;
            this.yd = SpeedY + (random.nextDouble() - 0.5) * 0.05;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.05;
            this.gravity = 0.02f;
            this.friction = 0.86f;
            this.hasPhysics = false;
            this.lifetime = 6 + random.nextInt(8);
            this.quadSize = 0.05f + random.nextFloat() * 0.07f;
            setSprite(Sprites.get(random.nextInt(SparkFrames), SparkFrames - 1));
        }

        @Override
        public void tick() {
            super.tick();
            alpha = 1.0f - (float) age / lifetime * 0.6f;
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

    public static class Shard extends TextureSheetParticle {
        protected Shard(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.gravity = 0.6f;
            this.friction = 0.96f;
            this.hasPhysics = true;
            this.lifetime = 30 + random.nextInt(25);
            this.quadSize = 0.08f + random.nextFloat() * 0.1f;
            setSprite(Sprites.get(random.nextInt(ShardFrames), ShardFrames - 1));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Mote extends TextureSheetParticle {
        protected Mote(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.gravity = 0.0f;
            this.friction = 0.92f;
            this.hasPhysics = false;
            this.lifetime = 12 + random.nextInt(8);
            this.quadSize = 0.07f + random.nextFloat() * 0.06f;
            setSprite(Sprites.get(random.nextInt(MoteFrames), MoteFrames - 1));
        }

        @Override
        public void tick() {
            super.tick();
            alpha = 1.0f - (float) age / lifetime * 0.7f;
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

    public record VapourProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Vapour(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record SparkProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Spark(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record ShardProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Shard(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
