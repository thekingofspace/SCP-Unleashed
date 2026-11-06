package net.rai.scp_unleashed.scp002.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class EmberParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Heat;

    protected EmberParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = -0.02f;
        this.friction = 0.9f;
        this.hasPhysics = false;
        this.lifetime = 14 + random.nextInt(20);
        this.StartSize = 0.12f + random.nextFloat() * 0.16f;
        this.Heat = 0.75f + random.nextFloat() * 0.25f;
        this.quadSize = StartSize;
        tint(0.0f);
        setSpriteFromAge(Sprites);
    }

    private void tint(float Age) {
        float Cool = Mth.clamp(Age * (1.3f - Heat * 0.4f), 0.0f, 1.0f);
        float Red = 1.0f;
        float Green = Mth.lerp(Cool, 0.92f, 0.28f);
        float Blue = Mth.lerp(Cool, 0.62f, 0.08f);
        setColor(Red, Green, Blue);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            tint(Age);
            setAlpha(Mth.clamp(1.15f - Age, 0.0f, 1.0f));
            quadSize = StartSize * (1.0f - Age * 0.65f);
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
            return new EmberParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
