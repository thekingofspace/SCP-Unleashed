package net.rai.scp_unleashed.scp428.client;

import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.dclass.client.DClassSkins;
import net.rai.scp_unleashed.playerclone.client.PlayerCloneGeoModel;
import net.rai.scp_unleashed.playerclone.client.PlayerCloneSkins;
import net.rai.scp_unleashed.scp428.Scp428Cords;
import net.rai.scp_unleashed.scp428.Scp428Instance;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp428InstanceGeoModel extends DClassGeoModel<Scp428Instance> {
    public static final int TwitchShortest = 70;
    public static final int TwitchSpread = 90;
    public static final int TwitchTicks = 6;

    @Override
    public ResourceLocation getModelResource(Scp428Instance Body) {
        if (Body.isPlayerLook()) {
            return skinOf(Body).model() == PlayerSkin.Model.SLIM ? PlayerCloneGeoModel.SlimPath : PlayerCloneGeoModel.WidePath;
        }
        return DClassGeoModel.ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp428Instance Body) {
        if (Body.isPlayerLook()) {
            return skinOf(Body).texture();
        }
        return DClassSkins.textureFor(Body.appearance());
    }

    public static PlayerSkin skinOf(Scp428Instance Body) {
        return PlayerCloneSkins.skinOf(Body.profile(), Body.getUUID());
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp428Instance Body, long InstanceId, AnimationState<Scp428Instance> Animated) {
        super.setCustomAnimations(Body, InstanceId, Animated);
        Scp428Tendrils.track(part(Scp428Tendrils.HeadBone));
        if (Body.isDeadOrDying()) {
            return;
        }
        float Partial = Animated.getPartialTick();
        float Time = Body.tickCount + Partial;
        float Phase = (Body.getId() * 0.618034f % 1.0f) * Mth.TWO_PI;
        float Rise = 1.0f - (float) Scp428Cords.smooth(Body.riseProgress(Partial));
        Scp428Poses.pose(this::part, new Scp428Poses.Stance(Body.holdingId() >= 0, Body.swipeProgress(Partial), Body.aiming(), Body.huddle(), Rise, Time, Phase,
                (float) (Body.level().getGameTime() % 24000L) + Partial, twitch(Body, Time)));
    }

    private static float twitch(Scp428Instance Body, float Time) {
        int Period = TwitchShortest + Math.floorMod(Body.getId() * 37, TwitchSpread);
        float Into = (Time + Body.getId() * 13) % Period;
        if (Into > TwitchTicks) {
            return 0.0f;
        }
        return Mth.sin(Into / TwitchTicks * Mth.PI) * (1.0f - Into / TwitchTicks * 0.5f);
    }
}
