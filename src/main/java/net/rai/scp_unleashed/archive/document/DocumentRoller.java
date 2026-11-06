package net.rai.scp_unleashed.archive.document;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.archive.ArchiveRules;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DocumentRoller {
    public static final int Tiers = AnomalyDocument.MaxDanger;
    public static final int[][] TierOdds = {
            {62, 28, 8, 2, 0},
            {28, 42, 22, 7, 1},
            {8, 24, 40, 22, 6},
            {2, 8, 24, 44, 22},
            {0, 3, 12, 35, 50}
    };

    private DocumentRoller() {
    }

    public static Map<ResourceLocation, AnomalyDocument> eligible(MinecraftServer Server) {
        return eligible(Server, null);
    }

    public static Map<ResourceLocation, AnomalyDocument> eligible(MinecraftServer Server, @Nullable UUID Reader) {
        return eligible(Server, Reader, ArchivePool.Normal);
    }

    public static Map<ResourceLocation, AnomalyDocument> eligible(MinecraftServer Server, @Nullable UUID Reader, ArchivePool Source) {
        return eligible(AnomalyDocuments.server(), DocumentClaims.get(Server), ArchiveRules.mode(Server), Reader, Source);
    }

    public static Map<ResourceLocation, AnomalyDocument> eligible(Map<ResourceLocation, AnomalyDocument> Listed, DocumentClaims Claims, ArchiveRules.Mode Rule, @Nullable UUID Reader) {
        return eligible(Listed, Claims, Rule, Reader, ArchivePool.Normal);
    }

    public static Map<ResourceLocation, AnomalyDocument> eligible(Map<ResourceLocation, AnomalyDocument> Listed, DocumentClaims Claims, ArchiveRules.Mode Rule, @Nullable UUID Reader,
                                                                  ArchivePool Source) {
        Map<ResourceLocation, AnomalyDocument> Pool = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, AnomalyDocument> Entry : Listed.entrySet()) {
            if (Entry.getValue().Pool() == Source && Entry.getValue().Weight() > 0.0f && Entry.getValue().Classification().isRollable()
                    && Claims.isAvailable(Entry.getKey(), Entry.getValue(), Rule, Reader)) {
                Pool.put(Entry.getKey(), Entry.getValue());
            }
        }
        return Pool;
    }

    public static int tierMask(Map<ResourceLocation, AnomalyDocument> Pool) {
        int Mask = 0;
        for (AnomalyDocument Document : Pool.values()) {
            Mask |= 1 << (Document.Danger() - 1);
        }
        return Mask;
    }

    public static double[] odds(int FragmentTier, int Mask) {
        double[] Weights = tierWeights(FragmentTier, Mask);
        double Total = 0.0;
        for (double Weight : Weights) {
            Total += Weight;
        }
        double[] Shares = new double[Tiers];
        for (int Tier = 0; Tier < Tiers; Tier++) {
            Shares[Tier] = Total > 0.0 ? Weights[Tier] / Total : 0.0;
        }
        return Shares;
    }

    public static List<ResourceLocation> roll(Map<ResourceLocation, AnomalyDocument> Pool, int FragmentTier, int Count, RandomSource Random) {
        Map<ResourceLocation, AnomalyDocument> Remaining = new LinkedHashMap<>(Pool);
        List<ResourceLocation> Picked = new ArrayList<>();
        while (Picked.size() < Count && !Remaining.isEmpty()) {
            double[] Weights = tierWeights(FragmentTier, tierMask(Remaining));
            int Tier = pickIndex(Weights, Random) + 1;
            List<Map.Entry<ResourceLocation, AnomalyDocument>> Candidates = new ArrayList<>();
            double Total = 0.0;
            for (Map.Entry<ResourceLocation, AnomalyDocument> Listed : Remaining.entrySet()) {
                if (Listed.getValue().Danger() == Tier) {
                    Candidates.add(Listed);
                    Total += Listed.getValue().Weight();
                }
            }
            if (Candidates.isEmpty()) {
                break;
            }
            double Roll = Random.nextDouble() * Total;
            Map.Entry<ResourceLocation, AnomalyDocument> Chosen = Candidates.getLast();
            for (Map.Entry<ResourceLocation, AnomalyDocument> Candidate : Candidates) {
                Roll -= Candidate.getValue().Weight();
                if (Roll < 0.0) {
                    Chosen = Candidate;
                    break;
                }
            }
            Picked.add(Chosen.getKey());
            Remaining.remove(Chosen.getKey());
        }
        return Picked;
    }

    private static double[] tierWeights(int FragmentTier, int Mask) {
        int Row = Math.max(1, Math.min(Tiers, FragmentTier)) - 1;
        double[] Weights = new double[Tiers];
        boolean Any = false;
        for (int Tier = 0; Tier < Tiers; Tier++) {
            if ((Mask & (1 << Tier)) != 0) {
                Weights[Tier] = TierOdds[Row][Tier];
                Any |= Weights[Tier] > 0.0;
            }
        }
        if (!Any) {
            for (int Tier = 0; Tier < Tiers; Tier++) {
                if ((Mask & (1 << Tier)) != 0) {
                    Weights[Tier] = 1.0 / (1 + Math.abs(Tier - Row));
                }
            }
        }
        return Weights;
    }

    private static int pickIndex(double[] Weights, RandomSource Random) {
        double Total = 0.0;
        for (double Weight : Weights) {
            Total += Weight;
        }
        double Roll = Random.nextDouble() * Total;
        int Last = 0;
        for (int Index = 0; Index < Weights.length; Index++) {
            if (Weights[Index] <= 0.0) {
                continue;
            }
            Last = Index;
            Roll -= Weights[Index];
            if (Roll < 0.0) {
                return Index;
            }
        }
        return Last;
    }
}
