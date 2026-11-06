package net.rai.scp_unleashed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.DuctWallBlock;
import net.rai.scp_unleashed.block.entity.DuctWallBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DuctWallModel extends BakedModelWrapper<BakedModel> {
    public static final String PortsFolder = "block/layer/duct_wall/";

    public DuctWallModel(BakedModel Original) {
        super(Original);
    }

    @Nullable
    private static BlockState disguiseOf(ModelData Data) {
        BlockState Look = Data.get(DuctWallBlockEntity.DisguiseProperty);
        return Look == null || Look.isAir() ? null : Look;
    }

    private static BakedModel modelOf(BlockState Look) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Look);
    }

    private static List<BakedModel> portsOf(BlockState State) {
        String Kind = State.getBlock() instanceof DuctWallBlock Wall ? Wall.getKind().getSerializedName() : "pipe";
        List<BakedModel> Found = new ArrayList<>();
        for (Direction Face : Direction.values()) {
            if (DuctWallBlock.showsPort(State, Face)) {
                ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, PortsFolder + Kind + "_port_" + Face.getSerializedName());
                Found.add(Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(Id)));
            }
        }
        return Found;
    }

    private static ChunkRenderTypeSet portTypes(BlockState State) {
        List<ChunkRenderTypeSet> Sets = new ArrayList<>();
        for (BakedModel Port : portsOf(State)) {
            Sets.add(Port.getRenderTypes(State, RandomSource.create(42L), ModelData.EMPTY));
        }
        return Sets.isEmpty() ? ChunkRenderTypeSet.none() : ChunkRenderTypeSet.union(Sets);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState State, RandomSource Random, ModelData Data) {
        BlockState Look = disguiseOf(Data);
        if (Look == null) {
            return originalModel.getRenderTypes(State, Random, Data);
        }
        return ChunkRenderTypeSet.union(modelOf(Look).getRenderTypes(Look, RandomSource.create(42L), ModelData.EMPTY), portTypes(State));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState State, @Nullable Direction Side, RandomSource Random, ModelData Data, @Nullable RenderType Type) {
        BlockState Look = disguiseOf(Data);
        if (State == null || Look == null) {
            return originalModel.getQuads(State, Side, Random, Data, Type);
        }
        List<BakedQuad> Quads = new ArrayList<>();
        BakedModel LookModel = modelOf(Look);
        if (Type == null || LookModel.getRenderTypes(Look, RandomSource.create(42L), ModelData.EMPTY).contains(Type)) {
            Quads.addAll(LookModel.getQuads(Look, Side, Random, ModelData.EMPTY, Type));
        }
        for (BakedModel Port : portsOf(State)) {
            if (Type == null || Port.getRenderTypes(State, RandomSource.create(42L), ModelData.EMPTY).contains(Type)) {
                Quads.addAll(Port.getQuads(State, Side, Random, ModelData.EMPTY, Type));
            }
        }
        return Quads;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData Data) {
        BlockState Look = disguiseOf(Data);
        return Look == null ? originalModel.getParticleIcon(Data) : modelOf(Look).getParticleIcon(ModelData.EMPTY);
    }
}
