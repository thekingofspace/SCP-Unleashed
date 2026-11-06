package net.rai.scp_unleashed.scp003.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp003.Scp003Creature;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp003GeoModel<T extends Scp003Creature> extends GeoModel<T> {
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;
    @Nullable
    private final String LookBone;
    private final float PitchLimit;
    private final float YawLimit;

    public Scp003GeoModel(String Name, @Nullable String LookBone, float PitchLimit, float YawLimit) {
        this.ModelPath = location("geo/entity/scp003/" + Name + ".geo.json");
        this.TexturePath = location("textures/entity/scp003/" + Name + ".png");
        this.AnimationPath = location("animations/entity/scp003/" + Name + ".animation.json");
        this.LookBone = LookBone;
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
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(T Creature, long InstanceId, AnimationState<T> Animated) {
        GeoBone Cocoon = getAnimationProcessor().getBone("cocoon");
        if (Cocoon != null) {
            Cocoon.setHidden(Creature.action() != Scp003Creature.ActionTransform);
        }
        if (LookBone == null || Creature.locksMovement()) {
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
