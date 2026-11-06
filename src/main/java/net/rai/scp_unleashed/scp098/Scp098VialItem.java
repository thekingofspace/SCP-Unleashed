package net.rai.scp_unleashed.scp098;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp098VialItem extends Item {
    public static final String Key = "item.scp_unleashed.scp098_sand_vial.";

    public Scp098VialItem(Properties Properties) {
        super(Properties);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.is(Scp098Registry.Vial.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState Target = World.getBlockState(Pos);
        if (!Scp098HiveBlockEntity.isSand(Target)) {
            return InteractionResult.PASS;
        }
        Player User = Context.getPlayer();
        if (User != null && !User.mayUseItemAt(Pos, Context.getClickedFace(), Context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (!(World instanceof ServerLevel Server)) {
            return InteractionResult.SUCCESS;
        }
        Server.setBlock(Pos, Scp098Registry.Hive.get().defaultBlockState(), Block.UPDATE_ALL);
        Server.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        Vec3 Top = Vec3.atCenterOf(Pos).add(0.0, 0.55, 0.0);
        float Pitch = 0.9f + Server.getRandom().nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp098Registry.VialLayers) {
            Server.playSound(null, Top.x, Top.y, Top.z, Layer.get(), SoundSource.PLAYERS, 0.9f, Pitch);
        }
        Server.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Target), Top.x, Top.y + 0.2, Top.z, 14, 0.25, 0.1, 0.25, 0.0);
        Server.sendParticles(Scp098Registry.Spore.get(), Top.x, Top.y, Top.z, 4, 0.2, 0.05, 0.2, 0.005);
        if (User == null || !User.getAbilities().instabuild) {
            Context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "warning").withStyle(ChatFormatting.GOLD));
        }
    }
}
