package net.rai.scp_unleashed.scp1730.rooms.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730EchoTrack;
import org.jetbrains.annotations.Nullable;

public final class Scp1730EchoClient {
    public static final float StepEvery = 1.7f;
    public static final int Shade = 14;
    public static final int Opacity = 165;
    private static final Scp1730EchoTrack Track = new Scp1730EchoTrack();
    private static final ByteBufferBuilder Bytes = new ByteBufferBuilder(8192);
    private static final MultiBufferSource.BufferSource Buffers = MultiBufferSource.immediate(Bytes);
    @Nullable
    private static RemotePlayer Ghost;
    @Nullable
    private static Scp1730EchoTrack.Frame Shown;
    @Nullable
    private static Scp1730EchoTrack.Frame ShownOld;
    private static float Stride;

    private Scp1730EchoClient() {
    }

    public static Scp1730EchoTrack track() {
        return Track;
    }

    public static void tick(Minecraft Client, boolean Inside) {
        ClientLevel World = Client.level;
        LocalPlayer User = Client.player;
        if (World == null || User == null || !Inside) {
            reset();
            return;
        }
        long Now = World.getGameTime();
        Track.record(new Scp1730EchoTrack.Frame(Now, User.getX(), User.getY(), User.getZ(), User.yBodyRot, User.getYHeadRot(), User.getXRot(),
                User.walkAnimation.position(), User.walkAnimation.speed(), User.isCrouching(), User.swinging));
        ShownOld = Track.before(Now);
        Shown = Track.replay(Now);
        if (Shown == null) {
            return;
        }
        if (Ghost == null || Ghost.level() != World) {
            Ghost = new RemotePlayer(World, User.getGameProfile());
        }
        Ghost.tickCount++;
        Ghost.walkAnimation.update(Math.min(1.0f, Shown.WalkSpeed()), 1.0f);
        Ghost.setPose(Shown.Crouching() ? Pose.CROUCHING : Pose.STANDING);
        if (ShownOld != null) {
            double Moved = Math.sqrt(Mth.square(Shown.X() - ShownOld.X()) + Mth.square(Shown.Z() - ShownOld.Z()));
            if (Moved < 1.0) {
                Stride += (float) Moved;
            }
            if (Stride >= StepEvery) {
                Stride = 0.0f;
                step(World, Shown);
            }
        }
    }

    static void step(ClientLevel World, Scp1730EchoTrack.Frame At) {
        BlockPos Below = BlockPos.containing(At.X(), At.Y() - 0.2, At.Z());
        BlockState State = World.getBlockState(Below);
        if (State.isAir()) {
            return;
        }
        SoundType Kind = State.getSoundType();
        World.playLocalSound(At.X(), At.Y(), At.Z(), Kind.getStepSound(), SoundSource.PLAYERS, Kind.getVolume() * 0.2f, Kind.getPitch() * 0.72f, false);
    }

    public static boolean isGhost(@Nullable Object Entity) {
        return Entity != null && Entity == Ghost;
    }

    public static void render(PoseStack Pose, Vec3 Camera, float Partial) {
        Minecraft Client = Minecraft.getInstance();
        Scp1730EchoTrack.Frame Now = Shown;
        RemotePlayer Body = Ghost;
        if (Now == null || Body == null || Client.level == null || Body.level() != Client.level) {
            return;
        }
        Scp1730EchoTrack.Frame Old = ShownOld == null ? Now : ShownOld;
        double X = Mth.lerp(Partial, Old.X(), Now.X());
        double Y = Mth.lerp(Partial, Old.Y(), Now.Y());
        double Z = Mth.lerp(Partial, Old.Z(), Now.Z());
        float Yaw = Mth.rotLerp(Partial, Old.Yaw(), Now.Yaw());
        float Head = Mth.rotLerp(Partial, Old.HeadYaw(), Now.HeadYaw());
        float Pitch = Mth.lerp(Partial, Old.Pitch(), Now.Pitch());
        Body.setPos(X, Y, Z);
        Body.xo = X;
        Body.yo = Y;
        Body.zo = Z;
        Body.xOld = X;
        Body.yOld = Y;
        Body.zOld = Z;
        Body.yBodyRot = Yaw;
        Body.yBodyRotO = Yaw;
        Body.setYRot(Yaw);
        Body.yRotO = Yaw;
        Body.yHeadRot = Head;
        Body.yHeadRotO = Head;
        Body.setXRot(Pitch);
        Body.xRotO = Pitch;
        EntityRenderer<? super RemotePlayer> Renderer = Client.getEntityRenderDispatcher().getRenderer(Body);
        MultiBufferSource Tinted = Type -> new Shadowed(Buffers.getBuffer(Type));
        Pose.pushPose();
        Pose.translate(X - Camera.x, Y - Camera.y, Z - Camera.z);
        Renderer.render(Body, Yaw, Partial, Pose, Tinted, 0);
        Pose.popPose();
        Buffers.endBatch();
    }

    public static void reset() {
        Track.clear();
        Shown = null;
        ShownOld = null;
        Ghost = null;
        Stride = 0.0f;
    }

    record Shadowed(VertexConsumer Inner) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float X, float Y, float Z) {
            Inner.addVertex(X, Y, Z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int Red, int Green, int Blue, int Alpha) {
            Inner.setColor(Shade, Shade, Shade + 4, Math.min(Alpha, Opacity));
            return this;
        }

        @Override
        public VertexConsumer setUv(float U, float V) {
            Inner.setUv(U, V);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int U, int V) {
            Inner.setUv1(U, V);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int U, int V) {
            Inner.setUv2(U, V);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float X, float Y, float Z) {
            Inner.setNormal(X, Y, Z);
            return this;
        }
    }
}
