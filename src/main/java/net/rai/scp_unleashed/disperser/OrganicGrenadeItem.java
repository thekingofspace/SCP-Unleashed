package net.rai.scp_unleashed.disperser;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.grenade.GrenadeItem;
import net.rai.scp_unleashed.grenade.GrenadeKind;
import net.rai.scp_unleashed.grenade.GrenadeRegistry;
import net.rai.scp_unleashed.grenade.GrenadeSounds;
import net.rai.scp_unleashed.grenade.GrenadeTuning;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.labware.LabOrganisms;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OrganicGrenadeItem extends GrenadeItem {
    public static final int EmptyTint = 0x8E9A9C;

    public OrganicGrenadeItem(Properties Settings) {
        super(GrenadeKind.Gas, Settings);
    }

    @Nullable
    public static ResourceLocation cultureOf(ItemStack Stack) {
        return Stack.get(DisperserRegistry.Culture.get());
    }

    public static boolean isLoaded(ItemStack Stack) {
        return LabOrganisms.isKnown(cultureOf(Stack));
    }

    public static boolean isEmptyGrenade(ItemStack Stack) {
        return Stack.is(DisperserRegistry.OrganicGrenade.get()) && cultureOf(Stack) == null;
    }

    public static ItemStack loaded(@Nullable ResourceLocation Culture) {
        ItemStack Stack = new ItemStack(DisperserRegistry.OrganicGrenade.get());
        if (Culture != null) {
            Stack.set(DisperserRegistry.Culture.get(), Culture);
        }
        return Stack;
    }

    public static int tintOf(ItemStack Stack) {
        ResourceLocation Culture = cultureOf(Stack);
        return LabOrganisms.isKnown(Culture) ? LabOrganisms.vialColourOf(Culture) : EmptyTint;
    }

    @Override
    public void releaseUsing(ItemStack Stack, Level World, LivingEntity User, int TimeLeft) {
        float Charge = charge(getUseDuration(Stack, User) - TimeLeft);
        InteractionHand Hand = User.getUsedItemHand();
        if (World instanceof ServerLevel Server) {
            OrganicGrenadeEntity.thrown(Server, User, cultureOf(Stack), Charge);
            GrenadeSounds.thrown(Server, User, Charge);
        }
        User.swing(Hand);
        if (User instanceof Player Thrower) {
            Thrower.awardStat(Stats.ITEM_USED.get(this));
            for (GrenadeKind Each : GrenadeKind.values()) {
                Thrower.getCooldowns().addCooldown(GrenadeRegistry.grenadeItem(Each), GrenadeTuning.CooldownTicks);
            }
            Thrower.getCooldowns().addCooldown(this, GrenadeTuning.CooldownTicks);
        }
        Stack.consume(1, User);
    }

    @Override
    public Projectile asProjectile(Level World, Position Pos, ItemStack Stack, Direction Facing) {
        return OrganicGrenadeEntity.dispensed(World, Pos.x(), Pos.y(), Pos.z(), cultureOf(Stack));
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.organic_grenade.tooltip").withStyle(ChatFormatting.GRAY));
        ResourceLocation Culture = cultureOf(Stack);
        if (Culture == null) {
            Lines.add(Component.translatable("item.scp_unleashed.organic_grenade.empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Lines.add(Component.translatable("item.scp_unleashed.organic_grenade.culture", LabOrganisms.styledNameOf(Culture)).withStyle(ChatFormatting.GRAY));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.organic_grenade.detail").withStyle(ChatFormatting.GOLD));
            if (Culture == null) {
                Lines.add(Component.translatable("item.scp_unleashed.organic_grenade.load").withStyle(ChatFormatting.AQUA));
            }
            Lines.add(Component.translatable("tooltip.scp_unleashed.grenade.throw").withStyle(ChatFormatting.GRAY));
        }
    }
}
