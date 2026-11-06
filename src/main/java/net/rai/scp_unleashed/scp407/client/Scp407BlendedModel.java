package net.rai.scp_unleashed.scp407.client;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import net.rai.scp_unleashed.scp407.Scp407Carpets;
import net.rai.scp_unleashed.scp407.Scp407GrowthBlock;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Sheets;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp407BlendedModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<int[]> Overlays = new ModelProperty<>();
    public static final ModelProperty<int[]> Skirts = new ModelProperty<>();
    public static final int Alternates = 4;
    public static final float EdgeLift = 1.2f;
    public static final float EdgeStep = 0.1f;
    public static final float SpeckLift = 1.7f;
    private static final String[] Stages = {"", "_brown", "_grey"};
    private static BakedQuad[] Cache = new BakedQuad[6 * Scp407Carpets.Slots * Scp407Carpets.Kinds * 3];
    private static BakedQuad[][] Slabs = new BakedQuad[6 * Scp407Carpets.Kinds * Alternates * 3][];

    public Scp407BlendedModel(BakedModel Original) {
        super(Original);
    }

    public static synchronized void forget() {
        Cache = new BakedQuad[6 * Scp407Carpets.Slots * Scp407Carpets.Kinds * 3];
        Slabs = new BakedQuad[6 * Scp407Carpets.Kinds * Alternates * 3][];
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter World, BlockPos Pos, BlockState State, ModelData Data) {
        ModelData Base = super.getModelData(World, Pos, State, Data);
        if (!(State.getBlock() instanceof Scp407GrowthBlock Growth)) {
            return Base;
        }
        List<Direction> Faces = new ArrayList<>(6);
        Growth.carpetFaces(State, Faces);
        List<Direction> Sides = new ArrayList<>(6);
        Growth.skirts(World, Pos, State, Sides);
        if (Faces.isEmpty() && Sides.isEmpty()) {
            return Base;
        }
        IntArrayList Codes = new IntArrayList(8);
        IntArrayList Pieces = new IntArrayList(6);
        int Kind = Scp407Carpets.variantAt(Pos);
        int Alt = (int) (Scp407Carpets.hash(Pos.getX(), Pos.getY(), Pos.getZ(), 9091) * Alternates) % Alternates;
        for (Direction Face : Faces) {
            Scp407Carpets.overlays(Pos, Face, Codes);
        }
        for (Direction Side : Sides) {
            Pieces.add((Side.ordinal() * Scp407Carpets.Kinds + Kind) * Alternates + Alt);
            Scp407Carpets.overlays(Pos, Side, Codes);
        }
        return Base.derive().with(Overlays, Codes.toIntArray()).with(Skirts, Pieces.toIntArray()).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState State, @Nullable Direction Side, RandomSource Rand, ModelData Data, @Nullable RenderType Type) {
        List<BakedQuad> Base = super.getQuads(State, Side, Rand, Data, Type);
        if (State == null || Type != null && Type != RenderType.cutout() || !State.hasProperty(Scp407GrowthBlock.Wither)) {
            return Base;
        }
        int[] Codes = Data.get(Overlays);
        int[] Pieces = Data.get(Skirts);
        boolean HasCodes = Codes != null && Codes.length > 0;
        boolean HasPieces = Pieces != null && Pieces.length > 0;
        int Stage = State.getValue(Scp407GrowthBlock.Wither);
        if (Side != null) {
            if (!HasPieces) {
                return Base;
            }
            List<BakedQuad> Out = null;
            for (int Piece : Pieces) {
                BakedQuad[] Slab = slab(Piece, Stage);
                for (int Index = 1; Index < Slab.length; Index++) {
                    if (Slab[Index].getDirection() == Side) {
                        if (Out == null) {
                            Out = new ArrayList<>(Base);
                        }
                        Out.add(Slab[Index]);
                    }
                }
            }
            return Out == null ? Base : Out;
        }
        if (!HasCodes && !HasPieces) {
            return Base;
        }
        List<BakedQuad> Out = new ArrayList<>(Base.size() + (HasCodes ? Codes.length : 0) + (HasPieces ? Pieces.length : 0));
        Out.addAll(Base);
        if (HasPieces) {
            for (int Piece : Pieces) {
                Out.add(slab(Piece, Stage)[0]);
            }
        }
        if (HasCodes) {
            for (int Code : Codes) {
                Out.add(quad(Code, Stage));
            }
        }
        return Out;
    }

    private static synchronized BakedQuad[] slab(int Piece, int Stage) {
        int Index = Piece * 3 + Stage;
        BakedQuad[] Found = Slabs[Index];
        if (Found == null) {
            int Alt = Piece % Alternates;
            int Kind = Piece / Alternates % Scp407Carpets.Kinds;
            Direction Face = Direction.from3DDataValue(Piece / Alternates / Scp407Carpets.Kinds);
            boolean Wall = Face.getAxis() != Direction.Axis.Y;
            TextureAtlasSprite Sprite = sprite("block/scp407/carpet/" + Scp407Carpets.Names[Kind] + (Wall ? "_wall_" : "_floor_") + Alt + Stages[Stage]);
            List<Scp407Sheets.Sheet> Sheets = Scp407Sheets.pad(Face);
            Found = new BakedQuad[Sheets.size()];
            for (int Sheet = 0; Sheet < Sheets.size(); Sheet++) {
                Found[Sheet] = bake(Sheets.get(Sheet), Sprite);
            }
            Slabs[Index] = Found;
        }
        return Found;
    }

    private static synchronized BakedQuad quad(int Code, int Stage) {
        int Index = Code * 3 + Stage;
        BakedQuad Found = Cache[Index];
        if (Found == null) {
            Found = build(Scp407Carpets.faceOf(Code), Scp407Carpets.slotOf(Code), Scp407Carpets.kindOf(Code), Stage);
            Cache[Index] = Found;
        }
        return Found;
    }

    private static BakedQuad build(Direction Face, int Slot, int Kind, int Stage) {
        String Name = Scp407Carpets.Names[Kind] + (Slot == Scp407Carpets.SpeckSlot ? "_speck" : "_fringe") + Stages[Stage];
        float Pixels = Slot == Scp407Carpets.SpeckSlot ? SpeckLift : EdgeLift + EdgeStep * Slot;
        return bake(Scp407Sheets.layer(Face, Pixels, Slot), sprite("block/scp407/carpet/" + Name));
    }

    private static TextureAtlasSprite sprite(String Texture) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Scp407Registry.location(Texture));
    }

    private static BakedQuad bake(Scp407Sheets.Sheet Sheet, TextureAtlasSprite Sprite) {
        Direction Normal = Sheet.Normal();
        QuadBakingVertexConsumer Builder = new QuadBakingVertexConsumer();
        Builder.setSprite(Sprite);
        Builder.setDirection(Normal);
        Builder.setShade(true);
        Builder.setHasAmbientOcclusion(false);
        for (int Vertex = 0; Vertex < 4; Vertex++) {
            float[] Point = Sheet.Points()[Vertex];
            float[] Uv = Sheet.Uvs()[Vertex];
            Builder.addVertex(Point[0], Point[1], Point[2]);
            Builder.setColor(255, 255, 255, 255);
            Builder.setUv(Sprite.getU(Uv[0]), Sprite.getV(Uv[1]));
            Builder.setUv2(0, 0);
            Builder.setNormal(Normal.getStepX(), Normal.getStepY(), Normal.getStepZ());
        }
        return Builder.bakeQuad();
    }
}
