package net.rai.scp_unleashed.scp444.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp444GlyphParticle extends TextureSheetParticle {
    public static final float PeakAlpha = 0.7f;
    private final float StartSize;

    protected Scp444GlyphParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.xd = SpeedX * 0.9 + (random.nextDouble() - 0.5) * 0.01;
        this.yd = SpeedY * 0.9 + 0.012 + random.nextDouble() * 0.01;
        this.zd = SpeedZ * 0.9 + (random.nextDouble() - 0.5) * 0.01;
        this.gravity = 0.0f;
        this.friction = 0.92f;
        this.hasPhysics = false;
        this.lifetime = 30 + random.nextInt(24);
        this.StartSize = 0.055f + random.nextFloat() * 0.03f;
        this.quadSize = StartSize;
        float Shade = 0.88f + random.nextFloat() * 0.12f;
        setColor(Shade, Shade * 0.96f, Shade);
        setAlpha(0.0f);
        pickSprite(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            float Age = (float) age / lifetime;
            float Rise = Mth.clamp(Age / 0.2f, 0.0f, 1.0f);
            float Fall = Mth.clamp((1.0f - Age) / 0.45f, 0.0f, 1.0f);
            setAlpha(PeakAlpha * Math.min(Rise, Fall));
            quadSize = StartSize * (0.85f + 0.15f * Rise);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp444GlyphParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
