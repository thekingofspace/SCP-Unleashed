package net.rai.scp_unleashed.scp001scarlet.king.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;

import java.util.List;

public class ScarletRifleItem extends GunItem {
    public static final int Capacity = 60;
    public static final int RechargeDelay = 24;
    public static final int RechargeStep = 4;
    public static final int RechargeAmount = 4;
    public static final int EmptyCooldown = 70;
    public static final int CellColour = 0xE0303A;
    public static final int RechargeColour = 0xFF8A3C;

    public ScarletRifleItem(Properties Settings) {
        super(GunKind.ScarletRifle, Settings);
    }

    public static boolean recharging(ItemStack Stack) {
        return GunItem.getReload(Stack) != null;
    }

    public static float charge(ItemStack Stack) {
        return Mth.clamp(GunItem.getAmmo(Stack) / (float) Capacity, 0.0F, 1.0F);
    }

    @Override
    public void onFired(ServerPlayer User, ItemStack Stack) {
        long Now = User.level().getGameTime();
        Stack.set(ScarletKingRegistry.RifleShotTick.get(), Now);
        if (GunItem.getAmmo(Stack) <= 0 && !User.getAbilities().instabuild) {
            Stack.set(GunsRegistry.Reload.get(), new GunTimer(Now, Now + EmptyCooldown));
            GunSounds.playAround(User, GunSounds.CueReloadStart, GunKind.ScarletRifle, 1.0F);
        }
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide) {
            return;
        }
        long Now = World.getGameTime();
        GunTimer Forced = GunItem.getReload(Stack);
        if (Forced != null) {
            if (Forced.isDone(Now)) {
                Stack.remove(GunsRegistry.Reload.get());
                GunItem.setAmmo(Stack, Capacity);
                if (Holder instanceof ServerPlayer User) {
                    GunSounds.playAround(User, GunSounds.CueReloadFinish, GunKind.ScarletRifle, User.getMainHandItem() == Stack ? 1.0F : 0.4F);
                }
            }
            return;
        }
        int Loaded = GunItem.getAmmo(Stack);
        if (Loaded >= Capacity || Now % RechargeStep != 0) {
            return;
        }
        Long Last = Stack.get(ScarletKingRegistry.RifleShotTick.get());
        if (Last != null && Now - Last < RechargeDelay && Now >= Last) {
            return;
        }
        GunItem.setAmmo(Stack, Math.min(Capacity, Loaded + RechargeAmount));
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return recharging(Stack) || GunItem.getAmmo(Stack) < Capacity;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return Math.round(13.0F * charge(Stack));
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return recharging(Stack) ? RechargeColour : CellColour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scarlet_rifle.tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.scarlet_rifle.cell", GunItem.getAmmo(Stack), Capacity).withStyle(ChatFormatting.RED));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.damage", Math.round(GunKind.ScarletRifle.getStats().getDamage() * 10.0F) / 10.0F)
                .withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.scarlet_rifle.recharge").withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.controls", Component.keybind("key.use"), Component.keybind("key.attack")).withStyle(ChatFormatting.GRAY));
    }
}
