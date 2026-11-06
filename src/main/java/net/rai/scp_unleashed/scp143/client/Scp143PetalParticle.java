package net.rai.scp_unleashed.scp143.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp143.Scp143PetalPath;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Scp143PetalParticle extends TextureSheetParticle {
    public static final int Face = 0;
    public static final int Tilt = 1;
    public static final int Edge = 2;
    public static final int Back = 3;
    private final Scp143PetalPath.Spawn Petal;
    private final SpriteSet Sprites;
    private final double[] Point = new double[3];
    private final int Shape;
    private final float SpinRate;
    private final float TumbleRate;
    private final float TumbleStart;
    private final boolean Synced;
    private float Tumble;
    private float OldTumble;
    private boolean Landed;
    private int Rest;

    public Scp143PetalParticle(ClientLevel World, Scp143PetalPath.Spawn Petal, SpriteSet Sprites, boolean Synced) {
        super(World, Petal.X(), Petal.Y(), Petal.Z());
        this.Petal = Petal;
        this.Sprites = Sprites;
        this.Synced = Synced;
        this.hasPhysics = false;
        this.gravity = 0.0f;
        this.friction = 1.0f;
        this.lifetime = Scp143PetalPath.Life + 80;
        int Seed = Petal.Seed();
        this.Shape = Scp143PetalPath.unit(Seed, 7) < 0.5 ? 0 : 1;
        this.quadSize = (float) (0.055 + 0.03 * Scp143PetalPath.unit(Seed, 8));
        this.SpinRate = (float) ((Scp143PetalPath.unit(Seed, 9) - 0.5) * 0.3);
        this.TumbleRate = (float) (0.1 + 0.14 * Scp143PetalPath.unit(Seed, 10));
        this.TumbleStart = (float) (Scp143PetalPath.unit(Seed, 11) * Math.PI * 2.0);
        this.roll = (float) (Scp143PetalPath.unit(Seed, 12) * Math.PI * 2.0);
        this.oRoll = this.roll;
        float Tint = 0.9f + 0.1f * (float) Scp143PetalPath.unit(Seed, 13);
        setColor(Tint, Tint, Tint);
        setSprite(Sprites.get(Shape * 4, 7));
        double Age = Math.max(0.0, World.getGameTime() - Petal.Born());
        Scp143PetalPath.position(Petal, Age, Point);
        setPos(Point[0], Point[1], Point[2]);
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    public int id() {
        return Petal.Id();
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        this.OldTumble = this.Tumble;
        if (Landed) {
            if (--Rest <= 0) {
                remove();
            }
            return;
        }
        double Age = level.getGameTime() - Petal.Born();
        if (Age > Scp143PetalPath.Life || ++this.age > this.lifetime) {
            remove();
            return;
        }
        Scp143PetalPath.position(Petal, Math.max(0.0, Age), Point);
        if (Scp143PetalPath.landed(level, Point[0], Point[1], Point[2])) {
            Landed = true;
            Rest = 16 + random.nextInt(16);
            setPos(Point[0], Scp143PetalPath.restHeight(level, Point[0], Point[1], Point[2]), Point[2]);
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            return;
        }
        setPos(Point[0], Point[1], Point[2]);
        this.roll += SpinRate;
        this.Tumble += TumbleRate;
    }

    @Override
    public void remove() {
        super.remove();
        if (Synced) {
            Scp143ClientState.forget(Petal.Id(), this);
        }
    }

    public void cut() {
        for (int Index = 0; Index < 3; Index++) {
            level.addParticle(ParticleTypes.CRIT, x, y, z, (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.3);
        }
        remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void render(VertexConsumer Buffer, Camera View, float Partial) {
        Vec3 Eye = View.getPosition();
        float Cx = (float) (Mth.lerp(Partial, xo, x) - Eye.x());
        float Cy = (float) (Mth.lerp(Partial, yo, y) - Eye.y());
        float Cz = (float) (Mth.lerp(Partial, zo, z) - Eye.z());
        Quaternionf Rotation = new Quaternionf();
        float Width = quadSize;
        int Frame;
        if (Landed) {
            Rotation.rotationX((float) (-Math.PI / 2.0));
            Rotation.rotateZ(roll);
            Frame = (Shape + Petal.Seed()) % 2 == 0 ? Face : Back;
        } else {
            Rotation.set(View.rotation());
            Rotation.rotateZ(Mth.lerp(Partial, oRoll, roll));
            float Phase = Mth.lerp(Partial, OldTumble, Tumble) + TumbleStart;
            float Turn = Mth.cos(Phase);
            float Open = Math.abs(Turn);
            Width = quadSize * Math.max(0.3f, Open);
            Frame = Open > 0.66f ? (Turn > 0.0f ? Face : Back) : Open > 0.33f ? Tilt : Edge;
        }
        setSprite(Sprites.get(Shape * 4 + Frame, 7));
        int Light = getLightColor(Partial);
        float U0 = getU0();
        float U1 = getU1();
        float V0 = getV0();
        float V1 = getV1();
        vertex(Buffer, Rotation, Cx, Cy, Cz, 1.0f, -1.0f, Width, quadSize, U1, V1, Light);
        vertex(Buffer, Rotation, Cx, Cy, Cz, 1.0f, 1.0f, Width, quadSize, U1, V0, Light);
        vertex(Buffer, Rotation, Cx, Cy, Cz, -1.0f, 1.0f, Width, quadSize, U0, V0, Light);
        vertex(Buffer, Rotation, Cx, Cy, Cz, -1.0f, -1.0f, Width, quadSize, U0, V1, Light);
    }

    private void vertex(VertexConsumer Buffer, Quaternionf Rotation, float Cx, float Cy, float Cz, float Sx, float Sy, float Width, float Height, float U, float V, int Light) {
        Vector3f Corner = new Vector3f(Sx * Width, Sy * Height, 0.0f).rotate(Rotation).add(Cx, Cy, Cz);
        Buffer.addVertex(Corner.x(), Corner.y(), Corner.z()).setUv(U, V).setColor(rCol, gCol, bCol, alpha).setLight(Light);
    }

    public record Provider(SpriteSet Sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType Type, ClientLevel World, double X, double Y, double Z, double SpeedX, double SpeedY, double SpeedZ) {
            Scp143PetalPath.Spawn Petal = new Scp143PetalPath.Spawn(-1, X, Y, Z, (float) SpeedX * 0.1f, (float) SpeedZ * 0.1f, 0.0f, 0.0f, World.getRandom().nextInt(),
                    World.getGameTime());
            return new Scp143PetalParticle(World, Petal, Sprites, false);
        }
    }
}
