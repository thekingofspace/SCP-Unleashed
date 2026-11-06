package net.rai.scp_unleashed.decal;

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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.rai.scp_unleashed.block.DecoratableBlock;
import net.rai.scp_unleashed.block.FaceLayer;
import net.rai.scp_unleashed.block.entity.RenderableBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class DecalItem extends Item {
    public static final int MaxLayersPerFace = 4;
    private static final int PlaceParticles = 8;

    private final DecalKind Kind;

    public DecalItem(DecalKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
    }

    public DecalKind getKind() {
        return Kind;
    }

    public static int getVariant(ItemStack Stack, DecalKind Kind) {
        int Variant = Stack.getOrDefault(DecalRegistry.DecalVariant.get(), 0);
        return Variant <= Kind.getVariants() ? Variant : 0;
    }

    public static int layerCount(Level World, BlockPos Pos, Direction Face) {
        return World.getBlockEntity(Pos) instanceof RenderableBlockEntity Entity ? Entity.getLayers(Face).size() : 0;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Direction Face = Context.getClickedFace();
        Player User = Context.getPlayer();
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof DecoratableBlock)) {
            if (!World.isClientSide && User != null) {
                message(User, "wont_stick");
            }
            return InteractionResult.PASS;
        }
        if (layerCount(World, Pos, Face) >= MaxLayersPerFace) {
            if (!World.isClientSide && User != null) {
                message(User, "face_full", MaxLayersPerFace);
            }
            return InteractionResult.FAIL;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack Stack = Context.getItemInHand();
        int Chosen = getVariant(Stack, Kind);
        int Variant = Chosen > 0 ? Chosen : 1 + World.random.nextInt(Kind.getVariants());
        FaceLayer Layer = Kind.layer(Variant, Face, Context.getHorizontalDirection(), World.random);
        if (!DecoratableBlock.addDecal(World, Pos, Face, Layer)) {
            return InteractionResult.FAIL;
        }
        if (User == null || !User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        World.playSound(null, Pos, Kind.placeSound(), SoundSource.BLOCKS, 0.8f, 0.9f + World.random.nextFloat() * 0.2f);
        World.gameEvent(GameEvent.BLOCK_CHANGE, Pos, GameEvent.Context.of(User, World.getBlockState(Pos)));
        if (World instanceof ServerLevel Server) {
            DecalEffects.burst(Server, Pos, Face, Kind.particle(State), PlaceParticles, 0.01);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (!User.isShiftKeyDown() || getPlayerPOVHitResult(World, User, ClipContext.Fluid.NONE).getType() != HitResult.Type.MISS) {
            return InteractionResultHolder.pass(Stack);
        }
        if (!World.isClientSide) {
            int Next = (getVariant(Stack, Kind) + 1) % (Kind.getVariants() + 1);
            if (Next == 0) {
                Stack.remove(DecalRegistry.DecalVariant.get());
            } else {
                Stack.set(DecalRegistry.DecalVariant.get(), Next);
            }
            User.displayClientMessage(Kind.variantLine(Next), true);
            User.playNotifySound(DecalRegistry.DecalCycle.get(), SoundSource.PLAYERS, 0.7f, 0.85f + Next * 0.08f);
        }
        return InteractionResultHolder.consume(Stack);
    }

    private static void message(Player User, String Key, Object... Arguments) {
        User.displayClientMessage(Component.translatable("message.scp_unleashed.decal." + Key, Arguments), true);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.decal.tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Kind.variantLine(getVariant(Stack, Kind)).copy().withStyle(ChatFormatting.GOLD));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.decal.apply").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.decal.cycle").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.decal.limit", MaxLayersPerFace).withStyle(ChatFormatting.GRAY));
        }
    }
}
