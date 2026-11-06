package net.rai.scp_unleashed.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public record LayoutPiece(PieceShape Shape, Placement Where, boolean Moves) {
    @FunctionalInterface
    public interface PieceShape {
        void draw(StorageLayoutContext Context);
    }

    public static LayoutPiece model(ModelResourceLocation Location, Placement Where) {
        return new LayoutPiece(Context -> Context.drawModel(Location), Where, false);
    }

    public static LayoutPiece block(BlockState State, Placement Where) {
        return new LayoutPiece(Context -> Context.drawBlock(State), Where, false);
    }

    public static LayoutPiece item(ItemStack Shown, Placement Where) {
        ItemStack Kept = Shown.copy();
        return new LayoutPiece(Context -> Context.drawItem(Kept, ItemDisplayContext.FIXED), Where, false);
    }

    public static LayoutPiece custom(PieceShape Shape, Placement Where) {
        return new LayoutPiece(Shape, Where, false);
    }

    public LayoutPiece moving() {
        return new LayoutPiece(Shape, Where, true);
    }

    public LayoutPiece at(Placement Wanted) {
        return new LayoutPiece(Shape, Wanted, Moves);
    }

    public void render(StorageLayoutContext Context, LayoutMotion Motion) {
        PoseStack Pose = Context.Pose();
        Pose.pushPose();
        moved(Where, Motion, Context, Moves).apply(Pose);
        Shape.draw(Context);
        Pose.popPose();
    }

    public static Placement moved(Placement Where, LayoutMotion Motion, StorageLayoutContext Context, boolean Moves) {
        if (!Moves || Motion.isStill()) {
            return Where;
        }
        float Time = Context.time();
        long Seed = Context.seed();
        return Where.moved(0.0f, Motion.liftAt(Time, Seed), 0.0f)
                .withRotation(Where.Yaw() + Motion.spinAt(Time), Where.Pitch(), Where.Roll() + Motion.swayAt(Time, Seed) + Motion.twitchAt(Time, Seed));
    }
}
