package net.rai.scp_unleashed.z0004.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.z0004.Z0004DeckSlot;
import net.rai.scp_unleashed.z0004.Z0004Ship;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class Z0004ShipRenderer extends SharedGeoRenderer<Z0004Ship> {
    public static final float Scale = 2.0f;
    public static final float ShadowRadius = 0.0f;
    public static final float CrewScale = 1.0f;
    public static final float CaptainScale = 1.12f;
    public static final double DeckRange = 64.0;
    private final Map<Z0004Ship, List<Z0004DeckHand>> Crews = new WeakHashMap<>();
    private final Z0004DeckRenderer Deck = new Z0004DeckRenderer();

    public Z0004ShipRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Z0004ShipGeoModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
    }

    @Override
    protected float getDeathMaxRotation(Z0004Ship Owner) {
        return 0.0f;
    }

    @Override
    public int getPackedOverlay(Z0004Ship Owner, float U, float PartialTick) {
        return Owner.sinking() ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(Owner, U, PartialTick);
    }

    @Override
    public void render(Z0004Ship Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
        if (entityRenderDispatcher.distanceToSqr(Owner) > DeckRange * DeckRange || Owner.isDeadOrDying() && Owner.deathTime > Z0004Ship.ReleaseTicks) {
            return;
        }
        for (Z0004DeckHand Hand : crewOf(Owner)) {
            GeoBone Found = getGeoModel().getAnimationProcessor().getBone(Hand.slot().Bone());
            if (Found == null || !Found.isTrackingMatrices()) {
                continue;
            }
            float Size = (Hand.slot().captain() ? CaptainScale : CrewScale) / Scale;
            Pose.pushPose();
            Pose.mulPose(Found.getLocalSpaceMatrix());
            Pose.scale(Size, Size, Size);
            Deck.draw(Pose, Hand, Buffers, Light, PartialTick);
            Pose.popPose();
        }
    }

    private List<Z0004DeckHand> crewOf(Z0004Ship Owner) {
        return Crews.computeIfAbsent(Owner, Ship -> {
            List<Z0004DeckHand> Made = new ArrayList<>();
            for (Z0004DeckSlot Slot : Z0004DeckSlot.All) {
                Made.add(new Z0004DeckHand(Ship, Slot));
            }
            return Made;
        });
    }
}
