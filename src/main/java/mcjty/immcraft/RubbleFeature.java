package mcjty.immcraft;

import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class RubbleFeature extends Feature<NoneFeatureConfiguration> {
    public RubbleFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if(!PortConfig.WORLDGEN.get()) return false;
        WorldGenLevel l=ctx.level();RandomSource r=ctx.random();BlockPos origin=ctx.origin();
        boolean placed=false;
        for(int pass=0;pass<3;pass++) {
            int max=pass==1?PortConfig.STICK_ATTEMPTS.get():PortConfig.ROCK_ATTEMPTS.get();
            if(max<=0) continue;
            // Deliberately retain the original re-sampled loop bound, rather than claiming max attempts are guaranteed.
            for(int i=0;i<r.nextInt(max);i++) {
                int x=origin.getX()+r.nextInt(16)+8,z=origin.getZ()+r.nextInt(16)+8;
                if(!l.hasChunk(x>>4,z>>4)) continue;
                BlockPos ground=new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1,z);
                if(pass==2) {
                    int y=ground.getY();
                    while(y>l.getMinBuildHeight()+1 && !l.isEmptyBlock(new BlockPos(x,y,z))) y--;
                    while(y>l.getMinBuildHeight()+1 && l.isEmptyBlock(new BlockPos(x,y,z))) y--;
                    if(y<=l.getMinBuildHeight()+1) continue;
                    ground=new BlockPos(x,y,z);
                }
                BlockPos pos=ground.above();
                if(l.isOutsideBuildHeight(pos) || !l.isEmptyBlock(pos) || !l.getFluidState(pos).isEmpty()) continue;
                if(!l.getBlockState(ground).isFaceSturdy(l,ground,Direction.UP)) continue;
                Kind kind=pass==1?Kind.STICKS:Kind.ROCK;
                if(!PortConfig.ground(l.getBlockState(ground),pass==1?PortConfig.STICK_BLOCKS.get():PortConfig.ROCK_BLOCKS.get())) continue;
                if(pass==1) {
                    boolean leaves=false;
                    for(int y=2;y<17;y++) {
                        BlockPos test=ground.above(y);
                        if(l.isOutsideBuildHeight(test)) break;
                        if(!l.isEmptyBlock(test)) { leaves=l.getBlockState(test).is(BlockTags.LEAVES);break; }
                    }
                    if(!leaves) continue;
                }
                if(l.setBlock(pos,ImmersiveCraft.DEVICES.get(kind).get().defaultBlockState().setValue(ImmersiveBlock.FACING,Direction.from2DDataValue(r.nextInt(4))),2)) {
                    if(kind==Kind.STICKS && l.getBlockEntity(pos) instanceof DeviceEntity be) be.items.setStackInSlot(0,new ItemStack(Items.STICK,1+r.nextInt(6)));
                    placed=true;
                }
            }
        }
        return placed;
    }
}
