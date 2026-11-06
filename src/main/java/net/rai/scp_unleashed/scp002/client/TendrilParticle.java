package net.rai.scp_unleashed.scp002.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class TendrilParticle extends TextureSheetParticle {
    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Wobble;
    private final float Phase;

    protected TendrilParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.0f;
        this.friction = 0.96f;
        this.hasPhysics = false;
        this.lifetime = 18 + random.nextInt(14);
        this.StartSize = 0.22f + random.nextFloat() * 0.2f;
        this.Wobble = 0.012f + random.nextFloat() * 0.012f;
        this.Phase = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        float Shade = 0.8f + random.nextFloat() * 0.2f;
        setColor(Shade, Shade * 0.92f, Shade * 0.92f);
        setAlpha(0.0f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(Sprites);
            float Age = (float) age / lifetime;
            setAlpha(Mth.clamp(Math.min(Age * 5.0f, (1.0f - Age) * 2.5f), 0.0f, 0.9f));
            quadSize = StartSize * (0.7f + 0.3f * Mth.sin(Age * Mth.PI));
            oRoll = roll;
            roll += Mth.sin(age * 0.45f + Phase) * 0.08f;
            xd += Mth.sin(age * 0.6f + Phase) * Wobble;
            zd += Mth.cos(age * 0.5f + Phase) * Wobble;
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        int Packed = super.getLightColor(PartialTick);
        int Block = Math.max(Packed & 0xFF, 7 << 4);
        return Packed & 0xFFFF0000 | Block;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new TendrilParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
