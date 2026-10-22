package mcjty.immcraft.api.cable;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Representation of a bundle (a collection of cables). This is implemented by the
 * bundle tile entity
 */
public interface IBundle {

    /**
     * Get the corresponding tile entity.
     * @return
     */
    BlockEntity getTileEntity();

    /**
     * Find a section in this bundle of the given type and connected to the given block id
     * @param type
     * @param subType
     * @param id
     * @return
     */
    ICableSection findSection(ICableType type, ICableSubType subType, int id);

}
