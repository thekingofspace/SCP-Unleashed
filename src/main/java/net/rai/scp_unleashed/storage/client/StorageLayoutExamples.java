package net.rai.scp_unleashed.storage.client;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.storage.StorageGeometry;

import java.util.List;

public final class StorageLayoutExamples {
    public static final String Property = "scp_unleashed.storage_examples";
    public static final float SmallHang = 11.5f;
    public static final float TallHang = 21.0f;
    public static final float CushionHeight = 2.0f;
    public static final int WarmTint = 0xFFFFEED8;

    private StorageLayoutExamples() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(Property);
    }

    public static void register() {
        StorageLayouts.register(Items.LIGHTNING_ROD, tallItemWithoutPlatform());
        StorageLayouts.register(Items.CLOCK, hungOnHook());
        StorageLayouts.register(Items.ENDER_EYE, onCushion());
    }

    public static StorageLayout tallItemWithoutPlatform() {
        return StorageLayout.sized(standing(false, 0.45f, 1.2f), standing(true, 0.7f, 3.0f));
    }

    private static InteriorLayout standing(boolean Tall, float Flat, float Solid) {
        Placement Spot = Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, StorageGeometry.CentreZ);
        return InteriorLayout.pedestal(Tall).withoutPlatform().withStance(ItemPose.resting(Spot.withScale(Flat), Spot.withScale(Solid)))
                .withMotion(LayoutMotion.twitch(6.0f, 90));
    }

    public static StorageLayout hungOnHook() {
        return StorageLayout.sized(hook(false, SmallHang, 0.32f), hook(true, TallHang, 0.55f));
    }

    private static InteriorLayout hook(boolean Tall, float Hang, float Scale) {
        BlockState Chain = Blocks.CHAIN.defaultBlockState();
        float Length = StorageGeometry.cavityTop(Tall) - Hang;
        Placement Point = Placement.at(StorageGeometry.CentreX, Hang, StorageGeometry.CentreZ);
        LayoutPiece Link = LayoutPiece.block(Chain, Point.withScale(1.0f, Length / 16.0f, 1.0f));
        return InteriorLayout.pedestal(Tall).withoutPlatform().withPieces(List.of(Link)).withStance(ItemPose.hanging(Point.withScale(Scale)))
                .withMotion(LayoutMotion.sway(6.0f, 64.0f));
    }

    public static StorageLayout onCushion() {
        return StorageLayout.sized(cushion(false, 0.3f, 0.5f), cushion(true, 0.42f, 0.7f));
    }

    private static InteriorLayout cushion(boolean Tall, float Flat, float Solid) {
        float Top = InteriorLayout.platformTop(Tall);
        Placement Pad = Placement.at(StorageGeometry.CentreX, Top, StorageGeometry.CentreZ).withScale(7.0f / 16.0f, CushionHeight / 16.0f, 7.0f / 16.0f);
        Placement Spot = Placement.at(StorageGeometry.CentreX, Top + CushionHeight, StorageGeometry.CentreZ);
        return InteriorLayout.pedestal(Tall).withPiece(LayoutPiece.block(Blocks.RED_WOOL.defaultBlockState(), Pad))
                .withStance(ItemPose.resting(Spot.withScale(Flat), Spot.withScale(Solid)))
                .withMotion(LayoutMotion.bob(0.4f, 80.0f)).withLight(InteriorLayout.FullBright).withTint(WarmTint);
    }
}
