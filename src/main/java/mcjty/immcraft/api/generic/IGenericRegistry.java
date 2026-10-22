package mcjty.immcraft.api.generic;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.registries.RegistryObject;
import java.util.function.*;
/** Forge 1.20.1 registration needs explicit names and factories instead of setRegistryName/reflection. */
public interface IGenericRegistry {
    <B extends Block> RegistryObject<B> registerBlock(String name,Supplier<B> factory,Function<B,? extends Item> itemFactory);
    <I extends Item> RegistryObject<I> registerItem(String name,Supplier<I> factory);
    <T extends BlockEntity> RegistryObject<BlockEntityType<T>> registerBlockEntity(String name,Supplier<BlockEntityType<T>> factory);
}
