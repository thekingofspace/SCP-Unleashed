package net.rai.scp_unleashed.scp439.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class Scp439FlakeParticle extends TextureSheetParticle {
    private final float Spin;

    protected Scp439FlakeParticle(ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz, SpriteSet Sprites) {
        super(World, X, Y, Z, Dx, Dy, Dz);
        pickSprite(Sprites);
        xd = Dx + (random.nextDouble() - 0.5) * 0.03;
        yd = Dy + random.nextDouble() * 0.02;
        zd = Dz + (random.nextDouble() - 0.5) * 0.03;
        gravity = 0.25f;
        friction = 0.92f;
        lifetime = 24 + random.nextInt(24);
        quadSize = 0.045f + random.nextFloat() * 0.04f;
        Spin = (random.nextFloat() - 0.5f) * 0.3f;
        roll = random.nextFloat() * 6.2831855f;
        oRoll = roll;
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        if (!onGround) {
            roll += Spin;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public Provider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double Dx, double Dy, double Dz) {
            return new Scp439FlakeParticle(World, X, Y, Z, Dx, Dy, Dz, Sprites);
        }
    }
}
