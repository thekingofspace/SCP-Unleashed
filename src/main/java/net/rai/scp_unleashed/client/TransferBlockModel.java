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
import net.rai.scp_unleashed.block.TransferBlock;
import net.rai.scp_unleashed.block.entity.TransferBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TransferBlockModel extends BakedModelWrapper<BakedModel> {
    public static final String SocketsFolder = "block/layer/transfer/";

    public TransferBlockModel(BakedModel Original) {
        super(Original);
    }

    @Nullable
    private static BlockState disguiseOf(ModelData Data) {
        BlockState Look = Data.get(TransferBlockEntity.DisguiseProperty);
        return Look == null || Look.isAir() ? null : Look;
    }

    private static int socketsOf(ModelData Data) {
        Integer Mask = Data.get(TransferBlockEntity.SocketsProperty);
        return Mask == null ? 0 : Mask;
    }

    private static BakedModel modelOf(BlockState Look) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(Look);
    }

    private static BakedModel socketOf(BlockState State, Direction Side) {
        String Tier = State.getBlock() instanceof TransferBlock Transfer ? Transfer.getConnectorTier().getSerializedName() : "basic";
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, SocketsFolder + Tier + "_socket_" + Side.getSerializedName());
        return Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(Id));
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState State, RandomSource Random, ModelData Data) {
        BlockState Look = disguiseOf(Data);
        if (Look == null) {
            return originalModel.getRenderTypes(State, Random, Data);
        }
        return ChunkRenderTypeSet.union(modelOf(Look).getRenderTypes(Look, RandomSource.create(42L), ModelData.EMPTY), originalModel.getRenderTypes(State, Random, ModelData.EMPTY));
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
        int Sockets = socketsOf(Data);
        if (Sockets != 0 && (Type == null || Type == RenderType.solid())) {
            for (Direction Face : Direction.values()) {
                if ((Sockets & (1 << Face.get3DDataValue())) != 0) {
                    Quads.addAll(socketOf(State, Face).getQuads(State, Side, Random, ModelData.EMPTY, Type));
                }
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
