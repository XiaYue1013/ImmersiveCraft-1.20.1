package mcjty.immcraft.compat;

import mcjty.immcraft.*;
import mcjty.theoneprobe.api.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.function.Function;

/** Loaded only after ModList confirms TOP. No API types appear in the event subscriber. */
final class TopRegistration implements Function<ITheOneProbe,Void> {
    public Void apply(ITheOneProbe top) { top.registerProvider(new Provider());return null; }
    private static final class Provider implements IProbeInfoProvider {
        public ResourceLocation getID() { return new ResourceLocation("immcraft","selected_item"); }
        public void addProbeInfo(ProbeMode mode,IProbeInfo info,Player player,Level level,BlockState state,IProbeHitData hit) {
            if(level.getBlockEntity(hit.getPos()) instanceof DeviceEntity be) {
                var text=ProbeText.text(be,player);if(!text.getString().isEmpty()) info.text(text);
            }
            else if(level.getBlockEntity(hit.getPos()) instanceof mcjty.immcraft.blocks.bundle.BundleTE be) { var text=ProbeText.bundle(be,player);if(!text.getString().isEmpty()) info.text(text); }
        }
    }
}
