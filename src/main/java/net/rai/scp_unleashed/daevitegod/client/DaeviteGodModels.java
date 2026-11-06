package net.rai.scp_unleashed.daevitegod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class DaeviteGodModels {
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Random = RandomSource.create();
    public static final ModelResourceLocation Spike = model("block/daevite_god/spike");
    public static final ModelResourceLocation SpikeThin = model("block/daevite_god/spike_thin");

    private DaeviteGodModels() {
    }

    private static ModelResourceLocation model(String Path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    public static List<ModelResourceLocation> all() {
        return List.of(Spike, SpikeThin);
    }

    public static BakedModel get(ModelResourceLocation Location) {
        return Minecraft.getInstance().getModelManager().getModel(Location);
    }

    public static void render(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, int Light, int Overlay) {
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = Quad.isShade() ? shade(Quad.getDirection()) : 1.0f;
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, Light, Overlay);
            }
        }
    }

    private static float shade(Direction Side) {
        return switch (Side) {
            case UP -> 1.0f;
            case DOWN -> 0.5f;
            case NORTH, SOUTH -> 0.8f;
            default -> 0.65f;
        };
    }
}
