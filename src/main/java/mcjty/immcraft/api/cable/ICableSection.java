package mcjty.immcraft.api.cable;

import net.minecraft.world.level.Level;

/**
 * Represents a section of a cable
 */
public interface ICableSection {

    /**
     * Return the id of the multiblock network for this cable.
     * @return
     */
    int getId();

    ICableType getType();

    ICableSubType getSubType();

    ICableConnector getConnector(Level worldObj, int directionId);
}
