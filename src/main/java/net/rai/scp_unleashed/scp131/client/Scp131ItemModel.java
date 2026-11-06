package net.rai.scp_unleashed.scp131.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp131.Scp131Colour;
import net.rai.scp_unleashed.scp131.Scp131Grief;
import net.rai.scp_unleashed.scp131.Scp131PodData;
import net.rai.scp_unleashed.scp131.Scp131PodItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class Scp131ItemModel extends GeoModel<Scp131PodItem> {
    private final Scp131Pose Pose = new Scp131Pose();

    public static ItemStack stackOf(@Nullable GeoRenderer<Scp131PodItem> Renderer) {
        return Renderer instanceof GeoItemRenderer<Scp131PodItem> Drawer && Drawer.getCurrentItemStack() != null ? Drawer.getCurrentItemStack() : ItemStack.EMPTY;
    }

    public static boolean sad() {
        Minecraft Client = Minecraft.getInstance();
        return Client.level != null && Scp131Grief.isGrieving(Client.level);
    }

    @Override
    public ResourceLocation getModelResource(Scp131PodItem Animatable) {
        return Scp131GeoModel.ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp131PodItem Animatable) {
        return Scp131GeoModel.texture(Scp131Colour.Red);
    }

    @Override
    public ResourceLocation getTextureResource(Scp131PodItem Animatable, @Nullable GeoRenderer<Scp131PodItem> Renderer) {
        ItemStack Stack = stackOf(Renderer);
        return Scp131GeoModel.texture(Stack.isEmpty() ? Scp131Colour.Red : Scp131PodData.of(Stack).Colour());
    }

    @Override
    public ResourceLocation getAnimationResource(Scp131PodItem Animatable) {
        return Scp131GeoModel.AnimationPath;
    }

    public void pose(ItemStack Stack, ItemDisplayContext Context, double Time) {
        Scp131ItemFace.pose(Pose, Scp131PodData.of(Stack).seed(), (float) (Time % 72000.0), sad(), Context);
        Scp131Rig.apply(this, Pose);
    }

    @Override
    public void setCustomAnimations(Scp131PodItem Animatable, long InstanceId, AnimationState<Scp131PodItem> State) {
        ItemStack Stack = State.getData(DataTickets.ITEMSTACK);
        ItemDisplayContext Context = State.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
        Double Tick = State.getData(DataTickets.TICK);
        pose(Stack == null ? ItemStack.EMPTY : Stack, Context == null ? ItemDisplayContext.NONE : Context, Tick == null ? RenderUtil.getCurrentTick() : Tick);
    }
}
