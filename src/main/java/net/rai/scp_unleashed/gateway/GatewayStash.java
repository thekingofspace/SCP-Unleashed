package net.rai.scp_unleashed.gateway;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GatewayStash extends SavedData {
    public static final String DataName = "scp_unleashed_gateway_stash";
    public static final String MarkerKey = "scp_unleashed_gateway_session";
    public static final String CuriosId = "curios";
    private static final SavedData.Factory<GatewayStash> DataFactory = new SavedData.Factory<>(GatewayStash::new, GatewayStash::load);

    private final Map<UUID, Entry> Entries = new HashMap<>();

    public record Spot(ResourceKey<Level> Dimension, Vec3 Pos, float Yaw) {
        public CompoundTag save() {
            CompoundTag Row = new CompoundTag();
            Row.putString("Dimension", Dimension.location().toString());
            Row.putDouble("X", Pos.x);
            Row.putDouble("Y", Pos.y);
            Row.putDouble("Z", Pos.z);
            Row.putFloat("Yaw", Yaw);
            return Row;
        }

        @Nullable
        public static Spot load(CompoundTag Row) {
            ResourceLocation Where = ResourceLocation.tryParse(Row.getString("Dimension"));
            if (Where == null) {
                return null;
            }
            return new Spot(ResourceKey.create(Registries.DIMENSION, Where), new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")), Row.getFloat("Yaw"));
        }
    }

    public static final class Entry {
        public final UUID Player;
        public final ResourceLocation Destination;
        public final UUID Session;
        public final ListTag Items;
        public final ListTag Curios;
        public final int Levels;
        public final float Progress;
        public final int Total;
        public final Spot Return;
        public final long Since;

        public Entry(UUID Player, ResourceLocation Destination, UUID Session, ListTag Items, ListTag Curios, int Levels, float Progress, int Total, Spot Return, long Since) {
            this.Player = Player;
            this.Destination = Destination;
            this.Session = Session;
            this.Items = Items;
            this.Curios = Curios;
            this.Levels = Levels;
            this.Progress = Progress;
            this.Total = Total;
            this.Return = Return;
            this.Since = Since;
        }

        public int itemCount() {
            return Items.size() + Curios.size();
        }
    }

    public static GatewayStash get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    @Nullable
    public Entry entry(UUID Id) {
        return Entries.get(Id);
    }

    public boolean holds(UUID Id) {
        return Entries.containsKey(Id);
    }

    public int size() {
        return Entries.size();
    }

    public List<Entry> entries() {
        return new ArrayList<>(Entries.values());
    }

    @Nullable
    public static UUID marker(Player User) {
        CompoundTag Persisted = User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return Persisted.hasUUID(MarkerKey) ? Persisted.getUUID(MarkerKey) : null;
    }

    public static void setMarker(Player User, @Nullable UUID Session) {
        CompoundTag Root = User.getPersistentData();
        CompoundTag Persisted = Root.getCompound(Player.PERSISTED_NBT_TAG);
        if (Session == null) {
            Persisted.remove(MarkerKey);
        } else {
            Persisted.putUUID(MarkerKey, Session);
        }
        Root.put(Player.PERSISTED_NBT_TAG, Persisted);
    }

    public static boolean matches(Player User, @Nullable Entry Found) {
        UUID Mark = marker(User);
        return Found != null && Mark != null && Mark.equals(Found.Session);
    }

    public static boolean curiosLoaded() {
        return ModList.get() != null && ModList.get().isLoaded(CuriosId);
    }

    public static void settleHands(ServerPlayer User) {
        User.closeContainer();
        User.inventoryMenu.removed(User);
        User.inventoryMenu.broadcastChanges();
    }

    public boolean stash(ServerPlayer User, ResourceLocation Destination, Spot Return, boolean Experience) {
        if (Entries.containsKey(User.getUUID())) {
            return true;
        }
        settleHands(User);
        HolderLookup.Provider Lookup = User.registryAccess();
        Inventory Pockets = User.getInventory();
        ListTag Items = new ListTag();
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (Stack.isEmpty()) {
                continue;
            }
            CompoundTag Row = new CompoundTag();
            Row.putInt("Slot", Slot);
            Row.put("Item", Stack.save(Lookup));
            Items.add(Row);
        }
        ListTag Worn = curiosLoaded() ? GatewayCurios.capture(User, Lookup) : new ListTag();
        int Levels = Experience ? User.experienceLevel : 0;
        float Progress = Experience ? User.experienceProgress : 0.0f;
        int Total = Experience ? User.totalExperience : 0;
        Entry Made = new Entry(User.getUUID(), Destination, UUID.randomUUID(), Items, Worn, Levels, Progress, Total, Return, User.server.overworld().getGameTime());
        Entries.put(User.getUUID(), Made);
        setDirty();
        flush(User.server);
        Pockets.clearContent();
        if (curiosLoaded()) {
            GatewayCurios.wipe(User);
        }
        if (Experience) {
            User.experienceLevel = 0;
            User.experienceProgress = 0.0f;
            User.totalExperience = 0;
        }
        setMarker(User, Made.Session);
        User.inventoryMenu.broadcastChanges();
        return true;
    }

    public static void wipe(ServerPlayer User, boolean Experience) {
        settleHands(User);
        User.getInventory().clearContent();
        if (curiosLoaded()) {
            GatewayCurios.wipe(User);
        }
        if (Experience) {
            User.experienceLevel = 0;
            User.experienceProgress = 0.0f;
            User.totalExperience = 0;
        }
        User.inventoryMenu.broadcastChanges();
    }

    public int restore(ServerPlayer User) {
        Entry Found = Entries.get(User.getUUID());
        if (Found == null) {
            return 0;
        }
        HolderLookup.Provider Lookup = User.registryAccess();
        List<ItemStack> Leftover = new ArrayList<>();
        Inventory Pockets = User.getInventory();
        List<ItemStack> Later = new ArrayList<>();
        for (Tag Raw : Found.Items) {
            CompoundTag Row = (CompoundTag) Raw;
            ItemStack Stack = ItemStack.parse(Lookup, Row.getCompound("Item")).orElse(ItemStack.EMPTY);
            if (Stack.isEmpty()) {
                continue;
            }
            int Slot = Row.getInt("Slot");
            if (Slot >= 0 && Slot < Pockets.getContainerSize() && Pockets.getItem(Slot).isEmpty()) {
                Pockets.setItem(Slot, Stack);
            } else {
                Later.add(Stack);
            }
        }
        if (curiosLoaded()) {
            Later.addAll(GatewayCurios.restore(User, Found.Curios, Lookup));
        }
        for (ItemStack Stack : Later) {
            Pockets.add(Stack);
            if (!Stack.isEmpty()) {
                Leftover.add(Stack);
            }
        }
        if (Found.Total > 0 || Found.Levels > 0) {
            if (User.totalExperience <= 0 && User.experienceLevel <= 0) {
                User.experienceLevel = Found.Levels;
                User.experienceProgress = Found.Progress;
                User.totalExperience = Found.Total;
            } else {
                User.giveExperiencePoints(Math.max(0, Found.Total));
            }
        }
        for (ItemStack Stack : Leftover) {
            ItemEntity Dropped = User.drop(Stack, false, false);
            if (Dropped != null) {
                Dropped.setNoPickUpDelay();
                Dropped.setTarget(User.getUUID());
                Dropped.setUnlimitedLifetime();
            }
        }
        Entries.remove(User.getUUID());
        setDirty();
        setMarker(User, null);
        User.inventoryMenu.broadcastChanges();
        return Leftover.size();
    }

    public void discard(UUID Id) {
        if (Entries.remove(Id) != null) {
            setDirty();
        }
    }

    public void flush(MinecraftServer Server) {
        Path Folder = Server.getWorldPath(LevelResource.ROOT).resolve("data");
        File Target = Folder.resolve(DataName + ".dat").toFile();
        if (!Folder.toFile().isDirectory() && !Folder.toFile().mkdirs()) {
            return;
        }
        setDirty();
        save(Target, Server.registryAccess());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Rows = new ListTag();
        for (Entry Found : Entries.values()) {
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Player", Found.Player);
            Row.putString("Destination", Found.Destination.toString());
            Row.putUUID("Session", Found.Session);
            Row.put("Items", Found.Items.copy());
            Row.put("Curios", Found.Curios.copy());
            Row.putInt("Levels", Found.Levels);
            Row.putFloat("Progress", Found.Progress);
            Row.putInt("Total", Found.Total);
            Row.put("Return", Found.Return.save());
            Row.putLong("Since", Found.Since);
            Rows.add(Row);
        }
        Saved.put("Entries", Rows);
        return Saved;
    }

    public static GatewayStash load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        GatewayStash Data = new GatewayStash();
        for (Tag Raw : Saved.getList("Entries", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Raw;
            ResourceLocation Destination = ResourceLocation.tryParse(Row.getString("Destination"));
            Spot Return = Spot.load(Row.getCompound("Return"));
            if (!Row.hasUUID("Player") || !Row.hasUUID("Session") || Destination == null || Return == null) {
                continue;
            }
            Entry Found = new Entry(Row.getUUID("Player"), Destination, Row.getUUID("Session"), Row.getList("Items", Tag.TAG_COMPOUND), Row.getList("Curios", Tag.TAG_COMPOUND),
                    Row.getInt("Levels"), Row.getFloat("Progress"), Row.getInt("Total"), Return, Row.getLong("Since"));
            Data.Entries.put(Found.Player, Found);
        }
        return Data;
    }
}
