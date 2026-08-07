package xy177.tinkersplannerantique.client.planner;

import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import xy177.tinkersplannerantique.TinkersPlannerAntique;

final class JeiCompat {

    private static boolean failed;

    private JeiCompat() {
    }

    static boolean showRecipes(ItemStack stack) {
        if (failed || !Loader.isModLoaded("jei") || stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            Class<?> internalClass = Class.forName("mezz.jei.Internal");
            Object runtime = internalClass.getMethod("getRuntime").invoke(null);
            if (runtime == null) {
                return false;
            }

            Object recipeRegistry = runtime.getClass().getMethod("getRecipeRegistry").invoke(runtime);
            Class<?> modeClass = Class.forName("mezz.jei.api.recipe.IFocus$Mode");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Object outputMode = Enum.valueOf((Class) modeClass.asSubclass(Enum.class), "OUTPUT");
            Object focus = findMethod(recipeRegistry.getClass(), "createFocus", 2).invoke(recipeRegistry, outputMode, stack.copy());

            Object recipesGui = runtime.getClass().getMethod("getRecipesGui").invoke(runtime);
            findMethod(recipesGui.getClass(), "show", 1).invoke(recipesGui, focus);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            if (TinkersPlannerAntique.logger != null) {
                TinkersPlannerAntique.logger.warn("JEI recipe access is unavailable.", e);
            }
            return false;
        }
    }

    private static Method findMethod(Class<?> owner, String name, int parameterCount) throws NoSuchMethodException {
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + "." + name);
    }
}
