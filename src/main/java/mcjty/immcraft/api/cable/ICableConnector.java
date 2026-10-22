package mcjty.immcraft.api.cable;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Implement this in your GenericTE so that you can support connections
 * with cables. Your connector should be able to connect with cables
 * of the given type (independend of subtype) and use the given subtype on
 * connection attempt as a preference for selecting a connection ID.
 */
public interface ICableConnector {
    ICableType getType();

    // Check if a connection is possible (ignoring subtype). The side is
    // given in block space.
    boolean canConnect(Direction blockSide);

    // Connect to a side (given in block space) and return an id representing this connection. Returns -1 if connection not possible
    // The subtype is used for the preferred subtype. The returned id is unique for this connector (even when considering sides).
    int connect(Direction blockSide, int networkId, ICableSubType subType);

    void disconnect(int connectorId);

    // The given rotation is the orientation of the block itself.
    Vec3 getConnectorLocation(int connectorId, Direction rotation);
}
