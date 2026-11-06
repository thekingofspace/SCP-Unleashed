package net.rai.scp_unleashed.fission.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class SonicRingParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float EndSize;

    protected SonicRingParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, 0.0, 0.0, 0.0);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.friction = 0.9f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = 10 + random.nextInt(5);
        this.StartSize = 0.05f;
        this.EndSize = 0.32f + random.nextFloat() * 0.12f;
        this.quadSize = StartSize;
        setColor(0.45f + random.nextFloat() * 0.1f, 0.95f, 1.0f);
        setAlpha(0.85f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float Life = (float) age / lifetime;
        quadSize = StartSize + (EndSize - StartSize) * (float) Math.sqrt(Life);
        setAlpha(0.85f * (1.0f - Life));
        setSpriteFromAge(Sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new SonicRingParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
