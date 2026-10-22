package mcjty.immcraft.api.multiblock;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * This represents a multiblock network
 */
public interface IMultiBlockNetwork<T extends IMultiBlock> {
    /** Bind mutable operations to a dimension; mandatory for addons operating outside the overworld. */
    IMultiBlockNetwork<T> bind(Level level);

    /**
     * Get the unique name that identifies this multiblock network
     * @return
     */
    String getNetworkName();

    /**
     * Get valid directions in which this multiblock network can expand
     * @return
     */
    Direction[] getDirections();

    /**
     * Is this tile from this network?
     * @param tile
     * @return
     */
    boolean fromThisNetwork(IMultiBlockTile tile);

    /**
     * Delete the given multiblock
     * @param id
     */
    void deleteMultiBlock(int id);

    /**
     * Create a multiblock and return the id
     * @param mb
     * @return
     */
    int createMultiBlock(T mb);

    /**
     * Get or create a multiblock with the given id
     * @param id
     * @return
     */
    T getOrCreateMultiBlock(int id);

    /**
     * Get the client information object we got from the server. Call this only client side
     * and expect this to be a) not present, b) not up-to-date
     * @param blockId
     * @return
     */
    IMultiBlockClientInfo getClientInfo(int blockId);

    // Refresh client side information about a multiblock
    void refreshInfo(int id);

    /**
     * After making changes to a multiblock call this method to make sure the multiblock data gets persisted.
     */
    void save(Level world);

    /**
     * Make a new multiblock and return the id
     * @return
     */
    int newMultiBlock();
}
