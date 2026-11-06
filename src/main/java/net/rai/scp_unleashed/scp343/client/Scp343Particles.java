package net.rai.scp_unleashed.scp343.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public final class Scp343Particles {
    public static final int FullBright = 0xF000F0;
    public static final float Rise = 0.012f;

    private Scp343Particles() {
    }

    private static float stepped(float Value) {
        return Math.round(Mth.clamp(Value, 0.0f, 1.0f) * 4.0f) / 4.0f;
    }

    public static class Light extends TextureSheetParticle {
        private final SpriteSet Sprites;
        private final float Size;
        private final float Sway;

        protected Light(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
            super(World, X, Y, Z, 0.0, 0.0, 0.0);
            this.Sprites = Sprites;
            this.xd = SpeedX * 0.5;
            this.yd = Math.max(Rise, SpeedY) + random.nextFloat() * 0.01f;
            this.zd = SpeedZ * 0.5;
            this.friction = 0.94f;
            this.gravity = 0.0f;
            this.hasPhysics = false;
            this.lifetime = 22 + random.nextInt(16);
            this.Size = 0.06f + random.nextFloat() * 0.05f;
            this.quadSize = Size;
            this.Sway = random.nextFloat() * Mth.TWO_PI;
            float Warm = 0.9f + random.nextFloat() * 0.1f;
            setColor(1.0f, Warm, 0.78f + random.nextFloat() * 0.12f);
            setSpriteFromAge(Sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) {
                return;
            }
            setSpriteFromAge(Sprites);
            float Life = (float) age / lifetime;
            setAlpha(stepped(Life < 0.25f ? Life * 4.0f : 1.4f - Life * 1.4f));
            xd += Mth.sin(Sway + age * 0.2f) * 0.0015;
            yd = Math.max(yd, Rise * 0.6);
        }

        @Override
        public int getLightColor(float Partial) {
            return FullBright;
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record LightProvider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Light(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
