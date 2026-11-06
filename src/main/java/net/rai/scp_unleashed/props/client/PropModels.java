package net.rai.scp_unleashed.props.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import org.joml.Vector3f;

public final class PropModels {
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    private PropModels() {
    }

    public static ModelResourceLocation standalone(String Path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    public static BakedModel get(ModelResourceLocation Location) {
        return Minecraft.getInstance().getModelManager().getModel(Location);
    }

    public static void render(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, Level World, int Light, int Overlay, RandomSource Random) {
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Vector3f Normal = Last.normal().transform(Quad.getDirection().step());
                float Shade = World.getShade(Direction.getNearest(Normal.x(), Normal.y(), Normal.z()), Quad.isShade());
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, Light, Overlay);
            }
        }
    }
}
