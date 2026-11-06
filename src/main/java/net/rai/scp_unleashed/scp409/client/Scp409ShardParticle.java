package net.rai.scp_unleashed.scp409.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp409ShardParticle extends TextureSheetParticle {
    public static final int GlowLevel = 11;
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Turn;

    protected Scp409ShardParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY + 0.04;
        this.zd = SpeedZ;
        this.gravity = 0.55f;
        this.friction = 0.94f;
        this.hasPhysics = true;
        this.lifetime = 18 + random.nextInt(22);
        this.StartSize = 0.045f + random.nextFloat() * 0.05f;
        this.quadSize = StartSize;
        this.Turn = (random.nextFloat() - 0.5f) * 0.5f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        float Shade = 0.85f + random.nextFloat() * 0.15f;
        setColor(Shade, Shade, Shade);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            oRoll = roll;
            if (!onGround) {
                roll += Turn;
            }
            float Age = (float) age / lifetime;
            quadSize = StartSize * (Age < 0.75f ? 1.0f : Mth.clamp((1.0f - Age) / 0.25f, 0.3f, 1.0f));
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        int Light = super.getLightColor(PartialTick);
        return LightTexture.pack(Math.max(LightTexture.block(Light), GlowLevel), LightTexture.sky(Light));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp409ShardParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
