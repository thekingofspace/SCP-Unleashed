package net.rai.scp_unleashed.scp439.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.metadata.animation.VillagerMetaDataSection;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.Scp439Look;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Scp439VillagerLayer extends GeoRenderLayer<Scp439Hive> {
    public static final String[] Levels = {"stone", "iron", "gold", "emerald", "diamond"};
    public static final String[] HeadBones = {"head", "nose", "hat", "hat_rim"};
    private static final Map<ResourceLocation, VillagerMetaDataSection.Hat> Hats = new HashMap<>();

    public Scp439VillagerLayer(GeoRenderer<Scp439Hive> Renderer) {
        super(Renderer);
    }

    public static void forget() {
        Hats.clear();
    }

    public static ResourceLocation texture(String Folder, String Name) {
        return ResourceLocation.withDefaultNamespace("textures/entity/villager/" + Folder + "/" + Name + ".png");
    }

    public static VillagerMetaDataSection.Hat hatOf(ResourceLocation Texture) {
        return Hats.computeIfAbsent(Texture, Missing -> Minecraft.getInstance().getResourceManager().getResource(Missing).flatMap(Found -> {
            try {
                return Found.metadata().getSection(VillagerMetaDataSection.SERIALIZER).map(VillagerMetaDataSection::getHat);
            } catch (IOException Problem) {
                return Optional.<VillagerMetaDataSection.Hat>empty();
            }
        }).orElse(VillagerMetaDataSection.Hat.NONE));
    }

    @Override
    public void render(PoseStack Pose, Scp439Hive Hive, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float Partial, int Light, int Overlay) {
        Scp439Look Seen = Hive.look();
        if (Seen.Kind() != Scp439Look.KindVillager || Hive.isInvisible()) {
            return;
        }
        ResourceLocation TypeTexture = texture("type", Seen.Type());
        boolean HasProfession = !"none".equals(Seen.Profession());
        ResourceLocation ProfessionTexture = texture("profession", Seen.Profession());
        VillagerMetaDataSection.Hat TypeHat = hatOf(TypeTexture);
        VillagerMetaDataSection.Hat ProfessionHat = HasProfession ? hatOf(ProfessionTexture) : VillagerMetaDataSection.Hat.NONE;
        boolean HeadShown = ProfessionHat == VillagerMetaDataSection.Hat.NONE || ProfessionHat == VillagerMetaDataSection.Hat.PARTIAL && TypeHat != VillagerMetaDataSection.Hat.FULL;
        boolean[] Before = new boolean[HeadBones.length];
        for (int Index = 0; Index < HeadBones.length; Index++) {
            GeoBone Found = Model.getBone(HeadBones[Index]).orElse(null);
            Before[Index] = Found != null && Found.isHidden();
            if (Found != null && !HeadShown) {
                Found.setHidden(true);
            }
        }
        draw(Pose, Hive, Model, Buffers, TypeTexture, Partial, Light, Overlay);
        for (int Index = 0; Index < HeadBones.length; Index++) {
            int Slot = Index;
            Model.getBone(HeadBones[Index]).ifPresent(Found -> Found.setHidden(Before[Slot]));
        }
        if (!HasProfession) {
            return;
        }
        draw(Pose, Hive, Model, Buffers, ProfessionTexture, Partial, Light, Overlay);
        if (!"nitwit".equals(Seen.Profession())) {
            draw(Pose, Hive, Model, Buffers, texture("profession_level", Levels[Math.max(1, Math.min(Levels.length, Seen.Level())) - 1]), Partial, Light, Overlay);
        }
    }

    private void draw(PoseStack Pose, Scp439Hive Hive, BakedGeoModel Model, MultiBufferSource Buffers, ResourceLocation Texture, float Partial, int Light, int Overlay) {
        RenderType Layer = RenderType.entityCutoutNoCull(Texture);
        getRenderer().reRender(Model, Pose, Buffers, Hive, Layer, Buffers.getBuffer(Layer), Partial, Light, Overlay, 0xFFFFFFFF);
    }
}
