package net.rai.scp_unleashed.scp469.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp469FeatherParticle extends TextureSheetParticle {
    public static final int Life = 50;
    public static final int LifeSpread = 50;
    public static final float Size = 0.13f;
    public static final float SizeSpread = 0.08f;
    public static final float Fall = 0.006f;
    public static final float Drag = 0.9f;
    public static final double Sway = 0.0045;
    public static final float Spin = 0.18f;

    private final float Phase;
    private final float Rate;
    private final float Start;

    protected Scp469FeatherParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, 0.0, 0.0, 0.0);
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.friction = Drag;
        this.gravity = Fall;
        this.hasPhysics = true;
        this.lifetime = Life + random.nextInt(LifeSpread);
        this.Start = Size + random.nextFloat() * SizeSpread;
        this.quadSize = Start;
        this.Phase = random.nextFloat() * Mth.TWO_PI;
        this.Rate = 0.12f + random.nextFloat() * 0.1f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        float Tint = 0.9f + random.nextFloat() * 0.1f;
        setColor(Tint, Tint, Tint * (0.97f + random.nextFloat() * 0.03f));
        pickSprite(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        oRoll = roll;
        float Swing = Mth.sin(age * Rate + Phase);
        xd += Swing * Sway;
        zd += Mth.cos(age * Rate * 0.8f + Phase) * Sway;
        roll += Swing * Spin * (onGround ? 0.0f : 1.0f);
        if (onGround) {
            xd *= 0.5;
            zd *= 0.5;
        }
        if (yd < -0.06) {
            yd = -0.06;
        }
    }

    @Override
    public float getQuadSize(float Partial) {
        float Left = (lifetime - age - Partial) / 10.0f;
        return Start * Mth.clamp(Left, 0.0f, 1.0f);
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
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp469FeatherParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
