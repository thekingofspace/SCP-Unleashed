package net.rai.scp_unleashed.storage.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class StorageMistParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float BaseAlpha;

    protected StorageMistParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.05f;
        this.friction = 0.93f;
        this.hasPhysics = false;
        this.lifetime = 30 + random.nextInt(24);
        this.StartSize = 0.07f + random.nextFloat() * 0.06f;
        this.BaseAlpha = 0.34f + random.nextFloat() * 0.14f;
        this.quadSize = StartSize;
        float Shade = 0.9f + random.nextFloat() * 0.08f;
        setColor(Shade * 0.94f, Shade * 0.98f, Math.min(1.0f, Shade + 0.04f));
        setAlpha(0.0f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            float Envelope = Mth.clamp(Age * 5.0f, 0.0f, 1.0f) * (1.0f - Age);
            setAlpha(BaseAlpha * Envelope);
            quadSize = StartSize * (1.0f + Age * 2.2f);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new StorageMistParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
