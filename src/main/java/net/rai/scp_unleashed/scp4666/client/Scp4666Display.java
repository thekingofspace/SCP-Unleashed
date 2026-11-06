package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.storage.client.InteriorLayout;
import net.rai.scp_unleashed.storage.client.ItemAnchor;
import net.rai.scp_unleashed.storage.client.ItemPose;
import net.rai.scp_unleashed.storage.client.LayoutMotion;
import net.rai.scp_unleashed.storage.client.LayoutPiece;
import net.rai.scp_unleashed.storage.client.Placement;
import net.rai.scp_unleashed.storage.client.StorageLayout;
import net.rai.scp_unleashed.storage.client.StorageLayouts;

public final class Scp4666Display {
    public static final float NecklaceScale = 0.62f;
    public static final float NecklaceTallScale = 0.98f;
    public static final float NecklaceHangZ = 12.5f;
    public static final float NecklaceHangY = 13.3f;
    public static final float NecklaceTallHangY = 27.6f;
    public static final float HookScale = 0.6f;
    public static final float HookZ = 10.2f;
    public static final float HookDrop = 3.4f;
    public static final float SwayAngle = 1.5f;
    public static final float SwayPeriod = 180.0f;
    public static final float CaneScale = 0.72f;
    public static final float CaneTallScale = 1.22f;
    public static final float DollScale = 0.6f;
    public static final float DollTallScale = 0.85f;
    public static final float DollYaw = -14.0f;
    public static final float DollSlump = 34.0f;
    public static final float DollTilt = 16.0f;
    private static final Placement CaneLying = Placement.at(7.91f, 7.44f, 9.02f).withRotation(30.0f, 0.0f, 90.0f).withScale(CaneScale);
    private static final Placement CaneLeaning = Placement.at(6.7f, 14.11f, 10.72f).withRotation(40.0f, 9.0f, -6.0f).withScale(CaneTallScale);
    private static final Placement DollSitting = Placement.at(7.64f, 10.44f, 11.81f).withRotation(DollYaw, DollSlump, DollTilt).withScale(DollScale);
    private static final Placement DollTallSitting = Placement.at(7.49f, 18.28f, 10.65f).withRotation(DollYaw, DollSlump, DollTilt).withScale(DollTallScale);

    private Scp4666Display() {
    }

    public static void register() {
        StorageLayouts.register(Scp4666Registry.Necklace.get(), StorageLayout.sized(necklace(false), necklace(true)));
        StorageLayouts.register(Scp4666Registry.Cane.get(), StorageLayout.sized(cane(false), cane(true)));
        StorageLayouts.register(Scp4666Registry.Doll.get(), StorageLayout.sized(doll(false), doll(true)));
    }

    public static InteriorLayout necklace(boolean Tall) {
        float HangY = Tall ? NecklaceTallHangY : NecklaceHangY;
        Placement Hang = Placement.at(8.0f, HangY, NecklaceHangZ).withScale(Tall ? NecklaceTallScale : NecklaceScale);
        BlockState Hook = Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.NORTH);
        InteriorLayout Base = Tall ? InteriorLayout.pedestal(true) : InteriorLayout.pedestal(false).withoutPlatform();
        return Base.withPiece(LayoutPiece.block(Hook, Placement.at(8.0f, HangY - HookDrop, HookZ).withScale(HookScale)))
                .withStance(ItemPose.hanging(Hang).withDisplay(ItemDisplayContext.NONE))
                .withMotion(LayoutMotion.sway(SwayAngle, SwayPeriod));
    }

    public static InteriorLayout cane(boolean Tall) {
        InteriorLayout Base = Tall ? InteriorLayout.pedestal(true).withoutPlatform() : InteriorLayout.pedestal(false);
        return Base.withStance(ItemPose.resting(Tall ? CaneLeaning : CaneLying).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }

    public static InteriorLayout doll(boolean Tall) {
        return InteriorLayout.pedestal(Tall).withStance(ItemPose.resting(Tall ? DollTallSitting : DollSitting).withAnchor(ItemAnchor.Centre).withDisplay(ItemDisplayContext.NONE));
    }
}
