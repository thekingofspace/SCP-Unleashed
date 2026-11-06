package net.rai.scp_unleashed.scp335.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class Scp335PixelParticle extends TextureSheetParticle {
    public static final int Life = 12;
    public static final int FullBright = 0xF000F0;
    public static final int[] Palette = {0xFF2A6A, 0x2AE8FF, 0xFFE03A, 0x4CFF5A, 0x6A4CFF, 0xFF8A1E, 0xE84CFF, 0x3A8CFF, 0xFFFFFF, 0xFF3A3A};

    protected Scp335PixelParticle(ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ, SpriteSet Sprites) {
        super(World, X, Y, Z, 0.0, 0.0, 0.0);
        this.xd = SpeedX;
        this.yd = SpeedY;
        this.zd = SpeedZ;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.hasPhysics = false;
        this.lifetime = Life;
        boolean Bright = random.nextInt(9) == 0;
        this.quadSize = Bright ? 0.07f : 0.035f + random.nextFloat() * 0.025f;
        int Colour = Bright ? 0xFFFFFF : pick(random);
        setColor(((Colour >> 16) & 0xFF) / 255.0f, ((Colour >> 8) & 0xFF) / 255.0f, (Colour & 0xFF) / 255.0f);
        pickSprite(Sprites);
    }

    public static int pick(RandomSource Random) {
        return Palette[Random.nextInt(Palette.length)];
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed && age > lifetime - 3 && random.nextBoolean()) {
            quadSize *= 0.7f;
        }
    }

    @Override
    protected int getLightColor(float PartialTick) {
        return FullBright;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            return new Scp335PixelParticle(World, X, Y, Z, SpeedX, SpeedY, SpeedZ, Sprites);
        }
    }
}
