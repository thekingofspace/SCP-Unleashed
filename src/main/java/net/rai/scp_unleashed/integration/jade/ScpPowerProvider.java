package net.rai.scp_unleashed.integration.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.BatteryBlockEntity;
import net.rai.scp_unleashed.block.entity.CreativeEnergySourceBlockEntity;
import net.rai.scp_unleashed.block.entity.NetworkObjectBlockEntity;
import net.rai.scp_unleashed.block.entity.NetworkRouterBlockEntity;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.block.entity.PowerSensorBlockEntity;
import net.rai.scp_unleashed.block.PowerSensorBlock;
import net.rai.scp_unleashed.block.entity.PortRangeBlockEntity;
import net.rai.scp_unleashed.block.entity.RedstonePortBlockEntity;
import net.rai.scp_unleashed.wire.BatteryBank;
import net.rai.scp_unleashed.wire.ChargingRedstone;
import net.rai.scp_unleashed.wire.WireDevice;
import net.rai.scp_unleashed.wire.WireHostBlock;
import net.rai.scp_unleashed.wire.WireNetwork;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import net.rai.scp_unleashed.wire.PowerSwitch;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.upgrade.Upgradeable;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.MutableComponent;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public enum ScpPowerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_power");
    private static final String DataKey = "ScpPower";
    private static final int BarColour = 0xFF2FB8C8;
    private static final int BarShadeColour = 0xFF15606A;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        WireNetworkManager Manager = WireNetworkManager.get(World);
        BlockPos Pos = Accessor.getPosition();
        BlockEntity Entity = Accessor.getBlockEntity();
        if (Accessor.getBlock() instanceof WireHostBlock Host) {
            Pos = Host.devicePos(Accessor.getBlockState(), Pos);
            Entity = World.isLoaded(Pos) ? World.getBlockEntity(Pos) : null;
        }
        CompoundTag Power = new CompoundTag();
        if (Entity instanceof OutletBlockEntity Outlet) {
            Power.putInt("Links", Outlet.getLinks().size());
            WireNetwork Network = Manager.networkAt(World, Outlet.getBlockPos());
            if (Network != null) {
                Power.putInt("Devices", Network.getLastDeviceCount());
                Power.putLong("Supply", Network.getLastSupply());
                Power.putLong("Demand", Network.getLastDemand());
                Power.putLong("Stored", Network.getLastStored());
                Power.putLong("Capacity", Network.getLastCapacity());
                Power.putLong("FromStorage", Network.getLastFromBackup());
                Power.putLong("ToStorage", Network.getLastToStorage());
                Power.putLong("Sunk", Network.getLastSunk());
                Power.putLong("Shortfall", Network.getLastShortfall());
            }
        } else if (Accessor.getBlock() instanceof PowerSwitch Switch && Switch.isSensor()) {
            if (!(Entity instanceof PowerSensorBlockEntity Sensor)) {
                return;
            }
            Power.putDouble("SensorFlow", Sensor.getFlow());
            Power.putBoolean("SensorOn", Accessor.getBlockState().getValue(PowerSensorBlock.Powered));
        } else if (Accessor.getBlock() instanceof PowerSwitch Switch) {
            Power.putBoolean("SwitchClosed", Switch.isClosed(Accessor.getBlockState()));
            Power.putBoolean("Breaker", Switch.tripsOnOverload());
            double[] Load = Manager.switchLoad(Pos);
            if (Load != null) {
                Power.putDouble("SwitchLoad", Load[0]);
                Power.putDouble("SwitchLimit", Load[1]);
            }
            double[] Pending = Switch.isClosed(Accessor.getBlockState()) ? null : Manager.openSwitchEstimate(World, Pos);
            if (Pending != null) {
                Power.putDouble("SwitchPending", Pending[0]);
                Power.putDouble("SwitchLimit", Pending[1]);
            }
            putUpgrades(Power, Entity);
        } else if (Entity instanceof BatteryBlockEntity) {
            BatteryBank Bank = Manager.bankAt(World, Pos);
            Power.putInt("Cells", Bank.getCellCount());
            Power.putLong("Stored", Bank.getStoredPower());
            Power.putLong("Capacity", Bank.getPowerCapacity());
            Power.putLong("BankWants", Bank.getPowerDemand());
            WireNetworkManager.DeviceFlow Flow = Manager.deviceFlow(World, Pos);
            if (Flow != null) {
                Power.putLong("BankIn", Flow.Input());
                Power.putLong("BankOut", Flow.Output());
            }
        } else if (Entity instanceof NetworkRouterBlockEntity) {
            Power.putInt("Routes", Manager.dataNetworkCount(World, Pos));
        } else if (ChargingRedstone.isChargeable(Accessor.getBlockState())) {
            ChargingRedstone Charge = Manager.chargeAt(Pos);
            if (Charge == null) {
                return;
            }
            Power.putBoolean("Charging", true);
            Power.putLong("Stored", Charge.getStoredPower());
            Power.putLong("Capacity", Charge.getPowerCapacity());
            putInput(Power, Charge, Manager.deviceFlow(World, Pos));
        } else if (Entity instanceof WireDevice Device) {
            if (Device instanceof NetworkObjectBlockEntity NetworkObject) {
                Power.putInt("Port", NetworkObject.getPort());
                Power.putString("Kind", NetworkObject.getKindName());
                Power.putBoolean("Powered", NetworkObject.isPowered());
                if (NetworkObject instanceof RedstonePortBlockEntity RedstonePort) {
                    Power.putInt("Signal", RedstonePort.getSignal());
                }
                if (NetworkObject instanceof PortRangeBlockEntity Range) {
                    Power.putInt("RangeMin", Range.getRangeMin());
                    Power.putInt("RangeMax", Range.getRangeMax());
                }
            }
            if (Entity instanceof CreativeEnergySourceBlockEntity Creative) {
                Power.putBoolean("CreativeSink", Creative.isSinkMode());
            }
            Power.putLong("Stored", Device.getStoredPower());
            Power.putLong("Capacity", Device.getPowerCapacity());
            WireNetworkManager.DeviceFlow Flow = Manager.deviceFlow(World, Pos);
            if (Device.isGenerator()) {
                Power.putInt("Generating", Device.getGenerationRate());
                Power.putLong("Output", Flow == null ? 0 : Flow.Output());
            }
            if (Device.isSink()) {
                Power.putLong("Sinking", Flow == null ? 0 : Flow.Input());
            } else {
                putInput(Power, Device, Flow);
            }
            putUpgrades(Power, Entity);
        } else {
            return;
        }
        Data.put(DataKey, Power);
    }

    private static void putInput(CompoundTag Power, WireDevice Device, WireNetworkManager.DeviceFlow Flow) {
        long Received = Flow == null ? 0 : Flow.Input();
        if (Device.getPowerDemand() > 0 || Received > 0) {
            Power.putLong("Input", Received);
            Power.putLong("Wants", Device.getPowerDemand());
        }
    }

    private static void putUpgrades(CompoundTag Power, BlockEntity Entity) {
        if (Entity instanceof Upgradeable Machine && !Machine.getUpgrades().isEmpty()) {
            ListTag Installed = new ListTag();
            for (UpgradeType Type : Machine.getUpgrades()) {
                Installed.add(StringTag.valueOf(Type.getSerializedName()));
            }
            Power.put("Upgrades", Installed);
        }
    }

    private static String padded(int Port) {
        return String.format("%04d", Port);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Power = Data.getCompound(DataKey);
        if (Power.contains("Links")) {
            Tooltip.add(Component.translatable(Power.getInt("Links") > 0 ? "jade.scp_unleashed.wired" : "jade.scp_unleashed.unwired"));
        }
        if (Power.contains("Port")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.port", padded(Power.getInt("Port"))));
            if (Power.contains("Signal")) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.signal_" + Power.getString("Kind"), Power.getInt("Signal")));
            }
            if (Power.contains("RangeMin")) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.relay_range", padded(Power.getInt("RangeMin")), padded(Power.getInt("RangeMax"))));
            }
            if (!Power.getBoolean("Powered")) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.no_power").withStyle(ChatFormatting.RED));
            }
        }
        if (Power.contains("SensorFlow")) {
            boolean On = Power.getBoolean("SensorOn");
            Tooltip.add(On ? Component.translatable("jade.scp_unleashed.sensor_on", Math.round(Power.getDouble("SensorFlow"))).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("jade.scp_unleashed.sensor_off").withStyle(ChatFormatting.GRAY));
        }
        if (Power.contains("SwitchClosed")) {
            boolean Closed = Power.getBoolean("SwitchClosed");
            String StateKey = Closed ? "jade.scp_unleashed.switch_closed" : Power.getBoolean("Breaker") ? "jade.scp_unleashed.switch_tripped" : "jade.scp_unleashed.switch_open";
            Tooltip.add(Component.translatable(StateKey).withStyle(Closed ? ChatFormatting.GREEN : ChatFormatting.RED));
            if (Power.contains("SwitchLoad")) {
                long Load = Math.round(Power.getDouble("SwitchLoad"));
                double Limit = Power.getDouble("SwitchLimit");
                Tooltip.add(Limit >= Integer.MAX_VALUE / 2.0
                        ? Component.translatable("jade.scp_unleashed.switch_load_free", Load)
                        : Component.translatable("jade.scp_unleashed.switch_load", Load, Math.round(Limit)));
            }
            if (Power.contains("SwitchPending")) {
                long Pending = Math.round(Power.getDouble("SwitchPending"));
                double Limit = Power.getDouble("SwitchLimit");
                Tooltip.add(Limit >= Integer.MAX_VALUE / 2.0
                        ? Component.translatable("jade.scp_unleashed.switch_pending_free", Pending)
                        : Component.translatable("jade.scp_unleashed.switch_pending", Pending, Math.round(Limit)));
            }
        }
        if (Power.contains("Upgrades")) {
            ListTag Installed = Power.getList("Upgrades", net.minecraft.nbt.Tag.TAG_STRING);
            Map<String, Integer> Counts = new LinkedHashMap<>();
            for (int Index = 0; Index < Installed.size(); Index++) {
                Counts.merge(Installed.getString(Index), 1, Integer::sum);
            }
            MutableComponent Names = Component.empty();
            for (Map.Entry<String, Integer> Entry : Counts.entrySet()) {
                if (!Names.getSiblings().isEmpty()) {
                    Names.append(", ");
                }
                Component Name = Component.translatable("upgrade.scp_unleashed." + Entry.getKey());
                Names.append(Entry.getValue() > 1 ? Component.translatable("jade.scp_unleashed.upgrade_count", Name, Entry.getValue()) : Name);
            }
            Tooltip.add(Component.translatable("jade.scp_unleashed.upgrades", Names).withStyle(ChatFormatting.AQUA));
        }
        if (Power.contains("Cells")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.cells", Power.getInt("Cells")));
        }
        if (Power.contains("Routes")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.routes", Power.getInt("Routes")));
        }
        if (Power.getBoolean("Charging")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.charging"));
        }
        if (Power.contains("Devices")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.devices", Power.getInt("Devices")));
        }
        if (Power.contains("CreativeSink")) {
            boolean Sink = Power.getBoolean("CreativeSink");
            Tooltip.add(Component.translatable(Sink ? "jade.scp_unleashed.creative_mode.sink" : "jade.scp_unleashed.creative_mode.source")
                    .withStyle(Sink ? ChatFormatting.AQUA : ChatFormatting.LIGHT_PURPLE));
        }
        long Capacity = Power.getLong("Capacity");
        if (Capacity > 0) {
            long Stored = Math.min(Power.getLong("Stored"), Capacity);
            IElementHelper Helper = IElementHelper.get();
            Tooltip.add(Helper.progress((float) Stored / Capacity, Component.translatable("jade.scp_unleashed.stored", Stored, Capacity),
                    Helper.progressStyle().color(BarColour, BarShadeColour).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
        if (Power.contains("Generating")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.generating", Power.getInt("Generating")));
        }
        if (Power.contains("Output")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.output", Power.getLong("Output")));
        }
        if (Power.contains("Input")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.input", Power.getLong("Input"), Power.getLong("Wants")));
        }
        if (Power.contains("Sinking")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.sinking", Power.getLong("Sinking")));
        }
        if (Power.getLong("BankIn") > 0 || Power.getLong("BankWants") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.bank_charging", Power.getLong("BankIn"), Power.getLong("BankWants")));
        }
        if (Power.getLong("BankOut") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.bank_discharging", Power.getLong("BankOut")));
        }
        if (Power.contains("Devices")) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.flow", Power.getLong("Supply"), Power.getLong("Demand")));
            if (Power.getLong("FromStorage") > 0) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.flow_from_storage", Power.getLong("FromStorage")).withStyle(ChatFormatting.GOLD));
            }
            if (Power.getLong("ToStorage") > 0) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.flow_to_storage", Power.getLong("ToStorage")));
            }
            if (Power.getLong("Sunk") > 0) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.flow_sunk", Power.getLong("Sunk")).withStyle(ChatFormatting.AQUA));
            }
            if (Power.getLong("Shortfall") > 0) {
                Tooltip.add(Component.translatable("jade.scp_unleashed.flow_short", Power.getLong("Shortfall")).withStyle(ChatFormatting.RED));
            }
        }
    }
}
