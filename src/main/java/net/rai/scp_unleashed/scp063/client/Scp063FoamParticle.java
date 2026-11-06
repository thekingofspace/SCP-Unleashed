package net.rai.scp_unleashed.scp063.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class Scp063FoamParticle extends TextureSheetParticle {
    public static final float Gravity = 0.55f;
    public static final float Drag = 0.9f;
    public static final int ShortestLife = 9;
    public static final int LifeSpread = 7;
    public static final float SmallestSize = 0.022f;
    public static final float SizeSpread = 0.016f;
    private final SpriteSet Sprites;

    protected Scp063FoamParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY + 0.01;
        this.zd = SpeedZ;
        this.gravity = Gravity;
        this.friction = Drag;
        this.hasPhysics = true;
        this.lifetime = ShortestLife + random.nextInt(LifeSpread);
        this.quadSize = SmallestSize + random.nextFloat() * SizeSpread;
        float Shade = 0.92f + random.nextFloat() * 0.08f;
        setColor(Shade, Shade, Math.min(1.0f, Shade + 0.04f));
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp063FoamParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
