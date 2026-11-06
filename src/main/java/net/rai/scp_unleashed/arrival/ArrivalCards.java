package net.rai.scp_unleashed.arrival;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.arrival.network.ArrivalCardPayload;
import net.rai.scp_unleashed.intro.IntroDirector;
import net.rai.scp_unleashed.scp2521.Scp2521Trips;
import net.rai.scp_unleashed.scp902.Scp902State;
import net.rai.scp_unleashed.scp902.Scp902Trip;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class ArrivalCards {
    public static final String Prefix = "arrival.scp_unleashed.";
    public static final int Length = 120;
    public static final float FadeInStart = 10.0f;
    public static final float FadeInTicks = 16.0f;
    public static final float FadeOutTicks = 24.0f;
    public static final float RuleStart = 16.0f;
    public static final float RuleTicks = 20.0f;
    public static final float TitleScale = 2.0f;
    public static final int RulePad = 16;
    public static final int RuleOffset = 12;
    public static final int SubtitleOffset = 18;
    public static final int TitleRise = -8;
    public static final ResourceKey<Level> Winter = key("scp4666_winter");
    public static final ResourceKey<Level> ShowStage = key("show_stage");
    public static final ResourceKey<Level> Wastes093 = key("scp093_wastes");
    public static final ResourceKey<Level> Forest = key("scp860_forest");
    public static final ResourceKey<Level> Store = key("scp3008_store");
    public static final ResourceKey<Level> Pocket = key("scp106_pocket");
    public static final ResourceKey<Level> Stairwell = key("scp087_stairwell");
    public static final ResourceKey<Level> Moon = key("moon");
    public static final ResourceKey<Level> Facility1730 = key("scp1730_facility");
    public static final ResourceKey<Level> Desert2317 = key("scp2317_desert");
    public static final ResourceKey<Level> Day001 = key("scp001_day");
    public static final ResourceKey<Level> Garden001 = key("scp001_garden");
    public static final ResourceKey<Level> Wastes902 = key("the_wastes");
    public static final ResourceKey<Level> Void2521 = key("scp2521_void");
    public static final ResourceKey<Level> Intro = key("intro");
    public static final List<ResourceKey<Level>> Listed = List.of(Winter, ShowStage, Wastes093, Forest, Store, Pocket, Stairwell, Moon, Day001, Garden001, Facility1730, Desert2317);
    public static final List<ResourceKey<Level>> Cutscenes = List.of(Wastes902, Void2521, Intro);

    private ArrivalCards() {
    }

    public static ResourceKey<Level> key(String Path) {
        return ResourceKey.create(Registries.DIMENSION, ArrivalRegistry.location(Path));
    }

    public static boolean listed(@Nullable ResourceKey<Level> Dimension) {
        return Dimension != null && Listed.contains(Dimension);
    }

    public static boolean cutsceneDimension(@Nullable ResourceKey<Level> Dimension) {
        return Dimension != null && Cutscenes.contains(Dimension);
    }

    public static boolean triggers(@Nullable ResourceKey<Level> From, @Nullable ResourceKey<Level> To, boolean Cutscene, boolean Seen) {
        return To != null && !To.equals(From) && listed(To) && !cutsceneDimension(From) && !Cutscene && !Seen;
    }

    public static String titleKey(ResourceKey<Level> Dimension) {
        return Prefix + Dimension.location().getPath() + ".title";
    }

    public static String subtitleKey(ResourceKey<Level> Dimension) {
        return Prefix + Dimension.location().getPath() + ".subtitle";
    }

    public static float share(float Ticks, int Span) {
        float In = Mth.clamp((Ticks - FadeInStart) / FadeInTicks, 0.0f, 1.0f);
        float Out = Mth.clamp((Span - Ticks) / FadeOutTicks, 0.0f, 1.0f);
        return Math.min(In, Out);
    }

    public static float ruleShare(float Ticks) {
        return Mth.clamp((Ticks - RuleStart) / RuleTicks, 0.0f, 1.0f);
    }

    public static ArrivalSeen seen(Player User) {
        return User.getData(ArrivalRegistry.Seen.get());
    }

    public static boolean hasSeen(Player User, ResourceKey<Level> Dimension) {
        return seen(User).has(Dimension.location());
    }

    public static void markSeen(Player User, ResourceKey<Level> Dimension) {
        User.setData(ArrivalRegistry.Seen.get(), seen(User).with(Dimension.location()));
    }

    public static void reset(Player User) {
        User.setData(ArrivalRegistry.Seen.get(), ArrivalSeen.Fresh);
    }

    public static boolean cutscene(ServerPlayer User) {
        UUID Id = User.getUUID();
        return IntroDirector.session(User) != null || Scp2521Trips.active(Id) || Scp902Trip.waiting(Id) || Scp902State.get(User.server).trip(Id) != null;
    }

    public static boolean changed(ServerPlayer User, ResourceKey<Level> From, ResourceKey<Level> To) {
        if (!triggers(From, To, cutscene(User), hasSeen(User, To))) {
            return false;
        }
        markSeen(User, To);
        if (!(User instanceof FakePlayer) && User.connection != null) {
            PacketDistributor.sendToPlayer(User, new ArrivalCardPayload(To.location()));
        }
        return true;
    }
}
