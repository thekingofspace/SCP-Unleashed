package net.rai.scp_unleashed.steam.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class SteamPlumeParticle extends TextureSheetParticle {
    public static final int FadeIn = 6;
    public static final float Lift = 0.0035f;
    public static final float MaxRise = 0.16f;

    private final SpriteSet Sprites;
    private final float BaseAlpha;
    private final float Growth;

    protected SteamPlumeParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.friction = 0.95f;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = 36 + random.nextInt(34);
        this.quadSize = 0.22f + random.nextFloat() * 0.2f;
        this.Growth = 1.012f + random.nextFloat() * 0.01f;
        float Tint = 0.88f + random.nextFloat() * 0.12f;
        setColor(Tint, Tint, Math.min(1.0f, Tint + 0.03f));
        this.BaseAlpha = 0.5f + random.nextFloat() * 0.2f;
        setAlpha(0.0f);
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        yd = Math.min(MaxRise, yd + Lift);
        float Life = (float) age / lifetime;
        float In = Math.min(1.0f, (float) age / FadeIn);
        float Out = Life > 0.45f ? 1.0f - (Life - 0.45f) / 0.55f : 1.0f;
        setAlpha(BaseAlpha * In * Out);
        quadSize *= Growth;
        oRoll = roll;
        roll += 0.01f;
        setSpriteFromAge(Sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new SteamPlumeParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
