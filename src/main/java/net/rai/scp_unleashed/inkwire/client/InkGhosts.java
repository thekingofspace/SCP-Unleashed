package net.rai.scp_unleashed.inkwire.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.inkwire.InvisibleInkItem;
import net.rai.scp_unleashed.wire.WireTier;

import java.util.EnumMap;
import java.util.Map;

public final class InkGhosts {
    public static final int WireColour = 0x80FFFFFF;
    public static final float GhostRed = 0.66f;
    public static final float GhostGreen = 0.88f;
    public static final float GhostBlue = 1.0f;
    public static final float GhostAlpha = 0.45f;
    public static final int MinimumGlow = 9;
    private static final long ModelSeed = 42L;
    private static final RenderType BlockGhost = RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
    private static final Map<WireTier, RenderType> WireGhosts = new EnumMap<>(WireTier.class);

    private InkGhosts() {
    }

    public static boolean revealing() {
        LocalPlayer User = Minecraft.getInstance().player;
        return User != null && InvisibleInkItem.isHeldBy(User);
    }

    public static int glow(int Light) {
        return LightTexture.pack(Math.max(LightTexture.block(Light), MinimumGlow), LightTexture.sky(Light));
    }

    public static MultiBufferSource wireBuffers(MultiBufferSource Buffers, WireTier Tier) {
        RenderType Ghost = WireGhosts.computeIfAbsent(Tier, Key -> RenderType.entityTranslucent(
                ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/ink_ghost/" + Key.getTextureName() + ".png")));
        return Ignored -> Buffers.getBuffer(Ghost);
    }

    public static void block(BlockEntity Tile, PoseStack Poses, MultiBufferSource Buffers, int PackedLight) {
        Level World = Tile.getLevel();
        BlockState State = Tile.getBlockState();
        BlockPos Pos = Tile.getBlockPos();
        BakedModel Baked = Minecraft.getInstance().getBlockRenderer().getBlockModel(State);
        VertexConsumer Sink = Buffers.getBuffer(BlockGhost);
        PoseStack.Pose Last = Poses.last();
        int Light = glow(PackedLight);
        RandomSource Seeded = RandomSource.create();
        for (Direction Side : Direction.values()) {
            BlockPos Next = Pos.relative(Side);
            if (World != null && World.getBlockState(Next).isSolidRender(World, Next)) {
                continue;
            }
            Seeded.setSeed(ModelSeed);
            for (BakedQuad Quad : Baked.getQuads(State, Side, Seeded, ModelData.EMPTY, null)) {
                Sink.putBulkData(Last, Quad, GhostRed, GhostGreen, GhostBlue, GhostAlpha, Light, OverlayTexture.NO_OVERLAY);
            }
        }
        Seeded.setSeed(ModelSeed);
        for (BakedQuad Quad : Baked.getQuads(State, null, Seeded, ModelData.EMPTY, null)) {
            Sink.putBulkData(Last, Quad, GhostRed, GhostGreen, GhostBlue, GhostAlpha, Light, OverlayTexture.NO_OVERLAY);
        }
    }
}
