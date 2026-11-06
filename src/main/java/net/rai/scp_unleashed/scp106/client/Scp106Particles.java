package net.rai.scp_unleashed.scp106.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public final class Scp106Particles {
    private Scp106Particles() {
    }

    public static class Goo extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Goo(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Sprites = Sprites;
            this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.01;
            this.yd = SpeedY - random.nextDouble() * 0.01;
            this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.01;
            this.gravity = 0.55f;
            this.friction = 0.92f;
            this.hasPhysics = true;
            this.lifetime = 22 + random.nextInt(14);
            this.quadSize = 0.05f + random.nextFloat() * 0.04f;
            float Shade = 0.8f + random.nextFloat() * 0.2f;
            setColor(Shade, Shade, Shade);
            setSprite(Sprites.get(0, 3));
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            if (onGround) {
                xd = 0.0;
                zd = 0.0;
                setSprite(Sprites.get(Math.min(3, 1 + age * 3 / Math.max(1, lifetime)), 3));
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Flat extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Spin;
        private final float FullSize;
        private final int Spread;

        protected Flat(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites, float Size, int Life, int Spread) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = Life;
            this.FullSize = Size;
            this.Spread = Spread;
            this.quadSize = Spread > 0 ? Size * 0.2f : Size;
            this.Spin = random.nextInt(4) * Mth.HALF_PI;
            setSize(Size * 2.0f, 0.1f);
            setSprite(Sprites.get(0, 3));
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            if (Spread > 0 && age <= Spread) {
                quadSize = FullSize * (0.2f + 0.8f * age / Spread);
            }
            int Fade = Math.max(0, age - lifetime * 2 / 3);
            int Stage = Fade == 0 ? 0 : Math.min(3, 1 + Fade * 3 / Math.max(1, lifetime / 3));
            setSprite(Sprites.get(Stage, 3));
        }

        @Override
        public void render(VertexConsumer Buffer, Camera View, float PartialTick) {
            Quaternionf Facing = new Quaternionf().rotationY(Spin).rotateX(-Mth.HALF_PI);
            renderRotatedQuad(Buffer, View, Facing, PartialTick);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public record GooProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Goo(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public record PuddleProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flat(World, X, Y, Z, Sprites, 0.22f + World.random.nextFloat() * 0.08f, 70 + World.random.nextInt(30), 0);
        }
    }

    public record PoolProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Flat(World, X, Y, Z, Sprites, 1.3f, 120, 12);
        }
    }
}
