package net.rai.scp_unleashed.scp465.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp465Particles {
    public static final int[] Colours = {0xA23CE6, 0xF2D93A, 0xE23B3B, 0x45D94A, 0x3FA9F5, 0xF27AC4, 0xF29A3A};

    private Scp465Particles() {
    }

    private static void tint(TextureSheetParticle Particle, int Colour) {
        Particle.setColor(((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f);
    }

    public static class Confetti extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Spin;
        private final float Sway;
        private final int Frames;

        protected Confetti(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.friction = 0.93f;
            this.gravity = 0.09f;
            this.hasPhysics = true;
            this.lifetime = 70 + random.nextInt(50);
            this.quadSize = 0.045f + random.nextFloat() * 0.025f;
            this.Spin = (random.nextFloat() - 0.5f) * 0.5f;
            this.Sway = random.nextFloat() * Mth.TWO_PI;
            this.Frames = 4;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = roll;
            tint(this, Colours[random.nextInt(Colours.length)]);
            setSprite(Sprites.get(random.nextInt(Frames), Frames - 1));
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
            }
            if (yd < 0.0) {
                yd = Math.max(yd, -0.06);
                xd += Mth.sin(age * 0.25f + Sway) * 0.004;
                zd += Mth.cos(age * 0.21f + Sway) * 0.004;
            }
            setSprite(Sprites.get((age / 3 + (int) (Sway * 4)) % Frames, Frames - 1));
            if (onGround) {
                xd = 0.0;
                zd = 0.0;
            }
            if (age > lifetime - 10) {
                setAlpha((lifetime - age) / 10.0f);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class Streamer extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Spin;
        private final float Sway;
        private final int Frames;

        protected Streamer(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX;
            this.yd = SpeedY;
            this.zd = SpeedZ;
            this.friction = 0.95f;
            this.gravity = 0.05f;
            this.hasPhysics = true;
            this.lifetime = 90 + random.nextInt(60);
            this.quadSize = 0.16f + random.nextFloat() * 0.06f;
            this.Spin = (random.nextFloat() - 0.5f) * 0.12f;
            this.Sway = random.nextFloat() * Mth.TWO_PI;
            this.Frames = 4;
            this.roll = random.nextFloat() * Mth.TWO_PI;
            this.oRoll = roll;
            tint(this, Colours[random.nextInt(Colours.length)]);
            setSprite(Sprites.get(random.nextInt(Frames), Frames - 1));
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
            }
            if (yd < 0.0) {
                yd = Math.max(yd, -0.04);
                xd += Mth.sin(age * 0.12f + Sway) * 0.003;
                zd += Mth.cos(age * 0.1f + Sway) * 0.003;
            }
            setSprite(Sprites.get((age / 5 + (int) (Sway * 3)) % Frames, Frames - 1));
            if (onGround) {
                xd = 0.0;
                zd = 0.0;
            }
            if (age > lifetime - 12) {
                setAlpha((lifetime - age) / 12.0f);
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public static class ConfettiProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public ConfettiProvider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Confetti(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }

    public static class StreamerProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public StreamerProvider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Streamer(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
