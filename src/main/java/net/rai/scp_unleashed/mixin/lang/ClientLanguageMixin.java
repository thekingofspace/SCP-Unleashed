package net.rai.scp_unleashed.mixin.lang;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.rai.scp_unleashed.lang.LangIndex;
import net.rai.scp_unleashed.lang.LangLoading;
import net.rai.scp_unleashed.lang.LangSink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(ClientLanguage.class)
public abstract class ClientLanguageMixin {
    @Inject(method = "loadFrom", at = @At("HEAD"))
    private static void beginLanguageFolders(ResourceManager Manager, List<String> Codes, boolean RightToLeft, CallbackInfoReturnable<ClientLanguage> Info) {
        LangLoading.begin(Manager, Codes);
    }

    @WrapOperation(method = "loadFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/language/ClientLanguage;appendFrom(Ljava/lang/String;Ljava/util/List;Ljava/util/Map;Ljava/util/Map;)V"))
    private static void appendLanguageFolders(String Code, List<Resource> Stack, Map<String, String> Strings, Map<String, Component> Components,
                                              Operation<Void> Original, @Local(argsOnly = true) ResourceManager Manager, @Local ResourceLocation Location) {
        LangLoading.Session Running = LangLoading.active();
        if (Running == null) {
            Original.call(Code, Stack, Strings, Components);
            return;
        }
        LangLoading.appendNamespace(Running, Manager, Location.getNamespace(), Code, Stack, Strings, Components,
                (Source, Output) -> Original.call(Code, List.of(Source), new LangSink(Output), Components));
    }

    @Inject(method = "loadFrom", at = @At("RETURN"))
    private static void finishLanguageFolders(ResourceManager Manager, List<String> Codes, boolean RightToLeft, CallbackInfoReturnable<ClientLanguage> Info) {
        LangLoading.Session Running = LangLoading.active();
        if (Running != null) {
            LangIndex.attach(Info.getReturnValue(), LangLoading.finish(Running));
        }
    }
}
