package net.rai.scp_unleashed.scp006.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp006GlintParticle extends TextureSheetParticle {
    public static final int FullBright = 0xF000F0;
    public static final int MinLife = 14;
    public static final int ExtraLife = 12;
    public static final float Peak = 0.85f;
    private final SpriteSet Sprites;

    public Scp006GlintParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z);
        this.Sprites = Sprites;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.friction = 0.9f;
        this.xd = SpeedX * 0.5;
        this.yd = SpeedY * 0.5 + 0.004;
        this.zd = SpeedZ * 0.5;
        this.lifetime = MinLife + this.random.nextInt(ExtraLife);
        this.quadSize = 0.045f + this.random.nextFloat() * 0.035f;
        float Shade = 0.9f + this.random.nextFloat() * 0.1f;
        setColor(Shade * 0.9f, Shade * 0.96f, Shade);
        setAlpha(0.0f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            setSpriteFromAge(Sprites);
            float Life = (float) this.age / this.lifetime;
            setAlpha(Peak * Mth.sin(Life * Mth.PI));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float PartialTick) {
        return FullBright;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet Sprites;

        public Provider(SpriteSet Sprites) {
            this.Sprites = Sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp006GlintParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
