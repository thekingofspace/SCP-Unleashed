package net.rai.scp_unleashed.scp001day.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;

public class Scp001DayFleshRenderer extends SharedGeoRenderer<Scp001DayFlesh> {
    public static final float CrawlerScale = 1.0f;
    public static final float ClingerScale = 1.0f;
    public static final float LurkerScale = 1.15f;
    public static final float MassScale = 1.6f;
    private final float Base;

    public Scp001DayFleshRenderer(EntityRendererProvider.Context Context, Scp001DayForm Form) {
        super(Context, new Scp001DayFleshModel(Form));
        Base = switch (Form) {
            case Crawler -> CrawlerScale;
            case Clinger -> ClingerScale;
            case Lurker -> LurkerScale;
            case Mass -> MassScale;
        };
        this.shadowRadius = 0.4f * Form.Width;
    }

    @Override
    public void render(Scp001DayFlesh Owner, float Yaw, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Scale = Base * Owner.sizeScale();
        withScale(Scale);
        super.render(Owner, Yaw, Partial, Pose, Buffers, Light);
    }
}
