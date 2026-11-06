package net.rai.scp_unleashed.ultralaser.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class UltralaserMoteParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected UltralaserMoteParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, boolean Pink) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = -0.004F;
        this.friction = 0.9F;
        this.hasPhysics = false;
        this.lifetime = 24 + random.nextInt(26);
        this.StartSize = 0.06F + random.nextFloat() * 0.07F;
        this.quadSize = StartSize;
        float Tint = random.nextFloat();
        if (Pink) {
            setColor(1.0F, 0.42F + 0.25F * Tint, 0.72F + 0.2F * Tint);
        } else {
            setColor(0.3F + 0.35F * Tint, 0.95F, 0.86F + 0.14F * Tint);
        }
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Left = 1.0F - (float) age / lifetime;
            setAlpha(Math.min(1.0F, Left * 1.6F));
            quadSize = StartSize * (0.35F + 0.65F * Left);
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

    public record TealProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new UltralaserMoteParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, false);
        }
    }

    public record PinkProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new UltralaserMoteParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, true);
        }
    }
}
