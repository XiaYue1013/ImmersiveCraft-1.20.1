package mcjty.immcraft.test;
import mcjty.immcraft.*;
import mcjty.immcraft.api.cable.*;
import mcjty.immcraft.cables.Cable;
import mcjty.immcraft.multiblock.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import java.util.Optional;
/** Development-only addon fixture. The original mod provides no concrete cable material itself. */
public final class TestCableType implements ICableType,ICableSubType,ICableHandler {
    public static final TestCableType INSTANCE=new TestCableType();
    public static void register() { ImmersiveCraft.api.registerCableType(INSTANCE);ImmersiveCraft.api.createCableNetwork("immcraft:test_cable",INSTANCE,INSTANCE); }
    public String getTypeID() { return "immcraft:test_cable"; }
    public String getReadableName() { return "Test Cable"; }
    public ICableHandler getCableHandler() { return this; }
    public ICableSubType getSubTypeByID(String id) { return getTypeID().equals(id)?this:null; }
    public Optional<Block> getBlock() { return Optional.of(Blocks.WHITE_WOOL); }
    public String getTextureName() { return "minecraft:block/white_wool"; }
    public String getNetworkName(ICableSubType subtype) { return "immcraft:test_cable"; }
    public ICable getCable(Level level,ICableSubType subtype,int id) { return (Cable)network(level).getOrCreateMultiBlock(id); }
    public void tick(IBundle be,ICableSection section) { be.getTileEntity().getPersistentData().putInt("test_callback_ticks",be.getTileEntity().getPersistentData().getInt("test_callback_ticks")+1); }
    @SuppressWarnings("unchecked") public static MultiBlockNetwork<Cable> network(Level level) { MultiBlockData.get(level);return (MultiBlockNetwork<Cable>)MultiBlockData.getNetwork("immcraft:test_cable"); }
}
