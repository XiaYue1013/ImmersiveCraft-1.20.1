package mcjty.immcraft.api.helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
public final class Broadcaster {
    public static void broadcast(Level world,BlockPos pos,String text,double radius) { if(world instanceof ServerLevel server) server.players().stream().filter(p->p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=radius*radius).forEach(p->p.displayClientMessage(Component.literal(text),false)); }
}
