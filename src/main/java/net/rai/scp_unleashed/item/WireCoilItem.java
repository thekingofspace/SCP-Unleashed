package net.rai.scp_unleashed.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.registry.ModDataComponents;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.WireLink;
import net.rai.scp_unleashed.wire.WirePath;
import net.rai.scp_unleashed.wire.WireTier;
import net.rai.scp_unleashed.wire.Wires;

import java.util.List;
import java.util.Locale;

public class WireCoilItem extends Item {
    private final WireTier Tier;

    public WireCoilItem(WireTier Tier, Properties Properties) {
        super(Properties);
        this.Tier = Tier;
    }

    public WireTier getTier() {
        return Tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Player User = Context.getPlayer();
        BlockState State = World.getBlockState(Pos);
        if (User == null || !(State.getBlock() instanceof WireConnector Connector)) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            ItemStack Stack = Context.getItemInHand();
            Direction Face = Connector.linkFace(State, Context.getClickedFace());
            GlobalPos Start = Stack.get(ModDataComponents.WireStart.get());
            if (Start == null) {
                if (!Connector.getConnectorTier().accepts(Tier)) {
                    message(User, refusal());
                    return InteractionResult.FAIL;
                }
                Stack.set(ModDataComponents.WireStart.get(), GlobalPos.of(World.dimension(), Pos.immutable()));
                Stack.set(ModDataComponents.WireStartFace.get(), Face);
                message(User, "started");
                Wires.playSound(World, Pos, ModSounds.WireAttach.get());
            } else {
                Direction StartFace = Stack.getOrDefault(ModDataComponents.WireStartFace.get(), Direction.UP);
                Stack.remove(ModDataComponents.WireStart.get());
                Stack.remove(ModDataComponents.WireStartFace.get());
                link(World, User, Start, StartFace, Pos, Face);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User.isShiftKeyDown() && Stack.has(ModDataComponents.WireStart.get())) {
            if (!World.isClientSide) {
                Stack.remove(ModDataComponents.WireStart.get());
                Stack.remove(ModDataComponents.WireStartFace.get());
                message(User, "cancelled");
                Wires.playSound(World, User.blockPosition(), ModSounds.WireDisconnect.get());
            }
            return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
        }
        return InteractionResultHolder.pass(Stack);
    }

    private void link(Level World, Player User, GlobalPos Start, Direction StartFace, BlockPos End, Direction EndFace) {
        if (!Start.dimension().equals(World.dimension())) {
            message(User, "wrong_dimension");
            return;
        }
        if (Start.pos().equals(End)) {
            message(User, "cancelled");
            Wires.playSound(World, End, ModSounds.WireDisconnect.get());
            return;
        }
        if (!World.isLoaded(Start.pos()) || !(World.getBlockEntity(Start.pos()) instanceof OutletBlockEntity First)) {
            message(User, "start_missing");
            return;
        }
        if (!(World.getBlockEntity(End) instanceof OutletBlockEntity Second)) {
            return;
        }
        WireLink Existing = First.linkTo(End);
        if (Existing != null) {
            int Refund = Wires.coilCost(First.attachFor(Existing).distanceTo(Existing.targetAttach()));
            First.removeLink(End);
            Second.removeLink(Start.pos());
            refund(User, Existing.Tier(), Refund);
            message(User, "unlinked");
            Wires.playSound(World, End, ModSounds.WireDisconnect.get());
            return;
        }
        if (!First.getConnectorTier().accepts(Tier) || !Second.getConnectorTier().accepts(Tier)) {
            message(User, refusal());
            return;
        }
        Direction FromFace = Wires.settledFace(First, StartFace, Second.attachAt(EndFace));
        Direction ToFace = Wires.settledFace(Second, EndFace, First.attachAt(FromFace));
        FromFace = Wires.settledFace(First, FromFace, Second.attachAt(ToFace));
        Vec3 From = First.attachAt(FromFace);
        Vec3 To = Second.attachAt(ToFace);
        double Length = From.distanceTo(To);
        int Cost = Wires.coilCost(Length);
        if (Length > Tier.getMaxLength()) {
            message(User, "too_long", formatLength(Length), (int) Tier.getMaxLength());
            return;
        }
        if (!First.hasFreeLink() || !Second.hasFreeLink()) {
            message(User, "full", Wires.MaxLinksPerOutlet);
            return;
        }
        if (WirePath.firstObstruction(World, From, To, Start.pos(), End) != null) {
            message(User, "blocked");
            return;
        }
        if (!User.getAbilities().instabuild) {
            Inventory Pockets = User.getInventory();
            if (Pockets.clearOrCountMatchingItems(Candidate -> Candidate.is(this), 0, User.inventoryMenu.getCraftSlots()) < Cost) {
                message(User, "need_coils", Cost, getDescription());
                return;
            }
            Pockets.clearOrCountMatchingItems(Candidate -> Candidate.is(this), Cost, User.inventoryMenu.getCraftSlots());
        }
        Direction FirstFace = First.isFullBlock() ? FromFace : First.getFacing();
        Direction SecondFace = Second.isFullBlock() ? ToFace : Second.getFacing();
        int Slot = WireLink.freeSlot(First.getLinks(), Second.getLinks());
        First.addLink(new WireLink(End.immutable(), SecondFace, Slot, Tier, Second.isFullBlock(), FirstFace));
        Second.addLink(new WireLink(Start.pos(), FirstFace, Slot, Tier, First.isFullBlock(), SecondFace));
        message(User, "linked", formatLength(Length), Cost, getDescription());
        Wires.playSound(World, End, ModSounds.WireConnect.get());
    }

    private static void refund(Player User, WireTier Kind, int Count) {
        if (User.getAbilities().instabuild || Count <= 0) {
            return;
        }
        ItemStack Refund = new ItemStack(Kind.getCoil(), Count);
        if (!User.getInventory().add(Refund)) {
            User.drop(Refund, false);
        }
    }

    private String refusal() {
        return ConnectorTier.Ultimate.accepts(Tier) ? "needs_connector" : "needs_tether";
    }

    private static String formatLength(double Length) {
        return String.format(Locale.ROOT, "%.1f", Length);
    }

    private static void message(Player User, String Key, Object... Arguments) {
        User.displayClientMessage(Component.translatable("message.scp_unleashed.wire." + Key, Arguments), true);
    }

    @Override
    public boolean isFoil(ItemStack Stack) {
        return Stack.has(ModDataComponents.WireStart.get()) || super.isFoil(Stack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.wire_coil.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.wire_coil.coil_cost", Wires.BlocksPerCoil, (int) Tier.getMaxLength()).withStyle(ChatFormatting.GRAY));
            Lines.add(Tier == WireTier.Creative ? Component.translatable("item.scp_unleashed.wire_coil.unlimited").withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.translatable("item.scp_unleashed.wire_coil.max_demand", Tier.getMaxDemand()).withStyle(ChatFormatting.GOLD));
            if (!ConnectorTier.Ultimate.accepts(Tier)) {
                Lines.add(Component.translatable("item.scp_unleashed.wire_coil.tether").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else if (!ConnectorTier.Basic.accepts(Tier)) {
                Lines.add(Component.translatable("item.scp_unleashed.wire_coil.heavy").withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
