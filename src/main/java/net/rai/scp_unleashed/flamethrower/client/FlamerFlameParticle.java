package net.rai.scp_unleashed.flamethrower.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

public class FlamerFlameParticle extends TextureSheetParticle {
    public static final float FlameFriction = 0.86F;
    public static final float EmberFriction = 0.94F;
    public static final float GlowShare = 0.68F;
    public static final float FadeShare = 0.78F;
    public static final double Sink = 0.012;
    public static final double Rise = 0.022;
    private final SpriteSet Sprites;
    private final boolean Ember;
    private final float StartSize;
    private final float EndSize;

    protected FlamerFlameParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, boolean Ember) {
        super(World, X, Y, Z);
        this.Sprites = Sprites;
        this.Ember = Ember;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.hasPhysics = true;
        if (Ember) {
            this.friction = EmberFriction;
            this.gravity = 0.55F;
            this.lifetime = 8 + random.nextInt(10);
            this.StartSize = 0.03F + random.nextFloat() * 0.02F;
            this.EndSize = StartSize * 0.5F;
        } else {
            this.friction = FlameFriction;
            this.gravity = 0.0F;
            this.lifetime = 13 + random.nextInt(8);
            this.StartSize = 0.18F + random.nextFloat() * 0.08F;
            this.EndSize = 0.6F + random.nextFloat() * 0.25F;
        }
        this.quadSize = StartSize;
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float Share = (float) age / lifetime;
        if (!Ember) {
            yd += Share < 0.4F ? -Sink : Rise * (Share - 0.4F) / 0.6F;
        }
        setSpriteFromAge(Sprites);
        float Eased = Share * Share * (3.0F - 2.0F * Share);
        quadSize = StartSize + (EndSize - StartSize) * (Ember ? Share : Eased);
        setAlpha(Share < FadeShare ? 1.0F : Math.max(0.0F, 1.0F - (Share - FadeShare) / (1.0F - FadeShare)));
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return Ember || (age + PartialTick) / lifetime < GlowShare ? LightTexture.FULL_BRIGHT : super.getLightColor(PartialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record FlameProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new FlamerFlameParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, false);
        }
    }

    public record EmberProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new FlamerFlameParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, true);
        }
    }
}
