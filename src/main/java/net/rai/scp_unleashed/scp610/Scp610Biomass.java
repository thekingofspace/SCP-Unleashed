package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.rai.scp_unleashed.faction.FactionLive;
import net.rai.scp_unleashed.faction.FactionState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp610Biomass {
    public static final double Start = 50.0;
    public static final long TrickleTicks = 600L;
    public static final String TrickleTimer = "BiomassTrickle";
    public static final double Trickle = 1.0;
    public static final double TrickleFlesh = 256.0;
    public static final double GrowthCost = 0.1;
    public static final double CombatCost = 6.0;
    public static final double UtilityCost = 10.0;
    public static final double AdvancedCost = 60.0;
    public static final double MergeCost = 30.0;
    public static final double MatriarchCost = 150.0;
    public static final double KillGain = 3.0;
    public static final double KillReach = 32.0;
    public static final int LivestockGain = 8;
    public static final int MeatGain = 2;
    public static final int RottenGain = 1;
    public static final int CarryCap = 16;
    public static final int AdvancedLoadedCap = 8;
    public static final double UtilityShare = 0.5;
    public static final List<Scp610Form> UtilityWeights = List.of(Scp610Form.Harvester, Scp610Form.Harvester, Scp610Form.Harvester, Scp610Form.Tender, Scp610Form.Tender,
            Scp610Form.Weaver, Scp610Form.Weaver, Scp610Form.SporeBearer, Scp610Form.SporeBearer, Scp610Form.Burrower);

    private Scp610Biomass() {
    }

    public static double capOf(Scp610Hive Hive) {
        return Scp610Limits.biomassCap(Hive.flesh());
    }

    public static double trickleFor(Scp610Hive Hive) {
        return Trickle + Hive.flesh() / TrickleFlesh;
    }

    public static boolean affordsGrowth(Scp610Hive Hive) {
        return Hive.biomass() >= GrowthCost - 1.0e-9;
    }

    public static double add(Scp610Hive Hive, double Amount) {
        double Before = Hive.biomass();
        Hive.setBiomass(Before + Amount);
        return Hive.biomass() - Before;
    }

    public static boolean spend(Scp610Hive Hive, double Cost) {
        if (Hive.biomass() < Cost - 1.0e-9) {
            return false;
        }
        Hive.setBiomass(Hive.biomass() - Cost);
        return true;
    }

    public static double costOf(Scp610Form Form) {
        if (Form == Scp610Form.Matriarch) {
            return MatriarchCost;
        }
        if (Form.evolved()) {
            return AdvancedCost;
        }
        return Form.utility() ? UtilityCost : CombatCost;
    }

    public static int capFor(Scp610Form Form) {
        return switch (Form) {
            case Tender, Weaver, SporeBearer, SiegeBrute, Stalker -> 2;
            case Harvester, Spitter -> 3;
            case Burrower, BroodSac -> 1;
            default -> Integer.MAX_VALUE;
        };
    }

    public static int countAt(int Site, Scp610Form Form) {
        int Count = 0;
        for (Mob Member : FactionLive.atSite(Scp610Faction.Id, Site)) {
            if (Member instanceof Scp610Creature Creature && Creature.form() == Form) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean underCap(Scp610Hive Hive, Scp610Form Form) {
        return countAt(Hive.id(), Form) < capFor(Form);
    }

    public static int advancedLoaded() {
        int Count = 0;
        for (Entity Member : FactionLive.members(Scp610Faction.Id)) {
            if (Member instanceof Scp610Creature Creature && Creature.isAlive() && Creature.form().evolved()) {
                Count++;
            }
        }
        return Count;
    }

    @Nullable
    public static Scp610Form pickUtility(RandomSource Random, Scp610Hive Hive) {
        List<Scp610Form> Open = new ArrayList<>();
        for (Scp610Form Form : UtilityWeights) {
            if (underCap(Hive, Form)) {
                Open.add(Form);
            }
        }
        return Open.isEmpty() ? null : Open.get(Random.nextInt(Open.size()));
    }

    public static Scp610Form pickHiveRecruit(RandomSource Random, Scp610Hive Hive) {
        if (Random.nextDouble() < UtilityShare) {
            Scp610Form Utility = pickUtility(Random, Hive);
            if (Utility != null) {
                return Utility;
            }
        }
        return Scp610Faction.pickRecruit(Random);
    }

    @Nullable
    public static Scp610Form pickAdvanced(RandomSource Random, Scp610Hive Hive) {
        if (advancedLoaded() >= AdvancedLoadedCap) {
            return null;
        }
        List<Scp610Form> Open = new ArrayList<>();
        for (Scp610Form Form : List.of(Scp610Form.Stalker, Scp610Form.Spitter)) {
            if (underCap(Hive, Form)) {
                Open.add(Form);
            }
        }
        return Open.isEmpty() ? null : Open.get(Random.nextInt(Open.size()));
    }

    public static int meatValue(ItemStack Stack) {
        if (Stack.is(Items.ROTTEN_FLESH)) {
            return RottenGain;
        }
        if (Stack.is(Items.BEEF) || Stack.is(Items.PORKCHOP) || Stack.is(Items.MUTTON) || Stack.is(Items.CHICKEN) || Stack.is(Items.RABBIT)) {
            return MeatGain;
        }
        return 0;
    }

    @Nullable
    public static Scp610Hive nearest(ServerLevel World, BlockPos Pos, double Reach) {
        Scp610Hive Best = null;
        double BestGap = Reach * Reach;
        for (Scp610Hive Hive : Scp610Hives.list(World.getServer())) {
            if (!Hive.Site.dimension().equals(World.dimension())) {
                continue;
            }
            double Gap = Hive.anchor().distSqr(Pos);
            if (Gap <= BestGap) {
                BestGap = Gap;
                Best = Hive;
            }
        }
        return Best;
    }

    public static void killed(ServerLevel World, LivingEntity Victim, Scp610Creature Killer) {
        if (Victim instanceof Scp610Creature) {
            return;
        }
        if (Killer instanceof Scp610Harvester Harvester) {
            Harvester.load(LivestockGain);
            return;
        }
        Scp610Hive Hive = nearest(World, Victim.blockPosition(), KillReach);
        if (Hive != null) {
            add(Hive, KillGain);
        }
    }

    public static void tick(MinecraftServer Server) {
        if (Server.getTickCount() % 20 != 0) {
            return;
        }
        FactionState State = Scp610Hives.state(Server);
        if (!State.phase().active()) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        long Last = State.timer(TrickleTimer);
        if (Last > Now + 1000000L) {
            Last = 0L;
        }
        if (Now - Last < TrickleTicks) {
            return;
        }
        State.setTimer(TrickleTimer, Now);
        for (Scp610Hive Hive : Scp610Hives.list(Server)) {
            add(Hive, trickleFor(Hive));
        }
    }
}
