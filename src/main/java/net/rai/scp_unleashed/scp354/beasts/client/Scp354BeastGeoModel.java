package net.rai.scp_unleashed.scp354.beasts.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp354.beasts.Scp354Beast;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastController;
import net.rai.scp_unleashed.scp354.beasts.Scp354BeastRegistry;
import net.rai.scp_unleashed.scp354.beasts.Scp354Crawler;
import net.rai.scp_unleashed.scp354.beasts.Scp354Leaper;
import net.rai.scp_unleashed.scp354.beasts.Scp354Wingbeast;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.IdentityHashMap;
import java.util.Map;

public class Scp354BeastGeoModel<T extends Scp354Beast> extends GeoModel<T> {
    public static final String RootBone = "root";
    public static final String LookBone = "look";
    public static final String BodyController = "body";
    public static final float PitchLimit = 30.0f;
    public static final float YawLimit = 45.0f;
    public static final String[] CrawlerSegments = {"body", "seg1", "seg2", "seg3", "seg4", "seg5", "seg6", "tail"};
    public static final double CrawlerSpacing = 3.0 * 2.5 / 16.0;
    public static final float SegmentYaw = 40.0f;
    public static final float SegmentPitch = 60.0f;
    public static final float TongueLength = 4.0f;
    public static final float BankLimit = 40.0f;
    public static final float TonguePitchLimit = 60.0f;
    public static final double TrailFloor = 1.0e-4;
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;
    private final Map<GeoBone, float[]> Nudged = new IdentityHashMap<>();

    public Scp354BeastGeoModel(String Name) {
        ModelPath = Scp354BeastRegistry.location("geo/entity/scp354/" + Name + ".geo.json");
        TexturePath = texture(Name);
        AnimationPath = Scp354BeastRegistry.location("animations/entity/scp354/" + Name + ".animation.json");
    }

    public static ResourceLocation texture(String Name) {
        return Scp354BeastRegistry.location("textures/entity/scp354/" + Name + ".png");
    }

    public static ResourceLocation glow(String Name) {
        return Scp354BeastRegistry.location("textures/entity/scp354/" + Name + "_glow.png");
    }

    @Override
    public ResourceLocation getModelResource(T Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Creature) {
        return AnimationPath;
    }

    public static boolean readyToDraw(Scp354Beast Creature, long InstanceId) {
        if (!Creature.isEmerging()) {
            return true;
        }
        AnimationController<?> Controller = Creature.getAnimatableInstanceCache().getManagerForId(InstanceId).getAnimationControllers().get(BodyController);
        return Controller instanceof Scp354BeastController Body && Body.playing("emerge");
    }

    @Override
    public void setCustomAnimations(T Creature, long InstanceId, AnimationState<T> Animated) {
        float Partial = Animated.getPartialTick();
        GeoBone Root = getAnimationProcessor().getBone(RootBone);
        if (Root != null) {
            Root.setHidden(!readyToDraw(Creature, InstanceId));
        }
        for (String Name : Creature.hiddenBones()) {
            GeoBone Hidden = getAnimationProcessor().getBone(Name);
            if (Hidden != null) {
                Hidden.setHidden(!Creature.showsBone(Name));
            }
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look != null) {
            place(Look, Mth.clamp(Creature.lookPitch(Partial), -PitchLimit, PitchLimit), Mth.clamp(Creature.lookYaw(Partial), -YawLimit, YawLimit), 0.0f);
        }
        if (Creature instanceof Scp354Wingbeast Flyer) {
            bank(Flyer, Partial);
        } else if (Creature instanceof Scp354Crawler Crawler) {
            follow(Crawler, Partial);
        } else if (Creature instanceof Scp354Leaper Leaper) {
            tongue(Leaper);
        }
    }

    public static void place(GeoBone Bone, float Pitch, float Yaw, float Roll) {
        BoneSnapshot Rest = Bone.getInitialSnapshot();
        Bone.setRotX(Rest.getRotX() - Pitch * Mth.DEG_TO_RAD);
        Bone.setRotY(Rest.getRotY() - Yaw * Mth.DEG_TO_RAD);
        Bone.setRotZ(Rest.getRotZ() + Roll * Mth.DEG_TO_RAD);
    }

