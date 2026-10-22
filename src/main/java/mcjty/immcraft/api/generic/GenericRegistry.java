package mcjty.immcraft.api.generic;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.function.*;
public final class GenericRegistry implements IGenericRegistry {
    private final DeferredRegister<Block> blocks;private final DeferredRegister<Item> items;private final DeferredRegister<BlockEntityType<?>> entities;
    public GenericRegistry(String modid,IEventBus bus) {
        blocks=DeferredRegister.create(ForgeRegistries.BLOCKS,modid);items=DeferredRegister.create(ForgeRegistries.ITEMS,modid);entities=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,modid);
        blocks.register(bus);items.register(bus);entities.register(bus);
    }
    public <B extends Block> RegistryObject<B> registerBlock(String name,Supplier<B> factory,Function<B,? extends Item> itemFactory) {
        var block=blocks.register(name,factory);if(itemFactory!=null) items.register(name,()->itemFactory.apply(block.get()));return block;
    }
    public <I extends Item> RegistryObject<I> registerItem(String name,Supplier<I> factory) { return items.register(name,factory); }
    public <T extends BlockEntity> RegistryObject<BlockEntityType<T>> registerBlockEntity(String name,Supplier<BlockEntityType<T>> factory) { return entities.register(name,factory); }
}
