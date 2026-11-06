package net.rai.scp_unleashed.rift.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftParticleOptions;

public class RiftParticle extends TextureSheetParticle {
    public static final int MoteFrames = 4;

    private final SpriteSet Sprites;
    private final Style Look;
    private final int Anchor;
    private final float BaseSize;
    private final float BaseAlpha;
    private final float Twinkle;

    protected RiftParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, RiftParticleOptions Options,
                           SpriteSet Sprites, Style Look) {
        super(World, X, Y, Z);
        this.Sprites = Sprites;
        this.Look = Look;
        this.Anchor = Options.Anchor();
        xd = SpeedX;
        yd = SpeedY;
        zd = SpeedZ;
        hasPhysics = false;
        gravity = 0.0f;
        Twinkle = random.nextFloat() * 6.0f;
        int Colour = Options.Colour();
        float Lift = Look == Style.Mote ? 0.25f + random.nextFloat() * 0.45f : 0.55f + random.nextFloat() * 0.3f;
        setColor(Mth.lerp(Lift, ((Colour >> 16) & 255) / 255.0f, 1.0f), Mth.lerp(Lift, ((Colour >> 8) & 255) / 255.0f, 1.0f),
                Mth.lerp(Lift, (Colour & 255) / 255.0f, 1.0f));
        switch (Look) {
            case Mote -> {
                lifetime = 60 + random.nextInt(40);
                friction = 0.93f;
                BaseSize = (0.05f + random.nextFloat() * 0.06f) * Options.Scale();
                BaseAlpha = 0.75f + random.nextFloat() * 0.25f;
            }
            case Spark -> {
                lifetime = 5 + random.nextInt(7);
                friction = 0.82f;
                BaseSize = (0.06f + random.nextFloat() * 0.05f) * Options.Scale();
                BaseAlpha = 1.0f;
            }
            default -> {
                lifetime = 10;
                friction = 1.0f;
                BaseSize = 0.3f * Options.Scale();
                BaseAlpha = 0.95f;
            }
        }
        quadSize = BaseSize;
        setAlpha(Look == Style.Mote ? 0.0f : BaseAlpha);
        if (Look == Style.Mote) {
            setSprite(Sprites.get(0, MoteFrames - 1));
        } else {
            setSpriteFromAge(Sprites);
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float Life = (float) age / lifetime;
        switch (Look) {
            case Mote -> tickMote(Life);
            case Spark -> {
                setSpriteFromAge(Sprites);
                quadSize = BaseSize * (1.0f - 0.6f * Life);
                setAlpha(BaseAlpha * (1.0f - Life * Life));
            }
            default -> {
                setSpriteFromAge(Sprites);
                quadSize = BaseSize * (1.0f + 8.0f * (1.0f - (1.0f - Life) * (1.0f - Life)));
                setAlpha(BaseAlpha * (1.0f - Life));
            }
        }
        move(xd, yd, zd);
        xd *= friction;
        yd *= friction;
        zd *= friction;
    }

    private void tickMote(float Life) {
        setSprite(Sprites.get((age / 3 + (int) Twinkle) % MoteFrames, MoteFrames - 1));
        float Fade = Math.min(1.0f, age / 6.0f) * Math.min(1.0f, (lifetime - age) / 12.0f);
        Entity Target = Anchor >= 0 ? level.getEntity(Anchor) : null;
        if (Target instanceof RiftEntity Rift && !Rift.isRemoved()) {
            Vec3 Center = Rift.getCenter(1.0f);
            double DeltaX = Center.x - x;
            double DeltaY = Center.y - y;
            double DeltaZ = Center.z - z;
            double Distance = Math.sqrt(DeltaX * DeltaX + DeltaY * DeltaY + DeltaZ * DeltaZ);
            double Swallow = Rift.getRiftHeight() * 0.09;
            if (Distance < Swallow) {
                remove();
                return;
            }
            double Pull = (0.0035 + 0.022 * Rift.getShownExtraction(1.0f) + 0.06 * Rift.getCollapse(1.0f)) / Math.max(Distance, 0.3);
            xd += DeltaX * Pull - DeltaZ * Pull * 0.35;
            yd += DeltaY * Pull;
            zd += DeltaZ * Pull + DeltaX * Pull * 0.35;
            Fade *= (float) Mth.clamp(Distance / (Swallow * 3.0), 0.0, 1.0);
            quadSize = BaseSize * (float) Mth.clamp(0.35 + Distance / Rift.getRiftHeight(), 0.35, 1.0);
        } else {
            quadSize = BaseSize * (1.0f - 0.5f * Life);
            if (Anchor >= 0 && age < lifetime - 10) {
                lifetime = age + 10;
            }
        }
        setAlpha(BaseAlpha * Fade * (0.75f + 0.25f * Mth.sin(age * 0.9f + Twinkle)));
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public enum Style {
        Mote,
        Spark,
        Shockwave
    }

    public record Provider(SpriteSet Sprites, Style Look) implements ParticleProvider<RiftParticleOptions> {
        @Override
        public Particle createParticle(RiftParticleOptions Options, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new RiftParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Options, Sprites, Look);
        }
    }
}
