package net.rai.scp_unleashed.gear;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.EnumMap;
import java.util.Map;

public final class GearRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    private static final Map<GearPiece, DeferredItem<GearArmorItem>> Pieces = new EnumMap<>(GearPiece.class);

    public static final DeferredItem<GearArmorItem> GasMask = piece(GearPiece.GasMask);
    public static final DeferredItem<GearArmorItem> HazmatHood = piece(GearPiece.HazmatHood);
    public static final DeferredItem<GearArmorItem> HazmatSuit = piece(GearPiece.HazmatSuit);
    public static final DeferredItem<GearArmorItem> HazmatTrousers = piece(GearPiece.HazmatTrousers);
    public static final DeferredItem<GearArmorItem> HazmatBoots = piece(GearPiece.HazmatBoots);
    public static final DeferredItem<GearArmorItem> MtfHelmet = piece(GearPiece.MtfHelmet);
    public static final DeferredItem<GearArmorItem> MtfVest = piece(GearPiece.MtfVest);
    public static final DeferredItem<GearArmorItem> MtfPants = piece(GearPiece.MtfPants);
    public static final DeferredItem<GearArmorItem> MtfBoots = piece(GearPiece.MtfBoots);
    public static final DeferredItem<GearArmorItem> SiteGuardCap = piece(GearPiece.SiteGuardCap);
    public static final DeferredItem<GearArmorItem> SiteGuardShirt = piece(GearPiece.SiteGuardShirt);
    public static final DeferredItem<GearArmorItem> SiteGuardTrousers = piece(GearPiece.SiteGuardTrousers);
    public static final DeferredItem<GearArmorItem> SiteGuardBoots = piece(GearPiece.SiteGuardBoots);
    public static final DeferredItem<GearArmorItem> DClassJumpsuit = piece(GearPiece.DClassJumpsuit);
    public static final DeferredItem<GearArmorItem> DClassTrousers = piece(GearPiece.DClassTrousers);
    public static final DeferredItem<GearArmorItem> DClassShoes = piece(GearPiece.DClassShoes);
    public static final DeferredItem<GearArmorItem> GocHelmet = piece(GearPiece.GocHelmet);
    public static final DeferredItem<GearArmorItem> GocArmor = piece(GearPiece.GocArmor);
    public static final DeferredItem<GearArmorItem> GocTrousers = piece(GearPiece.GocTrousers);
    public static final DeferredItem<GearArmorItem> GocBoots = piece(GearPiece.GocBoots);
    public static final DeferredItem<GearArmorItem> SerpentsHandHood = piece(GearPiece.SerpentsHandHood);
    public static final DeferredItem<GearArmorItem> SerpentsHandJacket = piece(GearPiece.SerpentsHandJacket);
    public static final DeferredItem<GearArmorItem> SerpentsHandTrousers = piece(GearPiece.SerpentsHandTrousers);
    public static final DeferredItem<GearArmorItem> SerpentsHandBoots = piece(GearPiece.SerpentsHandBoots);
    public static final DeferredItem<GearArmorItem> ChaosInsurgencyHelmet = piece(GearPiece.ChaosInsurgencyHelmet);
    public static final DeferredItem<GearArmorItem> ChaosInsurgencyVest = piece(GearPiece.ChaosInsurgencyVest);
    public static final DeferredItem<GearArmorItem> ChaosInsurgencyPants = piece(GearPiece.ChaosInsurgencyPants);
    public static final DeferredItem<GearArmorItem> ChaosInsurgencyBoots = piece(GearPiece.ChaosInsurgencyBoots);
    public static final DeferredItem<GearArmorItem> SiteGuardTacticalHelmet = piece(GearPiece.SiteGuardTacticalHelmet);
    public static final DeferredItem<GearArmorItem> SiteGuardTacticalUniform = piece(GearPiece.SiteGuardTacticalUniform);
    public static final DeferredItem<GearArmorItem> SiteGuardTacticalTrousers = piece(GearPiece.SiteGuardTacticalTrousers);
    public static final DeferredItem<GearArmorItem> SiteGuardTacticalBoots = piece(GearPiece.SiteGuardTacticalBoots);
    public static final DeferredItem<GearArmorItem> ScientistGlasses = piece(GearPiece.ScientistGlasses);
    public static final DeferredItem<GearArmorItem> ScientistLabCoat = piece(GearPiece.ScientistLabCoat);
    public static final DeferredItem<GearArmorItem> ScientistSlacks = piece(GearPiece.ScientistSlacks);
    public static final DeferredItem<GearArmorItem> ScientistShoes = piece(GearPiece.ScientistShoes);
    public static final DeferredItem<GearArmorItem> BrokenGodMask = piece(GearPiece.BrokenGodMask);
    public static final DeferredItem<GearArmorItem> BrokenGodRobe = piece(GearPiece.BrokenGodRobe);
    public static final DeferredItem<GearArmorItem> BrokenGodGreaves = piece(GearPiece.BrokenGodGreaves);
    public static final DeferredItem<GearArmorItem> BrokenGodBoots = piece(GearPiece.BrokenGodBoots);
    public static final DeferredItem<GearArmorItem> SarkicHood = piece(GearPiece.SarkicHood);
    public static final DeferredItem<GearArmorItem> SarkicRobe = piece(GearPiece.SarkicRobe);
    public static final DeferredItem<GearArmorItem> SarkicLeggings = piece(GearPiece.SarkicLeggings);
    public static final DeferredItem<GearArmorItem> SarkicBoots = piece(GearPiece.SarkicBoots);

    private GearRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        GearMaterials.MaterialRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        GearSounds.SoundEventRegister.register(ModEventBus);
    }

    public static DeferredItem<GearArmorItem> item(GearPiece Piece) {
        return Pieces.get(Piece);
    }

    private static DeferredItem<GearArmorItem> piece(GearPiece Piece) {
        DeferredItem<GearArmorItem> Entry = ItemRegister.register(Piece.getId(), () -> new GearArmorItem(Piece, new Item.Properties().durability(Piece.getDurability())));
        Pieces.put(Piece, Entry);
        return Entry;
    }
}
