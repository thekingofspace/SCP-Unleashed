package net.rai.scp_unleashed.gear.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.gear.GearArmorItem;
import net.rai.scp_unleashed.gear.GearEquipSounds;
import net.rai.scp_unleashed.gear.GearOutfit;
import net.rai.scp_unleashed.gear.GearPiece;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.gear.GearTags;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GearOutfitGameTests {
    public static final String Arena = "gear_arena";
    public static final List<GearOutfit> NewOutfits = List.of(GearOutfit.Goc, GearOutfit.SerpentsHand, GearOutfit.ChaosInsurgency, GearOutfit.SiteGuardTactical,
            GearOutfit.Scientist, GearOutfit.BrokenGod, GearOutfit.Sarkic);
    public static final Map<GearOutfit, Integer> ExpectedArmour = Map.of(GearOutfit.Goc, 20, GearOutfit.SerpentsHand, 11, GearOutfit.ChaosInsurgency, 19,
            GearOutfit.SiteGuardTactical, 15, GearOutfit.Scientist, 10, GearOutfit.BrokenGod, 16, GearOutfit.Sarkic, 12);
    public static final Map<GearOutfit, Double> ExpectedToughness = Map.of(GearOutfit.Goc, 6.0, GearOutfit.SerpentsHand, 0.0, GearOutfit.ChaosInsurgency, 8.0,
            GearOutfit.SiteGuardTactical, 2.0, GearOutfit.Scientist, 0.0, GearOutfit.BrokenGod, 4.0, GearOutfit.Sarkic, 0.0);
    public static final Set<GearPiece> LegCovering = EnumSet.of(GearPiece.ScientistLabCoat, GearPiece.BrokenGodRobe, GearPiece.SarkicRobe);
    public static final int RowLength = 4;
    private static final EquipmentSlot[] ArmourSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private GearOutfitGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "gear_outfits")
    public static void newOutfitsDressADClass(GameTestHelper Helper) {
        int F = floor(Helper);
        DClass Plain = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(1.5, F, 1.5));
        List<DClass> Dressed = new ArrayList<>();
        for (int Index = 0; Index < NewOutfits.size(); Index++) {
            DClass Wearer = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(3.5 + (Index % RowLength) * 2.0, F, 2.5 + (Index / RowLength) * 2.0));
            for (GearPiece Piece : pieces(NewOutfits.get(Index))) {
                Wearer.setItemSlot(Piece.getArmorType().getSlot(), new ItemStack(GearRegistry.item(Piece).get()));
            }
            Dressed.add(Wearer);
        }
        Helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    for (int Index = 0; Index < NewOutfits.size(); Index++) {
                        checkOutfit(Helper, NewOutfits.get(Index), Dressed.get(Index), Plain);
                    }
                })
                .thenExecute(() -> clean(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "gear_outfits")
    public static void newOutfitsKeepTheirPlaceInTheLineup(GameTestHelper Helper) {
        int Site = total(GearOutfit.SiteGuard);
        int Mtf = total(GearOutfit.Mtf);
        Helper.assertTrue(total(GearOutfit.SiteGuardTactical) == Site, "the tactical guard set does not match the site guard set");
        Helper.assertTrue(total(GearOutfit.SerpentsHand) < Site, "the Serpent's Hand set is not lighter than the site guard set");
        Helper.assertTrue(total(GearOutfit.Scientist) < Site, "the scientist outfit is not lighter than the site guard set");
        Helper.assertTrue(Math.abs(total(GearOutfit.Goc) - Mtf) <= 2, "the GOC set is far from the MTF set");
        Helper.assertTrue(Math.abs(total(GearOutfit.ChaosInsurgency) - Mtf) <= 2, "the Chaos Insurgency set is far from the MTF set");
        Helper.assertTrue(total(GearOutfit.BrokenGod) >= Site && total(GearOutfit.BrokenGod) <= Site + 2, "the Broken God set is not at or a little above the site guard set");
        Helper.assertTrue(total(GearOutfit.Sarkic) < Site && total(GearOutfit.Sarkic) >= Site - 4, "the Sarkic set is not a little lighter than the site guard set");
        Helper.assertTrue(new ItemStack(GearRegistry.ChaosInsurgencyHelmet.get()).is(GearTags.ToxicGasProtection), "the Chaos Insurgency helmet is not a respirator");
        for (GearOutfit Outfit : NewOutfits) {
            Helper.assertTrue(GearEquipSounds.layerFor(Outfit) != null, "no equip layer for " + Outfit.getKey());
            for (GearPiece Piece : pieces(Outfit)) {
                ItemStack Stack = new ItemStack(GearRegistry.item(Piece).get());
                Helper.assertTrue(!Stack.is(GearTags.HazmatSuit), Piece.getId() + " is tagged as hazmat");
                Helper.assertTrue(Piece == GearPiece.ChaosInsurgencyHelmet || !Stack.is(GearTags.ToxicGasProtection), Piece.getId() + " filters toxic gas");
                Helper.assertTrue(GearArmorItem.coversLegs(Stack) == LegCovering.contains(Piece), Piece.getId() + " has the wrong leg cover flag");
                Helper.assertTrue(Piece.hasCustomModel(), Piece.getId() + " has no custom model");
            }
        }
        Helper.succeed();
    }

    private static void checkOutfit(GameTestHelper Helper, GearOutfit Outfit, DClass Wearer, DClass Plain) {
        String Name = Outfit.getKey();
        Set<EquipmentSlot> Filled = EnumSet.noneOf(EquipmentSlot.class);
        for (EquipmentSlot Slot : ArmourSlots) {
            ItemStack Stack = Wearer.getItemBySlot(Slot);
            Helper.assertTrue(Stack.getItem() instanceof GearArmorItem, Name + " left the " + Slot.getName() + " slot empty");
            GearArmorItem Gear = (GearArmorItem) Stack.getItem();
            Helper.assertTrue(Gear.getPiece().getOutfit() == Outfit, Name + " has a stray piece in " + Slot.getName());
            Helper.assertTrue(Gear.getEquipmentSlot() == Slot, Name + " put " + Gear.getPiece().getId() + " in the wrong slot");
            Filled.add(Slot);
        }
        Helper.assertTrue(Filled.size() == ArmourSlots.length, Name + " does not cover all four slots");
        int Gained = Wearer.getArmorValue() - Plain.getArmorValue();
        Helper.assertTrue(Gained == ExpectedArmour.get(Outfit), Name + " gives " + Gained + " armour instead of " + ExpectedArmour.get(Outfit));
        double Toughness = Wearer.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - Plain.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        Helper.assertTrue(Math.abs(Toughness - ExpectedToughness.get(Outfit)) < 1.0E-4, Name + " gives " + Toughness + " toughness");
        boolean Respirator = Outfit == GearOutfit.ChaosInsurgency;
        Helper.assertTrue(GearTags.protectsFromToxicGas(Wearer) == Respirator, Name + " has the wrong toxic gas protection");
        Helper.assertTrue(!GearTags.wearsFullHazmatSuit(Wearer), Name + " counts as a sealed hazmat suit");
    }

    private static List<GearPiece> pieces(GearOutfit Outfit) {
        List<GearPiece> Found = new ArrayList<>();
        for (GearPiece Piece : GearPiece.values()) {
            if (Piece.getOutfit() == Outfit) {
                Found.add(Piece);
            }
        }
        return Found;
    }

    private static int total(GearOutfit Outfit) {
        ArmorMaterial Defence = Outfit.getArmor().value();
        int Sum = 0;
        for (ArmorItem.Type Kind : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            Sum += Defence.getDefense(Kind);
        }
        return Sum;
    }

    private static int floor(GameTestHelper Helper) {
        for (int Y = 0; Y < 3; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void clean(GameTestHelper Helper) {
        AABB Area = Helper.getBounds().inflate(4.0);
        for (Entity Found : Helper.getLevel().getEntities((Entity) null, Area, Candidate -> !(Candidate instanceof Player))) {
            Found.discard();
        }
    }
}
