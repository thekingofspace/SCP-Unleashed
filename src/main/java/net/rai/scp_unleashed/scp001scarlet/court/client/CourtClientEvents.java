package net.rai.scp_unleashed.scp001scarlet.court.client;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.court.CourtKind;
import net.rai.scp_unleashed.scp001scarlet.court.CourtMinion;
import net.rai.scp_unleashed.scp001scarlet.court.CourtProp;
import net.rai.scp_unleashed.scp001scarlet.court.ScarletCourtRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class CourtClientEvents {
    public static final float DaughterScale = 2.0f;

    private CourtClientEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        for (CourtKind Kind : CourtKind.values()) {
            EntityType<CourtMinion> Type = (EntityType<CourtMinion>) ScarletCourtRegistry.minionType(Kind);
            float Shadow = Kind.flying() ? 0.25f : Math.min(1.2f, Kind.width() * 0.7f);
            Event.registerEntityRenderer(Type, Context -> new CourtRenderer<>(Context, Kind.id(), 1.0f, Shadow));
        }
        Event.registerEntityRenderer(ScarletCourtRegistry.ThornsType.get(), Context -> new CourtRenderer<>(Context, "scarlet_daughter_thorns", DaughterScale, 1.6f));
        Event.registerEntityRenderer(ScarletCourtRegistry.BrideType.get(), Context -> new CourtRenderer<>(Context, "scarlet_daughter_bride", DaughterScale, 1.6f));
        Event.registerEntityRenderer(ScarletCourtRegistry.EyesType.get(), Context -> new CourtRenderer<>(Context, "scarlet_daughter_eyes", DaughterScale, 1.4f));
        Event.registerEntityRenderer(ScarletCourtRegistry.MoonType.get(), Context -> new CourtRenderer<>(Context, "scarlet_daughter_moon", DaughterScale, 1.4f));
        Event.registerEntityRenderer(ScarletCourtRegistry.EyesImageType.get(), Context -> new CourtRenderer<>(Context, "scarlet_daughter_eyes", DaughterScale, 0.0f));
        prop(Event, ScarletCourtRegistry.ThornWallType.get(), "scarlet_thorn_wall", 0.6f);
        prop(Event, ScarletCourtRegistry.BriarCageType.get(), "scarlet_briar_cage", 0.9f);
        prop(Event, ScarletCourtRegistry.ChainAnchorType.get(), "scarlet_chain_anchor", 0.5f);
        prop(Event, ScarletCourtRegistry.ThornPatchType.get(), "scarlet_thorn_patch", 0.0f);
        Event.registerEntityRenderer(ScarletCourtRegistry.BoltType.get(), NoopRenderer::new);
    }

    private static void prop(EntityRenderersEvent.RegisterRenderers Event, EntityType<CourtProp> Type, String Name, float Shadow) {
        Event.registerEntityRenderer(Type, Context -> new CourtRenderer<>(Context, Name, 1.0f, Shadow));
    }
}
