package mcjty.immcraft;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;

public final class RockProjectile extends ThrowableItemProjectile {
    public RockProjectile(EntityType<? extends RockProjectile> type,Level l) { super(type,l); }
    public RockProjectile(Level l,LivingEntity owner) { super(ImmersiveCraft.ROCK_ENTITY.get(),owner,l); }
    @Override protected Item getDefaultItem() { return ImmersiveCraft.DEVICES.get(Kind.ROCK).get().asItem(); }
    @Override protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        hit.getEntity().hurt(damageSources().thrown(this,getOwner()),PortConfig.ROCK_DAMAGE.get().floatValue());
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if(!level().isClientSide) { level().broadcastEntityEvent(this,(byte)3);discard(); }
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
