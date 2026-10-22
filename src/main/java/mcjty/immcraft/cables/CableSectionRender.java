package mcjty.immcraft.cables;


import mcjty.immcraft.api.cable.ICableSubType;
import net.minecraft.world.phys.Vec3;

/**
 * Cable section for the renderer (BundleISBM)
 */
public class CableSectionRender {
    private final ICableSubType subType;
    private final Vec3 vector;
    private final Vec3 vector1;
    private final Vec3 vector2;

    public CableSectionRender(ICableSubType subType, Vec3 vector, Vec3 vector1, Vec3 vector2) {
        this.subType = subType;
        this.vector = vector;
        this.vector1 = vector1;
        this.vector2 = vector2;
    }

    public ICableSubType getSubType() {
        return subType;
    }

    public Vec3 getVector() {
        return vector;
    }

    public Vec3 getVector1() {
        return vector1;
    }

    public Vec3 getVector2() {
        return vector2;
    }
}