    public void nudge(GeoBone Bone, float Pitch, float Yaw, float Roll) {
        float BaseX = Bone.getRotX();
        float BaseY = Bone.getRotY();
        float BaseZ = Bone.getRotZ();
        float[] Last = Nudged.get(Bone);
        if (Last != null && BaseX == Last[0] && BaseY == Last[1] && BaseZ == Last[2]) {
            BaseX -= Last[3];
            BaseY -= Last[4];
            BaseZ -= Last[5];
        }
        float AddX = Pitch * Mth.DEG_TO_RAD;
        float AddY = Yaw * Mth.DEG_TO_RAD;
        float AddZ = Roll * Mth.DEG_TO_RAD;
        Bone.setRotX(BaseX + AddX);
        Bone.setRotY(BaseY + AddY);
        Bone.setRotZ(BaseZ + AddZ);
        Nudged.put(Bone, new float[]{Bone.getRotX(), Bone.getRotY(), Bone.getRotZ(), AddX, AddY, AddZ});
    }

    private void bank(Scp354Wingbeast Flyer, float Partial) {
        GeoBone Body = getAnimationProcessor().getBone("body");
        if (Body != null) {
            nudge(Body, 0.0f, 0.0f, Flyer.deathTime <= 0 ? Mth.clamp(Flyer.bank(Partial), -BankLimit, BankLimit) : 0.0f);
        }
    }

    private void follow(Scp354Crawler Crawler, float Partial) {
        boolean Still = Crawler.deathTime > 0 || Crawler.isEmerging();
        Vec3 Head = Crawler.getPosition(Partial);
        float Yaw = Mth.rotLerp(Partial, Crawler.yBodyRotO, Crawler.yBodyRot);
        float Pitch = 0.0f;
        Vec3 Front = Head;
        for (int Index = 0; Index < CrawlerSegments.length; Index++) {
            GeoBone Segment = getAnimationProcessor().getBone(CrawlerSegments[Index]);
            if (Segment == null) {
                continue;
            }
            Vec3 Back = Crawler.trailPoint(Head, CrawlerSpacing * (Index + 1));
            Vec3 Forward = Front.subtract(Back);
            Front = Back;
            float TurnYaw = 0.0f;
            float TurnPitch = 0.0f;
            if (!Still && Forward.lengthSqr() >= TrailFloor) {
                TurnYaw = Mth.clamp(Mth.wrapDegrees(Scp354Crawler.headingYaw(Forward) - Yaw), -SegmentYaw, SegmentYaw);
                TurnPitch = Mth.clamp(Scp354Crawler.headingPitch(Forward) - Pitch, -SegmentPitch, SegmentPitch);
            }
            nudge(Segment, TurnPitch, TurnYaw, 0.0f);
            Yaw += TurnYaw;
            Pitch += TurnPitch;
        }
    }

    private void tongue(Scp354Leaper Leaper) {
        GeoBone Tongue = getAnimationProcessor().getBone("tongue");
        GeoBone Tip = getAnimationProcessor().getBone("tongue_tip");
        if (Tongue == null || Tip == null) {
            return;
        }
        float Extension = Leaper.action() == Scp354Leaper.ActionTongue ? Scp354Leaper.tongueExtension(Leaper.actionElapsed()) : 0.0f;
        if (Extension <= 0.0f) {
            Tongue.setScaleZ(1.0f);
            Tip.setScaleZ(1.0f);
            return;
        }
        float Reach = Leaper.tongueReach() * 16.0f / Leaper.renderScale();
        float Stretch = 1.0f + Extension * Math.max(0.0f, Reach - TongueLength) / TongueLength;
        Tongue.setScaleZ(Stretch);
        Tip.setScaleZ(1.0f / Stretch);
        nudge(Tongue, Mth.clamp(Leaper.tonguePitch(), -TonguePitchLimit, TonguePitchLimit) * Extension, 0.0f, 0.0f);
    }
}
