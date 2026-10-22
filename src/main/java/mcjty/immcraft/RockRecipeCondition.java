package mcjty.immcraft;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;

public final class RockRecipeCondition implements ICondition {
    public static final ResourceLocation ID=new ResourceLocation("immcraft","rock_recipe");
    public ResourceLocation getID() { return ID; }
    public boolean test(IContext context) { return PortConfig.ROCK_RECIPE.get(); }
    public static final class Serializer implements IConditionSerializer<RockRecipeCondition> {
        public void write(JsonObject json,RockRecipeCondition condition) { json.addProperty("type",ID.toString()); }
        public RockRecipeCondition read(JsonObject json) { return new RockRecipeCondition(); }
        public ResourceLocation getID() { return ID; }
    }
}
