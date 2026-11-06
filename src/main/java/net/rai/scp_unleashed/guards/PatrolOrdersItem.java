package net.rai.scp_unleashed.guards;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.security.TurretCategory;

import java.util.List;

public class PatrolOrdersItem extends Item {
    public static final String Prefix = "message.scp_unleashed.patrol_orders.";

    public PatrolOrdersItem(Properties Settings) {
        super(Settings);
    }

    public static boolean isWritten(ItemStack Stack) {
        return Stack.has(GuardsRegistry.Route.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Player User = Context.getPlayer();
        if (User == null || !(World.getBlockEntity(Pos) instanceof PatrolManagerBlockEntity Manager)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Vec3 At = Vec3.atCenterOf(Pos);
        if (!Manager.canIssue(User)) {
            Manager.denied(User);
            GuardSounds.play(World, At, GuardSounds.Refused, SoundSource.BLOCKS, 0.6f, 1.0f);
            return InteractionResult.FAIL;
        }
        PatrolRoute Route = Manager.route();
        if (Route.isEmpty()) {
            tell(User, "empty", ChatFormatting.RED);
            GuardSounds.play(World, At, GuardSounds.Refused, SoundSource.BLOCKS, 0.6f, 1.0f);
            return InteractionResult.FAIL;
        }
        ItemStack Stack = Context.getItemInHand();
        if (Stack.getCount() > 1 && !User.getAbilities().instabuild) {
            ItemStack Written = Stack.split(1);
            Written.set(GuardsRegistry.Route.get(), Route);
            if (!User.addItem(Written)) {
                User.drop(Written, false);
            }
        } else if (Stack.getCount() > 1) {
            ItemStack Written = Stack.copyWithCount(1);
            Written.set(GuardsRegistry.Route.get(), Route);
            if (!User.addItem(Written)) {
                User.drop(Written, false);
            }
        } else {
            Stack.set(GuardsRegistry.Route.get(), Route);
        }
        Manager.noteIssued();
        tell(User, Route.single() ? "written_post" : "written", ChatFormatting.GREEN, Route.size());
        GuardSounds.play(World, At, GuardSounds.Written, SoundSource.BLOCKS, 0.7f, 1.0f);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack Stack, Player User, LivingEntity Target, InteractionHand Hand) {
        if (!(Target instanceof DClass Guard) || !Guard.isAlive()) {
            return InteractionResult.PASS;
        }
        if (User.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        PatrolRoute Route = Stack.get(GuardsRegistry.Route.get());
        Vec3 At = Guard.getEyePosition();
        if (Route == null || User.isSecondaryUseActive()) {
            if (GuardDuty.clear(Guard)) {
                tell(User, "cleared", ChatFormatting.YELLOW, Guard.getDisplayName());
                GuardSounds.play(User.level(), At, GuardSounds.Cleared, SoundSource.NEUTRAL, 0.7f, 1.0f);
            } else {
                tell(User, "no_orders", ChatFormatting.GRAY, Guard.getDisplayName());
            }
            return InteractionResult.CONSUME;
        }
        GuardDuty.assign(Guard, Route);
        boolean Here = Guard.level().dimension().equals(Route.Dimension());
        tell(User, !Here ? "assigned_elsewhere" : Route.single() ? "assigned_post" : "assigned", ChatFormatting.GREEN, Guard.getDisplayName(), Route.size());
        GuardSounds.play(User.level(), At, GuardSounds.Assigned, SoundSource.NEUTRAL, 0.7f, 1.0f);
        return InteractionResult.CONSUME;
    }

    private static void tell(Player User, String Key, ChatFormatting Colour, Object... Arguments) {
        User.displayClientMessage(Component.translatable(Prefix + Key, Arguments).withStyle(Colour), true);
    }

    public static Component threats(GuardRules Rules) {
        MutableComponent Listed = null;
        for (TurretCategory Category : TurretCategory.values()) {
            if (!Rules.has(Category)) {
                continue;
            }
            Component Name = Component.translatable("gui.scp_unleashed.patrol_manager.category." + Category.getName());
            Listed = Listed == null ? Name.copy() : Listed.append(Component.literal(", ")).append(Name);
        }
        return Listed == null ? Component.translatable("gui.scp_unleashed.patrol_manager.threats.none") : Listed;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PatrolRoute Route = Stack.get(GuardsRegistry.Route.get());
        if (Route == null) {
            Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.blank").withStyle(ChatFormatting.GRAY));
            if (ItemTooltips.showDetails(Lines)) {
                Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.hint.write").withStyle(ChatFormatting.DARK_GRAY));
                Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.hint.clear").withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        String ModeKey = Route.single() ? "post" : Route.PingPong() ? "ping_pong" : "loop";
        Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.route", Route.size(), Component.translatable("gui.scp_unleashed.patrol_manager.mode." + ModeKey))
                .withStyle(ChatFormatting.GOLD));
        GuardRules Rules = Route.Rules();
        Lines.add(Component.translatable(Rules.HoldFire() ? "gui.scp_unleashed.patrol_manager.fire.hold" : "gui.scp_unleashed.patrol_manager.fire.free")
                .withStyle(Rules.HoldFire() ? ChatFormatting.YELLOW : ChatFormatting.RED));
        Lines.add(Component.translatable(Rules.Melee() ? "gui.scp_unleashed.patrol_manager.melee.on" : "gui.scp_unleashed.patrol_manager.melee.off")
                .withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.threats", threats(Rules)).withStyle(ChatFormatting.GRAY));
        if (!Route.OwnerName().isEmpty()) {
            Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.issuer", Route.OwnerName()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.hint.assign").withStyle(ChatFormatting.DARK_GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.patrol_orders.hint.withdraw").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
