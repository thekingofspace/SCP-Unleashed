package net.rai.scp_unleashed.scp018.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class BallTrailParticle extends TextureSheetParticle {
    public static final float FullAlpha = 0.42f;
    public static final float BaseSize = 0.11f;
    private final SpriteSet Sprites;
    private final float StartAlpha;
    private final float StartSize;

    protected BallTrailParticle(ClientLevel World, double X, double Y, double Z, double Strength, SpriteSet Sprites) {
        super(World, X, Y, Z, 0.0, 0.0, 0.0);
        this.Sprites = Sprites;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.hasPhysics = false;
        float Clamped = (float) Mth.clamp(Strength, 0.0, 1.0);
        this.lifetime = 5 + Math.round(Clamped * 5.0f) + random.nextInt(2);
        this.StartAlpha = FullAlpha * (0.35f + Clamped * 0.65f);
        this.StartSize = BaseSize * (0.85f + random.nextFloat() * 0.15f);
        this.quadSize = StartSize;
        float Shade = 0.88f + random.nextFloat() * 0.12f;
        setColor(Shade, Shade, Shade);
        setAlpha(StartAlpha);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            setAlpha(StartAlpha * (1.0f - Age));
            quadSize = StartSize * (1.0f - Age * 0.55f);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double Strength, double Unused, double Spare) {
            return new BallTrailParticle(World, X, Y, Z, Strength, Sprites);
        }
    }
}
