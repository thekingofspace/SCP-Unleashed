package net.rai.scp_unleashed.scp3008.staff.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp3008.staff.Scp3008Staff;

public class Scp3008StaffRenderer extends SharedGeoRenderer<Scp3008Staff> {
    public static final float Scale = 1.35f;

    public Scp3008StaffRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp3008StaffGeoModel());
        withScale(Scale);
    }

    @Override
    public void render(Scp3008Staff Staff, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Staff.setRenderPartial(PartialTick);
        shadowRadius = Staff.variant().Shadow;
        super.render(Staff, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
