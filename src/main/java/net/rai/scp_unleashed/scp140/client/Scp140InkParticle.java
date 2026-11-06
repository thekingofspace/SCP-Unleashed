package net.rai.scp_unleashed.scp140.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp140InkParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected Scp140InkParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.01;
        this.yd = SpeedY + 0.008 + random.nextDouble() * 0.012;
        this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.01;
        this.gravity = -0.004f;
        this.friction = 0.92f;
        this.hasPhysics = false;
        this.lifetime = 22 + random.nextInt(18);
        this.StartSize = 0.045f + random.nextFloat() * 0.035f;
        this.quadSize = StartSize;
        float Shade = 0.8f + random.nextFloat() * 0.2f;
        setColor(Shade, Shade, Shade);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            quadSize = StartSize * Mth.clamp((1.0f - Age) / 0.45f, 0.3f, 1.0f);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp140InkParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
