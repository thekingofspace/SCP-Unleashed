package net.rai.scp_unleashed.storage.client;

import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.storage.StorageGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record InteriorLayout(Optional<LayoutPiece> Platform, List<LayoutPiece> Pieces, ItemPose Stance, LayoutMotion Motion, int Light, int Tint,
                             StorageFit Fit) implements StorageLayout {
    public static final int InheritLight = -1;
    public static final int FullBright = 15;
    public static final int NoTint = 0xFFFFFFFF;
    public static final float SmallPlatformTop = 6.0f;
    public static final float TallPlatformTop = 12.0f;
    public static final InteriorLayout SmallPedestal = pedestalFor(false);
    public static final InteriorLayout TallPedestal = pedestalFor(true);

    public static LayoutPiece platform(boolean Tall) {
        return LayoutPiece.model(Tall ? StorageModels.TallPedestal : StorageModels.Pedestal, Placement.at(8.0f, 0.0f, 8.0f));
    }

    public static float platformTop(boolean Tall) {
        return Tall ? TallPlatformTop : SmallPlatformTop;
    }

    public static InteriorLayout pedestal(boolean Tall) {
        return Tall ? TallPedestal : SmallPedestal;
    }

    public static InteriorLayout floor(boolean Tall) {
        return pedestal(Tall).withoutPlatform().withStance(stance(Tall, StorageGeometry.CavityFloor));
    }

    public static InteriorLayout large(boolean Tall) {
        Placement Spot = Placement.at(StorageGeometry.CentreX, StorageGeometry.CavityFloor, StorageGeometry.CentreZ);
        ItemPose Big = ItemPose.resting(Spot.withScale(Tall ? 0.72f : 0.6f), Spot.withYaw(Tall ? 0.0f : 20.0f).withScale(Tall ? 1.4f : 1.05f));
        return pedestal(Tall).withoutPlatform().withStance(Big);
    }

    private static InteriorLayout pedestalFor(boolean Tall) {
        return new InteriorLayout(Optional.of(platform(Tall)), List.of(), stance(Tall, platformTop(Tall)), LayoutMotion.Still, InheritLight, NoTint, StorageFit.Both);
    }

    private static ItemPose stance(boolean Tall, float Top) {
        Placement Spot = Placement.at(StorageGeometry.CentreX, Top, StorageGeometry.CentreZ);
        return ItemPose.resting(Spot.withScale(Tall ? 0.7f : 0.45f), Spot.withYaw(Tall ? 15.0f : 30.0f).withScale(Tall ? 1.0f : 0.8f));
    }

    public InteriorLayout withPlatform(LayoutPiece Wanted) {
        return new InteriorLayout(Optional.of(Wanted), Pieces, Stance, Motion, Light, Tint, Fit);
    }

    public InteriorLayout withoutPlatform() {
        return new InteriorLayout(Optional.empty(), Pieces, Stance, Motion, Light, Tint, Fit);
    }

    public InteriorLayout withPieces(List<LayoutPiece> Wanted) {
        return new InteriorLayout(Platform, List.copyOf(Wanted), Stance, Motion, Light, Tint, Fit);
    }

    public InteriorLayout withPiece(LayoutPiece Extra) {
        List<LayoutPiece> Grown = new ArrayList<>(Pieces);
        Grown.add(Extra);
        return withPieces(Grown);
    }

    public InteriorLayout withStance(ItemPose Wanted) {
        return new InteriorLayout(Platform, Pieces, Wanted, Motion, Light, Tint, Fit);
    }

    public InteriorLayout withMotion(LayoutMotion Wanted) {
        return new InteriorLayout(Platform, Pieces, Stance, Wanted, Light, Tint, Fit);
    }

    public InteriorLayout withLight(int BlockLight) {
        return new InteriorLayout(Platform, Pieces, Stance, Motion, BlockLight, Tint, Fit);
    }

    public InteriorLayout withTint(int Argb) {
        return new InteriorLayout(Platform, Pieces, Stance, Motion, Light, Argb, Fit);
    }

    public InteriorLayout withFit(StorageFit Wanted) {
        return new InteriorLayout(Platform, Pieces, Stance, Motion, Light, Tint, Wanted);
    }

    @Override
    public boolean fits(boolean Tall) {
        return Fit.fits(Tall);
    }

    @Override
    public void render(StorageLayoutContext Context) {
        StorageLayoutContext Scoped = Context;
        if (Light != InheritLight) {
            Scoped = Scoped.withLight(Light);
        }
        if (Tint != NoTint) {
            Scoped = Scoped.withTint(Tint);
        }
        if (Platform.isPresent()) {
            Platform.get().render(Scoped, Motion);
        }
        for (LayoutPiece Part : Pieces) {
            Part.render(Scoped, Motion);
        }
        if (!Scoped.isEmpty()) {
            ItemStack Shown = Scoped.Stack();
            Placement Where = LayoutPiece.moved(Stance.choose(Scoped.isBlockLike(Shown)), Motion, Scoped, true);
            Scoped.item(Shown, Where, Stance.Display(), Stance.Anchor());
        }
    }
}
