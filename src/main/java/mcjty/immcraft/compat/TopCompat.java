package mcjty.immcraft.compat;

import mcjty.immcraft.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;

@Mod.EventBusSubscriber(modid="immcraft",bus=Mod.EventBusSubscriber.Bus.MOD)
public final class TopCompat {
    @SubscribeEvent public static void enqueue(InterModEnqueueEvent event) {
        if(ModList.get().isLoaded("theoneprobe")) InterModComms.sendTo("theoneprobe","getTheOneProbe",()->new TopRegistration());
    }
}
