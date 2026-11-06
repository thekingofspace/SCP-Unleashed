package net.rai.scp_unleashed.scp253.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp253.Scp253Display;
import net.rai.scp_unleashed.scp253.Scp253Growths;
import net.rai.scp_unleashed.scp253.Scp253Registry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Scp253GrowthPainter {
    public static final ResourceLocation SpriteId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp253/growth");
    public static final int Tile = 8;
    public static final int Sheet = 16;
    public static final int Remembered = 256;
    public static final float SkinLift = 0.25f;
    public static final float HatLift = 0.5f;
    private static final Map<Long, List<Scp253Growths.Growth>> Layouts = new HashMap<>();
    private static final int[][] Corners = {
            {3, 1, 5, 0, 1, 5, 0, 1, 2, 3, 1, 2},
            {3, 4, 2, 0, 4, 2, 0, 4, 5, 3, 4, 5},
            {0, 1, 2, 0, 1, 5, 0, 4, 5, 0, 4, 2},
            {3, 1, 2, 0, 1, 2, 0, 4, 2, 3, 4, 2},
            {3, 1, 5, 3, 1, 2, 3, 4, 2, 3, 4, 5},
            {0, 1, 5, 3, 1, 5, 3, 4, 5, 0, 4, 5}};
    private static final float[][] Normals = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}};
    private static final int[][] Spans = {{0, 2}, {0, 2}, {2, 1}, {0, 1}, {2, 1}, {0, 1}};

    private Scp253GrowthPainter() {
    }

    public static List<Scp253Growths.Growth> layout(long Seed) {
        List<Scp253Growths.Growth> Found = Layouts.get(Seed);
        if (Found == null) {
            if (Layouts.size() >= Remembered) {
                Layouts.clear();
            }
            Found = Scp253Growths.layout(Seed);
            Layouts.put(Seed, Found);
        }
        return Found;
    }

    public static void forget() {
        Layouts.clear();
    }

    @Nullable
    public static Scp253Display display(@Nullable LivingEntity Subject) {
        return Subject == null ? null : Subject.getExistingDataOrNull(Scp253Registry.Display.get());
    }

    public static float progress(LivingEntity Subject, Scp253Display Shown, float Partial) {
        return Shown.progressAt(Subject.level().getGameTime(), Partial);
    }

    public static boolean visible(float Progress) {
        return Scp253Growths.count(Progress) > 0;
    }

    public static float[] lifts(LivingEntity Subject, float Skin, float Hat) {
        boolean Head = armour(Subject, EquipmentSlot.HEAD);
        boolean Chest = armour(Subject, EquipmentSlot.CHEST);
        boolean Legs = armour(Subject, EquipmentSlot.LEGS);
        boolean Feet = armour(Subject, EquipmentSlot.FEET);
        float HeadLift = Math.max(Hat, Head ? 1.0f : 0.0f);
        float BodyLift = Math.max(Skin, Chest ? 1.0f : Legs ? 0.5f : 0.0f);
        float ArmLift = Math.max(Skin, Chest ? 1.0f : 0.0f);
        float LegLift = Math.max(Skin, Feet ? 1.0f : Legs ? 0.5f : 0.0f);
        return new float[] {HeadLift, BodyLift, ArmLift, ArmLift, LegLift, LegLift};
    }

    private static boolean armour(LivingEntity Subject, EquipmentSlot Slot) {
        ItemStack Worn = Subject.getItemBySlot(Slot);
        return !Worn.isEmpty() && Worn.getItem() instanceof ArmorItem Piece && Piece.getEquipmentSlot() == Slot;
    }

    public static void paint(PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay, List<Scp253Growths.Growth> Layout, float Progress, int Part,
                             float[] Bounds, float Lift) {
        VertexConsumer Buffer = null;
        TextureAtlasSprite Sprite = null;
        PoseStack.Pose Last = Pose.last();
        for (Scp253Growths.Growth Item : Layout) {
            if (Item.Part() != Part) {
                continue;
            }
            float Scale = Scp253Growths.scaleOf(Item, Progress);
            if (Scale <= 0.0f) {
                continue;
            }
            if (Buffer == null) {
                Buffer = Buffers.getBuffer(Sheets.cutoutBlockSheet());
                Sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SpriteId);
            }
            List<Scp253Growths.Piece> Pieces = Item.Pieces();
            for (int Index = 0; Index < Pieces.size(); Index++) {
                cube(Last, Buffer, Sprite, Scp253Growths.box(Item, Pieces.get(Index), Scale, Bounds, Lift), Item.Variant(), Index, Light, Overlay);
            }
        }
    }

    private static void cube(PoseStack.Pose Last, VertexConsumer Buffer, TextureAtlasSprite Sprite, float[] Box, int Variant, int Piece, int Light, int Overlay) {
        Matrix4f Matrix = Last.pose();
        Vector3f Point = new Vector3f();
        Vector3f Facing = new Vector3f();
        int TileU = (Variant % 2) * Tile;
        int TileV = (Variant / 2) * Tile;
        for (int Face = 0; Face < 6; Face++) {
            float Wide = Box[Spans[Face][0] + 3] - Box[Spans[Face][0]];
            float Tall = Box[Spans[Face][1] + 3] - Box[Spans[Face][1]];
            int Across = Mth.clamp(Math.round(Wide), 1, Tile);
            int Down = Mth.clamp(Math.round(Tall), 1, Tile);
            int OffsetU = (Piece * 3 + Face * 5 + Variant) % (Tile - Across + 1);
            int OffsetV = (Piece * 5 + Face * 3) % (Tile - Down + 1);
            float U1 = Sprite.getU((TileU + OffsetU) / (float) Sheet);
            float U2 = Sprite.getU((TileU + OffsetU + Across) / (float) Sheet);
            float V1 = Sprite.getV((TileV + OffsetV) / (float) Sheet);
            float V2 = Sprite.getV((TileV + OffsetV + Down) / (float) Sheet);
            float[] Uvs = {U2, V1, U1, V1, U1, V2, U2, V2};
            Last.transformNormal(Normals[Face][0], Normals[Face][1], Normals[Face][2], Facing);
            int[] Picks = Corners[Face];
            for (int Corner = 0; Corner < 4; Corner++) {
                float X = Box[Picks[Corner * 3]] / 16.0f;
                float Y = Box[Picks[Corner * 3 + 1]] / 16.0f;
                float Z = Box[Picks[Corner * 3 + 2]] / 16.0f;
                Matrix.transformPosition(X, Y, Z, Point);
                Buffer.addVertex(Point.x(), Point.y(), Point.z(), 0xFFFFFFFF, Uvs[Corner * 2], Uvs[Corner * 2 + 1], Overlay, Light, Facing.x(), Facing.y(), Facing.z());
            }
        }
    }
}
