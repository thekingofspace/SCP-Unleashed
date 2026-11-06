package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DisinfectingWipeItem extends Item {
    public static final int WipeTicks = 30;
    public static final int StrokeTicks = 10;
    public static final int FoamEvery = 2;
    public static final int Radius = 2;
    public static final int FinishFoam = 18;
    public static final int FinishDrips = 8;
    public static final double FaceSpread = 0.38;
    public static final double FoamLift = 0.04;

    public DisinfectingWipeItem(Properties Settings) {
        super(Settings);
    }

    @Nullable
    public static BlockHitResult target(Player User) {
        HitResult Hit = ProjectileUtil.getHitResultOnViewVector(User, Candidate -> !Candidate.isSpectator() && Candidate.isPickable(), User.blockInteractionRange());
        return Hit instanceof BlockHitResult Aimed && Hit.getType() == HitResult.Type.BLOCK ? Aimed : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User != null && target(User) != null) {
            User.startUsingItem(Context.getHand());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity Holder) {
        return WipeTicks;
    }

    @Override
    public void onUseTick(Level World, LivingEntity Holder, ItemStack Stack, int Remaining) {
        if (Remaining < 0 || !(Holder instanceof Player User)) {
            Holder.releaseUsingItem();
            return;
        }
        BlockHitResult Hit = target(User);
        if (Hit == null) {
            Holder.releaseUsingItem();
            return;
        }
        int Used = getUseDuration(Stack, Holder) - Remaining + 1;
        if (World.isClientSide && Used % FoamEvery == 0) {
            foam(World, Hit, World.getRandom());
        }
        if (Used % StrokeTicks == StrokeTicks / 2) {
            Vec3 Point = Hit.getLocation();
            LabwareSounds.play(World, User, Point.x, Point.y, Point.z, LabwareSounds.Wiping, SoundSource.PLAYERS, LabwareSounds.pitch(World));
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Holder) {
        if (!(Holder instanceof Player User)) {
            return Stack;
        }
        BlockHitResult Hit = target(User);
        if (Hit == null) {
            return Stack;
        }
        if (World instanceof ServerLevel Server) {
            wipe(Server, User, Hit);
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        Stack.consume(1, User);
        return Stack;
    }

    public static int wipe(ServerLevel World, @Nullable Player User, BlockHitResult Hit) {
        BlockPos Pos = Hit.getBlockPos();
        int Killed = LabSurfaces.clean(World, Pos, Radius);
        Direction Side = Hit.getDirection();
        Vec3 Point = Vec3.atCenterOf(Pos).add(Side.getStepX() * (0.5 + FoamLift), Side.getStepY() * (0.5 + FoamLift), Side.getStepZ() * (0.5 + FoamLift));
        double SpreadX = Side.getAxis() == Direction.Axis.X ? 0.02 : FaceSpread;
        double SpreadY = Side.getAxis() == Direction.Axis.Y ? 0.02 : FaceSpread;
        double SpreadZ = Side.getAxis() == Direction.Axis.Z ? 0.02 : FaceSpread;
        World.sendParticles(ParticleTypes.BUBBLE_POP, Point.x, Point.y, Point.z, FinishFoam, SpreadX, SpreadY, SpreadZ, 0.01);
        World.sendParticles(ParticleTypes.SPLASH, Point.x, Point.y, Point.z, FinishDrips, SpreadX, SpreadY, SpreadZ, 0.0);
        LabwareSounds.play(World, null, Point.x, Point.y, Point.z, LabwareSounds.Wiped, SoundSource.PLAYERS, LabwareSounds.pitch(World));
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.wiped").withStyle(ChatFormatting.AQUA), true);
        }
        return Killed;
    }

    private static void foam(Level World, BlockHitResult Hit, RandomSource Random) {
        Direction Side = Hit.getDirection();
        Vec3 Point = Hit.getLocation();
        for (int Index = 0; Index < 2; Index++) {
            double OffsetX = Side.getAxis() == Direction.Axis.X ? Side.getStepX() * FoamLift : (Random.nextDouble() - 0.5) * 0.5;
            double OffsetY = Side.getAxis() == Direction.Axis.Y ? Side.getStepY() * FoamLift : (Random.nextDouble() - 0.5) * 0.5;
            double OffsetZ = Side.getAxis() == Direction.Axis.Z ? Side.getStepZ() * FoamLift : (Random.nextDouble() - 0.5) * 0.5;
            World.addParticle(Index == 0 ? ParticleTypes.BUBBLE_POP : ParticleTypes.SPLASH, Point.x + OffsetX, Point.y + OffsetY, Point.z + OffsetZ,
                    Side.getStepX() * 0.02, 0.0, Side.getStepZ() * 0.02);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.disinfecting_wipe.tooltip").withStyle(ChatFormatting.AQUA));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.disinfecting_wipe.detail.use", Radius).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.disinfecting_wipe.detail.suit").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.disinfecting_wipe.detail.single").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
