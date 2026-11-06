package net.rai.scp_unleashed.scp179;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.scp002.Scp002Strikes;
import net.rai.scp_unleashed.scp163.Scp163Crashes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Scp179Omens extends SavedData {
    public static final String DataName = "scp_unleashed_scp179_omens";
    public static final int LeadTicks = 1200;
    public static final int PollTicks = 20;
    public static final float OmenVolume = 0.7f;
    private static final SavedData.Factory<Scp179Omens> DataFactory = new SavedData.Factory<>(Scp179Omens::new, Scp179Omens::load);

    private Scp179Omen Current = Scp179Omen.None;
    private long Since;
    private final Set<UUID> Announced = new LinkedHashSet<>();

    public static Scp179Omens get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public Scp179Omen current() {
        return Current;
    }

    public long since() {
        return Since;
    }

    public static boolean warns(boolean Inbound, boolean Waiting, long FireAt, long Now) {
        return Inbound || Waiting && FireAt - Now <= LeadTicks;
    }

    public static List<Sighting> sightings(MinecraftServer Server) {
        long Now = Server.overworld().getGameTime();
        List<Sighting> Found = new ArrayList<>();
        for (Scp002Strikes.Strike Entry : Scp002Strikes.get(Server).list()) {
            Scp002Strikes.Stage Phase = Entry.getPhase();
            if (warns(Phase == Scp002Strikes.Stage.Inbound, Phase == Scp002Strikes.Stage.Waiting, Entry.getFireAt(), Now)) {
                Found.add(new Sighting(Entry.getId(), Scp179Omen.Beware));
            }
        }
        for (Scp163Crashes.Crash Entry : Scp163Crashes.get(Server).list()) {
            Scp163Crashes.Stage Phase = Entry.getPhase();
            if (warns(Phase == Scp163Crashes.Stage.Inbound, Phase == Scp163Crashes.Stage.Waiting, Entry.getFireAt(), Now)) {
                Found.add(new Sighting(Entry.getId(), Scp179Omen.Friend));
            }
        }
        return Found;
    }

    public static Scp179Omen strongest(List<Sighting> Found) {
        Scp179Omen Best = Scp179Omen.None;
        for (Sighting Entry : Found) {
            if (Entry.Kind() == Scp179Omen.Beware) {
                return Scp179Omen.Beware;
            }
            Best = Entry.Kind();
        }
        return Best;
    }

    public Result poll(MinecraftServer Server) {
        long Now = Server.overworld().getGameTime();
        List<Sighting> Found = sightings(Server);
        Scp179Omen Next = strongest(Found);
        if (Next != Current) {
            Current = Next;
            Since = Now;
            setDirty();
        }
        Set<UUID> Live = new HashSet<>();
        boolean FreshBeware = false;
        boolean FreshFriend = false;
        for (Sighting Entry : Found) {
            Live.add(Entry.Id());
            if (Announced.add(Entry.Id())) {
                setDirty();
                if (Entry.Kind() == Scp179Omen.Beware) {
                    FreshBeware = true;
                } else {
                    FreshFriend = true;
                }
            }
        }
        if (Announced.retainAll(Live)) {
            setDirty();
        }
        List<ServerPlayer> Told = List.of();
        if (FreshBeware || FreshFriend) {
            Told = audience(Server);
            if (FreshBeware) {
                announce(Told, Scp179Omen.Beware);
            }
            if (FreshFriend) {
                announce(Told, Scp179Omen.Friend);
            }
        }
        return new Result(Current, Told);
    }

    public static List<ServerPlayer> audience(MinecraftServer Server) {
        if (!SatelliteLink.anyDeployed(Server)) {
            return List.of();
        }
        return SkyWatch.get(Server).online(Server, Scp179Registry.TargetId);
    }

    public static Notice notice(Scp179Omen Kind) {
        Component Word = Kind.word().copy().withStyle(Look -> Look.withColor(Kind.getColour() & 0xFFFFFF).withBold(true));
        return Notice.anomaly(DocumentClass.Thaumiel, Component.translatable("notice.scp_unleashed.scp179.pointing", Word))
                .withDetail(Component.translatable("notice.scp_unleashed.scp179.pointing.detail"));
    }

    private static void announce(List<ServerPlayer> Players, Scp179Omen Kind) {
        Notice Shown = notice(Kind);
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Kind == Scp179Omen.Beware ? Scp179Registry.BewareLayers : Scp179Registry.FriendLayers;
        for (ServerPlayer Player : Players) {
            Notices.send(Player, Shown);
            if (Player.connection != null && !Player.hasDisconnected()) {
                for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
                    Player.playNotifySound(Layer.get(), SoundSource.AMBIENT, OmenVolume, 1.0f);
                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        Root.putString("Omen", Current.getSerializedName());
        Root.putLong("Since", Since);
        ListTag Ids = new ListTag();
        for (UUID Id : Announced) {
            Ids.add(NbtUtils.createUUID(Id));
        }
        Root.put("Announced", Ids);
        return Root;
    }

    private static Scp179Omens load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp179Omens Data = new Scp179Omens();
        Data.Current = Scp179Omen.byName(Root.getString("Omen"));
        Data.Since = Root.getLong("Since");
        for (Tag Id : Root.getList("Announced", Tag.TAG_INT_ARRAY)) {
            Data.Announced.add(NbtUtils.loadUUID(Id));
        }
        return Data;
    }

    public record Sighting(UUID Id, Scp179Omen Kind) {
    }

    public record Result(Scp179Omen Omen, List<ServerPlayer> Told) {
    }
}
