package mcjty.immcraft.test;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@Mod.EventBusSubscriber(modid="immcraft")
public final class ServerSmoke {
    private static int ticks;
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(Boolean.getBoolean("immcraft.serverSmokeTest") && e.phase==TickEvent.Phase.END && ++ticks==200) {
            e.getServer().saveEverything(false,true,true);
            System.out.println("IMMCRAFT_DEDICATED: 200 normal server ticks, world saved, graceful shutdown");e.getServer().halt(false);
        }
    }
}
