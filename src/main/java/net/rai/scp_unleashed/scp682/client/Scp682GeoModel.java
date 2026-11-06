package net.rai.scp_unleashed.scp682.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp682.Scp682;
import net.rai.scp_unleashed.scp682.Scp682Registry;
import net.rai.scp_unleashed.scp682.Scp682Signature;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scp682GeoModel extends SharedGeoModel<Scp682> {
    public static final ResourceLocation ModelPath = Scp682Registry.location("geo/entity/scp682/scp682.geo.json");
    public static final ResourceLocation TexturePath = Scp682Registry.location("textures/entity/scp682/scp682.png");
    public static final ResourceLocation AnimationPath = Scp682Registry.location("animations/entity/scp682/scp682.animation.json");
    public static final float YawLimit = 35.0f;
    public static final float PitchLimit = 20.0f;
    public static final List<String> OpenEyes = List.of("eye_left", "eye_right");
    public static final List<String> Seals = List.of("seal_left", "seal_right");
    public static final List<String> ManyEyes = List.of("many_head_left", "many_neck_left", "many_chest_left", "many_body_left", "many_head_right", "many_neck_right",
            "many_chest_right", "many_body_right");
    public static final Map<Scp682Signature, List<String>> SignatureBones = Map.of(Scp682Signature.Crystal, List.of("sig_crystal_neck", "sig_crystal_chest", "sig_crystal_body"),
            Scp682Signature.Plated, List.of("sig_plated_chest_left", "sig_plated_chest_right", "sig_plated_body_left", "sig_plated_body_right"));
    public static final float MarkTicks = 20.0f;
    public static final float GrowTicks = 14.0f;
    public static final float SealTicks = 6.0f;
    public static final float ShedTicks = 30.0f;
    public static final float Settled = 1000.0f;
    public static final int Remembered = 256;
    private final Map<Long, float[]> Changes = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, float[]> Eldest) {
            return size() > Remembered;
        }
    };
    private final Map<Long, float[]> Marks = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, float[]> Eldest) {
            return size() > Remembered;
        }
    };

    @Override
    public ResourceLocation getModelResource(Scp682 Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp682 Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp682 Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp682 Owner, long InstanceId, AnimationState<Scp682> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        eyes(Owner, InstanceId, Animated.getPartialTick());
        marks(Owner, InstanceId, Animated.getPartialTick());
        GeoBone Look = getAnimationProcessor().getBone("look");
        if (Look == null) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        float Yaw = 0.0f;
        float Pitch = 0.0f;
        if (Data != null && Owner.canAct() && !Owner.isCalm()) {
            Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
            Pitch = Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit);
        }
        Look.setRotY(Look.getInitialSnapshot().getRotY() + Yaw * Mth.DEG_TO_RAD);
        Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
    }

    private void eyes(Scp682 Owner, long InstanceId, float Partial) {
        int State = Owner.eyes();
        float Now = Owner.tickCount + Partial;
        float[] Held = Changes.get(InstanceId);
        if (Held == null) {
            Held = new float[]{State, Now - Settled, State};
            Changes.put(InstanceId, Held);
        } else if ((int) Held[0] != State) {
            Held[2] = Held[0];
            Held[0] = State;
            Held[1] = Now;
        } else if (Held[1] > Now) {
            Held[1] = Now - Settled;
        }
        float Age = Now - Held[1];
        float Sealing = Mth.clamp(Age / SealTicks, 0.0f, 1.0f);
        float Growing = Mth.clamp(Age / GrowTicks, 0.0f, 1.0f);
        boolean Receding = State != Scp682.EyeMany && (int) Held[2] == Scp682.EyeMany && Age >= 0.0f && Age < ShedTicks;
        float Shrinking = Mth.clamp(Age / ShedTicks, 0.0f, 1.0f);
        for (String Name : OpenEyes) {
            show(Name, State != Scp682.EyeSealed, 1.0f);
        }
        for (String Name : Seals) {
            show(Name, State == Scp682.EyeSealed, 0.35f + 0.65f * Sealing);
        }
        for (String Name : ManyEyes) {
            show(Name, State == Scp682.EyeMany || Receding, State == Scp682.EyeMany ? 0.05f + 0.95f * Growing : 1.0f - 0.95f * Shrinking);
        }
    }

    private void marks(Scp682 Owner, long InstanceId, float Partial) {
        Scp682Signature[] All = Scp682Signature.values();
        int Mask = Owner.signatures();
        float Now = Owner.tickCount + Partial;
        float[] Held = Marks.get(InstanceId);
        if (Held == null) {
            Held = new float[All.length + 1];
            Arrays.fill(Held, Now - Settled);
            Held[All.length] = Mask;
            Marks.put(InstanceId, Held);
        }
        int Before = (int) Held[All.length];
        for (Scp682Signature Each : All) {
            if ((Mask & Each.bit()) != (Before & Each.bit())) {
                Held[Each.ordinal()] = Now;
            } else if (Held[Each.ordinal()] > Now) {
                Held[Each.ordinal()] = Now - Settled;
            }
        }
        Held[All.length] = Mask;
        for (Map.Entry<Scp682Signature, List<String>> Entry : SignatureBones.entrySet()) {
            Scp682Signature Each = Entry.getKey();
            float Age = Now - Held[Each.ordinal()];
            boolean Has = (Mask & Each.bit()) != 0;
            float Growing = Mth.clamp(Age / MarkTicks, 0.0f, 1.0f);
            float Shrinking = Mth.clamp(Age / ShedTicks, 0.0f, 1.0f);
            for (String Name : Entry.getValue()) {
                show(Name, Has || Age >= 0.0f && Age < ShedTicks, Has ? 0.1f + 0.9f * Growing : 1.0f - 0.9f * Shrinking);
            }
        }
    }

    private void show(String Name, boolean Shown, float Size) {
        GeoBone Found = getAnimationProcessor().getBone(Name);
        if (Found == null) {
            return;
        }
        Found.setHidden(!Shown);
        if (Shown) {
            Found.setScaleX(Size);
            Found.setScaleY(Size);
            Found.setScaleZ(Size);
        }
    }
}
