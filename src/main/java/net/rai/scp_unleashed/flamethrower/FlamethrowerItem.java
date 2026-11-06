package net.rai.scp_unleashed.flamethrower;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.network.GunShotPayload;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FlamethrowerItem extends GunItem {
    public static final String Prefix = "item.scp_unleashed.flamethrower";
    public static final String TooltipPrefix = "tooltip.scp_unleashed.flamethrower.";
    public static final int FuelBarColour = 0xF0862E;
    public static final int DouseGap = 10;
    private static final Map<UUID, Long> Doused = new HashMap<>();

    public FlamethrowerItem(Properties Properties) {
        super(GunKind.Flamethrower, Properties);
    }

    public static boolean isFlamethrower(ItemStack Stack) {
        return Stack.getItem() instanceof FlamethrowerItem;
    }

    @Override
    public boolean shoot(ServerLevel World, ServerPlayer User, ItemStack Stack, long Now) {
        if (FlamerStream.submerged(User)) {
            douse(World, User, Now);
            return true;
        }
        int Loaded = GunItem.getAmmo(Stack);
        boolean Free = User.getAbilities().instabuild;
        if (!Free) {
            GunItem.setAmmo(Stack, Loaded - 1);
        }
        FlamerStream.Pulse Burst = FlamerStream.pulse(World, User, Now, FlamerConfig.worldFire(), FlamerConfig.burnFlesh(),
                State -> State.is(FlamerRegistry.FlameBurnableFlesh));
        Vec3 Eye = User.getEyePosition();
        if (Burst.Fresh()) {
            GunSounds.play(World, null, Eye.x, Eye.y, Eye.z, FlamerSounds.ignite(), 1.0F);
            User.gameEvent(GameEvent.PROJECTILE_SHOOT);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new GunShotPayload(User.getId(), GunKind.Flamethrower.ordinal(), Eye, List.of()));
        if (!Free && Loaded - 1 <= 0) {
            sputter(World, User);
        }
        onFired(User, Stack);
        return true;
    }

    public static void sputter(ServerLevel World, ServerPlayer User) {
        Vec3 Nozzle = FlamerStream.nozzle(User);
        GunSounds.play(World, null, Nozzle.x, Nozzle.y, Nozzle.z, FlamerSounds.sputter(), 1.0F);
        World.sendParticles(ParticleTypes.SMOKE, Nozzle.x, Nozzle.y, Nozzle.z, 8, 0.08, 0.06, 0.08, 0.02);
        World.sendParticles(ParticleTypes.SMALL_FLAME, Nozzle.x, Nozzle.y, Nozzle.z, 3, 0.05, 0.05, 0.05, 0.01);
    }

    private static void douse(ServerLevel World, ServerPlayer User, long Now) {
        Long Last = Doused.get(User.getUUID());
        if (Last != null && Now - Last < DouseGap && Now >= Last) {
            return;
        }
        Doused.put(User.getUUID(), Now);
        Vec3 Nozzle = FlamerStream.nozzle(User);
        GunSounds.play(World, null, Nozzle.x, Nozzle.y, Nozzle.z, FlamerSounds.douse(), 1.0F);
        World.sendParticles(ParticleTypes.BUBBLE, Nozzle.x, Nozzle.y, Nozzle.z, 10, 0.1, 0.1, 0.1, 0.05);
    }

    public static void forget(UUID Id) {
        Doused.remove(Id);
    }

    @Override
    public boolean blocksTrigger(Player User, ItemStack Stack) {
        return FlamerStream.submerged(User);
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return GunItem.getReload(Stack) != null ? ReloadBarColour : FuelBarColour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        GunStats Stats = getKind().getStats();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(TooltipPrefix + "organic").withStyle(ChatFormatting.GOLD));
        int Fuel = GunItem.getAmmo(Stack);
        Lines.add(Component.translatable(TooltipPrefix + "fuel", Fuel, Stats.getCapacity()).withStyle(Fuel > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_RED));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        float PerSecond = Stats.getDamage() * 20.0F / Stats.getInterval();
        Lines.add(Component.translatable(TooltipPrefix + "damage", number(PerSecond)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "range", number(Stats.getRange())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "tank", number(Stats.getCapacity() * Stats.getInterval() / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_time", number(Stats.getReloadTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "water").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(TooltipPrefix + "controls", Component.keybind("key.attack"), Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_hint", Component.keybind(ReloadKey)).withStyle(ChatFormatting.GRAY));
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
