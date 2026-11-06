package net.rai.scp_unleashed.raid.serpentshand;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.hostile.HostileTrophy;
import net.rai.scp_unleashed.stasis.StasisContents;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SerpentPodItem extends Item {
    public static final String Key = "item.scp_unleashed.serpents_hand_stasis_pod.";
    public static final long Sealed = Long.MAX_VALUE;

    public SerpentPodItem(Properties Settings) {
        super(Settings);
    }

    public static boolean isPod(ItemStack Stack) {
        return Stack.getItem() instanceof SerpentPodItem;
    }

    @Nullable
    public static StasisContents contents(ItemStack Stack) {
        return isPod(Stack) ? Stack.get(HostileRegistry.PodContents.get()) : null;
    }

    public static boolean filled(ItemStack Stack) {
        return contents(Stack) != null;
    }

    public static ItemStack empty() {
        return new ItemStack(HostileRegistry.Pod.get());
    }

    public static ItemStack filled(HostileTrophy Trophy, HolderLookup.Provider Lookup) {
        ItemStack Stack = empty();
        Stack.set(HostileRegistry.PodContents.get(), new StasisContents(Trophy.Data().copy(), Trophy.label(Lookup).copy(), Trophy.kindName().copy(), Trophy.Health(),
                Trophy.MaxHealth(), Sealed));
        return Stack;
    }

    @Nullable
    public static HostileTrophy trophy(ItemStack Stack) {
        StasisContents Held = contents(Stack);
        if (Held == null) {
            return null;
        }
        ResourceLocation Type = ResourceLocation.tryParse(Held.EntityData().getString("id"));
        if (Type == null) {
            return null;
        }
        return new HostileTrophy(HostileTrophy.Kind.Creature, Type, Held.copyData(), Held.Health(), Held.MaxHealth());
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        ItemStack Stack = Context.getItemInHand();
        StasisContents Held = contents(Stack);
        if (Held == null) {
            return InteractionResult.PASS;
        }
        Level World = Context.getLevel();
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos Clicked = Context.getClickedPos();
        Direction Face = Context.getClickedFace();
        BlockState State = World.getBlockState(Clicked);
        VoxelShape Shape = State.getCollisionShape(World, Clicked);
        BlockPos Spot = Shape.isEmpty() ? Clicked : Clicked.relative(Face);
        double Y = Spot.getY();
        if (!Shape.isEmpty() && Face == Direction.UP) {
            Y = Clicked.getY() + Shape.max(Direction.Axis.Y);
        }
        Player User = Context.getPlayer();
        float Yaw = User != null ? User.getYRot() + 180.0f : 0.0f;
        boolean Freed = SerpentPods.release((ServerLevel) World, Held, new Vec3(Spot.getX() + 0.5, Y, Spot.getZ() + 0.5), Yaw, User);
        if (!Freed) {
            return InteractionResult.FAIL;
        }
        if (User == null || !User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onDestroyed(ItemEntity Dropped, DamageSource Source) {
        StasisContents Held = contents(Dropped.getItem());
        if (Held != null && Dropped.level() instanceof ServerLevel World) {
            SerpentPods.release(World, Held, Dropped.position(), World.random.nextFloat() * 360.0f, null);
        }
    }

    @Override
    public int getEntityLifespan(ItemStack Stack, Level World) {
        return filled(Stack) ? Integer.MAX_VALUE : super.getEntityLifespan(Stack, World);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        StasisContents Held = contents(Stack);
        if (Held != null) {
            MutableComponent Name = Held.Label().copy().withStyle(ChatFormatting.WHITE);
            if (Held.isNamed()) {
                Name.append(Component.literal(" (").append(Held.Kind()).append(")").withStyle(ChatFormatting.GRAY));
            }
            Lines.add(Component.translatable(Key + "holds", Name).withStyle(ChatFormatting.GRAY));
            if (Held.MaxHealth() > 0.0f) {
                Lines.add(Component.translatable(Key + "health", Math.round(Held.Health()), Math.round(Held.MaxHealth())).withStyle(ChatFormatting.RED));
            }
        } else {
            Lines.add(Component.translatable(Key + "holds", Component.translatable(Key + "empty").withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY));
        }
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "release").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "single").withStyle(ChatFormatting.GRAY));
        }
    }
}
