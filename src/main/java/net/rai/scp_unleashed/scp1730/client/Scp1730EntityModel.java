package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Painter;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Tentacle;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp1730EntityModel<T extends Entity & GeoAnimatable> extends SharedGeoModel<T> {
    public static final float YawLimit = 30.0f;
    public static final float PitchLimit = 15.0f;
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;
    private final String LookBone;

    public Scp1730EntityModel(String Name, String LookBone) {
        this.ModelPath = Scp1730Registry.location("geo/entity/scp1730/" + Name + ".geo.json");
        this.TexturePath = Scp1730Registry.location("textures/entity/scp1730/" + Name + ".png");
        this.AnimationPath = Scp1730Registry.location("animations/entity/scp1730/" + Name + ".animation.json");
        this.LookBone = LookBone;
    }

    @Override
    public ResourceLocation getModelResource(T Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(T Owner, long InstanceId, AnimationState<T> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        if (Owner instanceof Scp1730Tentacle Tentacle) {
            GeoBone Root = getAnimationProcessor().getBone("root");
            if (Root != null) {
                float Pitch = Mth.lerp(Animated.getPartialTick(), Tentacle.xRotO, Tentacle.getXRot());
                Root.setRotX(Root.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
            }
            return;
        }
        if (Owner instanceof Scp1730Painter Painter) {
            GeoBone Tentacle = getAnimationProcessor().getBone("tentacle_root");
            if (Tentacle != null) {
                Tentacle.setHidden(!Painter.tentacleShown());
            }
        }
        if (LookBone == null) {
            return;
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null) {
            return;
        }
        float Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
        float Pitch = Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit);
        Look.setRotY(Look.getInitialSnapshot().getRotY() + Yaw * Mth.DEG_TO_RAD);
        Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
    }
}
