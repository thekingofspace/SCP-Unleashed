package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666YuleMan;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class Scp4666GeoModel extends SharedGeoModel<Scp4666YuleMan> {
    public static final ResourceLocation ModelPath = Scp4666Registry.location("geo/entity/scp4666/scp4666_yule_man.geo.json");
    public static final ResourceLocation TexturePath = Scp4666Registry.location("textures/entity/scp4666/scp4666_yule_man.png");
    public static final ResourceLocation AnimationPath = Scp4666Registry.location("animations/entity/scp4666/scp4666_yule_man.animation.json");
    public static final String LookBone = "look";
    public static final String SackBone = "sack";
    public static final String[] Irises = {"iris_left", "iris_right"};
    public static final String[] Lids = {"lid_upper_left", "lid_upper_right"};
    public static final Set<Integer> SackModes = Set.of(Scp4666YuleMan.ModeIdle, Scp4666YuleMan.ModeStalk, Scp4666YuleMan.ModeFlee, Scp4666YuleMan.ModeLoom,
            Scp4666YuleMan.ModeDrag, Scp4666YuleMan.ModeLift);
    public static final Set<Integer> GazeModes = Set.of(Scp4666YuleMan.ModeIdle, Scp4666YuleMan.ModeStalk, Scp4666YuleMan.ModeStare, Scp4666YuleMan.ModeSink,
            Scp4666YuleMan.ModeDrag, Scp4666YuleMan.ModeBeat, Scp4666YuleMan.ModeLift);
    public static final Set<Integer> BlinkModes = Set.of(Scp4666YuleMan.ModeIdle, Scp4666YuleMan.ModeStalk, Scp4666YuleMan.ModeDrag, Scp4666YuleMan.ModeLift);
    public static final float LookUp = 20.0f;
    public static final float LookDown = 30.0f;
    public static final float LookTurn = 45.0f;
    public static final float GazeYaw = 40.0f;
    public static final float GazePitch = 35.0f;
    public static final float GazeWide = 1.0f;
    public static final float GazeTall = 0.6f;
    public static final float GazeEase = 0.18f;
    public static final int BlinkPeriod = 300;
    public static final int BlinkSpread = 220;
    public static final float BlinkTicks = 3.5f;
    public static final float BlinkDepth = 2.0f;

    private final Map<Scp4666YuleMan, float[]> Gaze = new WeakHashMap<>();

    @Override
    public ResourceLocation getModelResource(Scp4666YuleMan Man) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp4666YuleMan Man) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp4666YuleMan Man) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp4666YuleMan Man, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    public static boolean looking(Scp4666YuleMan Man) {
        int Mode = Man.mode();
        return Man.isAlive() && (Mode == Scp4666YuleMan.ModeIdle || Mode == Scp4666YuleMan.ModeStare || Mode == Scp4666YuleMan.ModeStalk);
    }

    public static boolean showsSack(Scp4666YuleMan Man) {
        return SackModes.contains(Man.mode());
    }

    @Override
    public void setCustomAnimations(Scp4666YuleMan Man, long InstanceId, AnimationState<Scp4666YuleMan> Animated) {
        GeoBone Sack = getAnimationProcessor().getBone(SackBone);
        if (Sack != null) {
            Sack.setHidden(!showsSack(Man));
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look == null) {
            return;
        }
        float Pitch = 0.0f;
        float Yaw = 0.0f;
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data != null && looking(Man)) {
            Pitch = Mth.clamp(Data.headPitch(), -LookUp, LookDown);
            Yaw = Mth.clamp(Data.netHeadYaw(), -LookTurn, LookTurn);
        }
        Look.setRotX(-Pitch * Mth.DEG_TO_RAD);
        Look.setRotY(-Yaw * Mth.DEG_TO_RAD);
        Look.setRotZ(0.0f);
        float Partial = Animated.getPartialTick();
        gaze(Man, Partial, Yaw, Pitch);
        blink(Man, Partial);
    }

    private void gaze(Scp4666YuleMan Man, float Partial, float LookYaw, float LookPitch) {
        float[] State = Gaze.computeIfAbsent(Man, Key -> new float[2]);
        float WantX = 0.0f;
        float WantY = 0.0f;
        if (Man.isAlive() && GazeModes.contains(Man.mode())) {
            Camera View = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 Toward = View.getPosition().subtract(Man.getEyePosition(Partial));
            float Body = Mth.rotLerp(Partial, Man.yBodyRotO, Man.yBodyRot);
            float Heading = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
            float Across = Mth.wrapDegrees(Heading - Body) - LookYaw;
            float Flat = (float) Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
            float Down = (float) (-Mth.atan2(Toward.y, Flat) * Mth.RAD_TO_DEG) - LookPitch;
            WantX = -Mth.clamp(Across / GazeYaw, -1.0f, 1.0f) * GazeWide;
            WantY = -Mth.clamp(Down / GazePitch, -1.0f, 1.0f) * GazeTall;
        }
        State[0] += (WantX - State[0]) * GazeEase;
        State[1] += (WantY - State[1]) * GazeEase;
        for (String Name : Irises) {
            GeoBone Iris = getAnimationProcessor().getBone(Name);
            if (Iris != null) {
                Iris.setPosX(Iris.getPosX() + State[0]);
                Iris.setPosY(Iris.getPosY() + State[1]);
            }
        }
    }

    private void blink(Scp4666YuleMan Man, float Partial) {
        if (!Man.isAlive() || !BlinkModes.contains(Man.mode())) {
            return;
        }
        float Clock = Man.tickCount + Partial + Math.floorMod(Man.getId() * 977, BlinkPeriod);
        int Round = (int) Math.floor(Clock / BlinkPeriod);
        float Within = Clock - Round * BlinkPeriod;
        float Moment = 20.0f + Math.floorMod(Round * 7919 + Man.getId() * 31, BlinkSpread);
        float Into = Within - Moment;
        if (Into < 0.0f || Into > BlinkTicks) {
            return;
        }
        float Closed = 1.0f - Math.abs(Into / BlinkTicks * 2.0f - 1.0f);
        for (String Name : Lids) {
            GeoBone Lid = getAnimationProcessor().getBone(Name);
            if (Lid != null) {
                Lid.setPosY(Lid.getPosY() - Closed * BlinkDepth);
            }
        }
    }
}
