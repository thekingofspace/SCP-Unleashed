package net.rai.scp_unleashed.scp427.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp427.Scp427Flesh;
import net.rai.scp_unleashed.scp427.Scp427Hair;
import net.rai.scp_unleashed.scp427.Scp427Registry;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp427FleshModel extends GeoModel<Scp427Flesh> {
    public static final ResourceLocation ModelPath = Scp427Registry.location("geo/entity/scp427/flesh.geo.json");
    public static final ResourceLocation TexturePath = Scp427Registry.location("textures/entity/scp427/flesh.png");
    public static final ResourceLocation AnimationPath = Scp427Registry.location("animations/entity/scp427/flesh.animation.json");
    public static final String LookBone = "look";
    public static final float PitchLimit = 30.0f;
    public static final float YawLimit = 45.0f;
    private GeoBone[][] Strands;
    private GeoBone Anchor;

    @Override
    public ResourceLocation getModelResource(Scp427Flesh Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp427Flesh Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp427Flesh Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp427Flesh Creature, long InstanceId, AnimationState<Scp427Flesh> Animated) {
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look != null) {
            BoneSnapshot Rest = Look.getInitialSnapshot();
            boolean Free = Data != null && !Creature.busy() && !Creature.isDeadOrDying();
            Look.setRotX(Rest.getRotX() - (Free ? Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD : 0.0f));
            Look.setRotY(Rest.getRotY() - (Free ? Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD : 0.0f));
        }
        if (!bindStrands()) {
            return;
        }
        Scp427Hair Hair = Creature.hair();
        float Partial = Animated.getPartialTick();
        for (int Chain = 0; Chain < Strands.length; Chain++) {
            for (int Part = 0; Part < Strands[Chain].length; Part++) {
                GeoBone Bone = Strands[Chain][Part];
                BoneSnapshot Initial = Bone.getInitialSnapshot();
                Bone.setRotX(Initial.getRotX() + Hair.local(Chain, Part, 0, Partial));
                Bone.setRotZ(Initial.getRotZ() + Hair.local(Chain, Part, 1, Partial));
            }
        }
    }

    private boolean bindStrands() {
        AnimationProcessor<Scp427Flesh> Processor = getAnimationProcessor();
        GeoBone First = Processor.getBone("hair_0_0");
        if (First == null) {
            return false;
        }
        if (First == Anchor && Strands != null) {
            return true;
        }
        GeoBone[][] Found = new GeoBone[Scp427Hair.Chains][Scp427Hair.Segments];
        for (int Chain = 0; Chain < Scp427Hair.Chains; Chain++) {
            for (int Part = 0; Part < Scp427Hair.Segments; Part++) {
                GeoBone Bone = Processor.getBone("hair_" + Chain + "_" + Part);
                if (Bone == null) {
                    return false;
                }
                Found[Chain][Part] = Bone;
            }
        }
        Strands = Found;
        Anchor = First;
        return true;
    }
}
