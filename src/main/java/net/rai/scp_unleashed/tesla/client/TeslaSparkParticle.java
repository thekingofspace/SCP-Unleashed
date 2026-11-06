package net.rai.scp_unleashed.tesla.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class TeslaSparkParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected TeslaSparkParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.55f;
        this.friction = 0.9f;
        this.hasPhysics = true;
        this.lifetime = 6 + random.nextInt(9);
        this.StartSize = 0.04f + random.nextFloat() * 0.035f;
        this.quadSize = StartSize;
        float Tint = random.nextFloat();
        setColor(0.72f + 0.28f * Tint, 0.86f + 0.14f * Tint, 1.0f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Left = 1.0f - (float) age / lifetime;
            setAlpha(Math.max(0.0f, Left));
            quadSize = StartSize * (0.5f + 0.5f * Left);
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new TeslaSparkParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
