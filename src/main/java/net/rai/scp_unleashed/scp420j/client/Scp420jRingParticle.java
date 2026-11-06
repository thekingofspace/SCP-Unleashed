package net.rai.scp_unleashed.scp420j.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp420jRingParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Wobble;

    protected Scp420jRingParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY + 0.006;
        this.zd = SpeedZ;
        this.gravity = -0.002f;
        this.friction = 0.95f;
        this.hasPhysics = false;
        this.lifetime = 46 + random.nextInt(16);
        this.StartSize = 0.07f + random.nextFloat() * 0.02f;
        this.Wobble = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        float Grey = 0.86f + random.nextFloat() * 0.08f;
        setColor(Grey, Grey, Grey * 0.98f);
        setAlpha(0.8f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            quadSize = StartSize * (1.0f + Age * 2.6f);
            xd += Mth.sin(Wobble + age * 0.15f) * 0.0006;
            setAlpha(Mth.clamp(0.85f * (1.0f - Age * Age), 0.0f, 1.0f));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp420jRingParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
