package mcjty.immcraft.compat;

import mcjty.immcraft.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class JadeCompat implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) { registration.registerBlockDataProvider(Provider.INSTANCE,DeviceEntity.class);registration.registerBlockDataProvider(Provider.INSTANCE,mcjty.immcraft.blocks.bundle.BundleTE.class); }
    @Override public void registerClient(IWailaClientRegistration registration) { registration.registerBlockComponent(Provider.INSTANCE,ImmersiveBlock.class);registration.registerBlockComponent(Provider.INSTANCE,mcjty.immcraft.blocks.bundle.BundleBlock.class); }
    private enum Provider implements IBlockComponentProvider,IServerDataProvider<BlockAccessor> {
        INSTANCE;
        public ResourceLocation getUid() { return new ResourceLocation("immcraft","selected_item"); }
        public void appendServerData(CompoundTag tag,BlockAccessor accessor) {
            if(accessor.getBlockEntity() instanceof DeviceEntity be) tag.putString("immcraft_text",Component.Serializer.toJson(ProbeText.text(be,accessor.getPlayer())));
            else if(accessor.getBlockEntity() instanceof mcjty.immcraft.blocks.bundle.BundleTE be) tag.putString("immcraft_text",Component.Serializer.toJson(ProbeText.bundle(be,accessor.getPlayer())));
        }
        public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config) {
            CompoundTag tag=accessor.getServerData();
            if(tag.contains("immcraft_text")) {
                Component text=Component.Serializer.fromJson(tag.getString("immcraft_text"));
                if(text!=null && !text.getString().isEmpty()) tooltip.add(text);
            }
        }
    }
}
