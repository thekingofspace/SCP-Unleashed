package net.rai.scp_unleashed.scp020.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp020.Scp020SporeMemory;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public final class Scp020SporeParticles {
    public static final int MoteGlow = 9;
    public static final int FilmGlow = 6;
    public static final int MoteFrames = 4;
    public static final int FilmLifetime = 1200;
    public static final float FilmAlpha = 0.4f;
    public static final float FilmPulse = 0.06f;
    public static final float FilmPulseRate = 0.05f;
    public static final float MoteGravity = -0.01f;
    public static final float MoteFriction = 0.95f;
    public static final int MoteLife = 34;
    public static final int MoteLifeRange = 30;
    public static final float MoteSize = 0.04f;
    public static final float MoteSizeRange = 0.035f;
    public static final float MotePeak = 0.6f;
    public static final float MotePeakRange = 0.3f;
    public static final double MoteWander = 0.0016;
    public static final float[][] MoteShades = {{0.80f, 0.90f, 0.74f}, {0.86f, 0.88f, 0.82f}, {0.72f, 0.84f, 0.66f}, {0.90f, 0.94f, 0.86f}};

    private Scp020SporeParticles() {
    }

    private static int glow(int Packed, int Floor) {
        return LightTexture.pack(Math.max(LightTexture.block(Packed), Floor), LightTexture.sky(Packed));
    }

    public static class Mote extends TextureSheetParticle {
        @Nullable
        private final Scp020SporeMemory.Face Patch;
        private final float Peak;
        private final int Frame;

        public Mote(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, @Nullable Scp020SporeMemory.Face Patch) {
            super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
            this.Patch = Patch;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.gravity = MoteGravity;
            this.friction = MoteFriction;
            this.hasPhysics = false;
            this.lifetime = MoteLife + random.nextInt(MoteLifeRange);
            this.quadSize = MoteSize + random.nextFloat() * MoteSizeRange;
            this.Peak = MotePeak + random.nextFloat() * MotePeakRange;
            this.Frame = random.nextInt(MoteFrames);
            float[] Shade = MoteShades[random.nextInt(MoteShades.length)];
            setColor(Shade[0], Shade[1], Shade[2]);
            setAlpha(0.0f);
            setSprite(Sprites.get(Frame, MoteFrames - 1));
        }

        @Override
        public void tick() {
            if (!Scp020SporeSight.shown() || Patch != null && !Scp020SporeSight.present(Patch)) {
                remove();
                return;
            }
            super.tick();
            if (removed) {
                return;
            }
            float Age = (float) age / lifetime;
            float Fade = Mth.clamp(Math.min(Age * 5.0f, (1.0f - Age) * 2.2f), 0.0f, 1.0f) * Peak;
            setAlpha(Math.round(Fade * 4.0f) / 4.0f);
            xd += (random.nextDouble() - 0.5) * MoteWander;
            yd += (random.nextDouble() - 0.5) * MoteWander * 0.6;
            zd += (random.nextDouble() - 0.5) * MoteWander;
        }

        @Override
        public void render(VertexConsumer Buffer, Camera View, float Partial) {
            if (Scp020SporeSight.shown()) {
                super.render(Buffer, View, Partial);
            }
        }

        @Override
        protected int getLightColor(float Partial) {
            return glow(super.getLightColor(Partial), MoteGlow);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Film extends TextureSheetParticle {
        private final Scp020SporeMemory.Face Patch;
        private final Quaternionf Facing;
        private final int Variant;

        public Film(ClientLevel World, Scp020SporeMemory.Face Patch, int Variant, double Lift, SpriteSet Sprites) {
            super(World, Patch.centre(Lift).x, Patch.centre(Lift).y, Patch.centre(Lift).z);
            this.Patch = Patch;
            this.Variant = Variant;
            this.Facing = facing(Patch.Side());
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = FilmLifetime;
            this.quadSize = 0.5f;
            setColor(1.0f, 1.0f, 1.0f);
            setAlpha(shade());
            setSprite(Sprites.get(Scp020SporeMemory.sprite(Patch, Variant), Scp020SporeMemory.SpriteCount - 1));
        }

        public static Quaternionf facing(Direction Side) {
            Direction Rightward = Scp020SporeMemory.right(Side);
            Direction Upward = Scp020SporeMemory.up(Side);
            Matrix3f Basis = new Matrix3f(Rightward.getStepX(), Rightward.getStepY(), Rightward.getStepZ(), Upward.getStepX(), Upward.getStepY(), Upward.getStepZ(),
                    Side.getStepX(), Side.getStepY(), Side.getStepZ());
            return new Quaternionf().setFromNormalized(Basis);
        }

        private float shade() {
            float Pulse = 1.0f + FilmPulse * Mth.sin(level.getGameTime() * FilmPulseRate);
            return Math.round(FilmAlpha * Scp020SporeSight.reveal() * Pulse * 16.0f) / 16.0f;
        }

        public int variant() {
            return Variant;
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (!Scp020SporeSight.keeps(Patch, this)) {
                remove();
                return;
            }
            setAlpha(shade());
        }

        @Override
        public void render(VertexConsumer Buffer, Camera View, float Partial) {
            if (Scp020SporeSight.shown()) {
                renderRotatedQuad(Buffer, View, Facing, Partial);
            }
        }

        @Override
        public AABB getRenderBoundingBox(float Partial) {
            return new AABB(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5);
        }

        @Override
        protected int getLightColor(float Partial) {
            return glow(super.getLightColor(Partial), FilmGlow);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record MoteProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Mote(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, null);
        }
    }

    public record FilmProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return null;
        }
    }
}
