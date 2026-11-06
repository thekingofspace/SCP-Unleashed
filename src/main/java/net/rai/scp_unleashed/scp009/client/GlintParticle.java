package net.rai.scp_unleashed.scp009.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class GlintParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected GlintParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.012f;
        this.friction = 0.9f;
        this.hasPhysics = true;
        this.lifetime = 14 + random.nextInt(14);
        this.StartSize = 0.05f + random.nextFloat() * 0.035f;
        this.quadSize = StartSize;
        float Shade = 0.85f + random.nextFloat() * 0.15f;
        setColor(Shade, Shade, Shade);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            quadSize = StartSize * (Age < 0.7f ? 1.0f : Mth.clamp((1.0f - Age) / 0.3f, 0.35f, 1.0f));
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new GlintParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
