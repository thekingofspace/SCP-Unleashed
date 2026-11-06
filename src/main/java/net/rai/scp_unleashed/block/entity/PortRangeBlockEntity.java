package net.rai.scp_unleashed.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.WireChannels;
import net.rai.scp_unleashed.wire.WireMessage;
import net.rai.scp_unleashed.wire.WireNetworkManager;

import java.util.Collection;
import java.util.List;

public class PortRangeBlockEntity extends NetworkObjectBlockEntity {
    public static final String RelayKey = "Relays";
    public static final int MaxRelays = 4;
    public static final int OwnSlot = 0;
    public static final int MinSlot = 1;
    public static final int MaxSlot = 2;

    private int RangeMin;
    private int RangeMax;

    public PortRangeBlockEntity(BlockPos Pos, BlockState State) {
        super(ModBlockEntityTypes.PortRange.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, PortRangeBlockEntity Range) {
        Range.showActive(Range.isPowered());
    }

    public int getRangeMin() {
        return Math.min(RangeMin, RangeMax);
    }

    public int getRangeMax() {
        return Math.max(RangeMin, RangeMax);
    }

    @Override
    public int getSlotCount() {
        return 3;
    }

    @Override
    public int getSlot(int Slot) {
        return switch (Slot) {
            case MinSlot -> RangeMin;
            case MaxSlot -> RangeMax;
            default -> Port;
        };
    }

    @Override
    public void setSlot(int Slot, int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Slot == MinSlot && Clamped != RangeMin) {
            RangeMin = Clamped;
            changed();
        } else if (Slot == MaxSlot && Clamped != RangeMax) {
            RangeMax = Clamped;
            changed();
        } else if (Slot == OwnSlot) {
            super.setSlot(Slot, Requested);
        }
    }

    @Override
    public Collection<String> getSubscriptions() {
        return List.of(WireChannels.anyOnPort(Port));
    }

    @Override
    public void receiveMessage(WireMessage Message) {
        if (!(level instanceof ServerLevel World) || !isPowered() || WireChannels.portOf(Message.Channel()) != Port) {
            return;
        }
        int Relays = Message.Payload().getInt(RelayKey);
        if (Relays >= MaxRelays) {
            return;
        }
        String Type = WireChannels.typeOf(Message.Channel());
        CompoundTag Payload = Message.Payload().copy();
        Payload.putInt(RelayKey, Relays + 1);
        WireNetworkManager Manager = WireNetworkManager.get(World);
        for (int Target : Manager.listeningPorts(World, worldPosition, Type, getRangeMin(), getRangeMax())) {
            if (Target != Port) {
                Manager.sendFromDevice(World, worldPosition, WireChannels.onPort(Type, Target), Payload);
            }
        }
    }

    @Override
    public String getKindName() {
        return "port_range";
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("RangeMin", RangeMin);
        Tag.putInt("RangeMax", RangeMax);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        RangeMin = WireChannels.clampPort(Tag.getInt("RangeMin"));
        RangeMax = WireChannels.clampPort(Tag.getInt("RangeMax"));
    }
}
