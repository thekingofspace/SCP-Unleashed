package net.rai.scp_unleashed.assembler.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class AssemblerSparkParticle extends TextureSheetParticle {
    private static final double DeckHeight = 1.0 / 16.0;

    private final SpriteSet Sprites;
    private final float StartSize;
    private final double FloorY;

    protected AssemblerSparkParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.9f;
        this.friction = 0.92f;
        this.hasPhysics = false;
        this.FloorY = Math.floor(Y) + DeckHeight;
        this.lifetime = 5 + random.nextInt(8);
        this.StartSize = 0.018f + random.nextFloat() * 0.016f;
        this.quadSize = StartSize;
        float Heat = random.nextFloat();
        setColor(1.0f, 0.78f + 0.22f * Heat, 0.38f + 0.5f * Heat);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed && y < FloorY) {
            setPos(x, FloorY, z);
            yd = Math.abs(yd) * 0.3;
            xd *= 0.6;
            zd *= 0.6;
        }
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Left = 1.0f - (float) age / lifetime;
            setColor(1.0f, 0.45f + 0.5f * Left, 0.12f + 0.55f * Left * Left);
            setAlpha(Math.max(0.0f, Math.min(1.0f, Left * 1.6f)));
            quadSize = StartSize * (0.45f + 0.55f * Left);
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
            return new AssemblerSparkParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
