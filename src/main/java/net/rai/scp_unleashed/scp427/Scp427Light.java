package net.rai.scp_unleashed.scp427;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Scp427Light {
    public static final int Brightness = 12;
    public static final int StaleTicks = 2;
    public static final String DataName = "scp_unleashed_scp427_lights";
    private static final Map<UUID, Lit> Active = new HashMap<>();

    private Scp427Light() {
    }

    public record Lit(ResourceKey<Level> Dimension, BlockPos Pos, long Seen) {
    }

    public static boolean isOurs(BlockState State) {
        return State.is(Blocks.LIGHT) && State.getValue(LightBlock.LEVEL) == Brightness;
    }

    private static boolean canHold(ServerLevel World, BlockPos Pos) {
        if (!World.isInWorldBounds(Pos) || !World.isLoaded(Pos)) {
            return false;
        }
        BlockState State = World.getBlockState(Pos);
        return State.isAir() || isOurs(State);
    }

    @Nullable
    private static BlockPos spot(ServerLevel World, Entity Holder) {
        BlockPos Eyes = BlockPos.containing(Holder.getEyePosition());
        if (canHold(World, Eyes)) {
            return Eyes;
        }
        BlockPos Above = Holder.blockPosition().above();
        if (!Above.equals(Eyes) && canHold(World, Above)) {
            return Above;
        }
        BlockPos Feet = Holder.blockPosition();
        return canHold(World, Feet) ? Feet : null;
    }

    @Nullable
    public static Lit litFor(Entity Holder) {
        return Active.get(Holder.getUUID());
    }

    public static int activeCount() {
        return Active.size();
    }

    public static void refresh(Entity Holder) {
        if (!(Holder.level() instanceof ServerLevel World)) {
            return;
        }
        UUID Id = Holder.getUUID();
        BlockPos Wanted = spot(World, Holder);
        Lit Current = Active.get(Id);
        if (Current != null && (Current.Dimension() != World.dimension() || !Current.Pos().equals(Wanted))) {
            Active.remove(Id);
            extinguish(World.getServer(), Current);
        }
        if (Wanted == null) {
            return;
        }
        if (!isOurs(World.getBlockState(Wanted))) {
            World.setBlock(Wanted, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Brightness), Block.UPDATE_ALL);
        }
        Active.put(Id, new Lit(World.dimension(), Wanted.immutable(), World.getServer().getTickCount()));
        Ledger.of(World.getServer()).track(World.dimension(), Wanted);
    }

    public static void release(Entity Holder) {
        if (Holder.level().isClientSide) {
            return;
        }
        Lit Current = Active.remove(Holder.getUUID());
        if (Current != null && Holder.getServer() != null) {
            extinguish(Holder.getServer(), Current);
        }
    }

    public static void sweep(MinecraftServer Server) {
        if (Active.isEmpty()) {
            return;
        }
        long Now = Server.getTickCount();
        List<UUID> Stale = new ArrayList<>();
        for (Map.Entry<UUID, Lit> Entry : Active.entrySet()) {
            if (Now - Entry.getValue().Seen() > StaleTicks) {
                Stale.add(Entry.getKey());
            }
        }
        for (UUID Id : Stale) {
            Lit Current = Active.remove(Id);
            if (Current != null) {
                extinguish(Server, Current);
            }
        }
    }

    public static void releaseAll(MinecraftServer Server) {
        List<Lit> All = new ArrayList<>(Active.values());
        Active.clear();
        for (Lit Current : All) {
            extinguish(Server, Current);
        }
    }

    private static boolean shared(Lit Gone) {
        for (Lit Other : Active.values()) {
            if (Other.Dimension() == Gone.Dimension() && Other.Pos().equals(Gone.Pos())) {
                return true;
            }
        }
        return false;
    }

    private static void extinguish(MinecraftServer Server, Lit Gone) {
        if (shared(Gone)) {
            return;
        }
        ServerLevel World = Server.getLevel(Gone.Dimension());
        if (World != null) {
            clear(World, Gone.Pos());
        }
        Ledger.of(Server).forget(Gone.Dimension(), Gone.Pos());
    }

    private static void clear(ServerLevel World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!isOurs(State)) {
            return;
        }
        BlockState Restored = State.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        World.setBlock(Pos, Restored, Block.UPDATE_ALL);
    }

    public static int recover(MinecraftServer Server) {
        Ledger Book = Ledger.of(Server);
        int Cleared = 0;
        for (Ledger.Entry Entry : List.copyOf(Book.Entries)) {
            ServerLevel World = Server.getLevel(ResourceKey.create(Registries.DIMENSION, Entry.Dimension()));
            if (World != null && isOurs(World.getBlockState(Entry.Pos()))) {
                clear(World, Entry.Pos());
                Cleared++;
            }
        }
        Book.Entries.clear();
        Book.setDirty();
        Active.clear();
        return Cleared;
    }

    public static final class Ledger extends SavedData {
        public record Entry(ResourceLocation Dimension, BlockPos Pos) {
        }

        private final Set<Entry> Entries = new LinkedHashSet<>();

        public static Ledger of(MinecraftServer Server) {
            return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Ledger::new, Ledger::load), DataName);
        }

        private static Ledger load(CompoundTag Root, HolderLookup.Provider Lookup) {
            Ledger Made = new Ledger();
            for (Tag Raw : Root.getList("Lights", Tag.TAG_COMPOUND)) {
                CompoundTag Saved = (CompoundTag) Raw;
                ResourceLocation Dimension = ResourceLocation.tryParse(Saved.getString("Dimension"));
                if (Dimension != null) {
                    Made.Entries.add(new Entry(Dimension, BlockPos.of(Saved.getLong("Pos"))));
                }
            }
            return Made;
        }

        @Override
        public CompoundTag save(CompoundTag Root, HolderLookup.Provider Lookup) {
            ListTag Lights = new ListTag();
            for (Entry Saved : Entries) {
                CompoundTag Written = new CompoundTag();
                Written.putString("Dimension", Saved.Dimension().toString());
                Written.putLong("Pos", Saved.Pos().asLong());
                Lights.add(Written);
            }
            Root.put("Lights", Lights);
            return Root;
        }

        public void track(ResourceKey<Level> Dimension, BlockPos Pos) {
            if (Entries.add(new Entry(Dimension.location(), Pos.immutable()))) {
                setDirty();
            }
        }

        public void forget(ResourceKey<Level> Dimension, BlockPos Pos) {
            if (Entries.remove(new Entry(Dimension.location(), Pos.immutable()))) {
                setDirty();
            }
        }

        public int size() {
            return Entries.size();
        }
    }
}
