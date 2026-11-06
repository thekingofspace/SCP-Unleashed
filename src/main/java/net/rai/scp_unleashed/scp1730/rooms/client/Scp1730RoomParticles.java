package net.rai.scp_unleashed.scp1730.rooms.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp1730RoomParticles {
    public static final int[] Alive = new int[6];
    public static final int Mist = 0;
    public static final int Dust = 1;
    public static final int Confetti = 2;
    public static final int Paper = 3;
    public static final int Led = 4;
    public static final int Streamer = 5;
    public static final float[][] ConfettiColours = {{0.95f, 0.22f, 0.25f}, {1.0f, 0.82f, 0.18f}, {0.25f, 0.55f, 1.0f}, {0.3f, 0.86f, 0.38f}, {0.92f, 0.35f, 0.88f},
            {1.0f, 0.97f, 0.9f}, {1.0f, 0.55f, 0.15f}};

    private Scp1730RoomParticles() {
    }

    public static int alive(int Style) {
        return Alive[Style];
    }

    public static class RoomParticle extends TextureSheetParticle {
        private final int Style;
        private final float Peak;
        private final float Size;
        private final float Phase;
        private final boolean Glows;
        private final int MinLight;
        private final SpriteSet Frames;

        RoomParticle(ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz, SpriteSet Sprites, int Style) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Style = Style;
            this.xd = Dx;
            this.yd = Dy;
            this.zd = Dz;
            this.Phase = random.nextFloat() * Mth.TWO_PI;
            this.alpha = 0.0f;
            this.hasPhysics = false;
            this.gravity = 0.0f;
            this.friction = 1.0f;
            float Made;
            int Light = 0;
            boolean Lit = false;
            float Top;
            switch (Style) {
                case Mist -> {
                    Made = 0.4f + random.nextFloat() * 0.3f;
                    lifetime = 120 + random.nextInt(50);
                    Top = 0.05f + random.nextFloat() * 0.07f;
                    setColor(0.62f, 0.76f, 1.0f);
                    Light = 7;
                }
                case Dust -> {
                    Made = 0.025f + random.nextFloat() * 0.02f;
                    lifetime = 160 + random.nextInt(80);
                    Top = 0.3f + random.nextFloat() * 0.3f;
                    setColor(0.86f, 0.84f, 0.8f);
                    Light = 3;
                }
                case Confetti -> {
                    float Roll = random.nextFloat();
                    Made = 0.018f + Roll * Roll * Roll * 0.05f;
                    lifetime = 120 + random.nextInt(90);
                    Top = 1.0f;
                    float[] Colour = ConfettiColours[random.nextInt(ConfettiColours.length)];
                    setColor(Colour[0], Colour[1], Colour[2]);
                    hasPhysics = true;
                    gravity = 0.01f;
                    friction = 0.95f;
                    Light = 9;
                }
                case Streamer -> {
                    Made = 0.16f + random.nextFloat() * 0.12f;
                    lifetime = 220 + random.nextInt(120);
                    Top = 1.0f;
                    float[] Colour = ConfettiColours[random.nextInt(ConfettiColours.length)];
                    setColor(Colour[0], Colour[1], Colour[2]);
                    hasPhysics = true;
                    gravity = 0.004f;
                    friction = 0.97f;
                    Light = 9;
                }
                case Paper -> {
                    Made = 0.09f + random.nextFloat() * 0.05f;
                    lifetime = 140 + random.nextInt(80);
                    Top = 0.9f;
                    setColor(0.9f, 0.89f, 0.84f);
                    Light = 3;
                }
                default -> {
                    Made = 0.035f;
                    lifetime = 8 + random.nextInt(24);
                    Top = 0.95f;
                    setColor(0.35f, 0.75f, 1.0f);
                    Lit = true;
                }
            }
            this.Size = Made;
            this.quadSize = Made;
            this.Peak = Top;
            this.Glows = Lit;
            this.MinLight = Light;
            this.Frames = Sprites;
            if (Style == Streamer) {
                setSprite(Sprites.get(random.nextInt(8), 8));
            } else {
                pickSprite(Sprites);
            }
            Alive[Style]++;
        }

        @Override
        public void tick() {
            super.tick();
            float Share = age / (float) Math.max(1, lifetime);
            float Fade = Mth.clamp(Math.min(Share * 6.0f, (1.0f - Share) * 4.0f), 0.0f, 1.0f);
            alpha = Peak * Fade;
            switch (Style) {
                case Mist -> {
                    quadSize = Size * (0.9f + 0.1f * Mth.sin(age * 0.05f + Phase));
                    xd *= 0.995;
                    zd *= 0.995;
                }
                case Confetti -> {
                    if (!onGround) {
                        xd += Mth.sin(age * 0.3f + Phase) * 0.003;
                        zd += Mth.cos(age * 0.27f + Phase) * 0.003;
                        float Flip = Mth.cos(age * 0.55f + Phase);
                        float Shine = Flip > 0.6f ? 1.0f : Flip > -0.2f ? 0.78f : 0.55f;
                        alpha = Peak * Fade * (0.75f + 0.25f * Shine);
                        quadSize = Size * (0.45f + 0.55f * Math.abs(Flip));
                    } else {
                        xd *= 0.5;
                        zd *= 0.5;
                        quadSize = Size;
                    }
                }
                case Streamer -> {
                    if (!onGround) {
                        xd += Mth.sin(age * 0.07f + Phase) * 0.0018;
                        zd += Mth.cos(age * 0.06f + Phase) * 0.0018;
                        setSprite(Frames.get((age / 3 + (int) (Phase * 4.0f)) % 8, 8));
                        oRoll = roll;
                        roll += Mth.sin(age * 0.05f + Phase) * 0.03f;
                    } else {
                        xd *= 0.3;
                        zd *= 0.3;
                        oRoll = roll;
                    }
                }
                case Paper -> {
                    xd += Mth.sin(age * 0.08f + Phase) * 0.0015;
                    zd += Mth.cos(age * 0.07f + Phase) * 0.0015;
                    roll += 0.02f;
                    oRoll = roll - 0.02f;
                }
                case Led -> alpha = (age / 4 + (int) (Phase * 3)) % 3 == 0 ? Peak * 0.25f : Peak;
                default -> {
                }
            }
        }

        @Override
        public void remove() {
            if (!removed) {
                Alive[Style] = Math.max(0, Alive[Style] - 1);
            }
            super.remove();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        protected int getLightColor(float Partial) {
            if (Glows) {
                return 0xF000F0;
            }
            int Packed = super.getLightColor(Partial);
            int Block = Math.max(Packed & 0xFFFF, MinLight << 4);
            return Packed & 0xFFFF0000 | Block;
        }
    }

    public record Provider(SpriteSet Sprites, int Style) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz) {
            return new RoomParticle(World, X, Y, Z, Dx, Dy, Dz, Sprites, Style);
        }
    }
}
