package net.rai.scp_unleashed.stranger.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class ShadowParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Phase;

    protected ShadowParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = -0.002f;
        this.friction = 0.94f;
        this.hasPhysics = false;
        this.lifetime = 22 + random.nextInt(18);
        this.StartSize = 0.12f + random.nextFloat() * 0.12f;
        this.Phase = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        float Shade = 0.85f + random.nextFloat() * 0.15f;
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
        float Visible = Math.min(Age * 6.0f, (1.0f - Age) * 2.0f);
        setAlpha(Math.round(Mth.clamp(Visible, 0.0f, 0.85f) * 6.0f) / 6.0f);
        quadSize = StartSize * (1.0f - Age * 0.45f);
        oRoll = roll;
        roll += Mth.sin(age * 0.3f + Phase) * 0.05f;
        xd += Mth.sin(age * 0.4f + Phase) * 0.002f;
        zd += Mth.cos(age * 0.35f + Phase) * 0.002f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new ShadowParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
