package net.rai.scp_unleashed.centrifuge.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class CentrifugeGritParticle extends TextureSheetParticle {
    private final float Jitter;
    private final float Hop;

    protected CentrifugeGritParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z);
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.8f;
        this.friction = 0.9f;
        this.hasPhysics = true;
        this.lifetime = 14 + random.nextInt(16);
        this.quadSize = 0.024f + random.nextFloat() * 0.02f;
        this.Jitter = 0.008f + random.nextFloat() * 0.008f;
        this.Hop = 0.025f + random.nextFloat() * 0.025f;
        float Shade = 0.72f + random.nextFloat() * 0.28f;
        setColor(Shade, Shade, Shade * 0.96f);
        pickSprite(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        xd += (random.nextFloat() - 0.5f) * Jitter;
        zd += (random.nextFloat() - 0.5f) * Jitter;
        if (onGround && age < lifetime - 4 && random.nextFloat() < 0.35f) {
            yd = Hop * (0.5f + random.nextFloat());
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new CentrifugeGritParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
