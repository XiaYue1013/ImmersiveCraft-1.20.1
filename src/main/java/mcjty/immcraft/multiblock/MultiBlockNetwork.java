package mcjty.immcraft.multiblock;
import mcjty.immcraft.api.multiblock.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;
import java.util.*;
public final class MultiBlockNetwork<T extends IMultiBlock> implements IMultiBlockNetwork<T> {
    static final class State<T extends IMultiBlock> { int lastId;final Map<Integer,T> blocks=new HashMap<>();final Map<String,Integer> relocations=new HashMap<>(); }
    private final String name;private final IMultiBlockFactory<T> factory;private final Direction[] directions;
    private final Map<Integer,IMultiBlockClientInfo> infos=new HashMap<>();private final Map<Integer,Long> updates=new HashMap<>();
    public MultiBlockNetwork(String name,IMultiBlockFactory<T> factory,Direction[] directions) {
        if(name.isBlank() || name.length()>128) throw new IllegalArgumentException("Invalid network name");
        this.name=name;this.factory=factory;this.directions=directions.clone();
        if(MultiBlockData.networks.putIfAbsent(name,this)!=null) throw new IllegalArgumentException("Duplicate network "+name);
    }
    private State<T> state() { return MultiBlockData.get(MultiBlockData.currentLevel()).state(this); }
    private void dirty() { MultiBlockData.get(MultiBlockData.currentLevel()).setDirty(); }
    public MultiBlockNetwork<T> bind(Level level) { MultiBlockData.get(level);return this; }
    public String getNetworkName() { return name; }
    public IMultiBlockFactory<T> getFactory() { return factory; }
    public Direction[] getDirections() { return directions.clone(); }
    public boolean fromThisNetwork(IMultiBlockTile tile) { return factory.isSameType(tile.getMultiBlock()); }
    public void deleteMultiBlock(int id) { state().blocks.remove(id);dirty(); }
    public int newMultiBlock() { int id=++state().lastId;dirty();return id; }
    public int createMultiBlock(T mb) { int id=newMultiBlock();state().blocks.put(id,mb);dirty();return id; }
    public T getOrCreateMultiBlock(int id) {
        if(id<0) throw new IllegalArgumentException("Negative network id");
        State<T> state=state();T result=state.blocks.get(id);
        if(result==null) { result=factory.create();state.blocks.put(id,result);state.lastId=Math.max(state.lastId,id);dirty(); }
        return result;
    }
    public T getMultiblock(int id) { return state().blocks.get(id); }
    public void relocate(int oldId,int newId,Collection<net.minecraft.core.BlockPos> positions) {
        for(var p:positions) state().relocations.put(oldId+"@"+p.asLong(),newId);dirty();
    }
    public int resolveId(int id,net.minecraft.core.BlockPos pos) {
        if(!state().relocations.containsKey(id+"@"+pos.asLong())) return id;
        Set<Integer> seen=new HashSet<>();int current=id;
        while(seen.add(current)) { Integer next=state().relocations.get(current+"@"+pos.asLong());if(next==null || next==current) break;current=next; }
        return current;
    }
    public IMultiBlockClientInfo getClientInfo(int id) { return infos.get(id); }
    public void registerClientInfo(int id,IMultiBlockClientInfo info) { infos.put(id,info); }
    public void clearClientInfo() { infos.clear();updates.clear(); }
    public void refreshInfo(int id) {
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server!=null && server.isSameThread()) return;
        if(System.currentTimeMillis()-updates.getOrDefault(id,0L)>100) { updates.put(id,System.currentTimeMillis());mcjty.immcraft.PortNetwork.requestNetworkInfo(name,id); }
    }
    public void save(Level level) { MultiBlockData.get(level).setDirty(); }
    public void clear() { state().blocks.clear();state().relocations.clear();state().lastId=0;dirty(); }
    State<T> decode(CompoundTag tag) {
        State<T> state=new State<>();state.lastId=tag.getInt("lastId");
        for(Tag entry:tag.getList("multiblocks",Tag.TAG_COMPOUND)) {
            CompoundTag n=(CompoundTag)entry;int id=n.getInt("channel");T block=factory.create();block.readFromNBT(n);state.blocks.put(id,block);state.lastId=Math.max(state.lastId,id);
        }
        CompoundTag relocations=tag.getCompound("relocations");for(String key:relocations.getAllKeys()) state.relocations.put(key,relocations.getInt(key));
        return state;
    }
    static CompoundTag encode(State<?> state) {
        CompoundTag tag=new CompoundTag();ListTag list=new ListTag();
        for(var e:state.blocks.entrySet()) { CompoundTag n=new CompoundTag();e.getValue().writeToNBT(n);n.putInt("channel",e.getKey());list.add(n); }
        tag.put("multiblocks",list);tag.putInt("lastId",state.lastId);
        CompoundTag moved=new CompoundTag();state.relocations.forEach(moved::putInt);tag.put("relocations",moved);return tag;
    }
    public void readFromNBT(CompoundTag n) { var data=MultiBlockData.get(MultiBlockData.currentLevel());State<T> s=state(),loaded=decode(n);s.lastId=loaded.lastId;s.blocks.clear();s.blocks.putAll(loaded.blocks);s.relocations.clear();s.relocations.putAll(loaded.relocations);data.setDirty(); }
    public void writeToNBT(CompoundTag n) { n.merge(encode(state())); }
}
