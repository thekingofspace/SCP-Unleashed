package net.rai.scp_unleashed.raid.hostile.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.mtf.MtfHelicopter;
import net.rai.scp_unleashed.mtf.client.MtfHelicopterRenderer;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;

public class ChaosHelicopterRenderer extends MtfHelicopterRenderer {
    public static final ResourceLocation TexturePath = HostileRegistry.location("textures/entity/chaos_insurgency/chaos_helicopter.png");

    public ChaosHelicopterRenderer(EntityRendererProvider.Context Context) {
        super(Context);
    }

    @Override
    public ResourceLocation getTextureLocation(MtfHelicopter Owner) {
        return TexturePath;
    }
}
