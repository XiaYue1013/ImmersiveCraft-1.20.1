package mcjty.immcraft.books;

import mcjty.immcraft.api.book.IBook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class ManualItem extends Item implements IBook {
    public ManualItem() { super(new Properties().stacksTo(1)); }
    public String getTitle() { return "Immersive Craft Manual"; }
    public ResourceLocation getJson() { return new ResourceLocation("immcraft","text/manual.json"); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(level.isClientSide) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->mcjty.immcraft.client.BookClient.open(player.getItemInHand(hand)));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
}
