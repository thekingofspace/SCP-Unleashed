package net.rai.scp_unleashed.guns.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class GunSparkParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected GunSparkParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, boolean Energy) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        float Tint = random.nextFloat();
        if (Energy) {
            this.gravity = -0.02F;
            this.friction = 0.86F;
            this.hasPhysics = false;
            this.lifetime = 10 + random.nextInt(14);
            this.StartSize = 0.05F + random.nextFloat() * 0.05F;
            setColor(0.35F + 0.3F * Tint, 0.95F, 0.85F + 0.15F * Tint);
        } else {
            this.gravity = 0.9F;
            this.friction = 0.92F;
            this.hasPhysics = true;
            this.lifetime = 4 + random.nextInt(6);
            this.StartSize = 0.035F + random.nextFloat() * 0.03F;
            setColor(1.0F, 0.7F + 0.25F * Tint, 0.3F + 0.3F * Tint);
        }
        this.quadSize = StartSize;
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Left = 1.0F - (float) age / lifetime;
            setAlpha(Math.max(0.0F, Left));
            quadSize = StartSize * (0.4F + 0.6F * Left);
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

    public record SparkProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new GunSparkParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, false);
        }
    }

    public record EnergyProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new GunSparkParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, true);
        }
    }
}
