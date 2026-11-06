package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class Scp409CrystalLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    public Scp409CrystalLayer(RenderLayerParent<T, M> Parent) {
        super(Parent);
    }

    public static boolean nearCamera(LivingEntity Owner) {
        Vec3 Eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return Owner.distanceToSqr(Eye) < Scp409Crystals.CoreDistance * Scp409Crystals.CoreDistance;
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, T Owner, float LimbSwing, float LimbAmount, float Partial, float Age, float HeadYaw, float HeadPitch) {
        Scp409ClientState.Entry Found = Scp409ClientState.get(Owner.getId());
        if (Found == null || !Found.visible() || Owner.isInvisible()) {
            return;
        }
        M Model = getParentModel();
        Scp409Faces Table = Scp409Faces.of(Model);
        if (Table.isEmpty()) {
            Table = Scp409Faces.box(Owner.getBbWidth() / Math.max(0.01f, Owner.getScale()), Owner.getBbHeight() / Math.max(0.01f, Owner.getScale()));
        }
        boolean WithCore = nearCamera(Owner);
        Table.place(Pose, Model.young, Table.anchors(Owner.getUUID()), Found.Growth(), Found.Stage(), null,
                (Where, Variant, Size) -> Scp409Crystals.cluster(Where, Buffers, Light, Variant, Size, WithCore));
    }
}
