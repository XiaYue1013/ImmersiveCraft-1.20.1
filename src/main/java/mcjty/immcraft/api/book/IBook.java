package mcjty.immcraft.api.book;

import net.minecraft.resources.ResourceLocation;

/** Implement on an Item to supply an Immersive Craft JSON book. */
public interface IBook {
    String getTitle();
    ResourceLocation getJson();
}
