package net.rai.scp_unleashed.extraction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftFormation;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.rifttech.ApocalypticRiftEntity;
import net.rai.scp_unleashed.rifttech.GridField;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class RiftConjurerItem extends Item {
    public static final double Reach = 48.0;
    public static final double AirDistance = 6.0;
    public static final int SpawnCooldown = 8;
    public static final int ApocalypticChoice = FragmentRarity.values().length + 1;
    public static final int Choices = FragmentRarity.values().length + 2;

    public RiftConjurerItem(Properties Properties) {
        super(Properties);
    }

    public static int selection(ItemStack Stack) {
        return Math.floorMod(Stack.getOrDefault(ExtractionRegistry.ConjurerSelection.get(), 0), Choices);
    }

    @Nullable
    public static FragmentRarity selected(ItemStack Stack) {
        int Index = selection(Stack);
        return Index == 0 || Index == ApocalypticChoice ? null : FragmentRarity.byTier(Index);
    }

    public static boolean apocalyptic(ItemStack Stack) {
        return selection(Stack) == ApocalypticChoice;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User.isSecondaryUseActive()) {
            Entity Found = ExtractionTargeting.findEntity(User, Reach, RiftConjurerItem::isOpenRift);
            if (Found instanceof RiftEntity Rift) {
                if (!World.isClientSide) {
                    MutableComponent Name = Rift instanceof ApocalypticRiftEntity ? apocalypticName() : rarityName(Rift.getRarity());
                    Rift.collapse();
                    User.displayClientMessage(Component.translatable("message.scp_unleashed.rift_conjurer.collapsed", Name), true);
                }
                return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
            }
            if (!World.isClientSide) {
                int Next = (selection(Stack) + 1) % Choices;
                Stack.set(ExtractionRegistry.ConjurerSelection.get(), Next);
                User.displayClientMessage(Component.translatable("message.scp_unleashed.rift_conjurer.selected", selectionName(Next)), true);
                World.playSound(null, User.getX(), User.getEyeY(), User.getZ(), ExtractionRegistry.ConjureCycle.get(), SoundSource.PLAYERS, 0.6F, 0.85F + 0.09F * Next);
            }
            return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
        }
        if (World instanceof ServerLevel Server) {
            open(Server, User, Stack);
        }
        User.getCooldowns().addCooldown(this, SpawnCooldown);
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    private static void open(ServerLevel World, Player User, ItemStack Stack) {
        if (!RiftFormation.allowedIn(World)) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.rift_conjurer.overworld"), true);
            return;
        }
        if (apocalyptic(Stack)) {
            openApocalyptic(World, User);
            return;
        }
        FragmentRarity Chosen = selected(Stack);
        FragmentRarity Grade = Chosen == null ? RiftFormation.rollRarity(World.random) : Chosen;
        Vec3 Bottom = placement(World, User, RiftEntity.heightOf(Grade), Candidate -> RiftEntity.boxAt(Candidate, Grade));
        RiftEntity Rift = RiftRegistry.Rift.get().create(World);
        if (Rift == null) {
            return;
        }
        Rift.setRarity(Grade);
        Rift.moveTo(Bottom.x, Bottom.y, Bottom.z, World.random.nextFloat() * 360.0F, 0.0F);
        if (!World.addFreshEntity(Rift)) {
            return;
        }
        World.playSound(null, Bottom.x, Bottom.y + RiftEntity.heightOf(Grade) * 0.5, Bottom.z, ExtractionRegistry.Conjure.get(), SoundSource.PLAYERS, 1.0F, 1.08F - 0.04F * Grade.getTier());
        User.displayClientMessage(Component.translatable("message.scp_unleashed.rift_conjurer.opened", rarityName(Grade)), true);
    }

    private static void openApocalyptic(ServerLevel World, Player User) {
        Vec3 Bottom = placement(World, User, ApocalypticRiftEntity.Height, ApocalypticRiftEntity::boxAt);
        if (GridField.openApocalyptic(World, Bottom, World.random) == null) {
            return;
        }
        World.playSound(null, Bottom.x, Bottom.y + ApocalypticRiftEntity.Height * 0.5, Bottom.z, ExtractionRegistry.Conjure.get(), SoundSource.PLAYERS, 1.2F, 0.7F);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.rift_conjurer.opened", apocalypticName()), true);
    }

    private static Vec3 placement(ServerLevel World, Player User, float Height, Function<Vec3, AABB> Box) {
        HitResult Hit = User.pick(Reach, 1.0F, false);
        Vec3 Bottom;
        if (Hit instanceof BlockHitResult Struck && Hit.getType() == HitResult.Type.BLOCK) {
            Direction Side = Struck.getDirection();
            Vec3 Point = Struck.getLocation();
            double Clearance = Height * RiftEntity.WidthRatio * 0.5 + 0.25;
            if (Side == Direction.UP) {
                Bottom = new Vec3(Point.x, Point.y + 0.35, Point.z);
            } else if (Side == Direction.DOWN) {
                Bottom = new Vec3(Point.x, Point.y - Height - 0.3, Point.z);
            } else {
                Bottom = new Vec3(Point.x + Side.getStepX() * Clearance, Point.y - Height * 0.5, Point.z + Side.getStepZ() * Clearance);
            }
        } else {
            Bottom = User.getEyePosition().add(User.getViewVector(1.0F).scale(AirDistance)).subtract(0.0, Height * 0.5, 0.0);
        }
        for (int Step = 0; Step <= 12; Step++) {
            double Lift = (Step % 2 == 0 ? Step / 2 : -(Step + 1) / 2) * 0.5;
            Vec3 Candidate = Bottom.add(0.0, Lift, 0.0);
            if (World.noCollision(Box.apply(Candidate).inflate(0.05))) {
                return Candidate;
            }
        }
        return Bottom;
    }

    private static boolean isOpenRift(Entity Candidate) {
        return Candidate instanceof RiftEntity Rift && Rift.isAlive() && !Rift.isCollapsing();
    }

    public static MutableComponent rarityName(FragmentRarity Grade) {
        return Component.translatable("hud.scp_unleashed.extraction.rarity." + Grade.getSerializedName()).withStyle(Style.EMPTY.withColor(Grade.getColour() & 0xFFFFFF));
    }

    public static MutableComponent apocalypticName() {
        return Component.translatable("hud.scp_unleashed.extraction.rarity." + ApocalypticRiftEntity.Label).withStyle(Style.EMPTY.withColor(ApocalypticRiftEntity.Colour & 0xFFFFFF));
    }

    public static MutableComponent selectionName(int Index) {
        int Wrapped = Math.floorMod(Index, Choices);
        if (Wrapped == ApocalypticChoice) {
            return apocalypticName();
        }
        return Wrapped == 0 ? Component.translatable("item.scp_unleashed.rift_conjurer.random").withStyle(ChatFormatting.LIGHT_PURPLE) : rarityName(FragmentRarity.byTier(Wrapped));
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.rift_conjurer.selection", selectionName(selection(Stack))).withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.rift_conjurer.open").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.rift_conjurer.cycle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.rift_conjurer.collapse").withStyle(ChatFormatting.GOLD));
        }
        Lines.add(Component.translatable("item.scp_unleashed.rift_conjurer.creative").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
