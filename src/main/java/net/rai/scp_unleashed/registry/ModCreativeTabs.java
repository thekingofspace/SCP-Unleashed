package net.rai.scp_unleashed.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CreativeTabRegister = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Scp_unleashed.ModId);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Resources = CreativeTabRegister.register("resources", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.resources"))
            .icon(() -> ModItems.EnergumDust.get().getDefaultInstance())
            .displayItems((Parameters, Output) -> {
                Output.accept(ModItems.EnergumOre.get());
                Output.accept(ModItems.EnergumDust.get());
                Output.accept(ModItems.EnergumBlock.get());
                Output.accept(ModItems.ChargedRedstone.get());
                Output.accept(ModItems.CopperWire.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Machines = CreativeTabRegister.register("machines", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.machines"))
            .icon(() -> ModItems.CoalGenerator.get().getDefaultInstance())
            .withTabsBefore(Resources.getId())
            .displayItems((Parameters, Output) -> {
                Output.accept(ModItems.WireCoil.get());
                Output.accept(ModItems.AdvancedWire.get());
                Output.accept(ModItems.SuperHeavyWire.get());
                Output.accept(ModItems.UltimateWire.get());
                Output.accept(ModItems.TetherWire.get());
                Output.accept(ModItems.Outlet.get());
                Output.accept(ModItems.AdvancedOutlet.get());
                Output.accept(ModItems.TetherOutlet.get());
                Output.accept(ModItems.BasicTransfer.get());
                Output.accept(ModItems.HeavyTransfer.get());
                Output.accept(ModItems.UltimateTransfer.get());
                Output.accept(ModItems.Breaker.get());
                Output.accept(ModItems.PowerCutoff.get());
                Output.accept(ModItems.PowerSensor.get());
                Output.accept(ModItems.CoalGenerator.get());
                Output.accept(ModItems.Battery.get());
                Output.accept(ModItems.ChargeDisperser.get());
                Output.accept(ModItems.NetworkRouter.get());
                Output.accept(ModItems.RedstoneSender.get());
                Output.accept(ModItems.RedstoneReceiver.get());
                Output.accept(ModItems.RedstoneConverter.get());
                Output.accept(ModItems.PortRange.get());
                Output.accept(ModItems.Pipe.get());
                Output.accept(ModItems.Vent.get());
                Output.accept(ModItems.PipeWallPort.get());
                Output.accept(ModItems.VentWallPort.get());
                Output.accept(ModItems.PipePressurizer.get());
                Output.accept(ModItems.VentPressurizer.get());
                Output.accept(ModItems.MiniTank.get());
                Output.accept(ModItems.Tank.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Upgrades = CreativeTabRegister.register("upgrades", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.upgrades"))
            .icon(() -> ModItems.AutoResetUpgrade.get().getDefaultInstance())
            .withTabsBefore(Machines.getId())
            .displayItems((Parameters, Output) -> {
                Output.accept(ModItems.UpgradeTemplate.get());
                Output.accept(ModItems.AutoResetUpgrade.get());
                Output.accept(ModItems.SpeedUpgrade.get());
            })
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Facility = CreativeTabRegister.register("facility", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.facility"))
            .icon(() -> iconOf("sliding_door", Items.IRON_DOOR))
            .withTabsBefore(Upgrades.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Decoration = CreativeTabRegister.register("decoration", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.decoration"))
            .icon(() -> iconOf("lcz_wall_middle", Items.SMOOTH_STONE))
            .withTabsBefore(Facility.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Decals = CreativeTabRegister.register("decals", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.decals"))
            .icon(() -> iconOf("decal_blood_splatter", Items.PAPER))
            .withTabsBefore(Decoration.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Anomalies = CreativeTabRegister.register("anomalies", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.anomalies"))
            .icon(() -> iconOf("rift_extractor", Items.ENDER_EYE))
            .withTabsBefore(Decals.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ScpItems = CreativeTabRegister.register("scp_items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.scp_items"))
            .icon(() -> iconOf("scp002_specimen", Items.ROTTEN_FLESH))
            .withTabsBefore(Anomalies.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ScpBlocks = CreativeTabRegister.register("scp_blocks", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.scp_blocks"))
            .icon(() -> iconOf("scp002_shell", Items.TUFF))
            .withTabsBefore(ScpItems.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Weapons = CreativeTabRegister.register("weapons", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.weapons"))
            .icon(() -> iconOf("pistol", Items.CROSSBOW))
            .withTabsBefore(ScpBlocks.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Equipment = CreativeTabRegister.register("equipment", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.equipment"))
            .icon(() -> iconOf("gas_mask", Items.LEATHER_HELMET))
            .withTabsBefore(Weapons.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Foods = CreativeTabRegister.register("foods", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.foods"))
            .icon(() -> iconOf("keter_kola", Items.COOKIE))
            .withTabsBefore(Equipment.getId())
            .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> Creative = CreativeTabRegister.register("creative", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.scp_unleashed.creative"))
            .icon(() -> iconOf("creative_energy_source", Items.COMMAND_BLOCK))
            .withTabsBefore(Foods.getId())
            .displayItems((Parameters, Output) -> {
                Output.accept(ModItems.CreativeWire.get());
                Output.accept(ModItems.CreativeEnergySource.get());
                Output.accept(ModItems.CreativePressurizer.get());
                Output.accept(ModItems.CreativeTank.get());
            })
            .build());

    private static ItemStack iconOf(String Name, Item Fallback) {
        Item Found = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
        return new ItemStack(Found == Items.AIR ? Fallback : Found);
    }
}
