package net.rai.scp_unleashed.scp354.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp354.Scp354Fluid;
import net.rai.scp_unleashed.scp354.Scp354Registry;
import org.joml.Quaternionf;

public final class Scp354Particles {
    public static final float AlphaSteps = 4.0f;

    private Scp354Particles() {
    }

    private static float stepped(float Value) {
        return Math.round(Mth.clamp(Value, 0.0f, 1.0f) * AlphaSteps) / AlphaSteps;
    }

    public static class Bubble extends TextureSheetParticle {
        private final SpriteSet Sprites;

        protected Bubble(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = (random.nextDouble() - 0.5) * 0.004;
            this.yd = 0.012 + random.nextDouble() * 0.014;
            this.zd = (random.nextDouble() - 0.5) * 0.004;
            this.gravity = 0.0f;
            this.friction = 1.0f;
            this.hasPhysics = false;
            this.lifetime = 30 + random.nextInt(50);
            this.quadSize = 0.04f + random.nextFloat() * 0.05f;
            setSpriteFromAge(Sprites);
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
            xd += Math.sin(age * 0.35 + quadSize * 40.0) * 0.0006;
            move(xd, yd, zd);
            BlockPos Cell = BlockPos.containing(x, y, z);
            boolean Wet = Scp354Fluid.isRed(level.getFluidState(Cell));
            boolean Covered = Scp354Fluid.isRed(level.getFluidState(Cell.above()));
            if (Wet && (Covered || y < Cell.getY() + Scp354Fluid.SurfaceHeight - 0.02)) {
                setSpriteFromAge(Sprites);
                return;
            }
            BlockPos Top = Wet ? Cell : Cell.below();
            if (Scp354Fluid.isRed(level.getFluidState(Top))) {
                level.addParticle(Scp354Registry.Ripple.get(), x, Top.getY() + Scp354Fluid.SurfaceHeight + 0.012, z, 0.0, 0.0, 0.0);
            }
            remove();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Ripple extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Grow;

        protected Ripple(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = 18 + random.nextInt(14);
            this.quadSize = 0.08f;
            this.Grow = 0.25f + random.nextFloat() * 0.2f;
            setSpriteFromAge(Sprites);
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
            setSpriteFromAge(Sprites);
            setAlpha(stepped(1.2f - (float) age / lifetime));
        }

        @Override
        public float getQuadSize(float Partial) {
            float Share = Mth.clamp((age + Partial) / lifetime, 0.0f, 1.0f);
            return quadSize + Grow * (1.0f - (1.0f - Share) * (1.0f - Share));
        }

        @Override
        public void render(VertexConsumer Buffer, Camera View, float Partial) {
            renderRotatedQuad(Buffer, View, new Quaternionf().rotationX(-Mth.HALF_PI), Partial);
            renderRotatedQuad(Buffer, View, new Quaternionf().rotationX(Mth.HALF_PI), Partial);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Drip extends TextureSheetParticle {
        protected Drip(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = 0.0;
            this.yd = -0.02;
            this.zd = 0.0;
            this.gravity = 0.55f;
            this.friction = 0.98f;
            this.hasPhysics = true;
            this.lifetime = 40 + random.nextInt(20);
            this.quadSize = 0.035f + random.nextFloat() * 0.03f;
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed && (onGround || Scp354Fluid.isRed(level.getFluidState(BlockPos.containing(x, y, z))))) {
                remove();
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public static class Splash extends TextureSheetParticle {
        protected Splash(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.xd = SpeedX * 0.8 + (random.nextDouble() - 0.5) * 0.06;
            this.yd = Math.abs(SpeedY) * 0.9 + 0.1 + random.nextDouble() * 0.12;
            this.zd = SpeedZ * 0.8 + (random.nextDouble() - 0.5) * 0.06;
            this.gravity = 0.9f;
            this.friction = 0.96f;
            this.hasPhysics = true;
            this.lifetime = 14 + random.nextInt(14);
            this.quadSize = 0.05f + random.nextFloat() * 0.06f;
            pickSprite(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (!removed && (onGround || age > 3 && Scp354Fluid.isRed(level.getFluidState(BlockPos.containing(x, y, z))))) {
                remove();
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public record BubbleProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Bubble(World, X, Y, Z, Sprites);
        }
    }

    public record RippleProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Ripple(World, X, Y, Z, Sprites);
        }
    }

    public record DripProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Drip(World, X, Y, Z, Sprites);
        }
    }

    public record SplashProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Splash(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
