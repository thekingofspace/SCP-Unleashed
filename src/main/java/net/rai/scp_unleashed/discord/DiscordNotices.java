package net.rai.scp_unleashed.discord;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.computer.ChatServerBlockEntity;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.net.ChatService;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.mtf.MtfTier;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.base.RaidBases;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerVariant;
import net.rai.scp_unleashed.warhead.WarheadSequence;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordNotices {
    public static final String Prefix = "discord.scp_unleashed.";
    public static final int RereadMemory = 2048;
    public static final Pattern AnomalyToken = Pattern.compile("^(?:scp(\\d{3,4})(j?)|z(\\d{4}))(?:_.*)?$");
    public static final Pattern AnomalyPackage = Pattern.compile("^net\\.rai\\.scp_unleashed\\.(scp\\d{3,4}j?|z\\d{4})(?:\\..*)?$");
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<String, Long> Reread = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> Sessions = new ConcurrentHashMap<>();

    private DiscordNotices() {
    }

    private static String text(String Key, Object... Args) {
        return DiscordText.translate(Prefix + Key, Args);
    }

    private static String pick(String Key, String Fallback, Object... Args) {
        return Language.getInstance().has(Prefix + Key) ? text(Key, Args) : text(Fallback, Args);
    }

    private static void post(DiscordCategory Category, DiscordEmbed Embed) {
        Discord.post(Category, Embed);
    }

    private static void guard(DiscordCategory Category, Runnable Body) {
        if (!Discord.wants(Category)) {
            return;
        }
        try {
            Body.run();
        } catch (RuntimeException Problem) {
            Log.warn("Could not build a Discord {} message: {}", Category.getKey(), Problem.getClass().getSimpleName());
        }
    }

    private static void guard(DiscordToggle Toggle, Runnable Body) {
        if (!Discord.settings().allows(Toggle)) {
            return;
        }
        guard(Toggle.getCategory(), Body);
    }

    private static DiscordFilters filters() {
        return Discord.settings().Filters();
    }

    private static void where(DiscordEmbed Embed, DiscordCategory Category, BlockPos Pos, ResourceKey<Level> Dimension) {
        if (Discord.settings().coordinatesFor(Category)) {
            Embed.position(Pos, Dimension);
        }
    }

    private static void where(DiscordEmbed Embed, DiscordCategory Category, BlockPos Pos, String Dimension) {
        if (Discord.settings().coordinatesFor(Category)) {
            Embed.position(Pos, Dimension);
        }
    }

    public static String face(DiscordSettings Settings, UUID Id, String Name) {
        String Template = Settings.FaceTemplate();
        if (Template.isEmpty()) {
            return "";
        }
        String Filled = Template.replace("{uuid}", Id.toString()).replace("{name}", URLEncoder.encode(Name, StandardCharsets.UTF_8));
        return DiscordUrls.isImage(Filled) ? Filled : "";
    }

    public static String face(Player User) {
        return face(Discord.settings(), User.getUUID(), User.getGameProfile().getName());
    }

    private static String name(Player User) {
        return User.getGameProfile().getName();
    }

    private static String yes(boolean Value) {
        return text(Value ? "yes" : "no");
    }

    public static String nodeId(String Kind, BlockPos Pos, ResourceKey<Level> Dimension) {
        long Mixed = HashCommon.mix(Pos.asLong() ^ ((long) Dimension.location().hashCode() << 17));
        return String.format(Locale.ROOT, "%s-%04X", Kind, (int) (Mixed & 0xFFFFL));
    }

    public static String duration(long Millis) {
        long Minutes = Math.max(0L, Millis / 60000L);
        long Hours = Minutes / 60L;
        return Hours > 0L ? text("duration.hours", Hours, Minutes % 60L) : text("duration.minutes", Math.max(1L, Minutes));
    }

    @Nullable
    public static AnomalyDocument document(@Nullable ResourceLocation Id) {
        return Id == null ? null : AnomalyDocuments.server().get(Id);
    }

    public static String designation(@Nullable ResourceLocation Id) {
        AnomalyDocument Found = document(Id);
        if (Found != null) {
            return Found.designation().getString();
        }
        return Id == null ? "" : Id.getPath().toUpperCase(Locale.ROOT).replace('_', '-');
    }

    public static Optional<ResourceLocation> anomalyToken(String Token) {
        if ("black_book".equals(Token)) {
            return Optional.of(ResourceLocation.fromNamespaceAndPath("scp_unleashed", "z_0001"));
        }
        Matcher Found = AnomalyToken.matcher(Token);
        if (!Found.matches()) {
            return Optional.empty();
        }
        String Path = Found.group(3) != null ? "z_" + Found.group(3) : "scp_" + Found.group(1) + (Found.group(2).isEmpty() ? "" : "_j");
        return Optional.of(ResourceLocation.fromNamespaceAndPath("scp_unleashed", Path));
    }

    public static Optional<ResourceLocation> anomalyOf(@Nullable Entity Source) {
        if (Source == null) {
            return Optional.empty();
        }
        Matcher Found = AnomalyPackage.matcher(Source.getClass().getName());
        return Found.matches() ? anomalyToken(Found.group(1)) : Optional.empty();
    }

    public static Optional<ResourceLocation> anomalyOf(DamageSource Source) {
        Optional<ResourceLocation> ByType = Source.typeHolder().unwrapKey().map(ResourceKey::location)
                .filter(Id -> "scp_unleashed".equals(Id.getNamespace())).flatMap(Id -> anomalyToken(Id.getPath()));
        if (ByType.isPresent()) {
            return ByType;
        }
        Optional<ResourceLocation> ByAttacker = anomalyOf(Source.getEntity());
        return ByAttacker.isPresent() ? ByAttacker : anomalyOf(Source.getDirectEntity());
    }

    public static void playerJoined(ServerPlayer User, boolean First) {
        Sessions.put(User.getUUID(), System.currentTimeMillis());
        guard(DiscordCategory.Players, () -> {
            if (!Discord.settings().Joins()) {
                return;
            }
            MinecraftServer Server = User.server;
            DiscordEmbed Embed = new DiscordEmbed().title(text(First ? "player.first_join" : "player.joined", name(User))).thumbnail(face(User))
                    .field(text("field.player"), name(User), true)
                    .field(text("field.first_join"), yes(First), true)
                    .field(text("field.online"), Server.getPlayerCount() + " / " + Server.getMaxPlayers(), true);
            where(Embed, DiscordCategory.Players, User.blockPosition(), User.level().dimension());
            post(DiscordCategory.Players, Embed);
        });
    }

    public static void playerLeft(ServerPlayer User) {
        Long Since = Sessions.remove(User.getUUID());
        guard(DiscordCategory.Players, () -> {
            if (!Discord.settings().Leaves()) {
                return;
            }
            MinecraftServer Server = User.server;
            DiscordEmbed Embed = new DiscordEmbed().title(text("player.left", name(User))).thumbnail(face(User))
                    .field(text("field.player"), name(User), true)
                    .field(text("field.online"), Math.max(0, Server.getPlayerCount() - 1) + " / " + Server.getMaxPlayers(), true);
            if (Since != null) {
                Embed.field(text("field.session"), duration(System.currentTimeMillis() - Since), true);
            }
            where(Embed, DiscordCategory.Players, User.blockPosition(), User.level().dimension());
            post(DiscordCategory.Players, Embed);
        });
    }

    public static void playerDied(ServerPlayer User, DamageSource Source) {
        guard(DiscordCategory.Players, () -> {
            DiscordDeaths Mode = Discord.settings().Deaths();
            Optional<ResourceLocation> Anomaly = anomalyOf(Source);
            if (Mode == DiscordDeaths.Off || Mode == DiscordDeaths.Anomalies && Anomaly.isEmpty()) {
                return;
            }
            DiscordEmbed Embed = new DiscordEmbed().title(text("player.died", name(User))).author(name(User), face(User))
                    .description(Source.getLocalizedDeathMessage(User).getString());
            Anomaly.ifPresent(Id -> Embed.field(text("field.anomaly"), designation(Id), true));
            Entity Killer = Source.getEntity();
            if (Killer != null && Killer != User) {
                Embed.field(text("field.killer"), Killer.getDisplayName().getString(), true);
            }
            where(Embed, DiscordCategory.Players, User.blockPosition(), User.level().dimension());
            post(DiscordCategory.Players, Embed);
        });
    }

    public static void documentOpened(ServerPlayer Reader, ResourceLocation Id, @Nullable AnomalyDocument Document, boolean Sealed) {
        guard(DiscordCategory.Documents, () -> {
            DiscordSettings Settings = Discord.settings();
            if (Sealed ? !Settings.allows(DiscordToggle.SealedFiles) : Settings.Filters().Reads() == DiscordReads.Off) {
                return;
            }
            if (Sealed && Settings.Filters().Reads() == DiscordReads.FirstRead) {
                DiscordReadLog.get(Reader.server).note(Reader.getUUID(), Id);
            }
            DiscordEmbed Embed = new DiscordEmbed().author(name(Reader), face(Reader))
                    .title(text(Sealed ? "document.opened" : "document.read", name(Reader)));
            if (Document != null) {
                Embed.description(text("document.line", Document.designation().getString(), Document.title().getString()))
                        .field(text("field.document"), Document.designation().getString(), true)
                        .field(text("field.class"), Document.objectClass().getString(), true);
            } else {
                Embed.description(text("document.unknown")).field(text("field.document"), Id.toString(), true);
            }
            Embed.field(text("field.reader"), name(Reader), true);
            where(Embed, DiscordCategory.Documents, Reader.blockPosition(), Reader.level().dimension());
            post(DiscordCategory.Documents, Embed);
        });
    }

    public static void documentRead(ServerPlayer Reader, ItemStack Stack) {
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stamp == null || !Discord.wants(DiscordCategory.Documents) || !fresh(filters(), Reader, Stamp.Document())) {
            return;
        }
        documentOpened(Reader, Stamp.Document(), document(Stamp.Document()), false);
    }

    private static boolean fresh(DiscordFilters Rules, ServerPlayer Reader, ResourceLocation Id) {
        return switch (Rules.Reads()) {
            case Off -> false;
            case FirstRead -> DiscordReadLog.get(Reader.server).note(Reader.getUUID(), Id);
            case EveryRead -> spaced(Rules.rereadGapMillis(), Reader.getUUID() + "|" + Id);
        };
    }

    private static boolean spaced(long Gap, String Key) {
        if (Gap <= 0L) {
            return true;
        }
        long Now = System.currentTimeMillis();
        Long Last = Reread.get(Key);
        if (Last != null && Now - Last < Gap) {
            return false;
        }
        if (Reread.size() > RereadMemory) {
            Reread.entrySet().removeIf(Entry -> Now - Entry.getValue() >= Gap);
        }
        Reread.put(Key, Now);
        return true;
    }

    public static void anomalyArrived(AnomalySpawners.Discovery Found) {
        guard(DiscordToggle.Arrivals, () -> {
            AnomalyDocument Document = Found.Document();
            String Designation = Document.designation().getString();
            DiscordEmbed Embed = new DiscordEmbed().title(text(Found.Manifested() ? "anomaly.manifested" : "anomaly.surfaced", Designation))
                    .description(text("document.line", Designation, Document.title().getString()))
                    .field(text("field.anomaly"), Designation, true)
                    .field(text("field.class"), Document.objectClass().getString(), true)
                    .field(text("field.investigator"), name(Found.Investigator()), true);
            where(Embed, DiscordCategory.Anomalies, Found.Pos(), Found.World().dimension());
            post(DiscordCategory.Anomalies, Embed);
        });
    }

    public static void containment(ServerLevel World, BlockPos Pos, ResourceLocation DocumentId, boolean Breach) {
        guard(DiscordCategory.Anomalies, () -> {
            if (!filters().Containment().allows(Breach)) {
                return;
            }
            String Designation = designation(DocumentId);
            DiscordEmbed Embed = new DiscordEmbed().title(text(Breach ? "anomaly.breach" : "anomaly.contained", Designation))
                    .field(text("field.anomaly"), Designation, true);
            where(Embed, DiscordCategory.Anomalies, Pos, World.dimension());
            post(DiscordCategory.Anomalies, Embed);
        });
    }

    public static void rift(ServerLevel World, BlockPos Pos, boolean Formed) {
        guard(DiscordToggle.Rifts, () -> {
            DiscordEmbed Embed = new DiscordEmbed().title(text(Formed ? "anomaly.rift_formed" : "anomaly.rift_collapsed"));
            where(Embed, DiscordCategory.Anomalies, Pos, World.dimension());
            post(DiscordCategory.Anomalies, Embed);
        });
    }

    public static void daeviteGod(ServerLevel World, BlockPos Pos, boolean Summoned, @Nullable LivingEntity Slayer) {
        guard(DiscordToggle.DaeviteGod, () -> {
            DiscordEmbed Embed = new DiscordEmbed().title(text(Summoned ? "anomaly.god_summoned" : "anomaly.god_slain"));
            if (Slayer != null) {
                Embed.field(text("field.slayer"), Slayer.getDisplayName().getString(), true);
                if (Slayer instanceof Player User) {
                    Embed.thumbnail(face(User));
                }
            }
            where(Embed, DiscordCategory.Anomalies, Pos, World.dimension());
            post(DiscordCategory.Anomalies, Embed);
        });
    }

    public static void raid(RaidContext Context, String Event, Component Line) {
        DiscordScarletWatch.raidNotice(Context, Event);
        guard(DiscordCategory.Security, () -> {
            if (!filters().Raids().allows(Event)) {
                return;
            }
            Raid Current = Context.Current();
            String FactionName = Context.Faction().displayName().getString();
            DiscordEmbed Embed = new DiscordEmbed().title(pick("raid." + Event, "raid.event", FactionName))
                    .description(Line.getString())
                    .field(text("field.faction"), FactionName, true)
                    .field(text("field.raid"), "#" + Current.id(), true)
                    .field(text("field.stage"), pick("raid.stage." + Current.stage().serialName(), "raid.stage.unknown"), true);
            String Group = Current.group();
            if (Group.startsWith(RaidBases.TeamPrefix)) {
                Embed.field(text("field.team"), Group.substring(RaidBases.TeamPrefix.length()), true);
            }
            if (Current.spawned() > 0) {
                Embed.field(text("field.raiders"), text("raid.counts", Current.spawned(), Current.lost(), Current.remaining()), true);
            }
            if (Current.forced()) {
                Embed.field(text("field.forced"), yes(true), true);
            }
            where(Embed, DiscordCategory.Security, Current.target(), Current.dimension());
            post(DiscordCategory.Security, Embed);
        });
    }

    public static void mtfCalled(ServerPlayer Caller, MtfTier Tier, BlockPos Ground) {
        guard(DiscordToggle.MtfSquads, () -> {
            String Squad = Tier.displayName().getString();
            DiscordEmbed Embed = new DiscordEmbed().title(text("mtf.called", name(Caller), Squad)).author(name(Caller), face(Caller))
                    .field(text("field.caller"), name(Caller), true)
                    .field(text("field.squad"), Squad, true)
                    .field(text("field.size"), String.valueOf(Tier.size()), true);
            where(Embed, DiscordCategory.Security, Ground, Caller.level().dimension());
            post(DiscordCategory.Security, Embed);
        });
    }

    public static void warheadLever(ServerLevel World, BlockPos Panel, Player User, boolean Armed, int Linked) {
        guard(DiscordCategory.Security, () -> {
            if (!filters().Warhead().allows(true)) {
                return;
            }
            DiscordEmbed Embed = new DiscordEmbed().title(text(Armed ? "warhead.armed" : "warhead.disarmed")).author(name(User), face(User))
                    .field(text("field.operator"), name(User), true)
                    .field(text("field.warheads"), String.valueOf(Linked), true);
            where(Embed, DiscordCategory.Security, Panel, World.dimension());
            post(DiscordCategory.Security, Embed);
        });
    }

    public static void warheadEngaged(ServerLevel World, BlockPos Panel, Player User, int Targets, long Deadline) {
        guard(DiscordCategory.Security, () -> {
            if (!filters().Warhead().allows(false)) {
                return;
            }
            long Seconds = Math.max(0L, (Deadline - World.getGameTime()) / 20L);
            DiscordEmbed Embed = new DiscordEmbed().title(text("warhead.engaged")).author(name(User), face(User))
                    .field(text("field.operator"), name(User), true)
                    .field(text("field.warheads"), String.valueOf(Targets), true)
                    .field(text("field.countdown"), text("seconds", Seconds), true);
            where(Embed, DiscordCategory.Security, Panel, World.dimension());
            post(DiscordCategory.Security, Embed);
        });
    }

    public static void warheadCancelled(ServerLevel World, BlockPos Panel, Player User) {
        guard(DiscordCategory.Security, () -> {
            if (!filters().Warhead().allows(false)) {
                return;
            }
            DiscordEmbed Embed = new DiscordEmbed().title(text("warhead.cancelled")).author(name(User), face(User))
                    .field(text("field.operator"), name(User), true);
            where(Embed, DiscordCategory.Security, Panel, World.dimension());
            post(DiscordCategory.Security, Embed);
        });
    }

    public static void warheadEnded(ServerLevel World, WarheadSequence Sequence, boolean Detonated) {
        guard(DiscordCategory.Security, () -> {
            if (!filters().Warhead().allows(false)) {
                return;
            }
            DiscordEmbed Embed = new DiscordEmbed().title(text(Detonated ? "warhead.detonated" : "warhead.failed"))
                    .field(text("field.warheads"), String.valueOf(Sequence.getTargets().size()), true);
            where(Embed, DiscordCategory.Security, Sequence.getPanel(), World.dimension());
            post(DiscordCategory.Security, Embed);
        });
    }

    public static void meltdown(ServerLevel World, BlockPos Pos, boolean Tower) {
        guard(DiscordToggle.Meltdowns, () -> {
            DiscordEmbed Embed = new DiscordEmbed().title(text(Tower ? "reactor.tower" : "reactor.fission"));
            where(Embed, DiscordCategory.Security, Pos, World.dimension());
            post(DiscordCategory.Security, Embed);
        });
    }

    public static String channelName(ComputerGrid.Reach Linked, String Key) {
        if (ChatChannels.Network.equals(Key)) {
            return ChatService.NetworkName;
        }
        MessageDatabaseBlockEntity Database = Linked.database(Key);
        return Database == null ? Key : Database.getChannelName();
    }

    public static void computerChat(ServerPlayer User, ServerLevel World, BlockPos Computer, String Key, ChatMessage Draft) {
        guard(DiscordToggle.PlayerMessages, () -> {
            ComputerGrid.Reach Linked = ComputerGrid.reach(World, Computer);
            DiscordEmbed Embed = chat(World, Linked, Key, Draft).author(name(User), face(User))
                    .field(text("field.player"), name(User), true)
                    .field(text("field.computer"), nodeId("PC", Computer, World.dimension()), true)
                    .field(text("field.channel"), channelName(Linked, Key), true);
            server(Embed, World, Linked);
            Draft.Attachment().map(ComputerFile::Name).ifPresent(File -> Embed.field(text("field.attachment"), File, true));
            where(Embed, DiscordCategory.Computers, Computer, World.dimension());
            post(DiscordCategory.Computers, Embed);
        });
    }

    public static void computerPosted(ServerLevel World, ComputerGrid.Reach Linked, String Key, ChatMessage Draft) {
        if (Draft.From() == ChatMessage.Sender.Player) {
            return;
        }
        guard(Draft.From() == ChatMessage.Sender.System ? DiscordToggle.ScheduledMessages : DiscordToggle.AnomalousMessages, () -> {
            String Speaker = Draft.nameComponent().getString();
            DiscordEmbed Embed = chat(World, Linked, Key, Draft).author(Speaker, "")
                    .field(text("field.sender"), text(Draft.From() == ChatMessage.Sender.System ? "computer.scheduled" : "computer.anomalous", Speaker), true)
                    .field(text("field.channel"), channelName(Linked, Key), true);
            server(Embed, World, Linked);
            post(DiscordCategory.Computers, Embed);
        });
    }

    private static DiscordEmbed chat(ServerLevel World, ComputerGrid.Reach Linked, String Key, ChatMessage Draft) {
        String Said = Draft.textComponent().getString();
        DiscordEmbed Embed = new DiscordEmbed().title(text("computer.title", channelName(Linked, Key)));
        if (Said.isEmpty()) {
            Embed.description(text("computer.attachment_only"));
        } else {
            Embed.quote(Said);
        }
        return Embed;
    }

    private static void server(DiscordEmbed Embed, ServerLevel World, ComputerGrid.Reach Linked) {
        ChatServerBlockEntity Hub = Linked.chatServer();
        if (Hub != null) {
            Embed.field(text("field.chat_server"), nodeId("SRV", Hub.getBlockPos(), World.dimension()), true);
        }
    }

    private static String gifts(List<StrangerGift> Gifts) {
        List<String> Names = new ArrayList<>();
        for (StrangerGift Gift : Gifts) {
            ItemStack Stack = Gift.toStack();
            if (!Stack.isEmpty()) {
                Names.add(Stack.getCount() > 1 ? Stack.getCount() + " \u00D7 " + Stack.getHoverName().getString() : Stack.getHoverName().getString());
            }
        }
        return String.join(", ", Names);
    }

    public static void strangerAppeared(ServerPlayer Reader, StrangerVariant Variant, BlockPos Pos, @Nullable ResourceLocation Document) {
        guard(DiscordToggle.Appearances, () -> {
            DiscordEmbed Embed = new DiscordEmbed().title(text("stranger.appeared", Variant.displayName().getString(), name(Reader))).thumbnail(face(Reader))
                    .field(text("field.player"), name(Reader), true);
            if (Document != null) {
                Embed.field(text("field.case"), designation(Document), true);
            }
            where(Embed, DiscordCategory.Strangers, Pos, Reader.level().dimension());
            post(DiscordCategory.Strangers, Embed);
        });
    }

    public static void strangerDelivered(ServerPlayer Reader, Entity Figure, StrangerVariant Variant, List<StrangerGift> Gifts, @Nullable ResourceLocation Document) {
        guard(DiscordToggle.Deliveries, () -> {
            DiscordEmbed Embed = new DiscordEmbed().title(text("stranger.delivered", Variant.displayName().getString(), name(Reader))).thumbnail(face(Reader))
                    .field(text("field.player"), name(Reader), true);
            String Given = gifts(Gifts);
            if (!Given.isEmpty()) {
                Embed.field(text("field.delivered"), Given, false);
            }
            if (Document != null) {
                Embed.field(text("field.case"), designation(Document), true);
            }
            where(Embed, DiscordCategory.Strangers, Figure.blockPosition(), Figure.level().dimension());
            post(DiscordCategory.Strangers, Embed);
        });
    }

    public static void strangerLeft(MinecraftServer Server, boolean Present, UUID Reader, StrangerVariant Variant, String Dimension, BlockPos Spot,
                                    @Nullable ResourceLocation Document) {
        if (!Present) {
            return;
        }
        guard(DiscordToggle.Departures, () -> {
            ServerPlayer Online = Server.getPlayerList().getPlayer(Reader);
            String Who = Online != null ? name(Online) : Server.getProfileCache() == null ? Reader.toString()
                    : Server.getProfileCache().get(Reader).map(Profile -> Profile.getName()).orElse(Reader.toString());
            DiscordEmbed Embed = new DiscordEmbed().title(text("stranger.left", Variant.displayName().getString(), Who))
                    .thumbnail(face(Discord.settings(), Reader, Who))
                    .field(text("field.player"), Who, true);
            if (Document != null) {
                Embed.field(text("field.case"), designation(Document), true);
            }
            where(Embed, DiscordCategory.Strangers, Spot, Dimension);
            post(DiscordCategory.Strangers, Embed);
        });
    }

    public static DiscordEmbed sample(DiscordCategory Category, @Nullable ServerPlayer Source) {
        String SectionName = DiscordText.translate(Category.langKey());
        DiscordEmbed Embed = new DiscordEmbed().title(text("test.title", SectionName)).description(text("test.description", SectionName));
        if (Source != null) {
            Embed.author(name(Source), face(Source)).field(text("field.requested_by"), name(Source), true);
            if (Discord.settings().coordinatesFor(Category)) {
                Embed.position(Source.blockPosition(), Source.level().dimension());
            }
        }
        return Embed;
    }
}
