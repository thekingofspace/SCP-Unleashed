package net.rai.scp_unleashed.rocketry.platform;

import net.minecraft.world.inventory.MenuType;
import net.rai.scp_unleashed.rocketry.RocketryRegistry;

import java.util.List;

public enum LaunchPlatformKind {
    Rocket("rocket_platform", 16000, 8000, 200000, 2000,
            List.of(LaunchCheck.Payload, LaunchCheck.Crew, LaunchCheck.Fuel, LaunchCheck.Power, LaunchCheck.Sky, LaunchCheck.Route)),
    Satellite("satellite_platform", 8000, 3000, 60000, 1000,
            List.of(LaunchCheck.Payload, LaunchCheck.Fuel, LaunchCheck.Power, LaunchCheck.Sky));

    private final String Name;
    private final int FuelCapacity;
    private final int FuelCost;
    private final int PowerCost;
    private final int ChargeRate;
    private final List<LaunchCheck> Checks;

    LaunchPlatformKind(String Name, int FuelCapacity, int FuelCost, int PowerCost, int ChargeRate, List<LaunchCheck> Checks) {
        this.Name = Name;
        this.FuelCapacity = FuelCapacity;
        this.FuelCost = FuelCost;
        this.PowerCost = PowerCost;
        this.ChargeRate = ChargeRate;
        this.Checks = Checks;
    }

    public String getName() {
        return Name;
    }

    public int getFuelCapacity() {
        return FuelCapacity;
    }

    public int getFuelCost() {
        return FuelCost;
    }

    public int getPowerCost() {
        return PowerCost;
    }

    public int getPowerCapacity() {
        return PowerCost;
    }

    public int getChargeRate() {
        return ChargeRate;
    }

    public List<LaunchCheck> getChecks() {
        return Checks;
    }

    public int requiredMask() {
        int Mask = 0;
        for (LaunchCheck Check : Checks) {
            Mask |= Check.bit();
        }
        return Mask;
    }

    public String getTitleKey() {
        return "container.scp_unleashed." + Name;
    }

    public MenuType<LaunchPlatformMenu> menuType() {
        return this == Rocket ? RocketryRegistry.RocketPlatformMenu.get() : RocketryRegistry.SatellitePlatformMenu.get();
    }
}
