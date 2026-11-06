package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.RedstoneConversion;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class RedstoneConverterBlockEntity extends RedstonePortBlockEntity {
    public static final int MaxHeld = 64;

    private final Map<BlockPos, Held> Stored = new HashMap<>();
    private int LastSent = -1;
    private int Output;

    public RedstoneConverterBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.RedstoneConverter.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, RedstoneConverterBlockEntity Converter) {
        if (World instanceof ServerLevel ServerWorld) {
            Converter.tickServer(ServerWorld);
        }
    }

    private void tickServer(ServerLevel World) {
        long Now = World.getGameTime();
        if (Stored.values().removeIf(Entry -> Entry.Until() <= Now)) {
            setChanged();
        }
        int Strongest = 0;
        for (Held Entry : Stored.values()) {
            Strongest = Math.max(Strongest, Entry.Signal());
        }
        Output = Strongest;
        boolean Powered = isPowered();
        int Signal = Powered ? Output : 0;
        if (Signal != LastSent || (Signal > 0 && isHeartbeat(Now))) {
            sendSignal(World, Port, Signal);
            LastSent = Signal;
        }
        showState(Powered, Signal, Block.UPDATE_CLIENTS);
    }

    @Override
    public Collection<String> getSubscriptions() {
        return List.of(WireChannels.anyOnPort(Port));
    }

    @Override
    public void receiveMessage(WireMessage Message) {
        if (level == null || WireChannels.Redstone.equals(WireChannels.typeOf(Message.Channel())) || WireChannels.portOf(Message.Channel()) != Port) {
            return;
        }
        OptionalInt Converted = RedstoneConversion.signalOf(Message.Payload());
        if (Converted.isEmpty()) {
            return;
        }
        int Signal = Converted.getAsInt();
        if (Signal == 0) {
            Stored.remove(Message.Source());
        } else if (Stored.containsKey(Message.Source()) || Stored.size() < MaxHeld) {
            int Duration = RedstoneConversion.durationOf(Message.Payload());
            Stored.put(Message.Source(), new Held(Signal, Duration > 0 ? level.getGameTime() + Duration : Long.MAX_VALUE));
        }
        setChanged();
    }

    @Override
    protected void portChanged(int OldPort) {
        if (level instanceof ServerLevel World && LastSent > 0) {
            sendSignal(World, OldPort, 0);
        }
        Stored.clear();
        LastSent = -1;
    }

    @Override
    public int getSignal() {
        return Output;
    }

    @Override
    public String getKindName() {
        return "converter";
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        ListTag Entries = new ListTag();
        Stored.forEach((Source, Entry) -> {
            CompoundTag Saved = new CompoundTag();
            Saved.putLong("Source", Source.asLong());
            Saved.putInt("Signal", Entry.Signal());
            Saved.putLong("Until", Entry.Until());
            Entries.add(Saved);
        });
        Tag.put("Stored", Entries);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Stored.clear();
        ListTag Entries = Tag.getList("Stored", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size() && Stored.size() < MaxHeld; Index++) {
            CompoundTag Saved = Entries.getCompound(Index);
            int Signal = Mth.clamp(Saved.getInt("Signal"), 0, 15);
            if (Signal > 0) {
                Stored.put(BlockPos.of(Saved.getLong("Source")), new Held(Signal, Saved.getLong("Until")));
            }
        }
    }

    private record Held(int Signal, long Until) {
    }
}
