package mcjty.immcraft.api.handles;

import net.minecraft.world.phys.AABB;

/**
 * Representing the selectable part of a handle
 */
public class HandleSelector {

    private final String id;
    private final AABB box;

    public HandleSelector(String id, AABB box) {
        this.id = id;
        this.box = box;
    }

    public String getId() {
        return id;
    }

    public AABB getBox() {
        return box;
    }
}
