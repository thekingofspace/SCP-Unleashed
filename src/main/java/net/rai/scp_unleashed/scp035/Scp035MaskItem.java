package net.rai.scp_unleashed.scp035;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.Optional;

public class Scp035MaskItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp035_mask.";
    public static final double SideInset = 0.3;

    public Scp035MaskItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.getItem() instanceof Scp035MaskItem;
    }

    public static ItemStack fresh() {
        return new ItemStack(Scp035Registry.MaskItem.get());
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack Stack) {
        return EquipmentSlot.HEAD;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack Stack) {
        return false;
    }

    @Override
    public int getEnchantmentLevel(ItemStack Stack, Holder<Enchantment> Wanted) {
        int Found = super.getEnchantmentLevel(Stack, Wanted);
        return Wanted.is(Enchantments.BINDING_CURSE) ? Math.max(1, Found) : Found;
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack Stack, HolderLookup.RegistryLookup<Enchantment> Lookup) {
        ItemEnchantments Base = super.getAllEnchantments(Stack, Lookup);
        Optional<Holder.Reference<Enchantment>> Binding = Lookup.get(Enchantments.BINDING_CURSE);
        if (Binding.isEmpty() || Base.getLevel(Binding.get()) > 0) {
            return Base;
        }
        ItemEnchantments.Mutable Bound = new ItemEnchantments.Mutable(Base);
        Bound.set(Binding.get(), 1);
        return Bound.toImmutable();
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User == null || !User.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level World = Context.getLevel();
        Direction Side = Context.getClickedFace();
        BlockPos Clicked = Context.getClickedPos();
        Vec3 Hit = Context.getClickLocation();
        Vec3 Spot;
        if (Side == Direction.UP) {
            Spot = Hit;
        } else {
            BlockPos Cell = Clicked.relative(Side);
            double X = Side.getAxis() == Direction.Axis.X ? Cell.getX() + 0.5 - Side.getStepX() * SideInset : Hit.x;
            double Z = Side.getAxis() == Direction.Axis.Z ? Cell.getZ() + 0.5 - Side.getStepZ() * SideInset : Hit.z;
            Spot = new Vec3(X, Side == Direction.DOWN ? Cell.getY() : Math.max(Cell.getY(), Hit.y - 0.25), Z);
        }
        if (!World.noCollision(Scp035Registry.Mask.get().getDimensions().makeBoundingBox(Spot))) {
            return InteractionResult.FAIL;
        }
        if (World instanceof ServerLevel Server) {
            Scp035Mask.place(Server, Spot, User.getYRot(), true);
            Context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Prefix + "fatal").withStyle(ChatFormatting.DARK_RED));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "gaze", (int) Scp035Lure.PullRange).withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable(Prefix + "bind").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable(Prefix + "carry").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "set").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "contain").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
