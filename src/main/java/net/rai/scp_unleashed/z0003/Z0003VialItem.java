package net.rai.scp_unleashed.z0003;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.joml.Vector3f;

import java.util.List;

public class Z0003VialItem extends Item {
    public static final Vector3f Fluid = new Vector3f(0.98f, 0.84f, 0.22f);
    public static final float PourVolume = 0.8f;
    public static final int CooldownTicks = 20;
    public static final int TakeCooldownTicks = 10;

    public Z0003VialItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Z0003VialItem;
    }

    public static ItemStack create(int Count) {
        return new ItemStack(Z0003Registry.Vial.get(), Count);
    }

    public static InteractionResult pour(Player User, InteractionHand Hand, LivingEntity Target) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!is(Stack) || !Z0003Petrify.isStatue(Target)) {
            return InteractionResult.PASS;
        }
        Z0003Statue Statue = Z0003Petrify.statueOf(Target);
        if (Statue == null || Statue.cracking() || User.getCooldowns().isOnCooldown(Stack.getItem())) {
            return InteractionResult.FAIL;
        }
        if (!(User.level() instanceof ServerLevel World)) {
            return InteractionResult.SUCCESS;
        }
        if (!Z0003Petrify.crack(Target)) {
            return InteractionResult.FAIL;
        }
        AABB Box = Target.getBoundingBox();
        World.sendParticles(new DustParticleOptions(Fluid, 1.0f), Box.getCenter().x, Box.maxY - 0.1, Box.getCenter().z, 18, Box.getXsize() * 0.3, 0.15,
                Box.getZsize() * 0.3, 0.0);
        Z0003Registry.play(World, Target.getBoundingBox().getCenter(), Z0003Registry.VialLayers, SoundSource.PLAYERS, PourVolume, 1.0f);
        User.awardStat(Stats.ITEM_USED.get(Stack.getItem()));
        User.getCooldowns().addCooldown(Stack.getItem(), CooldownTicks);
        User.swing(Hand, true);
        return InteractionResult.CONSUME;
    }

    public static int carriedSlot(Player Holder) {
        Inventory Pack = Holder.getInventory();
        for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
            if (is(Pack.getItem(Slot))) {
                return Slot;
            }
        }
        return -1;
    }

    public static boolean carried(Player Holder) {
        return carriedSlot(Holder) >= 0;
    }

    public static InteractionResult takeFrom(Player User, InteractionHand Hand, Player Holder) {
        if (User == Holder || User.isSpectator() || Z0003Petrify.isStatue(User) || !Z0003Petrify.isStatue(Holder) || !(User.level() instanceof ServerLevel World)
                || User.getCooldowns().isOnCooldown(Z0003Registry.Vial.get())) {
            return InteractionResult.PASS;
        }
        int Slot = carriedSlot(Holder);
        if (Slot < 0) {
            return InteractionResult.PASS;
        }
        Inventory Pack = Holder.getInventory();
        ItemStack Taken = Pack.removeItem(Slot, 1);
        if (Taken.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (User.getItemInHand(Hand).isEmpty()) {
            User.setItemInHand(Hand, Taken);
        } else if (!User.getInventory().add(Taken)) {
            if (Pack.getItem(Slot).isEmpty()) {
                Pack.setItem(Slot, Taken);
            } else {
                Pack.getItem(Slot).grow(Taken.getCount());
            }
            return InteractionResult.FAIL;
        }
        Pack.setChanged();
        User.getCooldowns().addCooldown(Z0003Registry.Vial.get(), TakeCooldownTicks);
        Z0003Petrify.prise(World, Holder);
        User.swing(Hand, true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        return pour(User, Hand, Target);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Prefix = getDescriptionId();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable(Prefix + ".detail.use", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + ".detail.endless").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
