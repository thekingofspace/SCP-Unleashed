package net.rai.scp_unleashed.scp439.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.dclass.client.DClassSkins;
import net.rai.scp_unleashed.playerclone.client.PlayerCloneGeoModel;
import net.rai.scp_unleashed.playerclone.client.PlayerCloneSkins;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.Scp439Look;
import net.rai.scp_unleashed.scp439.Scp439Registry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp439HiveModel extends SharedGeoModel<Scp439Hive> {
    public static final ResourceLocation VillagerModel = Scp439Registry.location("geo/entity/scp439/scp439_villager.geo.json");
    public static final ResourceLocation VillagerAnimation = Scp439Registry.location("animations/entity/scp439/scp439_villager.animation.json");
    public static final ResourceLocation VillagerSkin = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    public static final ResourceLocation TraderSkin = ResourceLocation.withDefaultNamespace("textures/entity/wandering_trader.png");

    @Override
    public ResourceLocation getModelResource(Scp439Hive Hive) {
        Scp439Look Seen = Hive.look();
        if (Seen.villagerLike()) {
            return VillagerModel;
        }
        if (Seen.Kind() == Scp439Look.KindPlayer) {
            return PlayerCloneSkins.skinOf(Hive.profile(), Hive.getUUID()).model() == PlayerSkin.Model.SLIM ? PlayerCloneGeoModel.SlimPath : PlayerCloneGeoModel.WidePath;
        }
        return DClassGeoModel.ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp439Hive Hive) {
        Scp439Look Seen = Hive.look();
        if (Seen.Kind() == Scp439Look.KindVillager) {
            return VillagerSkin;
        }
        if (Seen.Kind() == Scp439Look.KindTrader) {
            return TraderSkin;
        }
        if (Seen.Kind() == Scp439Look.KindPlayer) {
            return PlayerCloneSkins.skinOf(Hive.profile(), Hive.getUUID()).texture();
        }
        return DClassSkins.textureFor(Seen.Appearance());
    }

    @Override
    public ResourceLocation getAnimationResource(Scp439Hive Hive) {
        return Hive.look().villagerLike() ? VillagerAnimation : DClassGeoModel.AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp439Hive Hive, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Nullable
    private GeoBone bone(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    private void hide(String Name, boolean Hidden) {
        GeoBone Found = bone(Name);
        if (Found != null) {
            Found.setHidden(Hidden);
        }
    }

    public static float seconds(Scp439Hive Hive, float Partial) {
        return (Hive.tickCount + Partial) / 20.0f + Math.floorMod(Hive.getId(), 64) * 0.73f;
    }

    public static float jawAngle(Scp439Hive Hive, float Partial) {
        return Scp439HivePose.jaw(seconds(Hive, Partial), Hive.flinch(Partial), Hive.heave(Partial), Hive.crumble(Partial));
    }

    @Override
    public void setCustomAnimations(Scp439Hive Hive, long InstanceId, AnimationState<Scp439Hive> Animated) {
        float Partial = Animated.getPartialTick();
        Scp439Look Seen = Hive.look();
        boolean Rural = Seen.villagerLike();
        Scp439HivePose.apply(this::bone, Rural, seconds(Hive, Partial), Hive.flinch(Partial), Hive.heave(Partial), Hive.crumble(Partial));
        float Progress = Hive.progress(Partial);
        if (Rural) {
            hide("hat_rim", Progress >= Scp439HivePose.MaskShown);
            hide("nose", Progress >= Scp439HivePose.MaskShown);
            hide("hat", Progress >= Scp439HivePose.MaskShown);
            hide("head", false);
            return;
        }
        if (Seen.Kind() == Scp439Look.KindDClass) {
            int Style = DClassAppearance.style(Seen.Appearance());
            hide("hair_curly", Style != DClassAppearance.Curly);
            hide("hair_mohawk", Style != DClassAppearance.Mohawk);
            hide("hair_tail", Style != DClassAppearance.Long);
            hide("mouth", true);
            for (String Lid : new String[]{"right_lid", "left_lid"}) {
                GeoBone Found = bone(Lid);
                if (Found != null) {
                    Found.setHidden(false);
                    Found.setScaleY(1.0f);
                }
            }
        }
    }
}
