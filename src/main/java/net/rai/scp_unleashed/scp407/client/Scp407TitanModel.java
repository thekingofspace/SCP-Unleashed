package net.rai.scp_unleashed.scp407.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407RigData;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp407TitanModel extends GeoModel<Scp407Titan> {
    public static final String HeadBone = "head";
    public static final String RootBone = "root";
    public static final float YawLimit = 45.0f;
    public static final float PitchLimit = 25.0f;
    private static final ResourceLocation ModelPath = Scp407Registry.location("geo/entity/scp407/scp407_titan.geo.json");
    private static final ResourceLocation TexturePath = Scp407Registry.location("textures/entity/scp407/scp407_titan.png");
    private static final ResourceLocation AnimationPath = Scp407Registry.location("animations/entity/scp407/scp407_titan.animation.json");

    @Override
    public ResourceLocation getModelResource(Scp407Titan Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp407Titan Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp407Titan Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp407Titan Creature, long InstanceId, AnimationState<Scp407Titan> Animated) {
        float Stoop = Creature.isEmerging() ? 0.0f : Creature.stoop(Animated.getPartialTick());
        if (Stoop > 0.001f) {
            for (int Index = 0; Index < Scp407RigData.StoopBones.length; Index++) {
                GeoBone Part = getAnimationProcessor().getBone(Scp407RigData.StoopBones[Index]);
                if (Part == null) {
                    continue;
                }
                float[] Turn = Scp407RigData.StoopTurns[Index];
                Part.setRotX(Part.getRotX() - Turn[0] * Stoop * Mth.DEG_TO_RAD);
                Part.setRotY(Part.getRotY() - Turn[1] * Stoop * Mth.DEG_TO_RAD);
                Part.setRotZ(Part.getRotZ() + Turn[2] * Stoop * Mth.DEG_TO_RAD);
            }
            GeoBone Root = getAnimationProcessor().getBone(RootBone);
            if (Root != null) {
                Root.setPosY(Root.getPosY() - Scp407RigData.StoopDrop * Stoop);
            }
        }
        if (Creature.isActing()) {
            return;
        }
        GeoBone Head = getAnimationProcessor().getBone(HeadBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Head == null || Data == null) {
            return;
        }
        Head.setRotX(Head.getRotX() - Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
        Head.setRotY(Head.getRotY() - Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
    }
}
