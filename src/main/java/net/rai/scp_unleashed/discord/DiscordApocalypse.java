package net.rai.scp_unleashed.discord;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.worldend.WorldEndConfig;
import net.rai.scp_unleashed.worldend.WorldEndRules;
import net.rai.scp_unleashed.worldend.WorldEnding;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class DiscordApocalypse {
    public static final String Prefix = "discord.scp_unleashed.apocalypse.";
    public static final long RepeatMillis = 60000L;
    public static final long RitualRepeatMillis = 600000L;
    public static final int Alert = -1;
    public static final int Relief = 0xC9A227;
    public static final int Doom = 0x3A0008;
    public static final int Quiet = 0x7F8C8D;
    public static final int Memory = 256;
    public static final long DayTicks = 24000L;
    public static final String Scp5001 = "scp5001";
    public static final String Scarlet = "scp001_scarlet";
    public static final ResourceLocation Scp5001Source = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Scp5001);
    public static final ResourceLocation ScarletSource = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Scarlet);
    public static final int ScarletChains = 7;
    public static final int[] Thresholds = {25, 50, 75};
    public static final int Rearm = 5;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<String, Long> Recent = new ConcurrentHashMap<>();
    private static final Set<Integer> Spent = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, String> Sources = new ConcurrentHashMap<>(Map.of(Scp5001Source, Scp5001, ScarletSource, Scarlet));

    public enum Step {
        Breach("breach", Alert),
        Overdrive("overdrive", Alert),
        Purge("purge", Alert),
        Recontained("recontained", Relief),
        Xk1("xk1", Alert),
        Xk2("xk2", Alert),
        Summoning("summoning", Alert),
        Duel("duel", Alert),
        Saved("saved", Relief);

        private final String Key;
        private final int Colour;

        Step(String Key, int Colour) {
            this.Key = Key;
            this.Colour = Colour;
        }

        public String getKey() {
            return Key;
        }

        public int getColour() {
            return Colour;
        }
    }

    public record Fact(Component Name, Component Value, boolean Inline) {
        public static Fact of(Component Name, Component Value) {
            return new Fact(Name, Value, true);
        }

        public static Fact of(String NameKey, Component Value) {
            return new Fact(Component.translatable(NameKey), Value, true);
        }
    }

    private DiscordApocalypse() {
    }

    private static String text(String Key, Object... Args) {
        return DiscordText.translate(Prefix + Key, Args);
    }

    private static Component line(String Key, Object... Args) {
        return Component.translatable(Prefix + Key, Args);
    }

    private static boolean has(String Key) {
        return Language.getInstance().has(Prefix + Key);
    }

    private static Component either(String Key, String Fallback, Object... Args) {
        return has(Key) ? line(Key, Args) : line(Fallback, Args);
    }

    public static Fact stage(String Key) {
        return Fact.of(Prefix + "field.stage", line("stage." + Key));
    }

    public static Fact remaining(int DaysLeft) {
        return Fact.of(Prefix + "field.remaining", days(DaysLeft));
    }

    public static Component days(int Count) {
        return Count == 1 ? line("days.one") : line("days.many", Count);
    }

    public static String scenarioName(String Id) {
        return has("scenario." + Id) ? text("scenario." + Id) : Id;
    }

    public static void claim(ResourceLocation Source, String Scenario) {
        Sources.put(Source, Scenario);
    }

    public static String scenarioOf(@Nullable ResourceLocation Source) {
        if (Source == null) {
            return "unknown";
        }
        String Known = Sources.get(Source);
        return Known != null ? Known : Source.toString();
    }

    public static int totalDays(long Total) {
        return (int) Math.max(1L, Math.round(Total / (double) DayTicks));
    }

    public static int daysLeft(long Elapsed, long Total) {
        if (Total <= 0L) {
            return 0;
        }
        double Progress = Math.max(0.0, Math.min(1.0, Elapsed / (double) Total));
        if (Progress >= 1.0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(totalDays(Total) * (1.0 - Progress) - 1.0E-9));
    }

    public static String ordinal(int Value) {
        int Tens = Math.abs(Value) % 100;
        int Ones = Math.abs(Value) % 10;
        String Suffix = Tens >= 11 && Tens <= 13 ? "th" : Ones == 1 ? "st" : Ones == 2 ? "nd" : Ones == 3 ? "rd" : "th";
        return text("ordinal." + Suffix, Value);
    }

    private static boolean fresh(String Key, long Repeat) {
        long Now = System.currentTimeMillis();
        Long Last = Recent.get(Key);
        if (Last != null && Repeat > 0L && Now - Last < Repeat) {
            return false;
        }
        if (Recent.size() > Memory) {
            Recent.entrySet().removeIf(Entry -> Now - Entry.getValue() >= RitualRepeatMillis);
        }
        Recent.put(Key, Now);
        return true;
    }

    public static void resetForTests() {
        Recent.clear();
        Spent.clear();
    }

    public static void forget() {
        Recent.clear();
        Spent.clear();
    }

    public static boolean scenario(MinecraftServer Server, String Id, String Stage, Component Title, Component Detail, @Nullable BlockPos Where, int Colour) {
        return scenario(Server, Id, Stage, Title, Detail, Where, Level.OVERWORLD, Colour, List.of(), RepeatMillis).isPresent();
    }

    public static boolean scenario(MinecraftServer Server, String Id, String Stage, Component Title, Component Detail, @Nullable BlockPos Where, int Colour,
                                   List<Fact> Facts) {
        return scenario(Server, Id, Stage, Title, Detail, Where, Level.OVERWORLD, Colour, Facts, RepeatMillis).isPresent();
    }

    public static Optional<CompletableFuture<DiscordRelay.Result>> scenario(MinecraftServer Server, String Id, String Stage, Component Title, Component Detail,
                                                                             @Nullable BlockPos Where, ResourceKey<Level> Dimension, int Colour, List<Fact> Facts,
                                                                             long Repeat) {
        return send(Server, Id, Stage, Title, Detail, Where, Dimension, Colour, Facts, Repeat, null);
    }

    private static Optional<CompletableFuture<DiscordRelay.Result>> send(MinecraftServer Server, String Id, String Stage, Component Title, Component Detail,
                                                                         @Nullable BlockPos Where, ResourceKey<Level> Dimension, int Colour, List<Fact> Facts,
                                                                         long Repeat, @Nullable Player Face) {
        if (Server == null || !Discord.wants(DiscordCategory.Apocalypse)) {
            return Optional.empty();
        }
        try {
            if (!fresh(Id + "|" + Stage, Repeat)) {
                return Optional.empty();
            }
            DiscordEmbed Embed = new DiscordEmbed().author(text("author"), "").title(Title.getString()).description(Detail.getString())
                    .field(text("field.scenario"), scenarioName(Id), true);
            for (Fact Each : Facts) {
                Embed.field(Each.Name().getString(), Each.Value().getString(), Each.Inline());
            }
            if (Colour >= 0) {
                Embed.colour(Colour);
            }
            if (Face != null) {
                Embed.thumbnail(DiscordNotices.face(Face));
            }
            if (Where != null && Discord.settings().coordinatesFor(DiscordCategory.Apocalypse)) {
                Embed.position(Where, Dimension);
            }
            return Discord.post(DiscordCategory.Apocalypse, Embed);
        } catch (RuntimeException Problem) {
            Log.warn("Could not build a Discord apocalypse message for {} {}: {}", Id, Stage, Problem.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static boolean standard(MinecraftServer Server, String Id, String Stage, String Label, @Nullable BlockPos Where, int Colour, List<Fact> Extra,
                                    Object... TitleArgs) {
        List<Fact> Facts = new ArrayList<>();
        Facts.add(stage(Label));
        Facts.addAll(Extra);
        return send(Server, Id, Stage, line(Id + "." + Stage + ".title", TitleArgs), line(Id + "." + Stage + ".detail"), Where, Level.OVERWORLD, Colour, Facts,
                RepeatMillis, null).isPresent();
    }

    public static boolean scp5001(MinecraftServer Server, Step Stage, @Nullable BlockPos Site, Fact... Extra) {
        return standard(Server, Scp5001, Stage.getKey(), Stage.getKey(), Site, Stage.getColour(), List.of(Extra));
    }

    public static boolean countdown(MinecraftServer Server, String Id, int DaysLeft, int TotalDays, @Nullable BlockPos Where) {
        if (DaysLeft <= 0) {
            return false;
        }
        int Total = Math.max(TotalDays, DaysLeft);
        Component Title = DaysLeft == 1 ? line("countdown.one") : line("countdown.many", DaysLeft);
        Component Detail = either(Id + ".countdown.detail", "countdown.detail", Total - DaysLeft + 1, Total);
        return send(Server, Id, "countdown_" + DaysLeft, Title, Detail, Where, Level.OVERWORLD, Alert, List.of(stage("countdown"), remaining(DaysLeft)),
                RepeatMillis, null).isPresent();
    }

    public static boolean cancelled(MinecraftServer Server, String Id, Component StageAtCancel, @Nullable BlockPos Where) {
        return send(Server, Id, "cancelled", line("cancelled.title"), line("cancelled.detail"), Where, Level.OVERWORLD, Quiet,
                List.of(stage("cancelled"), Fact.of(Prefix + "field.cancelled_at", StageAtCancel)), RepeatMillis, null).isPresent();
    }

    public static boolean scarletManifestation(MinecraftServer Server, float Before, float After, @Nullable BlockPos Where) {
        int Now = Math.round(After * 100.0f);
        for (int Threshold : Thresholds) {
            if (Now < Threshold - Rearm) {
                Spent.remove(Threshold);
            }
        }
        if (After <= Before) {
            return false;
        }
        int Crossed = -1;
        for (int Threshold : Thresholds) {
            float Mark = Threshold / 100.0f;
            if (Before < Mark && After >= Mark && !Spent.contains(Threshold)) {
                Crossed = Threshold;
            }
        }
        if (Crossed < 0 || !Discord.wants(DiscordCategory.Apocalypse)) {
            return false;
        }
        for (int Threshold : Thresholds) {
            if (Threshold <= Crossed && Before < Threshold / 100.0f) {
                Spent.add(Threshold);
            }
        }
        List<Fact> Facts = List.of(stage("manifestation"), Fact.of(Prefix + "field.manifestation", Component.literal(Now + "%")));
        return send(Server, Scarlet, "manifestation_" + Crossed, line(Scarlet + ".manifestation.title", Crossed), line(Scarlet + ".manifestation.detail", Crossed),
                Where, Level.OVERWORLD, Alert, Facts, RepeatMillis, null).isPresent();
    }

    public static boolean scarletChain(MinecraftServer Server, int Broken, @Nullable BlockPos Where) {
        if (Broken <= 0) {
            return false;
        }
        int Total = Math.max(ScarletChains, Broken);
        List<Fact> Facts = List.of(stage("chains"), Fact.of(Prefix + "field.chains", line("chains.count", Broken, Total)));
        return send(Server, Scarlet, "chain_" + Broken, line(Scarlet + ".chain.title", ordinal(Broken).toUpperCase(Locale.ROOT)), line(Scarlet + ".chain.detail", Broken, Total), Where,
                Level.OVERWORLD, Alert, Facts, RepeatMillis, null).isPresent();
    }

    public static boolean scarletRitual(MinecraftServer Server, @Nullable BlockPos Where, ResourceKey<Level> Dimension) {
        if (!Discord.settings().allows(DiscordToggle.ScarletRituals)) {
            return false;
        }
        return send(Server, Scarlet, "ritual", line(Scarlet + ".ritual.title"), line(Scarlet + ".ritual.detail"), Where, Dimension, Alert, List.of(stage("ritual")),
                RitualRepeatMillis, null).isPresent();
    }

    public static boolean scarletXk2(MinecraftServer Server, int DaysLeft, @Nullable BlockPos Throne) {
        List<Fact> Extra = DaysLeft > 0 ? List.of(remaining(DaysLeft)) : List.of();
        return standard(Server, Scarlet, "xk2", "xk2", Throne, Alert, Extra);
    }

    public static boolean scarletCountdown(MinecraftServer Server, int DaysLeft, int TotalDays, @Nullable BlockPos Throne) {
        return countdown(Server, Scarlet, DaysLeft, TotalDays, Throne);
    }

    public static boolean scarletRite(MinecraftServer Server, @Nullable Player Leader, @Nullable BlockPos Throne) {
        List<Fact> Facts = new ArrayList<>();
        Facts.add(stage("rite"));
        if (Leader != null) {
            Facts.add(Fact.of(Prefix + "field.leader", Component.literal(Leader.getGameProfile().getName())));
        }
        return send(Server, Scarlet, "rite", line(Scarlet + ".rite.title"), line(Scarlet + ".rite.detail"), Throne, Level.OVERWORLD, Alert, Facts, RepeatMillis,
                Leader).isPresent();
    }

    public static boolean scarletSlain(MinecraftServer Server, @Nullable Entity Slayer, @Nullable BlockPos Where) {
        List<Fact> Facts = new ArrayList<>();
        Facts.add(stage("slain"));
        if (Slayer != null) {
            Facts.add(Fact.of("discord.scp_unleashed.field.slayer", Component.literal(Slayer.getDisplayName().getString())));
        }
        return send(Server, Scarlet, "slain", line(Scarlet + ".slain.title"), line(Scarlet + ".slain.detail"), Where, Level.OVERWORLD, Relief, Facts, RepeatMillis,
                Slayer instanceof Player User ? User : null).isPresent();
    }

    public static boolean scarletEscaped(MinecraftServer Server, @Nullable BlockPos Where) {
        return standard(Server, Scarlet, "escaped", "escaped", Where, Doom, List.of());
    }

    public static boolean scarletEnding(MinecraftServer Server, @Nullable BlockPos Where) {
        return ending(Server, ScarletSource, null, Where).isPresent();
    }

    public static boolean scarletCancelled(MinecraftServer Server, Component StageAtCancel, @Nullable BlockPos Where) {
        return cancelled(Server, Scarlet, StageAtCancel, Where);
    }

    private static Component deletion(MinecraftServer Server) {
        if (!WorldEndRules.endings(Server) || !WorldEndConfig.allowDeletion()) {
            return line("deletion.off");
        }
        return WorldEnding.dryRun(Server) ? line("deletion.dry_run") : line("deletion.armed");
    }

    public static void worldEnding(MinecraftServer Server, ResourceLocation Source, Component Reason, @Nullable Vec3 Focus) {
        String Id = scenarioOf(Source);
        BlockPos Where = Scp5001.equals(Id) ? DiscordApocalypseWatch.site(Server) : null;
        if (Where == null && Focus != null) {
            Where = BlockPos.containing(Focus);
        }
        ending(Server, Source, Reason, Where);
    }

    private static Optional<CompletableFuture<DiscordRelay.Result>> ending(MinecraftServer Server, ResourceLocation Source, @Nullable Component Reason,
                                                                           @Nullable BlockPos Where) {
        if (Server == null || !Discord.wants(DiscordCategory.Apocalypse)) {
            return Optional.empty();
        }
        String Id = scenarioOf(Source);
        String Said = Reason == null ? "" : Reason.getString().strip();
        String Body = either(Id + ".ending.detail", "ending.detail").getString();
        Component Detail = Component.literal(Said.isEmpty() ? Body : Said + "\n\n" + Body);
        List<Fact> Facts = List.of(stage("ending"), Fact.of(Prefix + "field.source", Component.literal(Source.toString())),
                Fact.of(Prefix + "field.deletion", deletion(Server)));
        return send(Server, Id, "ending", line("ending.title"), Detail, Where, Level.OVERWORLD, Doom, Facts, RepeatMillis, null);
    }

    public static boolean lastWords(MinecraftServer Server, String SourceId, String WorldName) {
        if (Server == null || !Discord.wants(DiscordCategory.Apocalypse)) {
            return false;
        }
        ResourceLocation Source = ResourceLocation.tryParse(SourceId == null ? "" : SourceId);
        String Id = scenarioOf(Source);
        List<Fact> Facts = new ArrayList<>();
        Facts.add(stage("final"));
        if (WorldName != null && !WorldName.isBlank()) {
            Facts.add(Fact.of(Prefix + "field.world", Component.literal(WorldName)));
        }
        Optional<CompletableFuture<DiscordRelay.Result>> Sent = send(Server, Id, "final", line("final.title"), either(Id + ".final.detail", "final.detail"), null,
                Level.OVERWORLD, Doom, Facts, RepeatMillis, null);
        if (Sent.isEmpty()) {
            return false;
        }
        Discord.keep(Sent.get());
        boolean Delivered = Discord.await(Sent.get(), Discord.FinalFlush);
        if (!Delivered) {
            Log.warn("The final Discord notice for the world ending was not confirmed within {} seconds", Discord.FinalFlush.toSeconds());
        }
        return Delivered;
    }
}
