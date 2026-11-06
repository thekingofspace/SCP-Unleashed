package net.rai.scp_unleashed.radio;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RadioTowerBlockEntity extends BlockEntity implements WireDevice {
    public static final int StartTicks = 5;
    public static final int GraceTicks = 20;
    public static final int ScanEvery = 40;
    public static final int RefreshEvery = 100;
    public static final int AnnounceGap = 200;

    private int Received;
    private int LastReceived;
    private int Demand;
    private int Masts;
    private int FedTicks;
    private int ShortTicks;
    private boolean Powered;
    private boolean Settled;
    private int Channel = RadioCarry.DefaultChannel;
    private List<RadioBleedKind> Sources = List.of();
    private RadioTowerData.Tower Published;
    private long LastAnnounce = Long.MIN_VALUE / 2;
    private int Age;

    public RadioTowerBlockEntity(BlockPos Pos, BlockState State) {
        super(RadioRegistry.TowerEntity.get(), Pos, State);
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, RadioTowerBlockEntity Tower) {
        if (World instanceof ServerLevel ServerWorld && State.getBlock() instanceof RadioTowerBlock) {
            Tower.tickServer(ServerWorld, State);
        }
    }

    public static int countMasts(Level World, BlockPos Base) {
        int Count = 0;
        BlockPos.MutableBlockPos Cursor = Base.mutable();
        while (Count < RadioMastBlock.MaxStack) {
            Cursor.move(0, 1, 0);
            if (!(World.getBlockState(Cursor).getBlock() instanceof RadioMastBlock)) {
                break;
            }
            Count++;
        }
        return Count;
    }

    private void tickServer(ServerLevel World, BlockState State) {
        Age++;
        int Delivered = Received;
        Received = 0;
        LastReceived = Delivered;
        int Asked = Demand;
        int Stacked = countMasts(World, worldPosition);
        boolean MastsChanged = Stacked != Masts;
        Masts = Stacked;
        Demand = VoiceConfig.demand(effectiveMasts());
        boolean Was = Powered;
        if (Demand <= 0) {
            FedTicks = 0;
            ShortTicks = 0;
            Powered = false;
        } else if (Asked > 0 && Delivered >= Asked) {
            ShortTicks = 0;
            FedTicks = Math.min(FedTicks + 1, StartTicks);
            if (!Powered && FedTicks >= StartTicks) {
                Powered = true;
            }
        } else if (Asked > 0) {
            FedTicks = 0;
            if (Powered && ++ShortTicks > GraceTicks) {
                Powered = false;
                ShortTicks = 0;
            }
        }
        if ((Age + worldPosition.hashCode()) % ScanEvery == 0 || Age == 1) {
            Sources = RadioBleed.scan(World, worldPosition);
        }
        if (State.getValue(RadioTowerBlock.Powered) != Powered) {
            World.setBlock(worldPosition, State.setValue(RadioTowerBlock.Powered, Powered), Block.UPDATE_CLIENTS);
        }
        if (MastsChanged || Was != Powered || Age % RefreshEvery == 0) {
            lamp(World);
        }
        publish(World, Age % RefreshEvery == 0);
        if (Was != Powered) {
            if (Settled) {
                World.playSound(null, worldPosition, (Powered ? RadioRegistry.TowerStart : RadioRegistry.TowerStop).get(), SoundSource.BLOCKS, 0.7f, 1.0f);
                announce(World);
            }
            setChanged();
        }
        Settled = true;
    }

    private void lamp(ServerLevel World) {
        if (Masts <= 0) {
            return;
        }
        BlockPos Top = worldPosition.above(Masts);
        BlockState Cap = World.getBlockState(Top);
        if (Cap.getBlock() instanceof RadioMastBlock && Cap.getValue(RadioMastBlock.Top) && Cap.getValue(RadioMastBlock.Lit) != Powered) {
            World.setBlock(Top, Cap.setValue(RadioMastBlock.Lit, Powered), Block.UPDATE_CLIENTS);
        }
    }

    private void publish(ServerLevel World, boolean Force) {
        RadioTowerData.Tower Current = record();
        if (Force || !Objects.equals(Current, Published)) {
            RadioTowerData.get(World).put(Current);
            Published = Current;
        }
    }

    public RadioTowerData.Tower record() {
        return new RadioTowerData.Tower(worldPosition, effectiveMasts(), Powered, Channel, Sources);
    }

    private void announce(ServerLevel World) {
        long Now = World.getGameTime();
        if (Now - LastAnnounce < AnnounceGap) {
            return;
        }
        LastAnnounce = Now;
        RadioNetwork.Ranges Limits = VoiceConfig.ranges();
        List<RadioRelay.Hearer> Hearers = RadioRelay.fromTower(World, record(), Limits, RadioTowerData.nodes(World, Limits));
        Component Line = Component.translatable(Powered ? "radio.scp_unleashed.tower.online" : "radio.scp_unleashed.tower.offline",
                RadioChat.tag(Channel), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()).withStyle(Powered ? ChatFormatting.GREEN : ChatFormatting.RED);
        for (RadioRelay.Hearer Each : Hearers) {
            Each.Listener().sendSystemMessage(Line);
        }
        RadioSignal.playTo(Hearers, RadioRegistry.MessageLayers, 0.5f, Powered ? 1.2f : 0.85f);
    }

    public void tune(ServerPlayer User, int Next) {
        Channel = RadioCarry.clampChannel(Next);
        setChanged();
        if (level instanceof ServerLevel World) {
            publish(World, true);
        }
        RadioSignal.playTo(User, List.of(RadioRegistry.Dial, RadioRegistry.Beep), 0.6f, 1.0f);
        User.displayClientMessage(Component.translatable("block.scp_unleashed.radio_booster_tower.tuned", RadioCarry.label(Channel)).withStyle(ChatFormatting.GREEN), true);
    }

    public int effectiveMasts() {
        return Math.min(Masts, RadioNetwork.MaxMasts);
    }

    public int masts() {
        return Masts;
    }

    public boolean isPowered() {
        return Powered;
    }

    public int channel() {
        return Channel;
    }

    public int demand() {
        return Demand;
    }

    public int received() {
        return LastReceived;
    }

    public List<RadioBleedKind> sources() {
        return Sources;
    }

    public int links(ServerLevel World) {
        RadioNetwork.Ranges Limits = VoiceConfig.ranges();
        if (Masts <= 0) {
            return 0;
        }
        RadioNetwork.Node Self = RadioNetwork.node(Limits, worldPosition, effectiveMasts());
        int Count = 0;
        for (RadioNetwork.Node Other : RadioTowerData.nodes(World, Limits)) {
            if (Other.Pos().equals(worldPosition)) {
                continue;
            }
            if (RadioNetwork.margin(Self.Antenna().distanceTo(Other.Antenna()), Math.min(Self.Link(), Other.Link())) >= 0.0) {
                Count++;
            }
        }
        return Count;
    }

    public RadioTowerStatus status() {
        if (Masts <= 0) {
            return RadioTowerStatus.NoMast;
        }
        return Powered ? RadioTowerStatus.Online : RadioTowerStatus.Unpowered;
    }

    public MutableComponent summary() {
        List<Component> Parts = new ArrayList<>();
        Parts.add(Component.translatable("radio.scp_unleashed.tower.summary.carrier", RadioCarry.label(Channel)));
        Parts.add(Component.translatable("radio.scp_unleashed.tower.summary.masts", effectiveMasts(), RadioNetwork.MaxMasts));
        Parts.add(Component.translatable(status().key()));
        if (level instanceof ServerLevel World && Powered) {
            Parts.add(Component.translatable("radio.scp_unleashed.tower.summary.links", links(World)));
        }
        MutableComponent Line = Component.empty();
        for (int Index = 0; Index < Parts.size(); Index++) {
            if (Index > 0) {
                Line.append(Component.literal("  "));
            }
            Line.append(Parts.get(Index));
        }
        return Line.withStyle(status().colour());
    }

    public void removed(ServerLevel World) {
        RadioTowerData.get(World).remove(worldPosition);
        RadioBleed.forget(World, worldPosition);
        Powered = false;
        lamp(World);
    }

    @Override
    public int getPowerDemand() {
        return Demand;
    }

    @Override
    public void insertPower(int Amount) {
        if (Amount > 0) {
            Received = (int) Math.min(Integer.MAX_VALUE, (long) Received + Amount);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Channel", Channel);
        Tag.putBoolean("Powered", Powered);
        Tag.putInt("Masts", Masts);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Channel = Tag.contains("Channel") ? RadioCarry.clampChannel(Tag.getInt("Channel")) : RadioCarry.DefaultChannel;
        Powered = Tag.getBoolean("Powered");
        Masts = Math.max(0, Tag.getInt("Masts"));
        FedTicks = Powered ? StartTicks : 0;
    }
}
