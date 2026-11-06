package net.rai.scp_unleashed.scp035.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp035WeepParticle extends TextureSheetParticle {
    public static final int ClingTicks = 12;
    private final float StartSize;

    protected Scp035WeepParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, 0.0, 0.0, 0.0);
        this.xd = SpeedX * 0.2;
        this.yd = Math.min(0.0, SpeedY);
        this.zd = SpeedZ * 0.2;
        this.gravity = 0.0f;
        this.friction = 0.92f;
        this.hasPhysics = true;
        this.lifetime = 34 + random.nextInt(22);
        this.StartSize = 0.028f + random.nextFloat() * 0.014f;
        this.quadSize = StartSize;
        float Shade = 0.85f + random.nextFloat() * 0.15f;
        setColor(Shade, Shade * 0.94f, Shade);
        pickSprite(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        if (age > ClingTicks) {
            gravity = 0.05f;
        } else {
            yd = -0.002;
        }
        float Age = (float) age / lifetime;
        quadSize = StartSize * (Age < 0.75f ? 1.0f : Mth.clamp((1.0f - Age) / 0.25f, 0.3f, 1.0f));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp035WeepParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
