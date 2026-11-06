package net.rai.scp_unleashed.scp005;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp005KeyItem extends Item {
    public static final float TurnTicks = 14.0f;
    private static long TurnStart = Long.MIN_VALUE / 4;
    private static InteractionHand TurnHand = InteractionHand.MAIN_HAND;
    private static boolean TurnPushes;

    public Scp005KeyItem(Properties Properties) {
        super(Properties);
    }

    private static void startTurn(Level World, InteractionHand Hand, boolean Pushes) {
        if (World.isClientSide) {
            TurnStart = World.getGameTime();
            TurnHand = Hand;
            TurnPushes = Pushes;
        }
    }

    public static float turnProgress(long Now, float PartialTick) {
        return (Now - TurnStart + PartialTick) / TurnTicks;
    }

    public static InteractionHand turnHand() {
        return TurnHand;
    }

    public static boolean turnPushes() {
        return TurnPushes;
    }

    public static HoldTime holdOf(ItemStack Stack) {
        return Stack.getOrDefault(Scp005Registry.HoldSetting.get(), HoldTime.Default);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = World.getBlockState(Pos);
        KeyHoldable Door = Scp005Doors.find(State);
        if (Door == null) {
            return InteractionResult.PASS;
        }
        Player User = Context.getPlayer();
        if (User != null && User.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.CONSUME;
        }
        startTurn(World, Context.getHand(), true);
        if (!(World instanceof ServerLevel Server)) {
            return InteractionResult.SUCCESS;
        }
        Direction Face = Context.getClickedFace();
        Vec3 Lock = Context.getClickLocation().add(Face.getStepX() * 0.05, Face.getStepY() * 0.05, Face.getStepZ() * 0.05);
        InteractionResult Result = Scp005Doors.use(Server, Pos, State, Door, holdOf(Stack), User, Lock);
        if (Result == InteractionResult.SUCCESS && User != null) {
            User.getCooldowns().addCooldown(this, Scp005Doors.CooldownTicks);
        }
        return Result;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User == null || !User.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        cycle(Context.getLevel(), User, Context.getItemInHand(), Context.getHand());
        return InteractionResult.sidedSuccess(Context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!User.isSecondaryUseActive()) {
            return InteractionResultHolder.pass(Stack);
        }
        cycle(World, User, Stack, Hand);
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    private static void cycle(Level World, Player User, ItemStack Stack, InteractionHand Hand) {
        if (World.isClientSide) {
            startTurn(World, Hand, false);
            return;
        }
        HoldTime Next = holdOf(Stack).next();
        Stack.set(Scp005Registry.HoldSetting.get(), Next);
        World.playSound(null, User.getX(), User.getY() + User.getEyeHeight() * 0.8, User.getZ(), Scp005Registry.KeyCycle.get(), SoundSource.PLAYERS, 0.5f, 0.85f + Next.ordinal() * 0.07f);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp005_key.hold", Next.describe()).withStyle(ChatFormatting.AQUA), true);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp005_key.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable("item.scp_unleashed.scp005_key.hold", holdOf(Stack).describe().copy().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp005_key.use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp005_key.bypass").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp005_key.refresh").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp005_key.cycle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp005_key.release").withStyle(ChatFormatting.GOLD));
        }
    }
}
