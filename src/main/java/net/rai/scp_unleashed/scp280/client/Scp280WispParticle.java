package net.rai.scp_unleashed.scp280.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp280WispParticle extends TextureSheetParticle {
    public static final float MaxAlpha = 0.85f;
    public static final float AlphaSteps = 6.0f;
    public static final int LightFloor = 6;
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Phase;

    protected Scp280WispParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = -0.004f;
        this.friction = 0.9f;
        this.hasPhysics = false;
        this.lifetime = 22 + random.nextInt(18);
        this.StartSize = 0.09f + random.nextFloat() * 0.09f;
        this.Phase = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        float Shade = 0.82f + random.nextFloat() * 0.18f;
        setColor(Shade, Shade, Shade);
        setAlpha(0.0f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        setSpriteFromAge(Sprites);
        float Age = (float) age / lifetime;
        float Visible = Math.min(Age * 5.0f, (1.0f - Age) * 1.8f);
        setAlpha(Math.round(Mth.clamp(Visible, 0.0f, MaxAlpha) * AlphaSteps) / AlphaSteps);
        quadSize = StartSize * (1.0f + Age * 0.5f);
        oRoll = roll;
        roll += Mth.sin(age * 0.23f + Phase) * 0.05f;
        xd += Mth.sin(age * 0.33f + Phase) * 0.0016f;
        zd += Mth.cos(age * 0.29f + Phase) * 0.0016f;
    }

    @Override
    protected int getLightColor(float PartialTick) {
        int Packed = super.getLightColor(PartialTick);
        return LightTexture.pack(Math.max(LightTexture.block(Packed), LightFloor), LightTexture.sky(Packed));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp280WispParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
