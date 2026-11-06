package net.rai.scp_unleashed.scp427.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp427FleshParticle extends TextureSheetParticle {
    private final float StartSize;
    private final float Turn;

    protected Scp427FleshParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.xd = SpeedX;
        this.yd = SpeedY + 0.05;
        this.zd = SpeedZ;
        this.gravity = 0.7f;
        this.friction = 0.92f;
        this.hasPhysics = true;
        this.lifetime = 14 + random.nextInt(14);
        this.StartSize = 0.05f + random.nextFloat() * 0.05f;
        this.quadSize = StartSize;
        this.Turn = (random.nextFloat() - 0.5f) * 0.4f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        pickSprite(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            oRoll = roll;
            if (!onGround) {
                roll += Turn;
            }
            float Age = (float) age / lifetime;
            quadSize = StartSize * (Age < 0.75f ? 1.0f : Mth.clamp((1.0f - Age) / 0.25f, 0.3f, 1.0f));
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp427FleshParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
