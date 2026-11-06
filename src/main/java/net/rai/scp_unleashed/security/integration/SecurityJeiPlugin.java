package net.rai.scp_unleashed.security.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.security.MotionDetectorBlockEntity;
import net.rai.scp_unleashed.security.SecurityRegistry;
import net.rai.scp_unleashed.security.SentryTurretBlockEntity;

@JeiPlugin
public class SecurityJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = SecurityRegistry.location("security_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(SecurityRegistry.SentryTurretItem.get()),
                Component.translatable("jei.scp_unleashed.info.sentry_turret.1", (int) SentryTurretBlockEntity.Range),
                Component.translatable("jei.scp_unleashed.info.sentry_turret.2", SentryTurretBlockEntity.IdleDraw, SentryTurretBlockEntity.FireDraw),
                Component.translatable("jei.scp_unleashed.info.sentry_turret.3"));
        Registration.addItemStackInfo(new ItemStack(SecurityRegistry.MotionDetectorItem.get()),
                Component.translatable("jei.scp_unleashed.info.motion_detector.1", MotionDetectorBlockEntity.DefaultRange),
                Component.translatable("jei.scp_unleashed.info.motion_detector.2"));
    }
}
