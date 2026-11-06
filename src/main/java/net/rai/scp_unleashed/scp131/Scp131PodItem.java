package net.rai.scp_unleashed.scp131;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public class Scp131PodItem extends Item implements GeoItem {
    public static final String Key = "item.scp_unleashed.scp131_eye_pod";
    public static final int ChirpOdds = 700;
    public static final int SadChirpOdds = 1500;
    public static final float HeldVolume = 0.3f;
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);

    public Scp131PodItem(Properties Properties) {
        super(Properties);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp131PodItem;
    }

    public static ItemStack capture(Scp131Pod Pod) {
        ItemStack Stack = new Scp131PodData(Pod.getColour(), Pod.getHealth(), Pod.getVoice()).stack();
        if (Pod.hasCustomName()) {
            Stack.set(DataComponents.CUSTOM_NAME, Pod.getCustomName());
        }
        return Stack;
    }

    @Nullable
    public static Scp131Pod release(ServerLevel World, Vec3 Where, ItemStack Stack, @Nullable Player User) {
        Scp131PodData Data = Scp131PodData.of(Stack);
        Scp131Pod Pod = Scp131Registry.Pod.get().create(World);
        if (Pod == null) {
            return null;
        }
        float Yaw = User != null ? (float) (Mth.atan2(User.getZ() - Where.z, User.getX() - Where.x) * Mth.RAD_TO_DEG) - 90.0f : World.random.nextFloat() * 360.0f;
        Pod.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        Pod.setYHeadRot(Yaw);
        Pod.setYBodyRot(Yaw);
        if (!World.noCollision(Pod) || World.containsAnyLiquid(Pod.getBoundingBox())) {
            return null;
        }
        Pod.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Where)), MobSpawnType.BUCKET, null);
        Pod.setColour(Data.Colour());
        Pod.setVoice(Data.Voice() != 0 ? Data.Voice() : World.random.nextInt());
        Pod.setHealth(Mth.clamp(Data.Health(), 1.0f, Pod.getMaxHealth()));
        Component Name = Stack.get(DataComponents.CUSTOM_NAME);
        if (Name != null) {
            Pod.setCustomName(Name);
        }
        if (!World.addFreshEntity(Pod)) {
            return null;
        }
        Pod.voice(Scp131Registry.ReleaseLayers, 0.7f, 1.0f + World.random.nextFloat() * 0.08f);
        if (User != null) {
            Pod.released(User);
        }
        return Pod;
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Level World = Context.getLevel();
        if (!(World instanceof ServerLevel Server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos Clicked = Context.getClickedPos();
        BlockPos Spot = World.getBlockState(Clicked).getCollisionShape(World, Clicked).isEmpty() ? Clicked : Clicked.relative(Context.getClickedFace());
        ItemStack Stack = Context.getItemInHand();
        Scp131Pod Pod = release(Server, Vec3.atBottomCenterOf(Spot), Stack, Context.getPlayer());
        if (Pod == null) {
            return InteractionResult.FAIL;
        }
        Stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide || !(Holder instanceof Player User) || !(Selected || User.getOffhandItem() == Stack)) {
            return;
        }
        boolean Sad = Scp131Grief.isGrieving(World);
        if (World.random.nextInt(Sad ? SadChirpOdds : ChirpOdds) != 0) {
            return;
        }
        Scp131PodData Data = Scp131PodData.of(Stack);
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Sad ? Scp131Registry.SadLayers : Scp131Registry.ChirpLayers;
        float Pitch = Scp131Pod.voiceFor(Data.Colour(), Data.Voice()) * (Sad ? Scp131Pod.SadPitch : 1.0f) * (0.95f + World.random.nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Layer.get(), SoundSource.NEUTRAL, HeldVolume * (Sad ? 0.8f : 1.0f), Pitch);
        }
    }

    @Override
    public Component getName(ItemStack Stack) {
        return Component.translatable(Key + ".coloured", Scp131PodData.of(Stack).Colour().displayName());
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Scp131PodData Data = Scp131PodData.of(Stack);
        Lines.add(Component.translatable(Key + ".colour", Data.Colour().displayName()).withStyle(ChatFormatting.GRAY));
        Level World = Context.level();
        boolean Sad = World != null && Scp131Grief.isGrieving(World);
        Lines.add(Component.translatable(Key + (Sad ? ".mood.sad" : ".mood.happy")).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Key + ".health", (int) Math.ceil(Data.Health()), (int) Scp131Pod.Health).withStyle(ChatFormatting.DARK_GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Key + ".details.0").withStyle(ChatFormatting.YELLOW));
            Lines.add(Component.translatable(Key + ".details.1").withStyle(ChatFormatting.YELLOW));
            Lines.add(Component.translatable(Key + ".details.2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> Accept) {
        Scp131Cues.Renderer.accept(Accept);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
