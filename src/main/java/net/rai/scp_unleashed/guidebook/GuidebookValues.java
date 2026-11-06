package net.rai.scp_unleashed.guidebook;

import net.minecraft.SharedConstants;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.archive.ArchiveBlockEntity;
import net.rai.scp_unleashed.block.entity.CoalGeneratorBlockEntity;
import net.rai.scp_unleashed.block.entity.EnergumBlockEntity;
import net.rai.scp_unleashed.extraction.ExtractionTargeting;
import net.rai.scp_unleashed.extraction.RiftDetectorItem;
import net.rai.scp_unleashed.extraction.RiftExtractorItem;
import net.rai.scp_unleashed.extraction.UnstableFragmentItem;
import net.rai.scp_unleashed.fission.GasDissolverBlockEntity;
import net.rai.scp_unleashed.fission.ReactorFuel;
import net.rai.scp_unleashed.fission.SonicVibratorBlockEntity;
import net.rai.scp_unleashed.fusion.FusionLayout;
import net.rai.scp_unleashed.fusion.FusionReactorBlockEntity;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;
import net.rai.scp_unleashed.oil.ChemicalProcessingRecipe;
import net.rai.scp_unleashed.oil.CombustionGeneratorBlockEntity;
import net.rai.scp_unleashed.oil.DistillingRecipe;
import net.rai.scp_unleashed.oil.OilExtractorBlockEntity;
import net.rai.scp_unleashed.powerbridge.PowerBridgeBlockEntity;
import net.rai.scp_unleashed.powerbridge.PowerBridgeConversion;
import net.rai.scp_unleashed.powerbridge.PowerBridgeKind;
import net.rai.scp_unleashed.pressure.DuctKind;
import net.rai.scp_unleashed.processing.ElectricFurnaceBlockEntity;
import net.rai.scp_unleashed.reactor.ControlRodTowerBlock;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.rifttech.CollectorTuning;
import net.rai.scp_unleashed.rifttech.GridTuning;
import net.rai.scp_unleashed.solar.SolarTier;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.steam.CoolingTowerBlockEntity;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;
import net.rai.scp_unleashed.steam.WaterBoilerBlockEntity;
import net.rai.scp_unleashed.wire.BatteryBank;
import net.rai.scp_unleashed.wire.ChargingRedstone;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.WireTier;
import net.rai.scp_unleashed.wire.Wires;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class GuidebookValues {
    private static final Map<String, Supplier<Object>> Values = new ConcurrentHashMap<>();

    static {
        register("charge_cost", () -> ChargingRedstone.ChargeCost);
        register("charge_rate", () -> ChargingRedstone.ChargeRate);
        register("charge_seconds", () -> seconds(ticksToCharge(ChargingRedstone.ChargeRate)));
        register("energum_output", () -> EnergumBlockEntity.PowerOutput);
        register("energum_seconds", () -> seconds(ticksToCharge(EnergumBlockEntity.PowerOutput)));
        register("heat_generator_output", () -> CoalGeneratorBlockEntity.PowerPerSlot);
        register("heat_generator_max", () -> CoalGeneratorBlockEntity.MaxOutput);
        register("wire_blocks_per_coil", () -> Wires.BlocksPerCoil);
        register("coated_wire_length", () -> (int) WireTier.Coated.getMaxLength());
        register("outlet_links", () -> Wires.MaxLinksPerOutlet);
        register("wire_coated_demand", () -> WireTier.Coated.getMaxDemand());
        register("wire_advanced_demand", () -> WireTier.Advanced.getMaxDemand());
        register("wire_advanced_length", () -> (int) WireTier.Advanced.getMaxLength());
        register("wire_super_heavy_demand", () -> WireTier.SuperHeavy.getMaxDemand());
        register("wire_super_heavy_length", () -> (int) WireTier.SuperHeavy.getMaxLength());
        register("wire_ultimate_demand", () -> WireTier.Ultimate.getMaxDemand());
        register("wire_ultimate_length", () -> (int) WireTier.Ultimate.getMaxLength());
        register("wire_tether_demand", () -> WireTier.Tether.getMaxDemand());
        register("wire_tether_length", () -> (int) WireTier.Tether.getMaxLength());
        register("outlet_demand", () -> ConnectorTier.Basic.getMaxDemand());
        register("heavy_connector_demand", () -> ConnectorTier.Heavy.getMaxDemand());
        register("ultimate_connector_demand", () -> ConnectorTier.Ultimate.getMaxDemand());
        register("tether_connector_demand", () -> ConnectorTier.Tether.getMaxDemand());
        register("battery_capacity", () -> BatteryBank.CapacityPerCell);
        register("battery_charge_rate", () -> BatteryBank.TransferPerCell);
        register("battery_max_cells", () -> BatteryBank.MaxCells);
        register("bridge_ratio_fe", () -> PowerBridgeConversion.RatioFe);
        register("bridge_ratio_sp", () -> PowerBridgeConversion.RatioUnits);
        register("bridge_sample", () -> PowerBridgeConversion.Sample);
        register("bridge_sample_kept", () -> PowerBridgeConversion.sampleOut(PowerBridgeKind.Inbound));
        register("bridge_buffer", () -> grouped(PowerBridgeBlockEntity.BufferCapacity));
        register("bridge_ceiling", () -> grouped(PowerBridgeBlockEntity.CeilingUnits));
        register("hub_draw", () -> NetworkHubBlockEntity.Draw);
        register("furnace_draw", () -> ElectricFurnaceBlockEntity.WorkDraw);
        register("heat_generator_capacity", () -> grouped(CoalGeneratorBlockEntity.Capacity));
        register("turbine_output", () -> SteamTurbineBlockEntity.MaxOutput);
        register("turbine_steam_rate", () -> grouped((long) SteamTurbineBlockEntity.MaxFlow * SharedConstants.TICKS_PER_SECOND));
        register("turbine_capacity", () -> grouped(SteamTurbineBlockEntity.Capacity));
        register("boiler_steam_rate", () -> WaterBoilerBlockEntity.BoilRate * SharedConstants.TICKS_PER_SECOND);
        register("boiler_heat_seconds", () -> seconds((double) WaterBoilerBlockEntity.MaxHeat / WaterBoilerBlockEntity.HeatUp));
        register("boiler_burn_speed", () -> WaterBoilerBlockEntity.BurnSpeed);
        register("boilers_per_turbine", () -> (SteamTurbineBlockEntity.MaxFlow + WaterBoilerBlockEntity.BoilRate - 1) / WaterBoilerBlockEntity.BoilRate);
        register("cooling_tower_rate", () -> grouped((long) CoolingTowerBlockEntity.CondenseRate * SharedConstants.TICKS_PER_SECOND));
        register("vent_pump_draw", DuctKind.Vent::getPumpPower);
        register("pipe_pump_draw", DuctKind.Pipe::getPumpPower);
        register("solar_panel_peak", SolarTier.Panel::getPeakOutput);
        register("solar_panel_capacity", () -> grouped(SolarTier.Panel.getCapacity()));
        register("solar_array_peak", SolarTier.Array::getPeakOutput);
        register("solar_array_capacity", () -> grouped(SolarTier.Array.getCapacity()));
        register("extractor_draw", () -> OilExtractorBlockEntity.WorkDraw);
        register("extractor_rate", () -> OilExtractorBlockEntity.OilPerStroke * SharedConstants.TICKS_PER_SECOND / OilExtractorBlockEntity.StrokeTicks);
        register("distiller_draw", () -> DistillingRecipe.DefaultPower);
        register("processor_draw", () -> ChemicalProcessingRecipe.DefaultPower);
        register("combustion_max", () -> CombustionGeneratorBlockEntity.MaxOffer);
        register("combustion_capacity", () -> grouped(CombustionGeneratorBlockEntity.Capacity));
        register("fusion_output", () -> grouped(FusionReactorBlockEntity.MaxOutput));
        register("fusion_startup", () -> grouped(FusionReactorBlockEntity.StartupEnergy));
        register("fusion_charge_rate", () -> grouped(FusionReactorBlockEntity.MaxChargeRate));
        register("fusion_fuel_minutes", () -> minutes(FusionReactorBlockEntity.FuelTicks));
        register("fusion_water_rate", () -> FusionReactorBlockEntity.WaterPerTick);
        register("fusion_tank_buckets", () -> FusionReactorBlockEntity.WaterCapacity / FluidType.BUCKET_VOLUME);
        register("fusion_ignition_buckets", () -> FusionReactorBlockEntity.IgnitionWater / FluidType.BUCKET_VOLUME);
        register("fusion_width", () -> FusionLayout.Width + 2);
        register("fusion_depth", () -> FusionLayout.Depth);
        register("fusion_height", () -> FusionLayout.Height);
        register("reactor_max_height", () -> ReactorTuning.MaxHeight);
        register("reactor_hot", () -> number(ReactorTuning.HotLevel));
        register("reactor_critical", () -> number(ReactorTuning.CriticalLevel));
        register("reactor_coolant_rate", () -> number(ReactorTuning.CoolantCap * SharedConstants.TICKS_PER_SECOND));
        register("reactor_rod_step", () -> number(ControlRodTowerBlock.HandStep));
        register("uranium_minutes", () -> minutes((double) ReactorFuel.Uranium.BurnTicks() * ReactorTuning.FuelBurnScale));
        register("thorium_minutes", () -> minutes((double) ReactorFuel.Thorium.BurnTicks() * ReactorTuning.FuelBurnScale));
        register("rift_life_min", () -> minutes(RiftEntity.Lifetime - RiftEntity.LifetimeSpread));
        register("rift_life_max", () -> minutes(RiftEntity.Lifetime + RiftEntity.LifetimeSpread));
        register("rift_interval", () -> minutes((RiftFormation.MinInterval + RiftFormation.MaxInterval) / 2.0));
        register("rift_distance_min", () -> RiftFormation.MinDistance);
        register("rift_distance_max", () -> RiftFormation.MaxDistance);
        register("rift_cap", () -> RiftFormation.CapCount);
        register("rift_cap_radius", () -> RiftFormation.CapRadius);
        register("detector_range_min", () -> number(RiftDetectorItem.rangeFor(FragmentRarity.Faint)));
        register("detector_range_max", () -> number(RiftDetectorItem.maxRange()));
        register("extractor_range", () -> number(ExtractionTargeting.Range));
        register("extract_seconds_min", () -> number(ExtractionTargeting.secondsFor(FragmentRarity.Faint)));
        register("extract_seconds_max", () -> number(ExtractionTargeting.secondsFor(FragmentRarity.Abyssal)));
        register("extractor_uses", () -> RiftExtractorItem.Durability);
        register("fragment_stack", () -> UnstableFragmentItem.StackSize);
        register("archive_paper", () -> ArchiveBlockEntity.PaperCost);
        register("archive_cards", () -> ArchiveBlockEntity.CardCount);
        register("stasis_minutes_faint", () -> minutes(StasisPodItem.budget(FragmentRarity.Faint)));
        register("stasis_minutes_abyssal", () -> minutes(StasisPodItem.budget(FragmentRarity.Abyssal)));
        register("carrier_radius", () -> number(LabContamination.CarrierRadius));
        register("rinse_seconds", () -> seconds(LabContamination.RinseTicks));
        register("dissolver_radius", () -> number(GasDissolverBlockEntity.ScrubRadius));
        register("vibrator_rate", () -> SonicVibratorBlockEntity.CleanRate * SharedConstants.TICKS_PER_SECOND);
        register("vibrator_draw", () -> SonicVibratorBlockEntity.WorkDraw);
        register("collector_range", () -> CollectorTuning.BaseRange);
        register("collector_range_max", CollectorTuning::maxRange);
        register("grid_side_min", () -> GridTuning.MinSide);
        register("grid_side_max", () -> GridTuning.MaxSide);
        register("grid_rise_min", () -> GridTuning.MinRise);
        register("grid_rise_max", () -> GridTuning.MaxRise);
        register("grid_draw_min", () -> GridTuning.grouped(GridTuning.drawFor(FragmentRarity.Faint, false)));
        register("grid_draw_max", () -> GridTuning.grouped(GridTuning.drawFor(FragmentRarity.Abyssal, false)));
        register("grid_cap", () -> GridTuning.RectangleCap);
        register("grid_box_cap", () -> GridTuning.BoxCap);
        register("apocalyptic_draw", () -> GridTuning.grouped(GridTuning.ApocalypticDraw));
        register("apocalyptic_min", () -> GridTuning.grouped(GridTuning.ApocalypticMinIntake));
        register("apocalyptic_minutes", GridTuning::apocalypticMinutes);
    }

    private GuidebookValues() {
    }

    public static void register(String Name, Supplier<Object> Value) {
        Values.put(Name, Value);
    }

    public static Object resolve(String Name) {
        Supplier<Object> Value = Values.get(Name);
        return Value == null ? Name : Value.get();
    }

    public static double ticksToCharge(int PowerPerTick) {
        return Math.ceil(ChargingRedstone.ChargeCost / (double) Math.max(1, Math.min(ChargingRedstone.ChargeRate, PowerPerTick)));
    }

    public static String seconds(double Ticks) {
        return number(Ticks / SharedConstants.TICKS_PER_SECOND);
    }

    public static String minutes(double Ticks) {
        return Long.toString(Math.round(Ticks / (SharedConstants.TICKS_PER_SECOND * 60.0)));
    }

    public static String grouped(long Value) {
        return String.format(Locale.ROOT, "%,d", Value);
    }

    public static String number(double Value) {
        double Rounded = Math.round(Value * 10.0) / 10.0;
        return Rounded == Math.floor(Rounded) ? Long.toString((long) Rounded) : String.format(Locale.ROOT, "%.1f", Rounded);
    }
}
