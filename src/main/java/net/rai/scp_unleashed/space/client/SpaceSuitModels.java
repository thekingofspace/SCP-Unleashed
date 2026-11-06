package net.rai.scp_unleashed.space.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.rai.scp_unleashed.gear.client.GearArmorModel;
import net.rai.scp_unleashed.space.SpaceRegistry;
import net.rai.scp_unleashed.space.SpaceSuitPiece;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class SpaceSuitModels {
    private static final Map<SpaceSuitPiece, ModelLayerLocation> Locations = new EnumMap<>(SpaceSuitPiece.class);
    private static final Map<SpaceSuitPiece, GearArmorModel> Baked = new EnumMap<>(SpaceSuitPiece.class);

    static {
        for (SpaceSuitPiece Piece : SpaceSuitPiece.values()) {
            Locations.put(Piece, new ModelLayerLocation(SpaceRegistry.location(Piece.getId()), "main"));
        }
    }

    private SpaceSuitModels() {
    }

    public static Map<SpaceSuitPiece, ModelLayerLocation> locations() {
        return Collections.unmodifiableMap(Locations);
    }

    public static void bake(EntityModelSet Models) {
        Baked.clear();
        Locations.forEach((Piece, Location) -> Baked.put(Piece, new GearArmorModel(Models.bakeLayer(Location))));
    }

    public static GearArmorModel get(SpaceSuitPiece Piece) {
        GearArmorModel Found = Baked.get(Piece);
        if (Found == null) {
            Found = new GearArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(Locations.get(Piece)));
            Baked.put(Piece, Found);
        }
        return Found;
    }

    public record Extensions(SpaceSuitPiece Piece) implements IClientItemExtensions {
        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
            return get(Piece);
        }
    }
}
