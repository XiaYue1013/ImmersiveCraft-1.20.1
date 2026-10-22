package mcjty.immcraft.api.cable;

import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Representation of a cable.
 */
public interface ICable {
    List<BlockPos> getPath();
}
