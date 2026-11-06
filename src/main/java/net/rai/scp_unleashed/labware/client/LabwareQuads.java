package net.rai.scp_unleashed.labware.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

public final class LabwareQuads {
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Random = RandomSource.create();

    private LabwareQuads() {
    }

    public static void render(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, Level World, int Light, int Overlay) {
        Vector3f Normal = new Vector3f();
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Direction Facing = Quad.getDirection();
                Last.normal().transform(Normal.set(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ()));
                float Shade = World.getShade(Direction.getNearest(Normal.x(), Normal.y(), Normal.z()), Quad.isShade());
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, Light, Overlay);
            }
        }
    }
}
