package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class StorageQuads {
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Random = RandomSource.create();

    private StorageQuads() {
    }

    public static void render(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, @Nullable BlockState State, Level World, float Red, float Green, float Blue,
                              float Alpha, int Light, int Overlay, boolean Shaded) {
        Vector3f Normal = new Vector3f();
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(State, Side, Random, ModelData.EMPTY, null)) {
                float Shade = 1.0f;
                if (Shaded) {
                    Direction Facing = Quad.getDirection();
                    Last.normal().transform(Normal.set(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ()));
                    Shade = World.getShade(Direction.getNearest(Normal.x(), Normal.y(), Normal.z()), Quad.isShade());
                }
                Consumer.putBulkData(Last, Quad, Shade * Red, Shade * Green, Shade * Blue, Alpha, Light, Overlay);
            }
        }
    }
}
