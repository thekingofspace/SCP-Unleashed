package net.rai.scp_unleashed.decal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.block.DecoratableBlock;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.ArrayList;
import java.util.List;

public class DecalScraperItem extends Item {
    public static final int Durability = 192;
    private static final int ScrapParticles = 6;
    private static final int DustParticles = 10;

    public DecalScraperItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Direction Face = Context.getClickedFace();
        Player User = Context.getPlayer();
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof DecoratableBlock)) {
            return InteractionResult.PASS;
        }
        boolean Everything = User != null && User.isShiftKeyDown();
        List<Direction> Faces = decoratedFaces(World, Pos, Face, Everything);
        if (Faces.isEmpty()) {
            if (!World.isClientSide && User != null) {
                User.displayClientMessage(Component.translatable(Everything ? "message.scp_unleashed.decal.nothing_on_block" : "message.scp_unleashed.decal.nothing_on_face"), true);
            }
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean Cleared = Everything ? DecoratableBlock.clearAllDecals(World, Pos) : DecoratableBlock.clearDecals(World, Pos, Face);
        if (!Cleared) {
            return InteractionResult.FAIL;
        }
        World.playSound(null, Pos, DecalRegistry.DecalScrape.get(), SoundSource.BLOCKS, 0.9f, 0.9f + World.random.nextFloat() * 0.2f);
        World.gameEvent(GameEvent.BLOCK_CHANGE, Pos, GameEvent.Context.of(User, State));
        if (World instanceof ServerLevel Server) {
            for (Direction Scraped : Faces) {
                DecalEffects.burst(Server, Pos, Scraped, new BlockParticleOption(ParticleTypes.BLOCK, State), DustParticles, 0.05);
                DecalEffects.burst(Server, Pos, Scraped, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), ScrapParticles, 0.06);
            }
        }
        if (User != null) {
            Context.getItemInHand().hurtAndBreak(Faces.size(), User, LivingEntity.getSlotForHand(Context.getHand()));
        }
        return InteractionResult.CONSUME;
    }

    private static List<Direction> decoratedFaces(Level World, BlockPos Pos, Direction Face, boolean Everything) {
        List<Direction> Faces = new ArrayList<>();
        for (Direction Side : Everything ? Direction.values() : new Direction[]{Face}) {
            if (DecalItem.layerCount(World, Pos, Side) > 0) {
                Faces.add(Side);
            }
        }
        return Faces;
    }

    @Override
    public boolean isValidRepairItem(ItemStack Stack, ItemStack Repair) {
        return Repair.is(Tags.Items.INGOTS_IRON) || super.isValidRepairItem(Stack, Repair);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.decal_scraper.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.decal_scraper.face").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.decal_scraper.block").withStyle(ChatFormatting.GRAY));
        }
    }
}
