package mcjty.immcraft.apiimpl;
import mcjty.immcraft.*;
import mcjty.immcraft.api.IImmersiveCraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import java.util.function.Function;
@Mod.EventBusSubscriber(modid="immcraft",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ApiLifecycle {
    @SubscribeEvent @SuppressWarnings("unchecked") public static void imc(InterModProcessEvent event) {
        event.getIMCStream(m->m.equals("getApi")).forEach(message->{
            Object callback=message.messageSupplier().get();
            if(callback instanceof Function<?,?> function) ((Function<IImmersiveCraft,?>)function).apply(ImmersiveCraft.api);
            else throw new IllegalArgumentException("getApi IMC requires Function<IImmersiveCraft, ?>");
        });
    }
    @Mod.EventBusSubscriber(modid="immcraft") public static final class ServerEvents {
        @SubscribeEvent public static void stopped(ServerStoppedEvent e) { mcjty.immcraft.multiblock.MultiBlockData.clearContext(); }
    }
}
