package net.rai.scp_unleashed.gear.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.rai.scp_unleashed.gear.GearPiece;

public final class GearModelLayers {
    private GearModelLayers() {
    }

    public static LayerDefinition create(GearPiece Piece) {
        return switch (Piece) {
            case GasMask -> gasMask();
            case HazmatHood -> hazmatHood();
            case HazmatSuit -> hazmatSuit();
            case HazmatTrousers -> hazmatTrousers();
            case HazmatBoots -> hazmatBoots();
            case MtfHelmet -> mtfHelmet();
            case MtfVest -> mtfVest();
            case MtfPants -> mtfPants();
            case MtfBoots -> mtfBoots();
            case SiteGuardCap -> siteGuardCap();
            case SiteGuardShirt -> siteGuardShirt();
            case SiteGuardTrousers -> siteGuardTrousers();
            case SiteGuardBoots -> siteGuardBoots();
            case GocHelmet -> gocHelmet();
            case GocArmor -> gocArmor();
            case GocTrousers -> gocTrousers();
            case GocBoots -> gocBoots();
            case SerpentsHandHood -> serpentsHandHood();
            case SerpentsHandJacket -> serpentsHandJacket();
            case SerpentsHandTrousers -> serpentsHandTrousers();
            case SerpentsHandBoots -> serpentsHandBoots();
            case ChaosInsurgencyHelmet -> chaosInsurgencyHelmet();
            case ChaosInsurgencyVest -> chaosInsurgencyVest();
            case ChaosInsurgencyPants -> chaosInsurgencyPants();
            case ChaosInsurgencyBoots -> chaosInsurgencyBoots();
            case SiteGuardTacticalHelmet -> siteGuardTacticalHelmet();
            case SiteGuardTacticalUniform -> siteGuardTacticalUniform();
            case SiteGuardTacticalTrousers -> siteGuardTacticalTrousers();
            case SiteGuardTacticalBoots -> siteGuardTacticalBoots();
            case ScientistGlasses -> scientistGlasses();
            case ScientistLabCoat -> scientistLabCoat();
            case ScientistSlacks -> scientistSlacks();
            case ScientistShoes -> scientistShoes();
            case BrokenGodMask -> brokenGodMask();
            case BrokenGodRobe -> brokenGodRobe();
            case BrokenGodGreaves -> brokenGodGreaves();
            case BrokenGodBoots -> brokenGodBoots();
            case SarkicHood -> sarkicHood();
            case SarkicRobe -> sarkicRobe();
            case SarkicLeggings -> sarkicLeggings();
            case SarkicBoots -> sarkicBoots();
            default -> throw new IllegalArgumentException("No custom armor model for " + Piece);
        };
    }

