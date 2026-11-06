package net.rai.scp_unleashed.scp244.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp244VapourParticle extends TextureSheetParticle {
    public static final float PeakAlpha = 0.5f;
    private final float StartSize;
    private final float EndSize;
    private final float Turn;

    protected Scp244VapourParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.012f;
        this.friction = 0.9f;
        this.hasPhysics = true;
        this.lifetime = 40 + random.nextInt(36);
        this.StartSize = 0.1f + random.nextFloat() * 0.05f;
        this.EndSize = 0.3f + random.nextFloat() * 0.15f;
        this.Turn = (random.nextFloat() - 0.5f) * 0.04f;
        this.quadSize = StartSize;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        float Shade = 0.84f + random.nextFloat() * 0.1f;
        setColor(Shade, Shade + 0.03f, Shade + 0.06f);
        setAlpha(0.0f);
        pickSprite(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float Age = (float) age / lifetime;
        quadSize = Mth.lerp((float) Math.sqrt(Age), StartSize, EndSize);
        oRoll = roll;
        roll += Turn;
        float In = Mth.clamp(Age / 0.15f, 0.0f, 1.0f);
        float Out = Mth.clamp((1.0f - Age) / 0.45f, 0.0f, 1.0f);
        setAlpha(PeakAlpha * Math.min(In, Out));
        if (onGround) {
            xd *= 1.08;
            zd *= 1.08;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp244VapourParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
