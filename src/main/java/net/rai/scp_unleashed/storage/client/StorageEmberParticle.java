package net.rai.scp_unleashed.storage.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class StorageEmberParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Heat;
    private final float Flicker;

    protected StorageEmberParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = -0.05f;
        this.friction = 0.92f;
        this.hasPhysics = false;
        this.lifetime = 18 + random.nextInt(18);
        this.StartSize = 0.035f + random.nextFloat() * 0.03f;
        this.Heat = 0.7f + random.nextFloat() * 0.3f;
        this.Flicker = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        tint(0.0f);
        setSpriteFromAge(Sprites);
    }

    private void tint(float Age) {
        float Cool = Mth.clamp(Age * (1.25f - Heat * 0.35f), 0.0f, 1.0f);
        setColor(1.0f, Mth.lerp(Cool, 0.86f, 0.32f), Mth.lerp(Cool, 0.5f, 0.08f));
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            tint(Age);
            float Wobble = 0.75f + 0.25f * Mth.sin(Flicker + age * 0.9f);
            setAlpha(Mth.clamp((1.1f - Age) * Wobble, 0.0f, 1.0f));
            quadSize = StartSize * (1.0f - Age * 0.6f);
            xd += (random.nextFloat() - 0.5f) * 0.0025f;
            zd += (random.nextFloat() - 0.5f) * 0.0025f;
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
            return new StorageEmberParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
