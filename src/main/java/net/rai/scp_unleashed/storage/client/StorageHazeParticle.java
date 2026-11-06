package net.rai.scp_unleashed.storage.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class StorageHazeParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float BaseAlpha;
    private final float Phase;

    protected StorageHazeParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = -0.03f;
        this.friction = 0.9f;
        this.hasPhysics = false;
        this.lifetime = 22 + random.nextInt(14);
        this.StartSize = 0.08f + random.nextFloat() * 0.05f;
        this.BaseAlpha = 0.16f + random.nextFloat() * 0.08f;
        this.Phase = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        setColor(1.0f, 0.82f + random.nextFloat() * 0.06f, 0.62f);
        setAlpha(0.0f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            float Envelope = Mth.clamp(Age * 4.0f, 0.0f, 1.0f) * (1.0f - Age);
            setAlpha(BaseAlpha * Envelope);
            quadSize = StartSize * (1.0f + Age * 1.4f);
            xd += Mth.sin(Phase + age * 0.45f) * 0.0012f;
            zd += Mth.cos(Phase + age * 0.4f) * 0.0012f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new StorageHazeParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
