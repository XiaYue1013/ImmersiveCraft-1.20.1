package mcjty.immcraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

public final class PortNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(ImmersiveCraft.ID,"input"),()->"1","1"::equals,"1"::equals);
    public record Input(int action) {
        static void encode(Input p,FriendlyByteBuf b) { b.writeByte(p.action); }
        static Input decode(FriendlyByteBuf b) { return new Input(b.readUnsignedByte()); }
        static void handle(Input p,Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context=supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player=context.getSender();
                if(player==null || player.isSpectator() || !player.mayBuild() || p.action>6) return;
                BlockHitResult hit=trace(player);
                if(Boolean.getBoolean("immcraft.mpServer")) System.out.println("IMMCRAFT_MP_INPUT: "+player.getScoreboardName()+" action="+p.action+" hit="+hit.getBlockPos()+" type="+hit.getType()+" eye="+player.getEyePosition()+" look="+player.getLookAngle());
                if(Boolean.getBoolean("immcraft.mpServer") && p.action==5) player.getPersistentData().putBoolean("mp_take_requested",true);
                if(hit.getType()!=HitResult.Type.BLOCK || !player.level().hasChunkAt(hit.getBlockPos()) || !player.level().mayInteract(player,hit.getBlockPos())) return;
                if(p.action==0) WorldInteractions.placeDisplay(player,hit,true);
                else if(p.action>=3 && player.level().getBlockEntity(hit.getBlockPos()) instanceof DeviceEntity device) {
                    int slot=ImmersiveBlock.selected(device,player);
                    if(Boolean.getBoolean("immcraft.mpServer")) System.out.println("IMMCRAFT_MP_INPUT: device="+device.kind+" open="+device.open+" slot="+slot+" item="+(slot>=0 && slot<device.items.getSlots()?device.items.getStackInSlot(slot):"none"));
                    if(p.action==3 || p.action==4) device.insertClick(player,slot,p.action==4?1:64);
                    else if(p.action==5) device.extractOne(player,slot);
                    else if(device.kind==Kind.WORKBENCH_SEC) {
                        var recipe=WorkbenchRecipes.current(device);var text=net.minecraft.network.chat.Component.literal("Ingredients: ");
                        for(var input:recipe.inputs()) text.append(input.getHoverName()).append(" x"+input.getCount()+" ");
                        player.displayClientMessage(text,true);
                    }
                }
                else if(player.level().getBlockEntity(hit.getBlockPos()) instanceof mcjty.immcraft.api.generic.GenericTE generic) {
                    if(p.action==3) generic.onClick(player);
                    else if(p.action==6) { var present=new java.util.ArrayList<String>();var missing=new java.util.ArrayList<String>();generic.calculateIngredients(present,missing,player);player.displayClientMessage(net.minecraft.network.chat.Component.literal(String.join(", ",present)+" Missing: "+String.join(", ",missing)),true); }
                    else if(p.action==1 || p.action==2) generic.onKeyPress(p.action==1?mcjty.immcraft.api.input.KeyType.KEY_NEXTITEM:mcjty.immcraft.api.input.KeyType.KEY_PREVIOUSITEM,player,hit.getDirection(),generic.getBlock().worldToBlockSpace(player.level(),hit.getBlockPos(),hit.getDirection()),hit.getLocation());
                }
                else if(player.level().getBlockEntity(hit.getBlockPos()) instanceof DeviceEntity stand && stand.kind==Kind.BOOKSTAND) stand.navigateBook(p.action==1?">":"<");
                else if(player.level().getBlockEntity(hit.getBlockPos()) instanceof DeviceEntity be && be.kind==Kind.WORKBENCH_SEC && ImmersiveBlock.selected(be,player)==1) {
                    be.schema=Math.floorMod(be.schema+(p.action==1?1:-1),WorkbenchRecipes.recipes(be).length);be.changed();
                }
            });
            context.setPacketHandled(true);
        }
    }
    public static void register() {
        CHANNEL.messageBuilder(Input.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Input::encode).decoder(Input::decode).consumerNetworkThread(Input::handle).add();
        CHANNEL.messageBuilder(NetworkInfoRequest.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->{b.writeUtf(p.name,128);b.writeVarInt(p.id);}).decoder(b->new NetworkInfoRequest(b.readUtf(128),b.readVarInt())).consumerNetworkThread(NetworkInfoRequest::handle).add();
        CHANNEL.messageBuilder(NetworkInfoResponse.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((p,b)->{b.writeResourceLocation(p.dimension);b.writeUtf(p.name,128);b.writeVarInt(p.id);b.writeByteArray(p.data);}).decoder(b->new NetworkInfoResponse(b.readResourceLocation(),b.readUtf(128),b.readVarInt(),b.readByteArray(65536))).consumerNetworkThread(NetworkInfoResponse::handle).add();
    }
    public static void send(int action) { CHANNEL.sendToServer(new Input(action)); }
    public static void requestNetworkInfo(String name,int id) { CHANNEL.sendToServer(new NetworkInfoRequest(name,id)); }
    private static final java.util.Map<ServerPlayer,Long> INFO_RATE=new java.util.WeakHashMap<>();
    public record NetworkInfoRequest(String name,int id) {
        static void handle(NetworkInfoRequest p,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();context.enqueueWork(()->{
                ServerPlayer player=context.getSender();if(player==null || p.id<0) return;
                long tick=player.level().getGameTime();if(tick-INFO_RATE.getOrDefault(player,-10L)<2) return;INFO_RATE.put(player,tick);
                var hit=trace(player);if(hit.getType()!=HitResult.Type.BLOCK || !player.level().hasChunkAt(hit.getBlockPos())) return;
                var network=mcjty.immcraft.multiblock.MultiBlockData.getNetwork(p.name);if(network==null) return;
                var be=player.level().getBlockEntity(hit.getBlockPos());boolean authorized=false;
                if(be instanceof mcjty.immcraft.blocks.bundle.BundleTE bundle) authorized=bundle.getCableSections().stream().anyMatch(s->s.getId()==p.id && s.getType().getCableHandler().getNetworkName(s.getSubType()).equals(p.name));
                if(be instanceof mcjty.immcraft.api.multiblock.IMultiBlockTile<?> tile) authorized=java.util.Objects.equals(tile.getID(),p.id) && network.fromThisNetwork(tile);
                if(!authorized) return;
                mcjty.immcraft.multiblock.MultiBlockData.get(player.level());var mb=network.getMultiblock(p.id);if(mb==null) return;
                FriendlyByteBuf out=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try { mb.getClientInfo().writeToBuf(out);if(out.readableBytes()>65536) return;byte[] data=new byte[out.readableBytes()];out.readBytes(data);
                    CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new NetworkInfoResponse(player.level().dimension().location(),p.name,p.id,data));
                } finally { out.release(); }
            });context.setPacketHandled(true);
        }
    }
    public record NetworkInfoResponse(ResourceLocation dimension,String name,int id,byte[] data) {
        static void handle(NetworkInfoResponse p,Supplier<NetworkEvent.Context> supplier) {
            var context=supplier.get();context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->mcjty.immcraft.client.ClientEvents.networkInfo(p)));context.setPacketHandled(true);
        }
    }
    public static double reach(Player player) {
        return Math.min(8,player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()));
    }
    public static BlockHitResult trace(Player p) {
        Vec3 start=p.getEyePosition();
        for(double d=0;d<=reach(p);d+=.5) {
            Vec3 point=start.add(p.getLookAngle().scale(d));
            if(!p.level().hasChunkAt(net.minecraft.core.BlockPos.containing(point))) return BlockHitResult.miss(point,net.minecraft.core.Direction.UP,net.minecraft.core.BlockPos.containing(point));
        }
        return p.level().clip(new ClipContext(start,start.add(p.getLookAngle().scale(reach(p))),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
    }
}
