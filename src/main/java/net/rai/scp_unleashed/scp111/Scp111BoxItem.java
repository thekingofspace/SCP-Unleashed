package net.rai.scp_unleashed.scp111;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.joml.Vector3f;

import java.util.List;

public class Scp111BoxItem extends Item {
    public static final String Key = "item.scp_unleashed.scp111_box";
    public static final int ConfettiPerVariant = 3;
    public static final float ConfettiSize = 0.6f;

    public Scp111BoxItem(Properties Properties) {
        super(Properties);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp111BoxItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World instanceof ServerLevel Server) {
            Scp111Clutch Inside = Scp111Clutch.of(Stack);
            for (ItemStack Eggs : Inside.eggStacks()) {
                ItemHandlerHelper.giveItemToPlayer(User, Eggs);
            }
            celebrate(Server, User);
            User.awardStat(Stats.ITEM_USED.get(this));
            Stack.consume(1, User);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    private static void celebrate(ServerLevel World, Player User) {
        Vec3 Front = User.getEyePosition().add(User.getLookAngle().scale(0.6)).add(0.0, -0.25, 0.0);
        RandomSource Random = World.random;
        for (Scp111Variant Variant : Scp111Variant.values()) {
            int Colour = Variant.getLabel();
            Vector3f Tint = new Vector3f((Colour >> 16 & 255) / 255.0f, (Colour >> 8 & 255) / 255.0f, (Colour & 255) / 255.0f);
            World.sendParticles(new DustParticleOptions(Tint, ConfettiSize), Front.x, Front.y, Front.z, ConfettiPerVariant, 0.25, 0.2, 0.25, 0.02);
        }
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), Front.x, Front.y, Front.z, 8, 0.15, 0.1, 0.15, 0.05);
        Scp111Snail.playLayers(World, Front, Scp111Registry.UnboxLayers, 0.9f, 0.95f + Random.nextFloat() * 0.1f);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Key + ".tagline").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        Scp111Clutch Inside = Scp111Clutch.of(Stack);
        Lines.add(Component.translatable(Key + ".count", Inside.size()).withStyle(ChatFormatting.GRAY));
        if (Inside.size() > 0 && Inside.Eggs().stream().anyMatch(Egg -> Egg.Remaining() < Scp111Egg.TotalTicks)) {
            Lines.add(Component.translatable(Key + ".warm").withStyle(ChatFormatting.GOLD));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".details.0").withStyle(ChatFormatting.YELLOW));
            Lines.add(Component.translatable(Key + ".details.1").withStyle(ChatFormatting.YELLOW));
            Lines.add(Component.translatable(Key + ".details.2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
