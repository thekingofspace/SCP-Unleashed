package net.rai.scp_unleashed.scp4666.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.List;

public class Scp4666ChoreItem extends Item {
    public static final int HeavyEvery = 20;
    public static final int HeavyTicks = 40;
    private final String Key;
    private final boolean Heavy;

    public Scp4666ChoreItem(Properties Properties, String Key, boolean Heavy) {
        super(Properties);
        this.Key = Key;
        this.Heavy = Heavy;
    }

    public boolean heavy() {
        return Heavy;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (!Heavy || World.isClientSide || !(Holder instanceof Player User) || World.getGameTime() % HeavyEvery != 0) {
            return;
        }
        burden(World, User);
    }

    public static boolean burden(Level World, Player User) {
        if (!Scp4666World.inside(World) || Scp4666World.exempt(User)) {
            return false;
        }
        User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HeavyTicks, 0, true, false, false));
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed." + Key + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
