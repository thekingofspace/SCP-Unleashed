package net.rai.scp_unleashed.anomaly;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.rai.scp_unleashed.config.ScpConfigs;

public final class AnomalyDropConfig {
    public static final String SophisticatedId = "sophisticatedbackpacks";
    public static final String BackpackedId = "backpacked";
    public static ModConfigSpec.BooleanValue ShulkerBoxes;
    public static ModConfigSpec.BooleanValue Bundles;
    public static ModConfigSpec.BooleanValue SophisticatedBackpacks;
    public static ModConfigSpec.BooleanValue Backpacked;
    public static ModConfigSpec.BooleanValue OtherContainers;

    public static void define(ModConfigSpec.Builder Builder) {
        ShulkerBoxes = Builder.comment("With scpDropAnomalies on, anomalies packed in shulker boxes also drop on death. The box itself stays with the player.")
                .translation("config.scp_unleashed.anomaly_drops.shulker_boxes").define("shulkerBoxes", true);
        Bundles = Builder.comment("With scpDropAnomalies on, anomalies packed in bundles also drop on death. The bundle itself stays with the player.")
                .translation("config.scp_unleashed.anomaly_drops.bundles").define("bundles", true);
        SophisticatedBackpacks = Builder.comment("With scpDropAnomalies on, anomalies packed in Sophisticated Backpacks also drop on death, whether the backpack is carried or worn.")
                .translation("config.scp_unleashed.anomaly_drops.sophisticated_backpacks").define("sophisticatedBackpacks", true);
        Backpacked = Builder.comment("With scpDropAnomalies on, anomalies packed in Backpacked backpacks also drop on death, whether the backpack is carried or equipped.")
                .translation("config.scp_unleashed.anomaly_drops.backpacked").define("backpacked", true);
        OtherContainers = Builder.comment("With scpDropAnomalies on, anomalies packed in any other item that holds items also drop on death.")
                .translation("config.scp_unleashed.anomaly_drops.other_containers").define("otherContainers", true);
    }

    private AnomalyDropConfig() {
    }

    private static boolean value(ModConfigSpec.BooleanValue Value) {
        return ScpConfigs.Server.isLoaded() ? Value.getAsBoolean() : Value.getDefault();
    }

    public static boolean opens(ItemStack Stack) {
        String Space = BuiltInRegistries.ITEM.getKey(Stack.getItem()).getNamespace();
        if (SophisticatedId.equals(Space)) {
            return value(SophisticatedBackpacks);
        }
        if (BackpackedId.equals(Space)) {
            return value(Backpacked);
        }
        if (Stack.getItem() instanceof BlockItem Placed && Placed.getBlock() instanceof ShulkerBoxBlock) {
            return value(ShulkerBoxes);
        }
        if (Stack.has(DataComponents.BUNDLE_CONTENTS)) {
            return value(Bundles);
        }
        return value(OtherContainers);
    }

    public static boolean equippedBackpacked() {
        return value(Backpacked);
    }
}
