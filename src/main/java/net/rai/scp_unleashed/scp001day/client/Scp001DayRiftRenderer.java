package net.rai.scp_unleashed.scp001day.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayRift;
import net.rai.scp_unleashed.shadercompat.client.RiftFallback;

public class Scp001DayRiftRenderer extends EntityRenderer<Scp001DayRift> {
    public static final int Glow = 0xFFF6DE;
    public static final float Half = 1.15f;

    public Scp001DayRiftRenderer(EntityRendererProvider.Context Context) {
        super(Context);
    }

    @Override
    public void render(Scp001DayRift Rift, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Open = Rift.openness(Partial);
        if (Open <= 0.001f) {
            return;
        }
        Camera View = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        double OffsetX = Rift.getX() - Eye.x;
        double OffsetZ = Rift.getZ() - Eye.z;
        double Length = Math.sqrt(OffsetX * OffsetX + OffsetZ * OffsetZ);
        float DepthX = Length > 0.001 ? (float) (OffsetX / Length) : 0.0f;
        float DepthZ = Length > 0.001 ? (float) (OffsetZ / Length) : 1.0f;
        int Colour = (Mth.clamp(Math.round(Open * 255.0f), 0, 255) << 24) | Glow;
        int Variant = Rift.getId() & 4095;
        int Profile = 1 + 16 * Mth.clamp(Math.round(Half * 100.0f), 5, 2000);
        RiftFallback.draw(Pose.last(), Buffers, 0.0f, Half + 0.1f, 0.0f, -DepthZ, DepthX, Half, Colour, 120, 0, Variant, Profile);
        super.render(Rift, Yaw, Partial, Pose, Buffers, Light);
    }

    @Override
    public boolean shouldRender(Scp001DayRift Rift, net.minecraft.client.renderer.culling.Frustum Culling, double X, double Y, double Z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp001DayRift Rift) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
