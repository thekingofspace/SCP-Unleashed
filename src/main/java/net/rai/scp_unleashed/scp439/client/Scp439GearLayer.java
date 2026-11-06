package net.rai.scp_unleashed.scp439.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.dclass.client.HumanoidArmourPass;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp439GearLayer extends GeoRenderLayer<Scp439Hive> {
    private final HumanoidModel<LivingEntity> Parent;
    private final HumanoidModel<LivingEntity> Inner;
    private final HumanoidModel<LivingEntity> Outer;
    private boolean Pending;

    public Scp439GearLayer(GeoRenderer<Scp439Hive> Renderer) {
        super(Renderer);
        Minecraft Game = Minecraft.getInstance();
        Parent = new HumanoidModel<>(Game.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        Inner = new HumanoidModel<>(Game.getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
        Outer = new HumanoidModel<>(Game.getEntityModels().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void preRender(PoseStack Pose, Scp439Hive Hive, BakedGeoModel Baked, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                          float Partial, int Light, int Overlay) {
        Pending = true;
    }

    @Override
    public void renderForBone(PoseStack Pose, Scp439Hive Hive, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        if (!Pending || Bone.getParent() != null) {
            return;
        }
        Pending = false;
        if (Hive.isInvisible() || !Hive.look().geared()) {
            return;
        }
        boolean Masked = Hive.progress(Partial) >= Scp439HivePose.MaskShown;
        GeoModel<Scp439Hive> Shape = getGeoModel();
        HumanoidArmourPass.renderWorn(Pose, Buffers, Hive, Slot -> Slot == EquipmentSlot.HEAD && Masked ? ItemStack.EMPTY : Hive.gear(Slot),
                Name -> Shape.getAnimationProcessor().getBone(Name), Bone, Partial, Light, Parent, Inner, Outer);
    }
}
