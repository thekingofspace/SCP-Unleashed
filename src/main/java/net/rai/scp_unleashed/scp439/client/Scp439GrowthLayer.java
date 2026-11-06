package net.rai.scp_unleashed.scp439.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.inventory.InventoryMenu;
import net.rai.scp_unleashed.playerclone.client.PlayerCloneSkins;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.Scp439Look;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp439GrowthLayer extends GeoRenderLayer<Scp439Hive> {
    public Scp439GrowthLayer(GeoRenderer<Scp439Hive> Renderer) {
        super(Renderer);
    }

    public static String shapeFor(Scp439Hive Hive) {
        Scp439Look Seen = Hive.look();
        if (Seen.villagerLike()) {
            return Scp439GrowthShapes.Villager;
        }
        if (Seen.Kind() == Scp439Look.KindPlayer && PlayerCloneSkins.skinOf(Hive.profile(), Hive.getUUID()).model() == PlayerSkin.Model.SLIM) {
            return Scp439GrowthShapes.Slim;
        }
        return Scp439GrowthShapes.Humanoid;
    }

    @Override
    public void renderForBone(PoseStack Pose, Scp439Hive Hive, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        if (Hive.isInvisible()) {
            return;
        }
        Scp439GrowthShapes.Shape Found = Scp439GrowthShapes.get(shapeFor(Hive));
        if (Found == null) {
            return;
        }
        Scp439GrowthShapes.Part Owned = Found.Parts().get(Bone.getName());
        if (Owned == null) {
            return;
        }
        float Progress = Hive.progress(Partial);
        boolean Visible = false;
        for (Scp439GrowthShapes.Cube Item0 : Owned.Cubes()) {
            if (Scp439GrowthMesh.share(Item0, Progress) > Scp439GrowthMesh.Shown) {
                Visible = true;
                break;
            }
        }
        if (!Visible) {
            return;
        }
        VertexConsumer Growth = Buffers.getBuffer(Sheets.cutoutBlockSheet());
        TextureAtlasSprite Sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Found.Sprite());
        float Width = Found.Width();
        float Height = Found.Height();
        float Gape = Bone.getName().equals(Found.JawPart()) ? Scp439HiveModel.jawAngle(Hive, Partial) : 0.0f;
        Scp439GrowthMesh.emitPart(Pose, (X, Y, Z, U, V, NormalX, NormalY, NormalZ) -> Growth.addVertex(X, Y, Z, 0xFFFFFFFF, Sprite.getU(U / Width),
                Sprite.getV(V / Height), Overlay, Light, NormalX, NormalY, NormalZ), Found, Bone.getName(), Progress, Gape);
    }
}
