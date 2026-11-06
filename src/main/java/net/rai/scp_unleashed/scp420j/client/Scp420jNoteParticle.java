package net.rai.scp_unleashed.scp420j.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp420jNoteParticle extends TextureSheetParticle {
    public static final float[][] Tints = {{1.0f, 0.55f, 0.62f}, {1.0f, 0.78f, 0.36f}, {0.62f, 0.92f, 0.46f}, {0.42f, 0.86f, 0.9f}, {0.78f, 0.6f, 1.0f}};
    private final float StartSize;
    private final float Sway;

    protected Scp420jNoteParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = -0.0015f;
        this.friction = 0.97f;
        this.hasPhysics = false;
        this.lifetime = 40 + random.nextInt(20);
        this.StartSize = 0.09f + random.nextFloat() * 0.03f;
        this.Sway = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        float[] Tint = Tints[random.nextInt(Tints.length)];
        setColor(Tint[0], Tint[1], Tint[2]);
        pickSprite(Sprites);
        setAlpha(0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            float Age = (float) age / lifetime;
            xd += Mth.cos(Sway + age * 0.2f) * 0.0012;
            zd += Mth.sin(Sway + age * 0.2f) * 0.0012;
            oRoll = roll;
            roll = Mth.sin(Sway + age * 0.16f) * 0.25f;
            setAlpha(Mth.clamp(Math.min(Age * 6.0f, (1.0f - Age) * 2.2f), 0.0f, 1.0f));
            quadSize = StartSize * (0.9f + 0.1f * Mth.sin(age * 0.3f + Sway));
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp420jNoteParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
