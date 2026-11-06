package net.rai.scp_unleashed.z0003;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbility;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.z0003.client.Z0003HandSync;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Z0003DeviceItem extends TridentItem {
    public static final int UseTicks = 72000;
    public static final int MinChargeTicks = TridentItem.THROW_THRESHOLD_TIME;
    public static final int FullChargeTicks = 20;
    public static final float ThrowVolume = 0.9f;

    public Z0003DeviceItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Z0003DeviceItem;
    }

    public static ItemStack create() {
        return new ItemStack(Z0003Registry.Device.get());
    }

    @Nullable
    public static Z0003Arming armingOf(ItemStack Stack) {
        return is(Stack) ? Stack.get(Z0003Registry.Arming.get()) : null;
    }

    public static boolean isArmed(ItemStack Stack) {
        return armingOf(Stack) != null;
    }

    public static void arm(ItemStack Stack, Z0003Arming Setting) {
        Stack.set(Z0003Registry.Arming.get(), Setting);
    }

    public static boolean disarm(ItemStack Stack) {
        return Stack.remove(Z0003Registry.Arming.get()) != null;
    }

    public static float charge(int HeldTicks) {
        return Mth.clamp((HeldTicks - MinChargeTicks) / (float) (FullChargeTicks - MinChargeTicks), 0.0f, 1.0f);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        User.startUsingItem(Hand);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseTicks;
    }

    @Override
    public void releaseUsing(ItemStack Stack, Level World, LivingEntity User, int TimeLeft) {
        if (!(User instanceof Player Thrower)) {
            return;
        }
        InteractionHand Hand = Thrower.getUsedItemHand();
        int Held = getUseDuration(Stack, User) - TimeLeft;
        if (Held < MinChargeTicks) {
            if (Thrower instanceof ServerPlayer Server) {
                resend(Server, Hand);
            }
            return;
        }
        if (World instanceof ServerLevel Server) {
            InteractionHand Other = Hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack Lead = Thrower.getItemInHand(Other);
            Z0003Device Thrown = Z0003Device.thrown(Server, Thrower, Stack, charge(Held));
            if (Lead.is(Items.LEAD)) {
                boolean Spent = !Thrower.getAbilities().instabuild;
                Thrown.tether(Thrower, Spent);
                if (Spent) {
                    Lead.shrink(1);
                }
            }
            Z0003Registry.play(Server, Thrower.position(), Z0003Registry.ThrowLayers, SoundSource.PLAYERS, ThrowVolume, 0.9f + Server.random.nextFloat() * 0.2f);
            Thrower.awardStat(Stats.ITEM_USED.get(this));
        }
        if (Thrower.getAbilities().instabuild) {
            return;
        }
        spend(Thrower, Hand, Stack);
        if (Thrower instanceof ServerPlayer Server) {
            Server.inventoryMenu.broadcastChanges();
        } else if (World.isClientSide && Thrower.isLocalPlayer()) {
            Z0003HandSync.emptied();
        }
    }

    public static int handSlot(Player Holder, InteractionHand Hand) {
        return Hand == InteractionHand.MAIN_HAND ? InventoryMenu.USE_ROW_SLOT_START + Holder.getInventory().selected : InventoryMenu.SHIELD_SLOT;
    }

    private static void spend(Player Thrower, InteractionHand Hand, ItemStack Stack) {
        ItemStack Current = Thrower.getItemInHand(Hand);
        ItemStack Gone = is(Current) ? Current : Stack;
        Thrower.getInventory().removeItem(Gone);
        if (Thrower.level().isClientSide) {
            Gone.setCount(0);
        }
    }

    private static void resend(ServerPlayer Holder, InteractionHand Hand) {
        Holder.inventoryMenu.setRemoteSlot(handSlot(Holder, Hand), ItemStack.EMPTY);
        Holder.inventoryMenu.broadcastChanges();
    }

    @Override
    public boolean canAttackBlock(BlockState State, Level World, BlockPos Spot, Player User) {
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack Stack, LivingEntity Target, LivingEntity Attacker) {
        return false;
    }

    @Override
    public void postHurtEnemy(ItemStack Stack, LivingEntity Target, LivingEntity Attacker) {
    }

    @Override
    public int getEnchantmentValue() {
        return 0;
    }

    @Override
    public boolean canPerformAction(ItemStack Stack, ItemAbility Ability) {
        return false;
    }

    @Override
    public Projectile asProjectile(Level World, Position Spot, ItemStack Stack, Direction Facing) {
        return Z0003Device.loose(World, new Vec3(Spot.x(), Spot.y(), Spot.z()), Vec3.ZERO, null, Stack);
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World instanceof ServerLevel Server) {
            releaseFrom(Server, Holder, Stack);
        }
    }

    @Nullable
    public static Z0003Device releaseFrom(ServerLevel Server, Entity Holder, ItemStack Stack) {
        Z0003Arming Setting = armingOf(Stack);
        if (Setting == null || Stack.isEmpty() || !Setting.due(Server.getGameTime())) {
            return null;
        }
        List<EquipmentSlot> Worn = new ArrayList<>();
        if (Holder instanceof LivingEntity Living && !(Holder instanceof Player)) {
            for (EquipmentSlot Each : EquipmentSlot.values()) {
                if (Living.getItemBySlot(Each) == Stack) {
                    Worn.add(Each);
                }
            }
        }
        ItemStack Released = Stack.copyWithCount(1);
        Stack.shrink(1);
        Vec3 Look = Holder.getLookAngle();
        Vec3 Spot = Holder.getEyePosition().add(Look.x * 0.4, -0.45 - Z0003Device.Height * 0.5, Look.z * 0.4);
        Z0003Device Made = Z0003Device.released(Server, Spot, Vec3.ZERO, Holder, Released);
        if (Holder instanceof LivingEntity Living) {
            if (Living.isUsingItem() && Living.getUseItem() == Stack) {
                Living.stopUsingItem();
            }
            if (!Worn.isEmpty() && Stack.isEmpty()) {
                List<Pair<EquipmentSlot, ItemStack>> Cleared = new ArrayList<>();
                for (EquipmentSlot Each : Worn) {
                    Living.setItemSlot(Each, ItemStack.EMPTY);
                    Cleared.add(Pair.of(Each, ItemStack.EMPTY));
                }
                Server.getChunkSource().broadcast(Living, new ClientboundSetEquipmentPacket(Living.getId(), Cleared));
            }
        }
        Made.activate();
        return Made;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (!(Dropped.level() instanceof ServerLevel Server)) {
            return false;
        }
        Z0003Arming Setting = armingOf(Stack);
        if (Setting == null || !Setting.due(Server.getGameTime())) {
            return false;
        }
        Z0003Device Made = Z0003Device.released(Server, Dropped.position(), Dropped.getDeltaMovement(), Dropped.getOwner(), Stack);
        if (!Dropped.onGround()) {
            Made.setDeltaMovement(Vec3.ZERO);
        }
        Made.activate();
        Dropped.discard();
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack Old, ItemStack Fresh, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(Old, Fresh);
    }

    @Override
    public boolean canContinueUsing(ItemStack Old, ItemStack Fresh) {
        return ItemStack.isSameItem(Old, Fresh);
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return !Source.is(DamageTypeTags.IS_FIRE) && !Source.is(DamageTypeTags.IS_EXPLOSION);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Prefix = getDescriptionId();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        Z0003Arming Setting = armingOf(Stack);
        if (Setting != null) {
            Lines.add(Component.translatable(Prefix + ".tooltip.armed").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        }
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable(Prefix + ".detail.throw", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + ".detail.pickup").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + ".detail.listen").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Prefix + ".detail.rope").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
