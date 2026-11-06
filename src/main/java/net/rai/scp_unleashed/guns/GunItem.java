package net.rai.scp_unleashed.guns;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public class GunItem extends Item {
    public static final int UseDuration = 72000;
    public static final int AmmoBarColour = 0xE8B23A;
    public static final int ReloadBarColour = 0x8CC8FF;
    public static final int ChargeBarColour = 0x3FE8C8;
    public static final String ReloadKey = "key.scp_unleashed.guns_reload";

    private final GunKind Kind;

    public GunItem(GunKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
    }

    public GunKind getKind() {
        return Kind;
    }

    public float shotDamage(ItemStack Stack) {
        return Kind.getStats().getDamage();
    }

    public void onFired(ServerPlayer User, ItemStack Stack) {
    }

    public boolean shoot(ServerLevel World, ServerPlayer User, ItemStack Stack, long Now) {
        return false;
    }

    public boolean blocksTrigger(Player User, ItemStack Stack) {
        return false;
    }

    public static float damageOf(ItemStack Stack, GunKind Kind) {
        return Stack.getItem() instanceof GunItem Gun && Gun.getKind() == Kind ? Gun.shotDamage(Stack) : Kind.getStats().getDamage();
    }

    @Nullable
    public static GunKind kindOf(ItemStack Stack) {
        return Stack.getItem() instanceof GunItem Gun ? Gun.getKind() : null;
    }

    public static int getAmmo(ItemStack Stack) {
        return Stack.getOrDefault(GunsRegistry.LoadedAmmo.get(), 0);
    }

    public static void setAmmo(ItemStack Stack, int Rounds) {
        Stack.set(GunsRegistry.LoadedAmmo.get(), Math.max(0, Rounds));
    }

    public static int getCharges(ItemStack Stack) {
        return Stack.getOrDefault(GunsRegistry.Charges.get(), 0);
    }

    public static void setCharges(ItemStack Stack, int Count) {
        Stack.set(GunsRegistry.Charges.get(), Math.max(0, Count));
    }

    @Nullable
    public static GunTimer getReload(ItemStack Stack) {
        return Stack.get(GunsRegistry.Reload.get());
    }

    @Nullable
    public static GunTimer getCharging(ItemStack Stack) {
        return Stack.get(GunsRegistry.Charging.get());
    }

    public static boolean isBusy(ItemStack Stack) {
        return getReload(Stack) != null || getCharging(Stack) != null;
    }

    public static ItemStack loaded(GunKind Kind) {
        ItemStack Stack = new ItemStack(GunsRegistry.gunItem(Kind));
        if (Kind.getStats().usesCharges()) {
            setCharges(Stack, Kind.getStats().getCharges());
        } else if (Kind.getStats().getCapacity() > 0) {
            setAmmo(Stack, Kind.getStats().getCapacity());
        }
        return Stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(Stack);
        }
        User.setSprinting(false);
        User.startUsingItem(Hand);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseDuration;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean canAttackBlock(BlockState State, Level World, BlockPos Pos, Player User) {
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack Stack, Player User, Entity Target) {
        return true;
    }

    @Override
    public boolean onEntitySwing(ItemStack Stack, LivingEntity User, InteractionHand Hand) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack OldStack, ItemStack NewStack, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(OldStack, NewStack);
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack OldStack, ItemStack NewStack) {
        return !ItemStack.isSameItem(OldStack, NewStack);
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        if (Kind.getStats().usesCharges()) {
            return getCharges(Stack) < Kind.getStats().getCharges();
        }
        return getReload(Stack) != null || getAmmo(Stack) < Kind.getStats().getCapacity();
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        if (Kind.getStats().usesCharges()) {
            return Math.round(13.0F * getCharges(Stack) / Kind.getStats().getCharges());
        }
        return Math.round(13.0F * Mth.clamp(getAmmo(Stack) / (float) Kind.getStats().getCapacity(), 0.0F, 1.0F));
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        if (Kind.getStats().usesCharges()) {
            return ChargeBarColour;
        }
        return getReload(Stack) != null ? ReloadBarColour : AmmoBarColour;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide || !(Holder instanceof ServerPlayer User)) {
            return;
        }
        boolean InHand = User.getMainHandItem() == Stack;
        long Now = World.getGameTime();
        GunTimer Reloading = getReload(Stack);
        if (Reloading != null) {
            if (!InHand) {
                Stack.remove(GunsRegistry.Reload.get());
            } else if (Reloading.isDone(Now)) {
                GunReloading.finish(User, Stack, Kind);
            }
        }
        if (GunHeat.cooled(Stack, Now)) {
            GunSounds.playAround(User, GunSounds.CueCooled, Kind, InHand ? 1.0F : 0.5F);
        }
        GunTimer Charging = getCharging(Stack);
        if (Charging != null) {
            if (!InHand) {
                GunFiring.abortCharge(User, Stack);
            } else if (Charging.isDone(Now)) {
                GunFiring.discharge(User, Stack, Kind);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Prefix = "item.scp_unleashed." + Kind.getName();
        GunStats Stats = Kind.getStats();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (Stats.usesCharges()) {
            int Left = getCharges(Stack);
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.charges", Left, Stats.getCharges())
                    .withStyle(Left > 0 ? ChatFormatting.AQUA : ChatFormatting.DARK_RED));
        } else {
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.loaded", getAmmo(Stack), Stats.getCapacity(), Kind.getAmmo().getDescription())
                    .withStyle(ChatFormatting.GOLD));
        }
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        String Damage = Stats.getPellets() > 1 ? number(Stats.getDamage()) + " x" + Stats.getPellets() : number(Stats.getDamage());
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.damage", Damage).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.action", Component.translatable("tooltip.scp_unleashed.guns.action." + Kind.getAction()),
                Math.round(1200.0F / Stats.getInterval())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.range", Math.round(Stats.getRange())).withStyle(ChatFormatting.DARK_GREEN));
        if (Stats.usesCharges()) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.charge_time", number(Stats.getChargeTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
            Lines.add(Component.translatable(Prefix + ".warning").withStyle(ChatFormatting.RED));
        } else {
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_time", number(Stats.getReloadTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        }
        if (Stats.usesHeat() && Stats.shotsToOverheat() > 0) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.overheat", number(Math.round(Stats.shotsToOverheat() * Stats.getInterval() / 2.0F) / 10.0F))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        if (Stats.getCrouchRecoil() < 1.0F) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.braced", Component.keybind("key.sneak")).withStyle(ChatFormatting.GRAY));
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.controls", Component.keybind("key.use"), Component.keybind("key.attack"))
                .withStyle(ChatFormatting.GRAY));
        if (!Stats.usesCharges()) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_hint", Component.keybind(ReloadKey)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
