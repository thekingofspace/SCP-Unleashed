package net.rai.scp_unleashed.client;

import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.block.FaceLayer;
import net.rai.scp_unleashed.block.entity.RenderableBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RenderableBlockModel extends BakedModelWrapper<BakedModel> {
    private static final ChunkRenderTypeSet CutoutTypes = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final ChunkRenderTypeSet TranslucentTypes = ChunkRenderTypeSet.of(RenderType.translucent());
    private static final FaceBakery Bakery = new FaceBakery();

    private final Map<PlacedLayer, List<BakedQuad>> QuadCache = new ConcurrentHashMap<>();

    public RenderableBlockModel(BakedModel Original) {
        super(Original);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState State, RandomSource Random, ModelData Data) {
        ChunkRenderTypeSet BaseTypes = originalModel.getRenderTypes(State, Random, Data);
        Map<Direction, List<FaceLayer>> Layers = Data.get(RenderableBlockEntity.LayersProperty);
        if (Layers == null || Layers.isEmpty()) {
            return BaseTypes;
        }
        boolean AnyCutout = false;
        boolean AnyTranslucent = false;
        for (List<FaceLayer> FaceLayers : Layers.values()) {
            boolean Translucent = false;
            for (FaceLayer Layer : FaceLayers) {
                Translucent |= Layer.Translucent();
                if (Translucent) {
                    AnyTranslucent = true;
                } else {
                    AnyCutout = true;
                }
            }
        }
        return ChunkRenderTypeSet.union(BaseTypes, AnyCutout ? CutoutTypes : ChunkRenderTypeSet.none(), AnyTranslucent ? TranslucentTypes : ChunkRenderTypeSet.none());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState State, @Nullable Direction Side, RandomSource Random, ModelData Data, @Nullable RenderType Type) {
        Map<Direction, List<FaceLayer>> Layers = Data.get(RenderableBlockEntity.LayersProperty);
        if (State == null || Layers == null || Layers.isEmpty()) {
            return originalModel.getQuads(State, Side, Random, Data, Type);
        }
        List<BakedQuad> Quads = new ArrayList<>();
        if (Type == null || originalModel.getRenderTypes(State, RandomSource.create(0L), Data).contains(Type)) {
            Quads.addAll(originalModel.getQuads(State, Side, Random, Data, Type));
        }
        if (Side != null) {
            boolean Translucent = false;
            for (FaceLayer Layer : Layers.getOrDefault(Side, List.of())) {
                Translucent |= Layer.Translucent();
                if (Type == null || Type == (Translucent ? RenderType.translucent() : RenderType.cutout())) {
                    Quads.addAll(QuadCache.computeIfAbsent(new PlacedLayer(Layer, Side), RenderableBlockModel::bakeLayer));
                }
            }
        }
        return Quads;
    }

    private static List<BakedQuad> bakeLayer(PlacedLayer Placed) {
        return switch (Placed.Layer().LayerKind()) {
            case Texture -> bakeTextureLayer(Placed.Layer(), Placed.Face());
            case Model -> bakeModelLayer(Placed.Layer(), Placed.Face());
        };
    }

    private static List<BakedQuad> bakeTextureLayer(FaceLayer Layer, Direction Face) {
        TextureAtlasSprite Sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Layer.Id());
        BlockElementFace ElementFace = new BlockElementFace(Face, BlockElementFace.NO_TINT, "", new BlockFaceUV(new float[]{0.0f, 0.0f, 16.0f, 16.0f}, Layer.spinDegrees()));
        return List.of(Bakery.bakeQuad(new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(16.0f, 16.0f, 16.0f), ElementFace, Sprite, Face, BlockModelRotation.X0_Y0, null, true));
    }

    private static List<BakedQuad> bakeModelLayer(FaceLayer Layer, Direction Face) {
        BakedModel LayerModel = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(Layer.Id()));
        Transformation Placement = placement(Face, Layer.spinDegrees());
        IQuadTransformer Mover = QuadTransformers.applying(Placement);
        RandomSource Random = RandomSource.create(0L);
        List<BakedQuad> Source = new ArrayList<>(LayerModel.getQuads(null, null, Random, ModelData.EMPTY, null));
        for (Direction Side : Direction.values()) {
            Source.addAll(LayerModel.getQuads(null, Side, Random, ModelData.EMPTY, null));
        }
        List<BakedQuad> Placed = new ArrayList<>(Source.size());
        for (BakedQuad Quad : Source) {
            BakedQuad Moved = Mover.process(Quad);
            Placed.add(new BakedQuad(Moved.getVertices(), Moved.getTintIndex(), Placement.rotateTransform(Moved.getDirection()), Moved.getSprite(), Moved.isShade(), Moved.hasAmbientOcclusion()));
        }
        return List.copyOf(Placed);
    }

    private static Transformation placement(Direction Face, int SpinDegrees) {
        BlockModelRotation FaceRotation = switch (Face) {
            case NORTH -> BlockModelRotation.X0_Y0;
            case EAST -> BlockModelRotation.X0_Y90;
            case SOUTH -> BlockModelRotation.X0_Y180;
            case WEST -> BlockModelRotation.X0_Y270;
            case UP -> BlockModelRotation.X270_Y0;
            case DOWN -> BlockModelRotation.X90_Y0;
        };
        Matrix4f Matrix = new Matrix4f(FaceRotation.getRotation().getMatrix()).rotateZ(SpinDegrees * Mth.DEG_TO_RAD);
        for (int Row = 0; Row < 3; Row++) {
            for (int Column = 0; Column < 3; Column++) {
                Matrix.setRowColumn(Row, Column, Math.round(Matrix.getRowColumn(Row, Column)));
            }
        }
        return new Transformation(Matrix).blockCenterToCorner();
    }

    private record PlacedLayer(FaceLayer Layer, Direction Face) {
    }
}
