package net.rai.scp_unleashed.mixin.gametest;

import net.minecraft.gametest.framework.GameTestBatchFactory;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Mixin(GameTestBatchFactory.class)
public abstract class GameTestFilterMixin {
    @Unique
    private static final String Scp_unleashed$FilterVariable = "SCP_GAMETESTS";

    @ModifyVariable(method = "fromTestFunction", at = @At("HEAD"), argsOnly = true)
    private static Collection<TestFunction> scpFilterTests(Collection<TestFunction> Tests) {
        String Wanted = System.getenv(Scp_unleashed$FilterVariable);
        if (Wanted == null || Wanted.isBlank()) {
            return Tests;
        }
        List<String> Prefixes = Arrays.stream(Wanted.toLowerCase(Locale.ROOT).split(",")).map(String::trim).filter(Part -> !Part.isEmpty()).toList();
        return Tests.stream().filter(Test -> Prefixes.stream().anyMatch(Prefix -> Test.batchName().toLowerCase(Locale.ROOT).startsWith(Prefix)
                || Test.testName().toLowerCase(Locale.ROOT).startsWith(Prefix))).toList();
    }
}
