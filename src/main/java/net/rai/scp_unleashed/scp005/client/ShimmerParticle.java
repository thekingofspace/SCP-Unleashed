package net.rai.scp_unleashed.scp005.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class ShimmerParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Hue;

    protected ShimmerParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY + 0.004;
        this.zd = SpeedZ;
        this.gravity = -0.006f;
        this.friction = 0.86f;
        this.hasPhysics = false;
        this.lifetime = 10 + random.nextInt(9);
        this.StartSize = 0.045f + random.nextFloat() * 0.035f;
        this.Hue = random.nextFloat();
        this.quadSize = StartSize;
        tint(0.0f);
        setSpriteFromAge(Sprites);
    }

    private void tint(float Age) {
        float Drift = Mth.clamp(Hue * 0.6f + Age * 0.5f, 0.0f, 1.0f);
        setColor(Mth.lerp(Drift, 0.86f, 0.80f), Mth.lerp(Drift, 0.97f, 0.84f), 1.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            tint(Age);
            setAlpha(Mth.clamp(1.4f - Age * 1.4f, 0.0f, 1.0f));
            quadSize = StartSize * (1.0f - Age * 0.4f);
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
            return new ShimmerParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
