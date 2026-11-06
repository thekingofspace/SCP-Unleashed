package net.rai.scp_unleashed.scp3008.econ.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class Scp3008EconArmorModels {
    public enum Piece {
        Colander("scp3008_econ_colander", Scp3008EconArmorModels::colander),
        Vest("scp3008_econ_vest", Scp3008EconArmorModels::vest),
        Slippers("scp3008_econ_slippers", Scp3008EconArmorModels::slippers);

        public final ModelLayerLocation Location;
        public final Supplier<LayerDefinition> Definition;

        Piece(String Name, Supplier<LayerDefinition> Definition) {
            this.Location = new ModelLayerLocation(Scp3008EconRegistry.location(Name), "main");
            this.Definition = Definition;
        }
    }

    private static final Map<Piece, HumanoidModel<LivingEntity>> Baked = new EnumMap<>(Piece.class);

    private Scp3008EconArmorModels() {
    }

    private static PartDefinition skeleton(MeshDefinition Mesh) {
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        return Root;
    }

    public static LayerDefinition colander() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = skeleton(Mesh);
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 4.0F, 10.0F)
                .texOffs(0, 14).addBox(-6.0F, -5.5F, -6.0F, 12.0F, 1.0F, 12.0F)
                .texOffs(0, 27).addBox(-4.0F, -9.9F, -4.0F, 8.0F, 1.0F, 8.0F)
                .texOffs(32, 27).addBox(-2.0F, -10.8F, -2.0F, 4.0F, 1.0F, 4.0F)
                .texOffs(40, 0).addBox(4.5F, -8.0F, -1.0F, 5.0F, 1.0F, 2.0F)
                .texOffs(40, 3).addBox(9.0F, -8.5F, -1.5F, 1.0F, 2.0F, 3.0F), PartPose.ZERO);
        Root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition vest() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = skeleton(Mesh);
        Root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 11.0F, 6.0F)
                .texOffs(32, 0).addBox(-4.0F, 0.5F, -3.9F, 8.0F, 5.0F, 1.0F)
                .texOffs(32, 6).addBox(-4.0F, 6.0F, -3.9F, 8.0F, 4.0F, 1.0F)
                .texOffs(32, 11).addBox(-4.0F, 1.0F, 2.9F, 8.0F, 8.0F, 1.0F)
                .texOffs(50, 0).addBox(2.5F, 9.0F, -4.4F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition slippers() {
        MeshDefinition Mesh = new MeshDefinition();
        PartDefinition Root = skeleton(Mesh);
        Root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, 9.6F, -4.0F, 6.0F, 3.0F, 7.0F)
                .texOffs(26, 0).addBox(-1.0F, 8.85F, -4.0F, 2.0F, 1.0F, 2.0F), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-3.0F, 9.7F, -3.9F, 6.0F, 3.0F, 7.0F)
                .texOffs(26, 0).addBox(-1.0F, 8.95F, -3.9F, 2.0F, 1.0F, 2.0F), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static void bake(EntityModelSet Models) {
        for (Piece Each : Piece.values()) {
            Baked.put(Each, new HumanoidModel<>(Models.bakeLayer(Each.Location)));
        }
    }

    public static HumanoidModel<LivingEntity> model(Piece Each) {
        return Baked.computeIfAbsent(Each, Missing -> new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(Missing.Location)));
    }
}
