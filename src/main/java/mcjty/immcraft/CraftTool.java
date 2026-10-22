package mcjty.immcraft;

import net.minecraft.world.item.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

/** Stone-tier ItemTool with an empty effective-block set, as in the original. */
public final class CraftTool extends DiggerItem {
    public CraftTool() { super(3,1,Tiers.STONE,TagKey.create(Registries.BLOCK,new ResourceLocation("immcraft","mineable/craft_tool")),new Item.Properties()); }
}
