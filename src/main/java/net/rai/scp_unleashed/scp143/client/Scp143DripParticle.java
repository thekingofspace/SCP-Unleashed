package net.rai.scp_unleashed.scp143.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class Scp143DripParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected Scp143DripParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, 0.0, 0.0, 0.0);
        this.Sprites = Sprites;
        this.xd = (random.nextDouble() - 0.5) * 0.02;
        this.yd = -0.01 - random.nextDouble() * 0.02;
        this.zd = (random.nextDouble() - 0.5) * 0.02;
        this.gravity = 0.45f;
        this.friction = 0.92f;
        this.hasPhysics = true;
        this.lifetime = 16 + random.nextInt(10);
        this.quadSize = 0.045f + random.nextFloat() * 0.025f;
        this.StartSize = quadSize;
        float Shade = 0.88f + random.nextFloat() * 0.12f;
        setColor(Shade, Shade, Shade);
        setSprite(Sprites.get(0, 3));
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        if (onGround) {
            setSprite(Sprites.get(3, 3));
            xd = 0.0;
            zd = 0.0;
            quadSize = StartSize * 1.2f;
            if (age < lifetime - 6) {
                age = lifetime - 6;
            }
            return;
        }
        float Fraction = (float) age / lifetime;
        setSprite(Sprites.get(Fraction < 0.4f ? 0 : Fraction < 0.75f ? 1 : 2, 3));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp143DripParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
