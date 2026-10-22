package mcjty.immcraft.multiblock;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;
import mcjty.immcraft.api.multiblock.*;

/** Definitions are process-wide; all mutable network state belongs to a dimension's SavedData. */
public final class MultiBlockData extends SavedData {
    public static final Map<String,MultiBlockNetwork<?>> networks=new java.util.concurrent.ConcurrentHashMap<>();
    private static final ThreadLocal<Level> CURRENT=new ThreadLocal<>();
    private final Map<String,MultiBlockNetwork.State<?>> states=new HashMap<>();
    private CompoundTag unloaded=new CompoundTag();
    public static MultiBlockNetwork<?> getNetwork(String name) { return networks.get(name); }
    public static MultiBlockData get(Level level) {
        if(!(level instanceof ServerLevel server)) throw new IllegalStateException("Mutable multiblock state requires a ServerLevel");
        CURRENT.set(level);
        return server.getDataStorage().computeIfAbsent(MultiBlockData::load,MultiBlockData::new,"ImmCraftMultiBlocks");
    }
    public static Level currentLevel() {
        Level level=CURRENT.get();
        if(level==null) {
            var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if(server==null) throw new IllegalStateException("Bind the network with MultiBlockData.get(level) before using it");
            level=server.overworld();
        }
        return level;
    }
    public static void clearContext() { CURRENT.remove();networks.values().forEach(MultiBlockNetwork::clearClientInfo); }
    private static MultiBlockData load(CompoundTag tag) { MultiBlockData data=new MultiBlockData();data.unloaded=tag.copy();return data; }
    @SuppressWarnings("unchecked") <T extends IMultiBlock> MultiBlockNetwork.State<T> state(MultiBlockNetwork<T> definition) {
        return (MultiBlockNetwork.State<T>)states.computeIfAbsent(definition.getNetworkName(),name -> definition.decode(unloaded.getCompound(name)));
    }
    public void save() { setDirty(); }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.merge(unloaded.copy());
        for(var e:states.entrySet()) tag.put(e.getKey(),MultiBlockNetwork.encode(e.getValue()));
        return tag;
    }
}
