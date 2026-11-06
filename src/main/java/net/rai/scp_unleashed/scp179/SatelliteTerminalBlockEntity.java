package net.rai.scp_unleashed.scp179;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.computer.PowerFeed;
import net.rai.scp_unleashed.wire.WireDevice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SatelliteTerminalBlockEntity extends BlockEntity implements WireDevice {
    public static final int Draw = 32;
    public static final int BootDelay = 10;
    public static final int MaxLocations = 12;
    public static final int RefreshTicks = 20;

    private final PowerFeed Feed = new PowerFeed();
    private final List<ResourceLocation> Locations = new ArrayList<>();
    private final List<String> Satellites = new ArrayList<>();
    private int SatelliteCount;
    private Scp179Omen Omen = Scp179Omen.None;
    private long OmenSince;
    private boolean Refreshed;

    public SatelliteTerminalBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp179Registry.TerminalEntity.get(), Pos, State);
    }

    public enum AddResult {
        Added,
        Known,
        Full
    }

    public boolean isRunning() {
        return Feed.isRunning();
    }

    public List<ResourceLocation> getLocations() {
        return List.copyOf(Locations);
    }

    public boolean hasLocation(ResourceLocation Target) {
        return Locations.contains(Target);
    }

    public List<String> getSatellites() {
        return List.copyOf(Satellites);
    }

    public int getSatelliteCount() {
        return SatelliteCount;
    }

    public boolean hasUplink() {
        return SatelliteCount > 0;
    }

    public Scp179Omen getOmen() {
        return Omen;
    }

    public long getOmenSince() {
        return OmenSince;
    }

    public AddResult addLocation(ResourceLocation Target) {
        if (Locations.contains(Target)) {
            return AddResult.Known;
        }
        if (Locations.size() >= MaxLocations) {
            return AddResult.Full;
        }
        Locations.add(Target);
        if (level instanceof ServerLevel World) {
            refresh(World.getServer(), World.getGameTime());
        }
        sync();
        return AddResult.Added;
    }

    public TerminalDisplay display() {
        if (!Feed.isRunning()) {
            return TerminalDisplay.Off;
        }
        if (SatelliteCount <= 0) {
            return TerminalDisplay.NoLink;
        }
        return Omen.isActive() ? TerminalDisplay.Alert : TerminalDisplay.On;
    }

    public void serverTick(ServerLevel World, BlockState State) {
        long Now = World.getGameTime();
        boolean Changed = Feed.tick(Now, Draw, BootDelay);
        if (!Refreshed || Now % RefreshTicks == 0L) {
            Refreshed = true;
            Changed |= refresh(World.getServer(), Now);
        }
        TerminalDisplay Wanted = display();
        if (State.hasProperty(SatelliteTerminalBlock.Display) && State.getValue(SatelliteTerminalBlock.Display) != Wanted) {
            World.setBlock(worldPosition, State.setValue(SatelliteTerminalBlock.Display, Wanted), Block.UPDATE_CLIENTS);
        }
        if (Changed) {
            sync();
        }
    }

    private boolean refresh(MinecraftServer Server, long Now) {
        boolean Changed = false;
        List<String> Names = SatelliteLink.names(Server);
        int Count = SatelliteLink.count(Server);
        if (!Names.equals(Satellites) || Count != SatelliteCount) {
            Satellites.clear();
            Satellites.addAll(Names);
            SatelliteCount = Count;
            Changed = true;
        }
        Scp179Omens Omens = Scp179Omens.get(Server);
        Scp179Omen Seen = Count > 0 && Locations.contains(Scp179Registry.TargetId) ? Omens.current() : Scp179Omen.None;
        if (Seen != Omen) {
            OmenSince = Seen.isActive() ? Omens.since() : Now;
            Omen = Seen;
            Changed = true;
        }
        return Changed;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public int getPowerDemand() {
        return Draw;
    }

    @Override
    public void insertPower(int Amount) {
        Feed.insert(Amount);
    }

    private void writeLocations(CompoundTag Root) {
        ListTag Saved = new ListTag();
        for (ResourceLocation Target : Locations) {
            Saved.add(StringTag.valueOf(Target.toString()));
        }
        Root.put("Locations", Saved);
    }

    private void readLocations(CompoundTag Root) {
        Locations.clear();
        ListTag Saved = Root.getList("Locations", Tag.TAG_STRING);
        for (int Index = 0; Index < Saved.size() && Locations.size() < MaxLocations; Index++) {
            ResourceLocation Target = ResourceLocation.tryParse(Saved.getString(Index));
            if (Target != null && !Locations.contains(Target)) {
                Locations.add(Target);
            }
        }
    }

    private void writeView(CompoundTag Root) {
        ListTag Names = new ListTag();
        for (String Name : Satellites) {
            Names.add(StringTag.valueOf(Name));
        }
        Root.put("Satellites", Names);
        Root.putInt("SatelliteCount", SatelliteCount);
        Root.putString("Omen", Omen.getSerializedName());
        Root.putLong("OmenSince", OmenSince);
    }

    private void readView(CompoundTag Root) {
        Satellites.clear();
        ListTag Names = Root.getList("Satellites", Tag.TAG_STRING);
        for (int Index = 0; Index < Names.size(); Index++) {
            Satellites.add(Names.getString(Index));
        }
        SatelliteCount = Root.getInt("SatelliteCount");
        Omen = Scp179Omen.byName(Root.getString("Omen"));
        OmenSince = Root.getLong("OmenSince");
    }

    @Override
    protected void saveAdditional(CompoundTag Root, HolderLookup.Provider Registries) {
        super.saveAdditional(Root, Registries);
        Root.putBoolean("Running", Feed.isRunning());
        writeLocations(Root);
    }

    @Override
    protected void loadAdditional(CompoundTag Root, HolderLookup.Provider Registries) {
        super.loadAdditional(Root, Registries);
        if (Root.contains("Running")) {
            Feed.restore(Root.getBoolean("Running"));
        }
        if (Root.contains("Locations")) {
            readLocations(Root);
        }
        if (Root.contains("SatelliteCount")) {
            readView(Root);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider Registries) {
        CompoundTag Root = new CompoundTag();
        Root.putBoolean("Running", Feed.isRunning());
        writeLocations(Root);
        writeView(Root);
        return Root;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder Components) {
        super.collectImplicitComponents(Components);
        if (!Locations.isEmpty()) {
            Components.set(Scp179Registry.TerminalLocations.get(), List.copyOf(Locations));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput Input) {
        super.applyImplicitComponents(Input);
        List<ResourceLocation> Stored = Input.get(Scp179Registry.TerminalLocations.get());
        if (Stored != null) {
            Locations.clear();
            for (ResourceLocation Target : Stored) {
                if (Locations.size() < MaxLocations && !Locations.contains(Target)) {
                    Locations.add(Target);
                }
            }
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag Root) {
        super.removeComponentsFromTag(Root);
        Root.remove("Locations");
    }
}
