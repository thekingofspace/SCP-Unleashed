package net.rai.scp_unleashed.intro.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.DClassRenderer;
import net.rai.scp_unleashed.intro.IntroScientist;

public class IntroScientistRenderer extends DClassRenderer<IntroScientist> {
    public IntroScientistRenderer(EntityRendererProvider.Context Context) {
        super(Context, new IntroScientistModel());
    }
}
