package net.rai.scp_unleashed.scp016.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp016.Scp016Host;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp016GeoModel<T extends Scp016Spawn> extends GeoModel<T> {
    public static final String LookBone = "look";
    public static final String PitchBone = "pitch";
    private final ResourceLocation ModelPath;
    private final ResourceLocation[] TexturePaths;
    private final ResourceLocation AnimationPath;
    private final float PitchLimit;
    private final float YawLimit;

    public Scp016GeoModel(String Name, float PitchLimit, float YawLimit) {
        this.ModelPath = location("geo/entity/scp016/" + Name + ".geo.json");
        this.TexturePaths = new ResourceLocation[Scp016Host.Variants];
        for (int Code = 0; Code < Scp016Host.Variants; Code++) {
            TexturePaths[Code] = location("textures/entity/scp016/" + Name + "_" + Scp016Host.name(Code) + ".png");
        }
        this.AnimationPath = location("animations/entity/scp016/" + Name + ".animation.json");
        this.PitchLimit = PitchLimit;
        this.YawLimit = YawLimit;
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(T Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Creature) {
        return TexturePaths[Scp016Host.clamp(Creature.host())];
    }

    @Override
    public ResourceLocation getAnimationResource(T Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(T Creature, long InstanceId, AnimationState<T> Animated) {
        GeoBone Pitch = getAnimationProcessor().getBone(PitchBone);
        if (Pitch != null) {
            float Partial = Animated.getPartialTick();
            Pitch.setRotX(Creature.bodyPitch(Partial) * Mth.DEG_TO_RAD);
            Pitch.setRotZ(Creature.bodyRoll(Partial) * Mth.DEG_TO_RAD);
        }
        if (Creature.locksMovement() || Creature.isDeadOrDying()) {
            return;
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
    }
}
