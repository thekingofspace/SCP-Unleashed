package net.rai.scp_unleashed.scp049.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class Scp049Particle extends TextureSheetParticle {
    public static final float AlphaSteps = 5.0f;
    public static final int FullBright = LightTexture.FULL_BRIGHT;

    public enum Style {
        Miasma(32, 14, -0.0035f, 0.94f, 0.09f, 0.06f, 0.7f, 4, false, true),
        Spore(34, 18, 0.0012f, 0.96f, 0.045f, 0.02f, 1.0f, 0, false, false),
        Glint(6, 4, 0.0f, 0.86f, 0.06f, 0.03f, 1.0f, 0, true, false),
        Glow(12, 6, -0.0005f, 0.9f, 0.065f, 0.02f, 0.85f, 0, true, true),
        CureSpore(20, 12, 0.006f, 0.9f, 0.055f, 0.02f, 1.0f, 10, false, false);

        private final int Life;
        private final int LifeSpread;
        private final float Gravity;
        private final float Friction;
        private final float Size;
        private final float SizeSpread;
        private final float MaxAlpha;
        private final int LightFloor;
        private final boolean Bright;
        private final boolean Soft;

        Style(int Life, int LifeSpread, float Gravity, float Friction, float Size, float SizeSpread, float MaxAlpha, int LightFloor, boolean Bright, boolean Soft) {
            this.Life = Life;
            this.LifeSpread = LifeSpread;
            this.Gravity = Gravity;
            this.Friction = Friction;
            this.Size = Size;
            this.SizeSpread = SizeSpread;
            this.MaxAlpha = MaxAlpha;
            this.LightFloor = LightFloor;
            this.Bright = Bright;
            this.Soft = Soft;
        }
    }

    private final SpriteSet Sprites;
    private final Style Kind;
    private final float StartSize;
    private final float Phase;

    protected Scp049Particle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, Style Kind) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.Kind = Kind;
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = Kind.Gravity;
        this.friction = Kind.Friction;
        this.hasPhysics = Kind == Style.Spore || Kind == Style.CureSpore;
        this.lifetime = Kind.Life + random.nextInt(Kind.LifeSpread + 1);
        this.StartSize = Kind.Size + random.nextFloat() * Kind.SizeSpread;
        this.Phase = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        this.roll = Kind == Style.Miasma ? random.nextFloat() * Mth.TWO_PI : 0.0f;
        this.oRoll = roll;
        float Shade = 0.85f + random.nextFloat() * 0.15f;
        setColor(Shade, Shade, Shade);
        setAlpha(Kind.Soft ? 0.0f : 1.0f);
        setSpriteFromAge(Sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        setSpriteFromAge(Sprites);
        float Age = (float) age / lifetime;
        if (Kind.Soft) {
            float Visible = Kind == Style.Glow ? Mth.sin(Math.min(1.0f, Age) * Mth.PI) : Math.min(Age * 5.0f, (1.0f - Age) * 1.6f);
            setAlpha(Math.round(Mth.clamp(Visible, 0.0f, Kind.MaxAlpha) * AlphaSteps) / AlphaSteps);
        }
        if (Kind == Style.Miasma) {
            quadSize = StartSize * (1.0f + Age * 0.6f);
            oRoll = roll;
            roll += Mth.sin(age * 0.2f + Phase) * 0.03f;
            xd += Mth.sin(age * 0.3f + Phase) * 0.0012f;
            zd += Mth.cos(age * 0.27f + Phase) * 0.0012f;
        } else if (Kind == Style.Spore) {
            xd += Mth.sin(age * 0.21f + Phase) * 0.0009f;
            zd += Mth.cos(age * 0.19f + Phase) * 0.0009f;
        } else if (Kind == Style.Glint || Kind == Style.CureSpore) {
            quadSize = StartSize * (1.0f - Age * 0.5f);
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        if (Kind.Bright) {
            return FullBright;
        }
        int Packed = super.getLightColor(PartialTick);
        return LightTexture.pack(Math.max(LightTexture.block(Packed), Kind.LightFloor), LightTexture.sky(Packed));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return Kind.Soft ? ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT : ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites, Style Kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp049Particle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, Kind);
        }
    }
}
