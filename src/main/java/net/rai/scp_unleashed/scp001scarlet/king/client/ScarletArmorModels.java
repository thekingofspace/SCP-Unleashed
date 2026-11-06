package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;

public final class ScarletArmorModels {
    public static final ModelLayerLocation Outer = new ModelLayerLocation(ScarletKingRegistry.location("scarlet_armor_outer"), "main");
    public static final ModelLayerLocation Inner = new ModelLayerLocation(ScarletKingRegistry.location("scarlet_armor_inner"), "main");
    private static ScarletArmorModel OuterModel;
    private static ScarletArmorModel InnerModel;

    private ScarletArmorModels() {
    }

    public static void bake(EntityModelSet Models) {
        OuterModel = new ScarletArmorModel(Models.bakeLayer(Outer));
        InnerModel = new ScarletArmorModel(Models.bakeLayer(Inner));
    }

    public static ScarletArmorModel outer() {
        if (OuterModel == null) {
            OuterModel = new ScarletArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(Outer));
        }
        return OuterModel;
    }

    public static ScarletArmorModel inner() {
        if (InnerModel == null) {
            InnerModel = new ScarletArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(Inner));
        }
        return InnerModel;
    }
}
