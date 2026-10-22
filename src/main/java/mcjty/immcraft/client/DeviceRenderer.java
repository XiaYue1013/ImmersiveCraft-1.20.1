package mcjty.immcraft.client;

import mcjty.immcraft.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

public final class DeviceRenderer implements BlockEntityRenderer<DeviceEntity> {
    private static final java.util.Map<DeviceEntity,java.util.Map<Integer,Integer>> BOOK_COLORS=new java.util.WeakHashMap<>();
    public static void clearBookCache() { BOOK_COLORS.clear(); }
    public DeviceRenderer(BlockEntityRendererProvider.Context context) {}
    private void model(String name,DeviceEntity be,PoseStack pose,MultiBufferSource buffers,int light) {
        Minecraft mc=Minecraft.getInstance();
        BakedModel model=mc.getModelManager().getModel(new ResourceLocation("immcraft","block/"+name));
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.cutout()),be.getBlockState(),model,1,1,1,light,OverlayTexture.NO_OVERLAY);
    }
    @Override public void render(DeviceEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!(be.getBlockState().getBlock() instanceof ImmersiveBlock)) return;
        Minecraft mc=Minecraft.getInstance();
        pose.pushPose();pose.translate(.5,0,.5);
        float angle=switch(be.facing()) { case NORTH -> 180;case EAST -> 90;case WEST -> -90;default -> 0; };
        pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(-.5,0,-.5);
        if(be.kind==Kind.BOOKSTAND) { BookClient.renderStand(be,partial,pose,buffers,light);pose.popPose();return; }
        if(be.kind==Kind.STICKS) {
            int n=be.items.getStackInSlot(0).getCount();
            model(n<=3?"sticks1":n<=6?"sticks2":n<=9?"sticks3":"sticks",be,pose,buffers,light);
        }
        if(be.kind.lid()) {
            float opening=Mth.lerp(partial,be.previousLid,be.lid);
            pose.pushPose();
            if(be.kind==Kind.CHEST) { pose.translate(0,.9,0);pose.mulPose(Axis.XP.rotationDegrees(-60*opening)); }
            else { pose.translate(1,1,.5);pose.mulPose(Axis.YP.rotationDegrees(-72*opening)); }
            model(be.kind==Kind.CHEST?"chestlid":"cupboardlid",be,pose,buffers,light);pose.popPose();
        }
        if(be.kind.lid() && !be.open && be.lid<.01) { pose.popPose();return; }
        if(mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(Vec3.atCenterOf(be.getBlockPos()))>PortConfig.RENDER.get()*PortConfig.RENDER.get()) { pose.popPose();return; }
        int selected=mc.player!=null && mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(be.getBlockPos())?mcjty.immcraft.ImmersiveBlock.selected(be,mc.player):-1;
        for(SlotLayout.Slot slot:SlotLayout.slots(be.kind)) {
            ItemStack stack=be.kind==Kind.WORKBENCH_SEC && slot.index()==1?WorkbenchRecipes.current(be).output():be.items.getStackInSlot(slot.index());
            if(stack.isEmpty() && be.kind==Kind.BOOKSHELF) BOOK_COLORS.computeIfAbsent(be,k->new java.util.HashMap<>()).remove(slot.index());
            boolean ghost=stack.isEmpty() && selected==slot.index() && mc.player!=null && be.valid(slot.index(),mc.player.getMainHandItem());
            if(ghost) stack=mc.player.getMainHandItem();
            if(stack.isEmpty()) continue;
            if(be.kind==Kind.BOOKSHELF) stack=bookModel(be,slot.index(),stack);
            Vec3 center=slot.center();pose.pushPose();pose.translate(center.x,center.y,center.z);
            float scale=slot.scale()*(mc.getItemRenderer().getModel(stack,be.getLevel(),mc.player,slot.index()).isGui3d()?1:2);
            pose.scale(scale,scale,scale);
            MultiBufferSource target=ghost?type -> new GhostVertexConsumer(buffers.getBuffer(RenderType.itemEntityTranslucentCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS))):buffers;
            mc.getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,target,be.getLevel(),slot.index());
            pose.popPose();
            if(!ghost && stack.getCount()>1) {
                pose.pushPose();pose.translate(center.x+.06,center.y+.08,center.z+.08);
                pose.mulPose(Axis.YP.rotationDegrees(-angle));
                pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(.003f,-.003f,.003f);
                String count=Integer.toString(stack.getCount());
                mc.font.drawInBatch(count,-mc.font.width(count)/2f,0,0xffffffff,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,light);
                pose.popPose();
            }
        }
        if(selected>=0) {
            for(var slot:SlotLayout.slots(be.kind)) if(slot.index()==selected) LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),slot.bounds(),.3f,.7f,1,.8f);
            if(be.kind==Kind.WORKBENCH_SEC && selected==1 && mc.player!=null) recipeOverlay(be,mc.player,pose,buffers,light);
        }
        if(ClientEvents.debug || PortConfig.DEBUG.get()) for(SlotLayout.Slot slot:SlotLayout.slots(be.kind)) LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),slot.bounds(),0,1,0,1);
        pose.popPose();
    }
    @Override public int getViewDistance() { return (int)Math.min(Integer.MAX_VALUE,Math.max(64,PortConfig.RENDER.get())); }
    private ItemStack bookModel(DeviceEntity be,int slot,ItemStack original) {
        String item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(original.getItem()).toString();
        String name=PortConfig.BOOKS.get().stream().filter(s->s.startsWith(item+"=")).map(s->s.substring(s.indexOf('=')+1)).findFirst().orElse("dummybook_yellow");
        String[] names={"dummybook_red","dummybook_blue","dummybook_green","dummybook_yellow","dummybook_small_red","dummybook_small_blue","dummybook_small_green","dummybook_small_yellow"};
        int index=BOOK_COLORS.computeIfAbsent(be,k->new java.util.HashMap<>()).computeIfAbsent(slot,k->be.getLevel().random.nextInt(8));
        if(!name.equals("*")) {
            index=3;
            for(int i=0;i<names.length;i++) if(name.equals(names[i])) { index=i;break; }
        }
        ItemStack visual=new ItemStack(ImmersiveCraft.DUMMY_BOOK.get(),original.getCount());visual.getOrCreateTag().putInt("CustomModelData",index+1);return visual;
    }
    private void recipeOverlay(DeviceEntity be,net.minecraft.world.entity.player.Player player,PoseStack pose,MultiBufferSource buffers,int light) {
        var recipe=WorkbenchRecipes.current(be);var mc=Minecraft.getInstance();
        net.minecraft.core.BlockPos base=be.getBlockPos().relative(be.facing().getClockWise());
        pose.pushPose();pose.translate(.5,1.5,.5);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(.008f,-.008f,.008f);
        int y=0;
        for(ItemStack need:recipe.inputs()) {
            int count=0;for(int i=0;i<9;i++) if(player.getInventory().getItem(i).is(need.getItem())) count+=player.getInventory().getItem(i).getCount();
            if(be.getLevel().hasChunkAt(base) && be.getLevel().getBlockEntity(base) instanceof DeviceEntity input) for(int i=0;i<input.items.getSlots();i++) if(input.items.getStackInSlot(i).is(need.getItem())) count+=input.items.getStackInSlot(i).getCount();
            var text=need.getHoverName().copy().append(" "+Math.min(count,need.getCount())+"/"+need.getCount());
            mc.font.drawInBatch(text,-mc.font.width(text)/2f,y,count>=need.getCount()?0xff55ff55:0xffff5555,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,light);y+=10;
        }
        pose.popPose();
    }
}
