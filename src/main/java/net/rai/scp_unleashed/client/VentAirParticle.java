package net.rai.scp_unleashed.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class VentAirParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float BaseAlpha;

    protected VentAirParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.friction = 0.86f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = 10 + random.nextInt(8);
        this.quadSize = 0.06f + random.nextFloat() * 0.05f;
        float Shade = 0.92f + random.nextFloat() * 0.08f;
        setColor(Shade, Shade, Math.min(1.0f, Shade + 0.03f));
        this.BaseAlpha = 0.55f + random.nextFloat() * 0.15f;
        setAlpha(BaseAlpha);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            setAlpha(BaseAlpha * (1.0f - (float) age / lifetime));
            quadSize *= 1.04f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new VentAirParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
