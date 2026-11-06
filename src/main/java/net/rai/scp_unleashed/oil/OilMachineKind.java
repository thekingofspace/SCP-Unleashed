package net.rai.scp_unleashed.oil;

import net.minecraft.world.inventory.MenuType;

public enum OilMachineKind {
    Extractor("oil_extractor", new int[]{8000}, 0, 0),
    Tower("distillation_tower", new int[]{8000, 8000, 8000}, 0, 0),
    Processor("chemical_processor", new int[]{8000, 8000}, 0, 0),
    Generator("combustion_generator", new int[]{8000}, 2000, 0),
    Refinery("oil_refinery", new int[]{8000, 8000}, 0, 1);

    private final String Name;
    private final int[] Capacities;
    private final int GasCapacity;
    private final int OutputSlots;

    OilMachineKind(String Name, int[] Capacities, int GasCapacity, int OutputSlots) {
        this.Name = Name;
        this.Capacities = Capacities;
        this.GasCapacity = GasCapacity;
        this.OutputSlots = OutputSlots;
    }

    public String getName() {
        return Name;
    }

    public int tankCount() {
        return Capacities.length;
    }

    public int capacity(int Tank) {
        return Tank >= 0 && Tank < Capacities.length ? Capacities[Tank] : 0;
    }

    public int getGasCapacity() {
        return GasCapacity;
    }

    public int getOutputSlots() {
        return OutputSlots;
    }

    public String getTitleKey() {
        return "container.scp_unleashed." + Name;
    }

    public MenuType<OilMachineMenu> menuType() {
        return switch (this) {
            case Extractor -> OilRegistry.ExtractorMenu.get();
            case Tower -> OilRegistry.TowerMenu.get();
            case Processor -> OilRegistry.ProcessorMenu.get();
            case Generator -> OilRegistry.GeneratorMenu.get();
            case Refinery -> OilRegistry.RefineryMenu.get();
        };
    }
}
