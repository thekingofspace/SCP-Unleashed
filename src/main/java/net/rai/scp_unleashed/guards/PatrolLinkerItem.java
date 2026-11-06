package net.rai.scp_unleashed.guards;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

public class PatrolLinkerItem extends Item {
    public static final String Prefix = "message.scp_unleashed.patrol_linker.";

    public PatrolLinkerItem(Properties Settings) {
        super(Settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Player User = Context.getPlayer();
        ItemStack Stack = Context.getItemInHand();
        BlockState State = World.getBlockState(Pos);
        boolean Manager = State.is(GuardsRegistry.PatrolManager.get());
        boolean Node = State.is(GuardsRegistry.PatrolNode.get());
        if (!Manager && !Node || User == null) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Vec3 At = Vec3.atCenterOf(Pos);
        if (Manager) {
            if (!(World.getBlockEntity(Pos) instanceof PatrolManagerBlockEntity Terminal)) {
                return InteractionResult.FAIL;
            }
            if (!Terminal.canEdit(User)) {
                Terminal.denied(User);
                GuardSounds.play(World, At, GuardSounds.Refused, SoundSource.PLAYERS, 0.6f, 1.0f);
                return InteractionResult.FAIL;
            }
            Stack.set(GuardsRegistry.Route.get(), Terminal.route());
            tell(User, "bound", ChatFormatting.AQUA, Pos.getX(), Pos.getY(), Pos.getZ(), Terminal.getPoints().size());
            GuardSounds.play(World, At, GuardSounds.Bind, SoundSource.PLAYERS, 0.6f, 1.0f);
            return InteractionResult.CONSUME;
        }
        PatrolRoute Bound = Stack.get(GuardsRegistry.Route.get());
        if (Bound == null) {
            tell(User, "unbound", ChatFormatting.RED);
            GuardSounds.play(World, At, GuardSounds.Refused, SoundSource.PLAYERS, 0.6f, 1.0f);
            return InteractionResult.FAIL;
        }
        if (!World.dimension().equals(Bound.Dimension()) || !World.isLoaded(Bound.Manager())
                || !(World.getBlockEntity(Bound.Manager()) instanceof PatrolManagerBlockEntity Terminal) || !Terminal.getRouteId().equals(Bound.RouteId())) {
            tell(User, "missing", ChatFormatting.RED);
            GuardSounds.play(World, At, GuardSounds.Refused, SoundSource.PLAYERS, 0.6f, 1.0f);
            return InteractionResult.FAIL;
        }
        if (!Terminal.canEdit(User)) {
            Terminal.denied(User);
            return InteractionResult.FAIL;
        }
        PatrolManagerBlockEntity.LinkResult Result = Terminal.link(Pos, State.getValue(PatrolNodeBlock.Facing));
        Stack.set(GuardsRegistry.Route.get(), Terminal.route());
        int Count = Terminal.getPoints().size();
        switch (Result) {
            case Added -> {
                tell(User, "added", ChatFormatting.GREEN, Count);
                GuardSounds.play(World, At, GuardSounds.Linked, SoundSource.PLAYERS, 0.6f, 0.9f + Math.min(Count, 12) * 0.05f);
            }
            case Removed -> {
                tell(User, "removed", ChatFormatting.YELLOW, Count);
                GuardSounds.play(World, At, GuardSounds.Unlinked, SoundSource.PLAYERS, 0.6f, 1.0f);
            }
            case Full -> {
                tell(User, "full", ChatFormatting.RED, PatrolRoute.MaxPoints);
                GuardSounds.play(World, At, GuardSounds.Refused, SoundSource.PLAYERS, 0.6f, 1.0f);
            }
            case TooFar -> {
                tell(User, "too_far", ChatFormatting.RED, (int) PatrolManagerBlockEntity.LinkRange);
                GuardSounds.play(World, At, GuardSounds.Refused, SoundSource.PLAYERS, 0.6f, 1.0f);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!User.isSecondaryUseActive() || !Stack.has(GuardsRegistry.Route.get())) {
            return InteractionResultHolder.pass(Stack);
        }
        if (!World.isClientSide) {
            Stack.remove(GuardsRegistry.Route.get());
            tell(User, "cleared", ChatFormatting.GRAY);
            GuardSounds.play(World, User.position(), GuardSounds.Unlinked, SoundSource.PLAYERS, 0.5f, 0.9f);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    private static void tell(Player User, String Key, ChatFormatting Colour, Object... Arguments) {
        User.displayClientMessage(Component.translatable(Prefix + Key, Arguments).withStyle(Colour), true);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PatrolRoute Bound = Stack.get(GuardsRegistry.Route.get());
        Lines.add(Component.translatable("item.scp_unleashed.patrol_linker.tooltip").withStyle(ChatFormatting.GRAY));
        if (Bound == null) {
            Lines.add(Component.translatable("item.scp_unleashed.patrol_linker.unbound").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            BlockPos Pos = Bound.Manager();
            Lines.add(Component.translatable("item.scp_unleashed.patrol_linker.bound", Pos.getX(), Pos.getY(), Pos.getZ(), Bound.size()).withStyle(ChatFormatting.AQUA));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.patrol_linker.hint.bind").withStyle(ChatFormatting.DARK_GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.patrol_linker.hint.link").withStyle(ChatFormatting.DARK_GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.patrol_linker.hint.clear").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
