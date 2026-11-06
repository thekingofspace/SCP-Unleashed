package net.rai.scp_unleashed.fission.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class GasPuffParticle extends TextureSheetParticle {
    public static final int FadeIn = 12;
    private final float BaseAlpha;
    private final float Growth;

    protected GasPuffParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.friction = 0.96f;
        this.gravity = 0.0f;
        this.hasPhysics = true;
        this.lifetime = 50 + random.nextInt(45);
        this.quadSize = 0.3f + random.nextFloat() * 0.35f;
        this.Growth = 1.004f + random.nextFloat() * 0.006f;
        float Tint = random.nextFloat();
        setColor(Mth.lerp(Tint, 0.42f, 0.66f), Mth.lerp(Tint, 0.78f, 0.86f), Mth.lerp(Tint, 0.16f, 0.3f));
        this.BaseAlpha = 0.22f + random.nextFloat() * 0.2f;
        setAlpha(0.0f);
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        pickSprite(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float Life = (float) age / lifetime;
        float In = Math.min(1.0f, (float) age / FadeIn);
        float Out = Life > 0.6f ? 1.0f - (Life - 0.6f) / 0.4f : 1.0f;
        setAlpha(BaseAlpha * In * Out);
        quadSize *= Growth;
        oRoll = roll;
        roll += 0.004f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new GasPuffParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
