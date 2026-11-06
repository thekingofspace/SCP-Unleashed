package net.rai.scp_unleashed.scp035.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp035GlintParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;

    protected Scp035GlintParticle(ClientLevel World, double X, double Y, double Z, SpriteSet Sprites) {
        super(World, X, Y, Z, 0.0, 0.0, 0.0);
        this.Sprites = Sprites;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = 10 + random.nextInt(8);
        this.StartSize = 0.03f + random.nextFloat() * 0.02f;
        this.quadSize = 0.0f;
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            quadSize = StartSize * Mth.sin(Age * Mth.PI);
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp035GlintParticle(World, X, Y, Z, Sprites);
        }
    }
}
