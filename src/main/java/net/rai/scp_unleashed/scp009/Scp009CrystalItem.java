package net.rai.scp_unleashed.scp009;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp009CrystalItem extends Item {
    public static final int LooseLifespan = Integer.MAX_VALUE;
    private final RedIceGrade Grade;

    public Scp009CrystalItem(RedIceGrade Grade, Properties Settings) {
        super(Settings);
        this.Grade = Grade;
    }

    public RedIceGrade getGrade() {
        return Grade;
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp009CrystalItem;
    }

    public static boolean isTossed(ItemEntity Drop) {
        return Drop.getOwner() instanceof Player;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Owner, int Slot, boolean Selected) {
        if (!World.isClientSide && Owner instanceof LivingEntity Bearer) {
            Scp009Doom.touch(Bearer);
            Scp009Sources.carrier(Bearer);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Drop) {
        if (!(Drop.level() instanceof ServerLevel World) || Drop.isRemoved() || Stack.isEmpty()) {
            return false;
        }
        if (Drop.isInWater() && isTossed(Drop) && RedIceGrowth.start(World, Drop.position(), Grade)) {
            if (Stack.getCount() <= 1) {
                Drop.discard();
                return true;
            }
            Drop.setItem(Stack.copyWithCount(Stack.getCount() - 1));
            return false;
        }
        if (Math.floorMod(World.getGameTime() + Drop.getId(), (long) Scp009Exposure.Interval) == 0L) {
            Scp009Sources.offer(World, Drop.position().add(0.0, 0.3, 0.0));
            World.sendParticles(Scp009Registry.Glint.get(), Drop.getX(), Drop.getY() + 0.35, Drop.getZ(), 1, 0.12, 0.12, 0.12, 0.01);
        }
        return false;
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return Grade.isUnique() ? LooseLifespan : super.getEntityLifespan(Stack, World);
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return !Grade.isUnique() || Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Key = getDescriptionId();
        Lines.add(Component.translatable(Key + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp009.touch").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("item.scp_unleashed.scp009.hazmat").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp009.storage").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp009.uncontained", (int) Scp009Exposure.Radius).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp009.loose").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp009.water", Grade.getMaxBlocks()).withStyle(ChatFormatting.GOLD));
        }
    }
}
