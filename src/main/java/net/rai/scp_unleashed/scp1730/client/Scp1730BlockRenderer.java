package net.rai.scp_unleashed.scp1730.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp1730BlockRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    private final double Wide;
    private final double Tall;
    private final int Distance;

    public Scp1730BlockRenderer(BlockEntityRendererProvider.Context Context, String Name, double Wide, double Tall, int Distance) {
        super(new Scp1730BlockModel<>(Name));
        this.Wide = Wide;
        this.Tall = Tall;
        this.Distance = Distance;
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack Pose, T Owner, software.bernie.geckolib.cache.object.BakedGeoModel Baked,
                               net.minecraft.client.renderer.RenderType Type, net.minecraft.client.renderer.MultiBufferSource Buffers,
                               com.mojang.blaze3d.vertex.VertexConsumer Buffer, float Partial, int Light, int Overlay) {
                if (Owner instanceof net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlockEntity Turret && (Turret.wrecked() || !Turret.live())) {
                    return;
                }
                super.render(Pose, Owner, Baked, Type, Buffers, Buffer, Partial, Light, Overlay);
            }
        });
    }

    @Override
    public int getViewDistance() {
        return Distance;
    }

    @Override
    public boolean shouldRenderOffScreen(T Owner) {
        return Wide > 1.0;
    }

    @Override
    public void render(T Owner, float Partial, com.mojang.blaze3d.vertex.PoseStack Pose, net.minecraft.client.renderer.MultiBufferSource Buffers, int Light, int Overlay) {
        super.render(Owner, Partial, Pose, Buffers, Light, Overlay);
        if (Owner instanceof net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity Folder && Folder.fieldUp()) {
            Pose.pushPose();
            Pose.translate(0.5, net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity.FieldLift, 0.5);
            float Time = Owner.getLevel() == null ? 0.0f : (Owner.getLevel().getGameTime() + Partial) * 0.004f;
            Scp1730Dome.render(Pose, Buffers, net.rai.scp_unleashed.scp1730.Scp1730Anchors.FolderRadius, 0.08f, Time);
            Pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(T Owner) {
        if (Owner instanceof net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity Folder && Folder.fieldUp()) {
            return new AABB(Owner.getBlockPos()).inflate(net.rai.scp_unleashed.scp1730.Scp1730Anchors.FolderRadius + 1.0);
        }
        return new AABB(Owner.getBlockPos()).inflate(Wide, 0.5, Wide).expandTowards(0.0, Tall, 0.0);
    }
}
