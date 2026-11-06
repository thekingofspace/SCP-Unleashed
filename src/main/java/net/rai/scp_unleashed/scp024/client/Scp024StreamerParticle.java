package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp024StreamerParticle extends TextureSheetParticle {
    public static final int[] Colours = {0xE8323C, 0xF2C232, 0x9A48DC, 0x22B8A8, 0xE8323C, 0xF2C232};
    public static final int Frames = 4;
    public static final int FadeTicks = 16;
    public static final double Flutter = -0.07;
    private final SpriteSet Sprites;
    private final float Phase;
    private final float Spin;
    private final float Sway;
    private final int Twist;
    private int Settled = -1;

    protected Scp024StreamerParticle(ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz, SpriteSet Sprites) {
        super(World, X, Y, Z);
        this.Sprites = Sprites;
        int Colour = Colours[random.nextInt(Colours.length)];
        float Shade = 0.88f + random.nextFloat() * 0.12f;
        setColor((Colour >> 16 & 255) / 255.0f * Shade, (Colour >> 8 & 255) / 255.0f * Shade, (Colour & 255) / 255.0f * Shade);
        quadSize = 0.16f + random.nextFloat() * 0.06f;
        lifetime = 100 + random.nextInt(50);
        gravity = 0.8f;
        friction = 0.96f;
        hasPhysics = true;
        xd = Dx;
        yd = Dy;
        zd = Dz;
        Phase = random.nextFloat() * Mth.TWO_PI;
        Spin = (random.nextFloat() - 0.5f) * 0.18f;
        Sway = 0.006f + random.nextFloat() * 0.006f;
        Twist = 2 + random.nextInt(3);
        roll = (random.nextFloat() - 0.5f) * 0.8f;
        oRoll = roll;
        setSprite(Sprites.get(random.nextInt(Frames), Frames - 1));
    }

    @Override
    public void tick() {
        oRoll = roll;
        super.tick();
        if (removed) {
            return;
        }
        if (onGround && Settled < 0) {
            Settled = age;
            lifetime = Math.min(lifetime, age + 40 + random.nextInt(20));
        }
        if (Settled >= 0) {
            xd = 0.0;
            zd = 0.0;
            setSprite(Sprites.get(0, Frames - 1));
        } else {
            if (yd < 0.0) {
                xd += Mth.sin(age * 0.22f + Phase) * Sway;
                zd += Mth.cos(age * 0.19f + Phase) * Sway;
                if (yd < Flutter) {
                    yd = Flutter;
                }
            }
            roll += Spin;
            setSprite(Sprites.get((age / Twist + (int) (Phase * 2.0f)) % Frames, Frames - 1));
        }
        int Remaining = lifetime - age;
        if (Remaining < FadeTicks) {
            alpha = Mth.ceil(Remaining / 4.0f) / (FadeTicks / 4.0f);
        }
    }

    @Override
    public int getLightColor(float PartialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public Provider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz) {
            return new Scp024StreamerParticle(World, X, Y, Z, Dx, Dy, Dz, Sprites);
        }
    }
}
