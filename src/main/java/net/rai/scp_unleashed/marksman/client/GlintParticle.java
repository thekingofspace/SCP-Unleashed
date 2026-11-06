package net.rai.scp_unleashed.marksman.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class GlintParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected GlintParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.0F;
        this.friction = 0.82F;
        this.hasPhysics = false;
        this.lifetime = 5 + random.nextInt(5);
        this.StartSize = 0.045F + random.nextFloat() * 0.04F;
        float Tint = random.nextFloat();
        setColor(1.0F, 0.88F + 0.12F * Tint, 0.62F + 0.3F * Tint);
        this.quadSize = StartSize;
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Left = 1.0F - (float) age / lifetime;
            setAlpha(Math.max(0.0F, 0.4F + 0.6F * Left));
            quadSize = StartSize * (0.5F + 0.5F * Left);
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
            return new GlintParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
