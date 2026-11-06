package net.rai.scp_unleashed.gear.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gear.GearPiece;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class GearArmorModels {
    private static final Map<GearPiece, ModelLayerLocation> Locations = new EnumMap<>(GearPiece.class);
    private static final Map<GearPiece, GearArmorModel> Baked = new EnumMap<>(GearPiece.class);

    static {
        for (GearPiece Piece : GearPiece.values()) {
            if (Piece.hasCustomModel()) {
                Locations.put(Piece, new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "gear_" + Piece.getId()), "main"));
            }
        }
    }

    private GearArmorModels() {
    }

    public static Map<GearPiece, ModelLayerLocation> locations() {
        return Collections.unmodifiableMap(Locations);
    }

    public static void bake(EntityModelSet Models) {
        Baked.clear();
        Locations.forEach((Piece, Location) -> Baked.put(Piece, new GearArmorModel(Models.bakeLayer(Location))));
    }

    public static GearArmorModel get(GearPiece Piece) {
        GearArmorModel Found = Baked.get(Piece);
        if (Found == null) {
            Found = new GearArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(Locations.get(Piece)));
            Baked.put(Piece, Found);
        }
        return Found;
    }
}
