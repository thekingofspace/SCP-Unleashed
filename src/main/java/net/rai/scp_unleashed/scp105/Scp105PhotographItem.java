package net.rai.scp_unleashed.scp105;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Scp105PhotographItem extends Item {
    public static final int DescribeDelay = 16;
    public static final int RevealMin = 30;
    public static final int RevealMax = 60;
    public static final int RetryDelay = 20;
    public static final int UseCooldown = 10;
    public static final double HeldDrop = 0.25;
    public static final String Key = "item.scp_unleashed.scp105_photograph.";

    public Scp105PhotographItem(Properties Settings) {
        super(Settings);
    }

    @Nullable
    public static Scp105Photo photoOf(ItemStack Stack) {
        return Stack.get(Scp105Registry.PhotoComponent.get());
    }

    public static ItemStack create(Scp105Photo Photo) {
        ItemStack Stack = new ItemStack(Scp105Registry.Photograph.get());
        Stack.set(Scp105Registry.PhotoComponent.get(), Photo);
        return Stack;
    }

    public static boolean peaceful(Level World) {
        return World.getDifficulty() == Difficulty.PEACEFUL;
    }

    public static Scp105Photo ensure(ItemStack Stack, Level World) {
        Scp105Photo Photo = photoOf(Stack);
        if (Photo == null) {
            Photo = Scp105Subjects.roll(World.getRandom(), peaceful(World));
            Stack.set(Scp105Registry.PhotoComponent.get(), Photo);
        }
        return Photo;
    }

    private static Scp105Photo settle(ServerLevel World, Scp105Photo Photo) {
        if (Photo.Subject() != Scp105Photo.Kind.Creature || !peaceful(World)) {
            return Photo;
        }
        Optional<EntityType<?>> Type = BuiltInRegistries.ENTITY_TYPE.getOptional(Photo.Id());
        if (Type.isPresent() && !Scp105Subjects.hostile(Type.get())) {
            return Photo;
        }
        Optional<Scp105Photo> Calmer = Scp105Subjects.rollCreature(World.getRandom(), true);
        return Calmer.map(Found -> Photo.retarget(Found.Subject(), Found.Id(), Found.Count(), Found.Young())).orElse(Photo);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        Scp105Photo Current = photoOf(Stack);
        if (Current != null && Current.developing()) {
            return InteractionResultHolder.pass(Stack);
        }
        if (World instanceof ServerLevel Server) {
            Scp105Photo Photo = settle(Server, ensure(Stack, World));
            long Now = Server.getGameTime();
            long Due = Now + RevealMin + Server.getRandom().nextInt(RevealMax - RevealMin + 1);
            Stack.set(Scp105Registry.PhotoComponent.get(), Photo.begin(Now, Due));
            User.displayClientMessage(Component.translatable(Scp105Subjects.Prefix + "photo.see").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            Server.playSound(null, User.getX(), User.getEyeY(), User.getZ(), Scp105Registry.Peek.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
            User.getCooldowns().addCooldown(this, UseCooldown);
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (!(World instanceof ServerLevel Server)) {
            return;
        }
        Scp105Photo Photo = photoOf(Stack);
        if (Photo == null) {
            ensure(Stack, World);
            return;
        }
        if (!Photo.developing()) {
            return;
        }
        long Now = Server.getGameTime();
        if (!Photo.Told() && Now >= Photo.Started() + DescribeDelay) {
            if (Holder instanceof Player Viewer) {
                Viewer.displayClientMessage(Scp105Subjects.description(Photo).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            }
            Photo = Photo.told();
            Stack.set(Scp105Registry.PhotoComponent.get(), Photo);
        }
        if (Now < Photo.Due()) {
            return;
        }
        Vec3 Eye = Holder.getEyePosition();
        float Yaw = Holder.getYRot();
        develop(Server, Stack, Photo, Eye, Holder.position(), Yaw, Scp105Reveal.ahead(Eye.subtract(0.0, HeldDrop, 0.0), Yaw, Scp105Reveal.PhotoReach));
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (!(Dropped.level() instanceof ServerLevel Server)) {
            return false;
        }
        Scp105Photo Photo = photoOf(Stack);
        if (Photo == null || !Photo.developing() || Server.getGameTime() < Photo.Due()) {
            return false;
        }
        Vec3 Feet = Dropped.position();
        boolean Done = develop(Server, Stack, Photo, Feet.add(0.0, 1.0, 0.0), Feet, Dropped.getYRot(), Feet.add(0.0, 0.2, 0.0));
        if (Done) {
            Dropped.discard();
        }
        return Done;
    }

    private static boolean develop(ServerLevel World, ItemStack Stack, Scp105Photo Photo, Vec3 Eye, Vec3 Feet, float Yaw, Vec3 Shreds) {
        if (!Scp105Reveal.appear(World, Eye, Feet, Yaw, Photo)) {
            Stack.set(Scp105Registry.PhotoComponent.get(), Photo.delay(World.getGameTime() + RetryDelay));
            return false;
        }
        ItemStack Remains = Stack.copyWithCount(1);
        Stack.shrink(1);
        Scp105Reveal.crumble(World, Shreds, Remains);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Scp105Photo Photo = photoOf(Stack);
        if (Photo != null && Photo.developing()) {
            Lines.add(Component.translatable(Key + "developing").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
            return;
        }
        Lines.add(Component.translatable(Key + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + "signed").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Key + "blurred").withStyle(ChatFormatting.GOLD));
        }
    }
}
