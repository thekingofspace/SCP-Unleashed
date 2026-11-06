package net.rai.scp_unleashed.disperser.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;

public class OrganismHazeParticle extends TextureSheetParticle {
    public static final float Drag = 0.9f;
    public static final float PeakAlpha = 0.55f;
    public static final int AlphaSteps = 8;
    public static final int MinLight = 5;

    private final SpriteSet Sprites;
    private final float StartSize;
    private final float Swirl;

    protected OrganismHazeParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites, ColorParticleOption Tint) {
        super(World, X, Y, Z, SpeedX, SpeedY, SpeedZ);
        this.Sprites = Sprites;
        this.xd = SpeedX + (random.nextDouble() - 0.5) * 0.008;
        this.yd = SpeedY;
        this.zd = SpeedZ + (random.nextDouble() - 0.5) * 0.008;
        this.gravity = 0.0f;
        this.friction = Drag;
        this.hasPhysics = true;
        this.lifetime = 34 + random.nextInt(26);
        this.StartSize = 0.16f + random.nextFloat() * 0.1f;
        this.Swirl = random.nextFloat() * Mth.TWO_PI;
        this.quadSize = StartSize;
        float Shade = 0.9f + random.nextFloat() * 0.1f;
        setColor(Shade * Tint.getRed(), Shade * Tint.getGreen(), Shade * Tint.getBlue());
        setAlpha(0.0f);
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
        float Envelope = Mth.clamp(Age * 7.0f, 0.0f, 1.0f) * Mth.clamp((1.0f - Age) * 1.8f, 0.0f, 1.0f);
        setAlpha(Math.round(PeakAlpha * Envelope * AlphaSteps) / (float) AlphaSteps);
        quadSize = StartSize * (1.0f + Age * 2.2f);
        xd += Mth.sin(age * 0.13f + Swirl) * 0.0012;
        zd += Mth.cos(age * 0.11f + Swirl) * 0.0012;
    }

    @Override
    protected int getLightColor(float PartialTick) {
        int Packed = super.getLightColor(PartialTick);
        int Glow = Math.max(Packed & 0xFF, MinLight << 4);
        return Packed & 0xFFFF0000 | Glow;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption Tint, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new OrganismHazeParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites, Tint);
        }
    }
}