    public static LayerDefinition gasMask() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 6.0F, 3.0F, new CubeDeformation(0.6F))
                .texOffs(26, 9).addBox(-3.0F, -5.0F, -4.95F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.1F, 0.1F, -0.2F))
                .texOffs(58, 0).addBox(1.0F, -5.0F, -4.95F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.1F, 0.1F, -0.2F))
                .texOffs(48, 0).addBox(-1.5F, -2.8F, -6.5F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 9).addBox(-4.0F, -6.2F, -1.0F, 8.0F, 1.0F, 5.0F, new CubeDeformation(0.68F, 0.0F, 0.68F))
                .texOffs(22, 0).addBox(-4.0F, -2.6F, -1.0F, 8.0F, 1.0F, 5.0F, new CubeDeformation(0.68F, 0.0F, 0.68F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition hazmatHood() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.8F))
                .texOffs(32, 0).addBox(-4.0F, -7.0F, -5.2F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.3F))
                .texOffs(0, 16).addBox(-3.5F, -6.5F, -5.4F, 7.0F, 4.0F, 1.0F, new CubeDeformation(0.2F, 0.2F, -0.25F))
                .texOffs(50, 0).addBox(-2.0F, -2.5F, -5.4F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(32, 16).addBox(-1.0F, -2.0F, -6.3F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 16).addBox(-4.2F, -2.2F, -6.6F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(16, 16).addBox(2.2F, -2.2F, -6.6F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition hazmatSuit() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.6F))
                .texOffs(0, 16).addBox(-1.5F, 1.5F, 2.4F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(56, 0).addBox(-0.5F, 0.6F, 3.4F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.6F))
                .texOffs(28, 16).addBox(-3.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.6F))
                .texOffs(12, 16).addBox(-1.0F, 6.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.75F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition hazmatTrousers() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 8.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.72F, 0.2F, 0.72F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.44F, 0.2F, 0.44F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.44F, 0.2F, 0.44F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition hazmatBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.75F, 0.5F, 0.75F))
                .texOffs(48, 0).addBox(-2.0F, 5.2F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.9F, 0.0F, 0.9F))
                .texOffs(10, 10).addBox(-2.0F, 10.0F, -3.6F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.62F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.75F, 0.5F, 0.75F))
                .texOffs(32, 0).addBox(-2.0F, 5.2F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.9F, 0.0F, 0.9F))
                .texOffs(0, 10).addBox(-2.0F, 10.0F, -3.6F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.62F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition mtfHelmet() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 4.0F, 9.0F, new CubeDeformation(0.5F))
                .texOffs(36, 0).addBox(-3.5F, -9.5F, -3.5F, 7.0F, 1.0F, 7.0F, new CubeDeformation(0.25F))
                .texOffs(0, 13).addBox(-4.5F, -5.5F, -1.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.6F))
                .texOffs(16, 21).addBox(-4.5F, -5.5F, -5.6F, 9.0F, 3.0F, 1.0F, new CubeDeformation(0.25F, 0.0F, -0.25F))
                .texOffs(36, 21).addBox(-5.4F, -6.2F, -5.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.1F, 0.0F, 0.0F))
                .texOffs(58, 13).addBox(4.4F, -6.2F, -5.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.1F, 0.0F, 0.0F))
                .texOffs(42, 21).addBox(-1.0F, -8.0F, -5.6F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.1F))
                .texOffs(44, 13).addBox(-5.5F, -7.2F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(-0.2F, 0.0F, 0.0F))
                .texOffs(30, 13).addBox(4.5F, -7.2F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(-0.2F, 0.0F, 0.0F))
                .texOffs(8, 21).addBox(-6.0F, -4.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.1F))
                .texOffs(0, 21).addBox(5.0F, -4.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.1F))
                .texOffs(48, 21).addBox(-1.0F, -8.2F, 4.8F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition mtfVest() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.32F))
                .texOffs(0, 25).addBox(-3.5F, 1.5F, -3.25F, 7.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 16).addBox(-3.5F, 1.5F, 2.25F, 7.0F, 8.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(16, 25).addBox(-4.0F, 6.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.62F, 0.0F, 0.62F))
                .texOffs(0, 34).addBox(-3.0F, -0.5F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F, 0.1F, 0.15F))
                .texOffs(40, 25).addBox(1.0F, -0.5F, -2.5F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F, 0.1F, 0.15F))
                .texOffs(58, 16).addBox(-3.3F, 5.5F, -3.95F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(52, 16).addBox(-1.0F, 5.5F, -3.95F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(56, 0).addBox(1.3F, 5.5F, -3.95F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F))
                .texOffs(18, 16).addBox(-3.6F, -2.7F, -2.5F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F))
                .texOffs(0, 16).addBox(-0.4F, -2.7F, -2.5F, 4.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition mtfPants() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.42F, 0.25F, 0.42F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F, 0.2F, 0.3F))
                .texOffs(18, 16).addBox(-2.0F, 4.0F, -3.05F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.15F, 0.0F, 0.0F))
                .texOffs(56, 0).addBox(-3.2F, 1.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F, 0.2F, 0.3F))
                .texOffs(8, 16).addBox(-2.0F, 4.0F, -3.05F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.15F, 0.0F, 0.0F))
                .texOffs(0, 16).addBox(2.2F, 1.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition mtfBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.6F, 0.5F, 0.6F))
                .texOffs(42, 0).addBox(-2.0F, 10.0F, -3.52F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.6F, 0.5F, 0.6F))
                .texOffs(32, 0).addBox(-2.0F, 10.0F, -3.52F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition siteGuardCap() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        PartDefinition Head = Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-4.0F, -7.0F, -4.0F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.65F, 0.0F, 0.65F))
                .texOffs(0, 0).addBox(-4.5F, -9.0F, -5.0F, 9.0F, 2.0F, 10.0F, new CubeDeformation(0.3F))
                .texOffs(32, 12).addBox(-1.5F, -8.6F, -5.75F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.3F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Head.addOrReplaceChild("peak", CubeListBuilder.create()
                .texOffs(38, 0).addBox(-4.0F, 0.0F, -3.5F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.5F, -4.2F, 0.32F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition siteGuardShirt() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.32F))
                .texOffs(0, 16).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 7.0F, 4.0F, new CubeDeformation(0.62F, 0.1F, 0.62F))
                .texOffs(38, 16).addBox(-3.5F, -0.6F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F, 0.0F, 0.25F))
                .texOffs(24, 16).addBox(1.5F, -0.6F, -2.5F, 2.0F, 3.0F, 5.0F, new CubeDeformation(0.0F, 0.0F, 0.25F))
                .texOffs(10, 27).addBox(1.0F, 2.5F, -3.05F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.35F))
                .texOffs(56, 0).addBox(-3.5F, 3.0F, -3.4F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(60, 0).addBox(-3.5F, 1.2F, -3.55F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.35F, 0.0F, -0.35F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F))
                .texOffs(0, 27).addBox(-2.5F, -2.75F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F, -0.3F, 0.0F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F))
                .texOffs(52, 16).addBox(-0.5F, -2.75F, -1.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F, -0.3F, 0.0F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition siteGuardTrousers() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.42F, 0.25F, 0.42F))
                .texOffs(0, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.52F, 0.12F, 0.52F))
                .texOffs(56, 0).addBox(-5.25F, 9.8F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 16).addBox(1.5F, 9.6F, 2.1F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F, 0.2F, 0.3F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.3F, 0.2F, 0.3F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition siteGuardBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.5F, 0.6F))
                .texOffs(42, 0).addBox(-2.0F, 10.0F, -3.52F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.5F, 0.6F))
                .texOffs(32, 0).addBox(-2.0F, 10.0F, -3.52F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition gocHelmet() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.4F))
                .texOffs(0, 16).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.45F, 0.4F, 0.45F))
                .texOffs(36, 16).addBox(-3.5F, -9.5F, -3.5F, 7.0F, 1.0F, 7.0F, new CubeDeformation(0.25F))
                .texOffs(46, 28).addBox(-4.0F, -5.25F, -5.55F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(32, 0).addBox(-4.5F, -5.5F, -2.0F, 9.0F, 2.0F, 7.0F, new CubeDeformation(0.55F))
                .texOffs(14, 28).addBox(-5.55F, -6.2F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(0, 28).addBox(4.55F, -6.2F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(28, 28).addBox(-4.0F, -7.7F, -5.5F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.3F, 0.1F, 0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition gocArmor() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.33F))
                .texOffs(16, 16).addBox(-3.5F, 1.0F, -3.42F, 7.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-3.5F, 1.0F, 2.42F, 7.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(32, 16).addBox(-4.0F, 6.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.63F, 0.0F, 0.63F))
                .texOffs(30, 24).addBox(-3.0F, -0.5F, -2.6F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F, 0.1F, 0.2F))
                .texOffs(16, 24).addBox(1.0F, -0.5F, -2.6F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F, 0.1F, 0.2F))
                .texOffs(44, 24).addBox(-3.3F, 4.8F, -4.32F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(56, 16).addBox(-1.0F, 4.8F, -4.32F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(56, 0).addBox(1.3F, 4.8F, -4.32F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.33F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.33F))
                .texOffs(0, 24).addBox(-1.0F, 0.75F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.5F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition gocTrousers() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.43F, 0.24F, 0.43F))
                .texOffs(0, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.53F, 0.1F, 0.53F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F))
                .texOffs(34, 16).addBox(-2.0F, 4.3F, -3.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.2F, 0.0F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F))
                .texOffs(24, 16).addBox(-2.0F, 4.3F, -3.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.2F, 0.0F, 0.0F))
                .texOffs(56, 0).addBox(2.2F, 1.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition gocBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.64F, 0.5F, 0.64F))
                .texOffs(42, 0).addBox(-2.0F, 10.0F, -3.56F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.47F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.64F, 0.5F, 0.64F))
                .texOffs(32, 0).addBox(-2.0F, 10.0F, -3.56F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.47F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition serpentsHandHood() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        PartDefinition Head = Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.75F))
                .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.6F))
                .texOffs(0, 25).addBox(-4.0F, -7.6F, -5.25F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.5F, 0.0F, 0.0F))
                .texOffs(0, 16).addBox(-4.0F, -1.2F, -2.5F, 8.0F, 2.0F, 7.0F, new CubeDeformation(0.95F, 0.2F, 0.85F))
                .texOffs(44, 16).addBox(-4.0F, -3.0F, -4.45F, 8.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Head.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(30, 16).addBox(-1.5F, 0.0F, -1.0F, 3.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.6F, 3.4F, -0.38F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition serpentsHandJacket() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.34F))
                .texOffs(0, 16).addBox(-3.0F, -0.5F, 1.6F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.1F))
                .texOffs(56, 0).addBox(0.5F, 8.0F, -3.1F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition serpentsHandTrousers() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.45F, 0.23F, 0.45F))
                .texOffs(0, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.53F, 0.08F, 0.53F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.33F, 0.2F, 0.33F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.33F, 0.2F, 0.33F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition serpentsHandBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.62F, 0.5F, 0.62F))
                .texOffs(42, 0).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.62F, 0.5F, 0.62F))
                .texOffs(32, 0).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition chaosInsurgencyHelmet() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.55F))
                .texOffs(0, 16).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.45F, 0.25F, 0.45F))
                .texOffs(32, 0).addBox(-3.5F, -9.5F, -3.5F, 7.0F, 1.0F, 7.0F, new CubeDeformation(0.2F))
                .texOffs(36, 16).addBox(-4.0F, -5.0F, -5.15F, 8.0F, 5.0F, 1.0F, new CubeDeformation(0.0F, 0.12F, 0.0F))
                .texOffs(30, 28).addBox(-3.0F, -5.0F, -5.6F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.05F))
                .texOffs(24, 28).addBox(1.0F, -5.0F, -5.6F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F, 0.0F, -0.05F))
                .texOffs(18, 28).addBox(-1.0F, -3.0F, -5.7F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 28).addBox(-4.7F, -2.6F, -6.3F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(54, 16).addBox(2.7F, -2.6F, -6.3F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(8, 28).addBox(-2.0F, -7.5F, 4.7F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition chaosInsurgencyVest() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.35F))
                .texOffs(16, 16).addBox(-3.5F, 1.0F, -3.42F, 7.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 16).addBox(-3.5F, 1.0F, 2.42F, 7.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(28, 24).addBox(-2.0F, 2.0F, 3.3F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(32, 16).addBox(-4.0F, 5.5F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.62F, 0.0F, 0.62F))
                .texOffs(14, 24).addBox(-3.0F, -0.5F, -2.6F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F, 0.1F, 0.2F))
                .texOffs(0, 24).addBox(1.0F, -0.5F, -2.6F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F, 0.1F, 0.2F))
                .texOffs(38, 24).addBox(-3.3F, 4.8F, -4.32F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(56, 16).addBox(-1.0F, 4.8F, -4.32F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(56, 0).addBox(1.3F, 4.8F, -4.32F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.35F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition chaosInsurgencyPants() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.41F, 0.22F, 0.41F))
                .texOffs(8, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.51F, 0.14F, 0.51F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.29F, 0.2F, 0.29F))
                .texOffs(42, 16).addBox(-2.0F, 4.0F, -3.1F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.17F, 0.0F, 0.0F))
                .texOffs(32, 0).addBox(-3.2F, 1.0F, -1.5F, 1.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.29F, 0.2F, 0.29F))
                .texOffs(32, 16).addBox(-2.0F, 4.0F, -3.1F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.17F, 0.0F, 0.0F))
                .texOffs(0, 16).addBox(2.2F, 1.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition chaosInsurgencyBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.58F, 0.5F, 0.58F))
                .texOffs(42, 0).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.58F, 0.5F, 0.58F))
                .texOffs(32, 0).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition siteGuardTacticalHelmet() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.45F))
                .texOffs(0, 16).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 3.0F, 9.0F, new CubeDeformation(0.55F))
                .texOffs(36, 16).addBox(-3.5F, -9.5F, -3.5F, 7.0F, 1.0F, 7.0F, new CubeDeformation(0.35F))
                .texOffs(32, 0).addBox(-4.5F, -5.5F, -1.5F, 9.0F, 2.0F, 6.0F, new CubeDeformation(0.62F))
                .texOffs(0, 28).addBox(-4.5F, -5.1F, -5.7F, 9.0F, 3.0F, 1.0F, new CubeDeformation(0.3F, 0.0F, -0.2F))
                .texOffs(26, 28).addBox(-5.6F, -5.4F, -5.2F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(20, 28).addBox(4.6F, -5.4F, -5.2F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition siteGuardTacticalUniform() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.32F))
                .texOffs(0, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.64F, 0.15F, 0.64F))
                .texOffs(24, 16).addBox(-3.5F, -1.0F, -2.5F, 7.0F, 2.0F, 5.0F, new CubeDeformation(0.0F, 0.1F, 0.25F))
                .texOffs(0, 30).addBox(-3.4F, 1.2F, -3.1F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(56, 16).addBox(0.4F, 1.2F, -3.1F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(48, 16).addBox(-3.4F, 5.4F, -3.2F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(56, 0).addBox(0.4F, 5.4F, -3.2F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition siteGuardTacticalTrousers() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.44F, 0.25F, 0.44F))
                .texOffs(8, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.53F, 0.11F, 0.53F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F, 0.2F, 0.32F))
                .texOffs(56, 0).addBox(-3.2F, 1.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.32F, 0.2F, 0.32F))
                .texOffs(0, 16).addBox(2.2F, 2.0F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition siteGuardTacticalBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.63F, 0.5F, 0.63F))
                .texOffs(42, 0).addBox(-2.0F, 10.0F, -3.55F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.47F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 8.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.63F, 0.5F, 0.63F))
                .texOffs(32, 0).addBox(-2.0F, 10.0F, -3.55F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.47F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scientistGlasses() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(20, 0).addBox(-4.0F, -5.0F, -4.6F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.3F, 0.0F, 0.0F))
                .texOffs(10, 0).addBox(-4.75F, -4.75F, -4.3F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.35F, -0.35F, 0.0F))
                .texOffs(0, 0).addBox(3.75F, -4.75F, -4.3F, 1.0F, 1.0F, 4.0F, new CubeDeformation(-0.35F, -0.35F, 0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scientistLabCoat() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.34F))
                .texOffs(32, 16).addBox(-4.0F, 8.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.62F, 0.1F, 0.62F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-2.0F, 0.2F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.52F, 0.2F, 0.52F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.2F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.52F, 0.2F, 0.52F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scientistSlacks() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.43F, 0.24F, 0.43F))
                .texOffs(0, 16).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.53F, 0.1F, 0.53F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition scientistShoes() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.6F, 0.5F, 0.6F))
                .texOffs(42, 0).addBox(-2.0F, 10.0F, -3.52F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 9.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.6F, 0.5F, 0.6F))
                .texOffs(32, 0).addBox(-2.0F, 10.0F, -3.52F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition brokenGodMask() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(42, 0).addBox(-4.0F, -3.0F, -4.5F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.3F, 0.0F, 0.0F))
                .texOffs(0, 0).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.45F, 0.0F, 0.45F))
                .texOffs(32, 0).addBox(0.0F, -7.0F, -4.8F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 9).addBox(0.5F, -5.2F, -5.6F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(8, 9).addBox(4.2F, -6.5F, -1.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition brokenGodRobe() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.34F))
                .texOffs(0, 27).addBox(-4.0F, 8.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.62F, 0.1F, 0.62F))
                .texOffs(48, 16).addBox(-3.5F, 0.5F, -2.9F, 7.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 35).addBox(-1.0F, 1.5F, -3.6F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 35).addBox(-4.0F, 8.4F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.8F, 0.1F, 0.8F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(44, 27).addBox(-3.75F, -2.75F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F))
                .texOffs(32, 16).addBox(-3.0F, 4.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.5F, 0.0F, 0.5F))
                .texOffs(56, 0).addBox(-4.1F, 2.5F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 27).addBox(-1.25F, -2.75F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-2.0F, 0.4F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.51F, 0.4F, 0.51F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.4F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.51F, 0.4F, 0.51F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition brokenGodGreaves() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.43F, 0.24F, 0.43F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F))
                .texOffs(0, 16).addBox(-3.2F, 1.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F))
                .texOffs(56, 0).addBox(2.2F, 1.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition brokenGodBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 8.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.0F, 0.6F))
                .texOffs(42, 0).addBox(-2.0F, 8.0F, -3.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.15F, 0.0F, 0.0F))
                .texOffs(0, 8).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 8.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.0F, 0.6F))
                .texOffs(32, 0).addBox(-2.0F, 8.0F, -3.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.15F, 0.0F, 0.0F))
                .texOffs(52, 0).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition sarkicHood() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        PartDefinition Head = Root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(10, 16).addBox(-4.0F, -3.0F, -4.5F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.3F, 0.0F, 0.0F))
                .texOffs(32, 0).addBox(-4.0F, -6.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.45F, 0.0F, 0.45F))
                .texOffs(0, 16).addBox(0.0F, -7.0F, -4.8F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(28, 16).addBox(0.5F, -5.2F, -5.6F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(52, 16).addBox(4.2F, -6.5F, -1.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.7F))
                .texOffs(44, 16).addBox(-3.6F, -10.2F, -2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(36, 16).addBox(1.6F, -10.2F, -2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Head.addOrReplaceChild("horn_right_tip", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-0.5F, -2.8F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.6F, -10.2F, -1.0F, -0.35F, 0.0F, -0.6F));
        Head.addOrReplaceChild("horn_left_tip", CubeListBuilder.create()
                .texOffs(58, 16).addBox(-0.5F, -2.8F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.6F, -10.2F, -1.0F, -0.35F, 0.0F, 0.6F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition sarkicRobe() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.34F))
                .texOffs(0, 27).addBox(-4.0F, 8.0F, -2.0F, 8.0F, 4.0F, 4.0F, new CubeDeformation(0.62F, 0.1F, 0.62F))
                .texOffs(48, 16).addBox(-3.5F, 0.5F, -2.9F, 7.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 35).addBox(-1.0F, 1.5F, -3.6F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 35).addBox(-4.0F, 8.4F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(0.8F, 0.1F, 0.8F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(44, 27).addBox(-3.75F, -2.75F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(40, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F))
                .texOffs(32, 16).addBox(-3.0F, 4.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.5F, 0.0F, 0.5F))
                .texOffs(56, 0).addBox(-4.1F, 2.5F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(24, 27).addBox(-1.25F, -2.75F, -2.5F, 5.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(24, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.34F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-2.0F, 0.4F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.51F, 0.4F, 0.51F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.4F, -2.0F, 4.0F, 7.0F, 4.0F, new CubeDeformation(0.51F, 0.4F, 0.51F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 64);
    }

    public static LayerDefinition sarkicLeggings() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, 9.0F, -2.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.43F, 0.24F, 0.43F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F))
                .texOffs(0, 16).addBox(-3.2F, 1.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.31F, 0.2F, 0.31F))
                .texOffs(56, 0).addBox(2.2F, 1.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }

    public static LayerDefinition sarkicBoots() {
        MeshDefinition Mesh = GearArmorModel.createEmptyMesh();
        PartDefinition Root = Mesh.getRoot();
        Root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(16, 0).addBox(-2.0F, 8.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.0F, 0.6F))
                .texOffs(42, 0).addBox(-2.0F, 8.0F, -3.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.15F, 0.0F, 0.0F))
                .texOffs(0, 8).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        Root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, 8.5F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.6F, 0.0F, 0.6F))
                .texOffs(32, 0).addBox(-2.0F, 8.0F, -3.2F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.15F, 0.0F, 0.0F))
                .texOffs(52, 0).addBox(-2.0F, 10.0F, -3.5F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.45F, 0.4F, 0.0F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(Mesh, 64, 32);
    }
}
