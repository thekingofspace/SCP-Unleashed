package net.rai.scp_unleashed.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.menu.CoalGeneratorMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MenuTypeRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);

    public static final DeferredHolder<MenuType<?>, MenuType<CoalGeneratorMenu>> CoalGenerator = MenuTypeRegister.register("coal_generator",
            () -> new MenuType<>(CoalGeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
