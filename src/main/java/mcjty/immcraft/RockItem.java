package mcjty.immcraft;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.*;

public final class RockItem extends BlockItem {
    public RockItem(Block block) { super(block,new Item.Properties()); }
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand) {
        ItemStack held=p.getItemInHand(hand);
        l.playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.SNOWBALL_THROW,SoundSource.NEUTRAL,.5f,.4f/(l.random.nextFloat()*.4f+.8f));
        if(!l.isClientSide) {
            RockProjectile rock=new RockProjectile(l,p);rock.setItem(held.copyWithCount(1));
            rock.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.5f,1);l.addFreshEntity(rock);
            if(!p.getAbilities().instabuild) held.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(held,l.isClientSide);
    }
}
