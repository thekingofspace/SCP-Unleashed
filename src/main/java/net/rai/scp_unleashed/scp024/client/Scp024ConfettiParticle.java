package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp024ConfettiParticle extends TextureSheetParticle {
    public static final int[] Colours = {0xFF4A4A, 0xFFD23C, 0x4CFF6A, 0x4CC8FF, 0xC46AFF, 0xFF6AC8, 0xFFFFFF, 0xFF9A3C};
    private final float Spin;
    private final float Sway;
    private final float Phase;

    protected Scp024ConfettiParticle(ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz, SpriteSet Sprites) {
        super(World, X, Y, Z);
        pickSprite(Sprites);
        int Colour = Colours[random.nextInt(Colours.length)];
        setColor((Colour >> 16 & 255) / 255.0f, (Colour >> 8 & 255) / 255.0f, (Colour & 255) / 255.0f);
        quadSize = 0.06f + random.nextFloat() * 0.04f;
        lifetime = 50 + random.nextInt(40);
        gravity = 0.06f;
        friction = 0.92f;
        xd = Dx + (random.nextDouble() - 0.5) * 0.22;
        yd = Dy + random.nextDouble() * 0.12;
        zd = Dz + (random.nextDouble() - 0.5) * 0.22;
        Spin = (random.nextFloat() - 0.5f) * 0.4f;
        Sway = 0.012f + random.nextFloat() * 0.012f;
        Phase = random.nextFloat() * Mth.TWO_PI;
        roll = random.nextFloat() * Mth.TWO_PI;
        oRoll = roll;
    }

    @Override
    public void tick() {
        oRoll = roll;
        roll += Spin;
        super.tick();
        float Wobble = Mth.sin(age * 0.3f + Phase);
        xd += Wobble * Sway;
        zd += Mth.cos(age * 0.27f + Phase) * Sway;
        if (yd < -0.08) {
            yd = -0.08;
        }
    }

    @Override
    public int getLightColor(float PartialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public Provider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz) {
            return new Scp024ConfettiParticle(World, X, Y, Z, Dx, Dy, Dz, Sprites);
        }
    }
}
