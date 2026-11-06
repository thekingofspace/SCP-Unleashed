package net.rai.scp_unleashed.archive.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.ArchiveBlockEntity;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentRoller;
import net.rai.scp_unleashed.scp256.Scp256Registry;
import net.rai.scp_unleashed.scp261.Scp261Registry;
import net.rai.scp_unleashed.scp294.Scp294Registry;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ArchiveOddsGameTests {
    public static final String Arena = "gear_arena";
    public static final int Rolls = 400;

    private ArchiveOddsGameTests() {
    }

    private static Map<ResourceLocation, AnomalyDocument> normalPool() {
        Map<ResourceLocation, AnomalyDocument> Pool = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, AnomalyDocument> Entry : AnomalyDocuments.server().entrySet()) {
            AnomalyDocument Document = Entry.getValue();
            if (Document.Pool() == ArchivePool.Normal && Document.Weight() > 0.0f && Document.Classification().isRollable()) {
                Pool.put(Entry.getKey(), Document);
            }
        }
        return Pool;
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "archive_odds_reach")
    public static void fragmentsOnlyDealTheirTiers(GameTestHelper Helper) {
        Map<ResourceLocation, AnomalyDocument> Pool = normalPool();
        Helper.assertTrue(!Pool.isEmpty(), "no archive files are loaded");
        RandomSource Dice = RandomSource.create(4021L);
        for (FragmentRarity Grade : FragmentRarity.values()) {
            int[] Row = DocumentRoller.TierOdds[Grade.getTier() - 1];
            for (int Roll = 0; Roll < Rolls; Roll++) {
                for (ResourceLocation Id : DocumentRoller.roll(Pool, Grade.getTier(), ArchiveBlockEntity.CardCount, Dice)) {
                    int Danger = Pool.get(Id).Danger();
                    Helper.assertTrue(Row[Danger - 1] > 0, "a " + Grade + " fragment dealt " + Id + " from tier " + Danger + " while its own tiers still had files");
                }
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "archive_odds_classes")
    public static void keterFilesSitInHighTiers(GameTestHelper Helper) {
        for (Map.Entry<ResourceLocation, AnomalyDocument> Entry : normalPool().entrySet()) {
            DocumentClass Grade = Entry.getValue().Classification();
            if (Grade == DocumentClass.Keter || Grade == DocumentClass.Apollyon) {
                Helper.assertTrue(Entry.getValue().Danger() >= 4, Entry.getKey() + " is " + Grade.getSerializedName() + " but sits at tier " + Entry.getValue().Danger());
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "archive_odds_spent")
    public static void spentReachStillDeals(GameTestHelper Helper) {
        Map<ResourceLocation, AnomalyDocument> Top = new LinkedHashMap<>();
        Map<ResourceLocation, AnomalyDocument> Bottom = new LinkedHashMap<>();
        normalPool().forEach((Id, Document) -> {
            if (Document.Danger() == AnomalyDocument.MaxDanger) {
                Top.put(Id, Document);
            }
            if (Document.Danger() == AnomalyDocument.MinDanger) {
                Bottom.put(Id, Document);
            }
        });
        Helper.assertTrue(!Top.isEmpty() && !Bottom.isEmpty(), "the archive needs files at both ends of the scale");
        RandomSource Dice = RandomSource.create(77L);
        int Faint = FragmentRarity.Faint.getTier();
        int Abyssal = FragmentRarity.Abyssal.getTier();
        Map.Entry<ResourceLocation, AnomalyDocument> Last = Bottom.entrySet().iterator().next();
        Map<ResourceLocation, AnomalyDocument> Mixed = new LinkedHashMap<>(Top);
        Mixed.put(Last.getKey(), Last.getValue());
        for (int Roll = 0; Roll < Rolls; Roll++) {
            Helper.assertTrue(DocumentRoller.roll(Top, Faint, ArchiveBlockEntity.CardCount, Dice).size() == Math.min(ArchiveBlockEntity.CardCount, Top.size()),
                    "a faint fragment stopped dealing once its own tiers ran out");
            Helper.assertTrue(DocumentRoller.roll(Bottom, Abyssal, ArchiveBlockEntity.CardCount, Dice).size() == Math.min(ArchiveBlockEntity.CardCount, Bottom.size()),
                    "an abyssal fragment stopped dealing once its own tiers ran out");
            List<ResourceLocation> Dealt = DocumentRoller.roll(Mixed, Faint, ArchiveBlockEntity.CardCount, Dice);
            Helper.assertTrue(Dealt.size() == Math.min(ArchiveBlockEntity.CardCount, Mixed.size()), "a faint fragment dealt a short hand");
            Helper.assertTrue(Dealt.get(0).equals(Last.getKey()), "a faint fragment dealt outside its tiers before its last in-range file");
        }
        double Shown = 0.0;
        for (double Share : DocumentRoller.odds(Faint, DocumentRoller.tierMask(Top))) {
            Shown += Share;
        }
        Helper.assertTrue(Math.abs(Shown - 1.0) < 1.0e-6, "the odds panel shows nothing for a faint fragment with only tier V files left");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "archive_stranger_blocks")
    public static void strangersBringBlockAnomalies(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer Reader = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "BlockCourier"));
        BlockPos Spot = Helper.absolutePos(new BlockPos(4, 2, 4));
        Map<ResourceLocation, Item> Expected = Map.<ResourceLocation, Item>of(Scp256Registry.DocumentId, Scp256Registry.TypewriterItem.get(),
                Scp261Registry.DocumentId, Scp261Registry.VendingItem.get(), Scp294Registry.DocumentId, Scp294Registry.MachineItem.get());
        for (Map.Entry<ResourceLocation, Item> Entry : Expected.entrySet()) {
            AnomalyDocument Document = AnomalyDocuments.server().get(Entry.getKey());
            if (Document == null) {
                Helper.fail(Entry.getKey() + " has no file");
                return;
            }
            if (!(Document.Condition() instanceof StrangerEncounterCondition Courier)) {
                Helper.fail(Entry.getKey() + " does not arrive with a stranger");
                return;
            }
            Helper.assertTrue(Courier.plan().Gifts().stream().anyMatch(Gift -> Gift.toStack().is(Entry.getValue())), Entry.getKey() + " is not handed over by the stranger");
            AnomalySpawners.spawnerFor(Entry.getKey()).ifPresent(Hook -> Hook.spawn(new AnomalySpawners.Discovery(World, Spot, Reader, Entry.getKey(), Document, false)));
        }
        for (BlockPos Near : BlockPos.betweenClosed(Spot.offset(-3, -2, -3), Spot.offset(3, 3, 3))) {
            BlockState State = World.getBlockState(Near);
            Helper.assertFalse(State.is(Scp256Registry.Typewriter.get()) || State.is(Scp261Registry.Vending.get()) || State.is(Scp294Registry.Machine.get()),
                    "a block anomaly was placed in the world at " + Near);
        }
        Helper.succeed();
    }
}
