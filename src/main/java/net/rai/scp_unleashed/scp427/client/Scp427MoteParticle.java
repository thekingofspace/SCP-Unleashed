package net.rai.scp_unleashed.scp427.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp427MoteParticle extends TextureSheetParticle {
    public static final int GlowLevel = 13;
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Drift;

    protected Scp427MoteParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX * 0.3;
        this.yd = 0.012 + random.nextFloat() * 0.01;
        this.zd = SpeedZ * 0.3;
        this.gravity = -0.002f;
        this.friction = 0.96f;
        this.hasPhysics = false;
        this.lifetime = 28 + random.nextInt(20);
        this.StartSize = 0.035f + random.nextFloat() * 0.03f;
        this.quadSize = StartSize;
        this.Drift = (random.nextFloat() - 0.5f) * 0.004f;
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            xd += Drift;
            float Age = (float) age / lifetime;
            quadSize = StartSize * (Age < 0.7f ? 1.0f : Mth.clamp((1.0f - Age) / 0.3f, 0.2f, 1.0f));
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
            return new Scp427MoteParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
