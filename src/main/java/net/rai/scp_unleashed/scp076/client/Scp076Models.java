package net.rai.scp_unleashed.scp076.client;

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
import net.rai.scp_unleashed.scp076.Scp076DoorBlock;
import net.rai.scp_unleashed.scp076.Scp076DoorBlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class Scp076Models {
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final RandomSource Random = RandomSource.create();
    public static final ModelResourceLocation LockHousing = model("block/scp076/lock_housing");
    public static final ModelResourceLocation LockDial = model("block/scp076/lock_dial");
    public static final ModelResourceLocation LockBolt = model("block/scp076/lock_bolt");
    public static final ModelResourceLocation DoorGlyph = model("block/scp076/door_glyph");
    public static final ModelResourceLocation CoffinLid = model("block/scp076/coffin_lid");
    public static final ModelResourceLocation Knife = model("block/scp076/knife");

    private Scp076Models() {
    }

    private static ModelResourceLocation model(String Path) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    public static ModelResourceLocation glyph(int Lock) {
        return model("block/scp076/lock_glyph_" + Lock);
    }

    public static ModelResourceLocation panel(int Column, int Row) {
        return model("block/scp076/door_panel_" + Column + "_" + Row);
    }

    public static List<ModelResourceLocation> all() {
        List<ModelResourceLocation> Found = new ArrayList<>(List.of(LockHousing, LockDial, LockBolt, DoorGlyph, CoffinLid, Knife));
        for (int Lock = 0; Lock < Scp076DoorBlockEntity.LockCount; Lock++) {
            Found.add(glyph(Lock));
        }
        for (int Column = 0; Column < Scp076DoorBlock.Columns; Column++) {
            for (int Row = 0; Row < Scp076DoorBlock.Rows; Row++) {
                Found.add(panel(Column, Row));
            }
        }
        return Found;
    }

    public static BakedModel get(ModelResourceLocation Location) {
        return Minecraft.getInstance().getModelManager().getModel(Location);
    }

    public static void render(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, int Light, int Overlay, float Tint, float Shading, Level World,
                              Direction Facing) {
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = Shading > 0.0f ? World.getShade(turned(Quad.getDirection(), Facing), Quad.isShade()) : 1.0f;
                Consumer.putBulkData(Last, Quad, Shade * Tint, Shade * Tint, Shade * Tint, 1.0f, Light, Overlay);
            }
        }
    }

    public static Direction turned(Direction Side, Direction Facing) {
        if (!Side.getAxis().isHorizontal()) {
            return Side;
        }
        Direction Result = Side;
        int Turns = Math.floorMod(Facing.get2DDataValue(), 4);
        for (int Step = 0; Step < Turns; Step++) {
            Result = Result.getClockWise();
        }
        return Result;
    }
}
