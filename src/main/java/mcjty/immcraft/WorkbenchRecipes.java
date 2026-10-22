package mcjty.immcraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Exact ten schemas from WorkbenchSecondaryTE, including consumption from the hotbar. */
public final class WorkbenchRecipes {
    public record Recipe(ItemStack output,ItemStack... inputs) {}
    private static ItemStack rock(int n) { return new ItemStack(ImmersiveCraft.DEVICES.get(Kind.ROCK).get(),n); }
    public static Recipe[] recipes(DeviceEntity be) {
        ItemStack tool=be.items.getStackInSlot(0);
        if(tool.is(ImmersiveCraft.SAW.get())) return new Recipe[]{
            new Recipe(new ItemStack(Blocks.OAK_PLANKS,4),new ItemStack(Blocks.OAK_LOG)),
            new Recipe(new ItemStack(Items.STICK,4),new ItemStack(Blocks.OAK_PLANKS,2))};
        if(tool.is(ImmersiveCraft.CHISEL.get())) return new Recipe[]{
            new Recipe(new ItemStack(ImmersiveCraft.DEVICES.get(Kind.FURNACE).get()),new ItemStack(Blocks.COBBLESTONE,8)),
            new Recipe(new ItemStack(ImmersiveCraft.DEVICES.get(Kind.CHEST).get()),new ItemStack(Blocks.OAK_PLANKS,8)),
            new Recipe(new ItemStack(ImmersiveCraft.DEVICES.get(Kind.CUPBOARD).get()),new ItemStack(Blocks.OAK_PLANKS,8))};
        return new Recipe[]{
            new Recipe(new ItemStack(ImmersiveCraft.SAW.get()),new ItemStack(Items.STICK,3),rock(2)),
            new Recipe(new ItemStack(ImmersiveCraft.CHISEL.get()),new ItemStack(Items.STICK),rock(2)),
            new Recipe(new ItemStack(Items.FLINT_AND_STEEL),new ItemStack(Items.FLINT),rock(1)),
            new Recipe(new ItemStack(Items.STONE_PICKAXE),new ItemStack(Items.STICK,2),rock(3)),
            new Recipe(new ItemStack(Items.STONE_AXE),new ItemStack(Items.STICK,2),rock(3))};
    }
    public static Recipe current(DeviceEntity be) { Recipe[] r=recipes(be);return r[Math.floorMod(be.schema,r.length)]; }
    public static boolean craft(DeviceEntity be,Player p) {
        BlockPos base=be.getBlockPos().relative(be.facing().getClockWise());
        if(!be.getLevel().hasChunkAt(base) || !be.getLevel().mayInteract(p,base) || !(be.getLevel().getBlockEntity(base) instanceof DeviceEntity inputs) || inputs.kind!=Kind.WORKBENCH || inputs.facing()!=be.facing()) return false;
        List<ItemStack> materials=new ArrayList<>();
        for(int i=0;i<4;i++) materials.add(inputs.items.getStackInSlot(i).copy());
        for(int i=0;i<9;i++) materials.add(p.getInventory().getItem(i).copy());
        Recipe recipe=current(be);
        for(ItemStack need:recipe.inputs) {
            int amount=need.getCount();
            for(ItemStack s:materials) {
                if(s.is(need.getItem())) { int n=Math.min(amount,s.getCount());amount-=n;s.shrink(n); }
            }
            if(amount>0) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("Missing: ").append(need.getHoverName()),true);return true;
            }
        }
        // Commit only after all ingredients matched; server execution serializes concurrent requests.
        for(int i=0;i<4;i++) inputs.items.setStackInSlot(i,materials.get(i));
        for(int i=0;i<9;i++) p.getInventory().setItem(i,materials.get(i+4));
        DeviceEntity.give(p,recipe.output.copy());inputs.changed();be.changed();return true;
    }
}
