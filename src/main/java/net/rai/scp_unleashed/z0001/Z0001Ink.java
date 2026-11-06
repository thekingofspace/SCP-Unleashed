package net.rai.scp_unleashed.z0001;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.z0001.network.Z0001DreadPayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Z0001Ink {
    public static final long DayLength = 24000L;
    public static final int MinDelay = 100;
    public static final int MaxDelay = 200;
    public static final int RetryWindow = 200;
    public static final int WriteCooldown = 10;
    public static final float TollVolume = 1.6f;
    public static final float QuillVolume = 0.6f;
    public static final int FirstPrintable = 0x21;
    public static final int LastPrintable = 0x7E;
    public static final int LogLength = 32;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<UUID, Pending> Waiting = new LinkedHashMap<>();
    private static final Map<UUID, Integer> LastWrite = new HashMap<>();

    private Z0001Ink() {
    }

    public record Written(Z0001Outcome Outcome, String Name) {
    }

    public record Pending(UUID TargetId, String TargetName, UUID WriterId, String WriterName, @Nullable ServerPlayer Writer, int WrittenAt, int Due, int GiveUp) {
        public boolean isSelf() {
            return TargetId.equals(WriterId);
        }
    }

    public static long today(Level World) {
        return Math.floorDiv(World.getDayTime(), DayLength);
    }

    public static long today(MinecraftServer Server) {
        return today(Server.overworld());
    }

    @Nullable
    public static String clean(@Nullable String Raw) {
        if (Raw == null) {
            return null;
        }
        String Trimmed = Raw.trim();
        if (Trimmed.isEmpty() || Trimmed.length() > Z0001Page.MaxName) {
            return null;
        }
        for (int Index = 0; Index < Trimmed.length(); Index++) {
            char Character = Trimmed.charAt(Index);
            if (Character < FirstPrintable || Character > LastPrintable) {
                return null;
            }
        }
        return Trimmed;
    }

    public static String printable(@Nullable String Raw) {
        if (Raw == null) {
            return "";
        }
        StringBuilder Built = new StringBuilder();
        for (int Index = 0; Index < Raw.length() && Built.length() < LogLength; Index++) {
            char Character = Raw.charAt(Index);
            Built.append(Character >= ' ' && Character <= LastPrintable ? Character : '?');
        }
        return Built.toString();
    }

    public static boolean isSpent(ItemStack Stack, long Today) {
        Z0001Page Page = Stack.get(Z0001Registry.Page.get());
        return Page != null && Page.isSpent(Today);
    }

    public static Written write(ServerPlayer Writer, InteractionHand Hand, String Raw) {
        MinecraftServer Server = Writer.server;
        ItemStack Stack = Writer.getItemInHand(Hand);
        String Name = clean(Raw);
        String Shown = Name == null ? printable(Raw) : Name;
        if (!Z0001BookItem.is(Stack) || !Writer.isAlive() || Writer.isSpectator()) {
            return refuse(Writer, Shown, Z0001Outcome.Invalid, "the writer was not holding the book");
        }
        int Now = Server.getTickCount();
        Integer Last = LastWrite.get(Writer.getUUID());
        if (Last != null && Now >= Last && Now - Last < WriteCooldown) {
            return new Written(Z0001Outcome.Invalid, "");
        }
        LastWrite.put(Writer.getUUID(), Now);
        long Today = today(Server);
        Z0001Page Page = Stack.get(Z0001Registry.Page.get());
        if (Page != null && Page.isSpent(Today)) {
            log(Writer, Shown, Z0001Outcome.Spent, "the page already holds " + Page.Name() + " today");
            return new Written(Z0001Outcome.Spent, Page.Name());
        }
        if (Name == null) {
            return refuse(Writer, Shown, Z0001Outcome.Invalid, "not a name");
        }
        ServerPlayer Target = Server.getPlayerList().getPlayerByName(Name);
        if (Target == null) {
            return refuse(Writer, Name, Z0001Outcome.NoTake, "no player of that name is online");
        }
        if (Target.isCreative() || Target.isSpectator()) {
            return refuse(Writer, Name, Z0001Outcome.Faded, "the named player is in " + Target.gameMode.getGameModeForPlayer().getName() + " mode");
        }
        if (!Target.isAlive() || Target.isDeadOrDying()) {
            return refuse(Writer, Name, Z0001Outcome.NoTake, "the named player is already dead");
        }
        boolean Self = Target.getUUID().equals(Writer.getUUID());
        if (!Self && !Target.canHarmPlayer(Writer)) {
            return refuse(Writer, Name, Z0001Outcome.NoTake, Server.isPvpAllowed() ? "the players may not harm each other" : "pvp is off");
        }
        String Canonical = Target.getGameProfile().getName();
        int Cooldown = Z0001Registry.cooldownDays(Server.getGameRules());
        Stack.set(Z0001Registry.Page.get(), new Z0001Page(Today, Today + Cooldown, Canonical));
        play(Writer.serverLevel(), Writer, Writer.position(), Z0001Registry.QuillLayers, QuillVolume, 1.0f);
        Pending Scheduled = schedule(Server, Target, Writer);
        log(Writer, Canonical, Z0001Outcome.Taken, Scheduled == null ? "the name was already written elsewhere"
                : "death in " + (Scheduled.Due() - Now) + " ticks, page day " + Today + ", next day " + (Today + Cooldown));
        return new Written(Z0001Outcome.Taken, Canonical);
    }

    private static Written refuse(ServerPlayer Writer, String Name, Z0001Outcome Outcome, String Reason) {
        log(Writer, Name, Outcome, Reason);
        return new Written(Outcome, "");
    }

    private static void log(ServerPlayer Writer, String Name, Z0001Outcome Outcome, String Reason) {
        Log.info("Z-0001: {} wrote \"{}\" in the black book at {} in {}: {} ({})", Writer.getGameProfile().getName(), Name, Writer.blockPosition().toShortString(),
                Writer.level().dimension().location(), Outcome.name(), Reason);
    }

    @Nullable
    private static Pending schedule(MinecraftServer Server, ServerPlayer Target, ServerPlayer Writer) {
        UUID Id = Target.getUUID();
        if (Waiting.containsKey(Id)) {
            return null;
        }
        int Now = Server.getTickCount();
        int Delay = MinDelay + Target.getRandom().nextInt(MaxDelay - MinDelay + 1);
        boolean Self = Id.equals(Writer.getUUID());
        Pending Entry = new Pending(Id, Target.getGameProfile().getName(), Writer.getUUID(), Writer.getGameProfile().getName(), Self ? null : Writer, Now, Now + Delay,
                Now + Delay + RetryWindow);
        Waiting.put(Id, Entry);
        warn(Target, Delay);
        return Entry;
    }

    private static void warn(ServerPlayer Target, int Delay) {
        String Name = Target.getGameProfile().getName();
        if (Target.connection != null && Target.connection.hasChannel(Z0001DreadPayload.PayloadType)) {
            PacketDistributor.sendToPlayer(Target, new Z0001DreadPayload(Name, Delay));
            return;
        }
        Target.displayClientMessage(Component.literal(Name).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
        Target.playNotifySound(Z0001Registry.Heartbeat.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    @Nullable
    public static Pending pending(UUID Target) {
        return Waiting.get(Target);
    }

    public static List<Pending> allPending() {
        return new ArrayList<>(Waiting.values());
    }

    public static void tick(MinecraftServer Server) {
        if (Waiting.isEmpty()) {
            return;
        }
        int Now = Server.getTickCount();
        for (Pending Entry : List.copyOf(Waiting.values())) {
            if (Now < Entry.Due()) {
                continue;
            }
            ServerPlayer Target = Server.getPlayerList().getPlayer(Entry.TargetId());
            if (Target == null) {
                Waiting.remove(Entry.TargetId());
                Log.info("Z-0001: {} was not online when the ink came due", Entry.TargetName());
                continue;
            }
            if (Target.isCreative() || Target.isSpectator()) {
                Waiting.remove(Entry.TargetId());
                Log.info("Z-0001: the ink faded for {} ({} mode)", Entry.TargetName(), Target.gameMode.getGameModeForPlayer().getName());
                continue;
            }
            if (Target.isDeadOrDying()) {
                Waiting.remove(Entry.TargetId());
                continue;
            }
            if (land(Server, Target, Entry)) {
                Waiting.remove(Entry.TargetId());
            } else if (Now >= Entry.GiveUp()) {
                Waiting.remove(Entry.TargetId());
                Log.info("Z-0001: the ink did not land on {} before it dried", Entry.TargetName());
            }
        }
    }

    public static boolean land(MinecraftServer Server, ServerPlayer Target, Pending Entry) {
        if (ControlSessions.isControlling(Target)) {
            ControlManager.release(Target);
            if (ControlSessions.isControlling(Target)) {
                return false;
            }
        }
        ServerLevel World = Target.serverLevel();
        Vec3 At = Target.position();
        Target.hurt(source(Server, Entry), Float.MAX_VALUE);
        if (!Target.isDeadOrDying()) {
            return false;
        }
        play(World, null, At, Z0001Registry.TollLayers, TollVolume, 1.0f);
        World.sendParticles(ParticleTypes.SQUID_INK, At.x, At.y + Target.getBbHeight() * 0.5, At.z, 8, 0.2, 0.35, 0.2, 0.01);
        Log.info("Z-0001: {} died to the black book, written by {} {} ticks earlier", Entry.TargetName(), Entry.WriterName(), Server.getTickCount() - Entry.WrittenAt());
        return true;
    }

    public static Z0001InkSource source(MinecraftServer Server, Pending Entry) {
        Holder<DamageType> Type = Server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Z0001Registry.BlackBookDamage);
        if (Entry.isSelf()) {
            return new Z0001InkSource(Type, null, "");
        }
        ServerPlayer Writer = Server.getPlayerList().getPlayer(Entry.WriterId());
        return new Z0001InkSource(Type, Writer != null ? Writer : Entry.Writer(), Entry.WriterName());
    }

    public static void died(ServerPlayer Victim) {
        Waiting.remove(Victim.getUUID());
    }

    public static void leaving(ServerPlayer User) {
        LastWrite.remove(User.getUUID());
        Pending Entry = Waiting.remove(User.getUUID());
        if (Entry == null || User.isCreative() || User.isSpectator() || User.isDeadOrDying()) {
            return;
        }
        Log.info("Z-0001: {} left while their name was on the page", Entry.TargetName());
        land(User.server, User, Entry);
    }

    public static void clear() {
        Waiting.clear();
        LastWrite.clear();
    }

    public static boolean forget(UUID Target) {
        return Waiting.remove(Target) != null;
    }

    public static void play(ServerLevel World, @Nullable ServerPlayer Except, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(Except, At.x, At.y, At.z, Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }
}
