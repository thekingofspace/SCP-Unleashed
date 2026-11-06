package net.rai.scp_unleashed.gear.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforgespi.locating.IModFile;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gear.GearPiece;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GearVanillaLayerGameTests {
    public static final String Arena = "gear_arena";
    public static final int LayerWidth = 64;
    public static final int LayerHeight = 32;
    private static final byte[] Signature = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private GearVanillaLayerGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "gear_vanilla_layers")
    public static void everyGearPieceHasItsVanillaArmorLayer(GameTestHelper Helper) {
        IModFile Source = ModList.get().getModFileById(Scp_unleashed.ModId).getFile();
        for (GearPiece Piece : GearPiece.values()) {
            boolean Inner = Piece.getArmorType() == ArmorItem.Type.LEGGINGS;
            for (ArmorMaterial.Layer Sheet : Piece.getOutfit().getArmor().value().layers()) {
                checkLayer(Helper, Source, Piece, Sheet.texture(Inner));
            }
        }
        Helper.succeed();
    }

    private static void checkLayer(GameTestHelper Helper, IModFile Source, GearPiece Piece, ResourceLocation Location) {
        Path Found = Source.findResource(("assets/" + Location.getNamespace() + "/" + Location.getPath()).split("/"));
        Helper.assertTrue(Files.isRegularFile(Found), Piece.getId() + " has no vanilla armor layer at " + Location);
        byte[] Header;
        try (InputStream Stream = Files.newInputStream(Found)) {
            Header = Stream.readNBytes(24);
        } catch (IOException Problem) {
            Helper.fail(Piece.getId() + " could not read " + Location + ": " + Problem.getMessage());
            return;
        }
        Helper.assertTrue(Header.length == 24 && Arrays.equals(Arrays.copyOf(Header, Signature.length), Signature), Location + " is not a PNG");
        int Width = ByteBuffer.wrap(Header, 16, 4).getInt();
        int Height = ByteBuffer.wrap(Header, 20, 4).getInt();
        Helper.assertTrue(Width == LayerWidth && Height == LayerHeight, Location + " is " + Width + "x" + Height + " instead of " + LayerWidth + "x" + LayerHeight);
    }
}
