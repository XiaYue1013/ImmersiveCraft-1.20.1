package mcjty.immcraft;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import org.jetbrains.annotations.Nullable;

public final class ImmersiveBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public final Kind kind;
    public ImmersiveBlock(Kind k) {
        super(Properties.of().strength(k.display() || k==Kind.STICKS || k==Kind.ROCK?0:2).noOcclusion().dynamicShape()
            .sound(k==Kind.FURNACE || k==Kind.ROCK?SoundType.STONE:SoundType.WOOD)
            .offsetType(k==Kind.ROCK?BlockBehaviour.OffsetType.XZ:BlockBehaviour.OffsetType.NONE)
            .lightLevel(s -> s.getValue(LIT)?13:0));
        kind=k;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(LIT,false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(FACING,LIT); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f=kind.wall() && ctx.getClickedFace().getAxis().isHorizontal()?ctx.getClickedFace():ctx.getHorizontalDirection().getOpposite();
        BlockState state=defaultBlockState().setValue(FACING,f);
        if(kind==Kind.WORKBENCH) {
            BlockPos partner=ctx.getClickedPos().relative(f.getCounterClockWise());
            if(!ctx.getLevel().hasChunkAt(partner) || !ctx.getLevel().getBlockState(partner).canBeReplaced(ctx)) return null;
        }
        return state.canSurvive(ctx.getLevel(),ctx.getClickedPos())?state:null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,@Nullable net.minecraft.world.entity.LivingEntity placer,ItemStack stack) {
        if(kind==Kind.WORKBENCH && !level.isClientSide) {
            Direction facing=state.getValue(FACING);
            level.setBlockAndUpdate(pos.relative(facing.getCounterClockWise()),ImmersiveCraft.DEVICES.get(Kind.WORKBENCH_SEC).get().defaultBlockState().setValue(FACING,facing));
        }
    }
    @Override public BlockState rotate(BlockState s,Rotation r) { return s.setValue(FACING,r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s,Mirror m) { return rotate(s,m.getRotation(s.getValue(FACING))); }
    @Override public boolean canSurvive(BlockState s,LevelReader l,BlockPos p) {
        Direction support=kind.wall()?s.getValue(FACING).getOpposite():Direction.DOWN;
        if(kind.wall() || kind.display() || kind==Kind.STICKS || kind==Kind.ROCK) {
            BlockPos base=p.relative(support);
            return l.hasChunkAt(base) && l.getBlockState(base).isFaceSturdy(l,base,support.getOpposite());
        }
        return true;
    }
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos np) {
        if(!canSurvive(s,l,p)) return Blocks.AIR.defaultBlockState();
        if(kind==Kind.WORKBENCH || kind==Kind.WORKBENCH_SEC) {
            Direction partner=kind==Kind.WORKBENCH?s.getValue(FACING).getCounterClockWise():s.getValue(FACING).getClockWise();
            if(d==partner && !other.is(ImmersiveCraft.DEVICES.get(kind==Kind.WORKBENCH?Kind.WORKBENCH_SEC:Kind.WORKBENCH).get())) return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(s,d,other,l,p,np);
    }
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext ctx) {
        AABB box=switch(kind) {
            case CHEST -> new AABB(0,0,0,1,.9,1);
            case CUPBOARD,SHELF,BOOKSHELF -> new AABB(0,0,0,1,1,.5);
            case DISPLAY -> new AABB(0,0,0,1,.4,1);
            case WALL_DISPLAY -> new AABB(0,0,0,1,1,.2);
            case ROCK -> new AABB(.2,0,.2,.8,.4,.8);
            case STICKS -> new AABB(.1,0,.1,.9,.4,.9);
            default -> new AABB(0,0,0,1,1,1);
        };
        if(kind==Kind.CHEST && l.getBlockEntity(p) instanceof DeviceEntity be && be.open) box=new AABB(0,0,0,1,1,1);
        Vec3 a=SlotLayout.toWorld(new Vec3(box.minX,box.minY,box.minZ),s.getValue(FACING));
        Vec3 b=SlotLayout.toWorld(new Vec3(box.maxX,box.maxY,box.maxZ),s.getValue(FACING));
        var shape=Shapes.create(new AABB(a,b));
        if(kind==Kind.ROCK) { Vec3 offset=s.getOffset(l,p);return shape.move(offset.x,offset.y,offset.z); }
        return shape;
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext ctx) {
        return kind.display() || kind==Kind.STICKS || kind==Kind.ROCK?Shapes.empty():getShape(s,l,p,ctx);
    }
    @Override public RenderShape getRenderShape(BlockState s) { return kind.display() || kind==Kind.BOOKSTAND?RenderShape.INVISIBLE:RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s) { return new DeviceEntity(pos,s); }
    @Override public boolean triggerEvent(BlockState state,Level level,BlockPos pos,int id,int value) {
        super.triggerEvent(state,level,pos,id,value);
        BlockEntity be=level.getBlockEntity(pos);
        return be!=null && be.triggerEvent(id,value);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t) {
        return createTickerHelper(t,ImmersiveCraft.DEVICE.get(),DeviceEntity::tick);
    }
    public static int selected(DeviceEntity be,Player player) {
        if(be.kind==Kind.STICKS) return 0;
        Vec3 start=player.getEyePosition().subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
        return SlotLayout.select(be.kind,be.facing(),start,start.add(player.getLookAngle().scale(PortNetwork.reach(player))));
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit) {
        if(l.isClientSide) return InteractionResult.SUCCESS;
        if(!l.mayInteract(player,p) || !player.mayBuild() || player.isSpectator()) return InteractionResult.FAIL;
        if(kind==Kind.ROCK && player.getItemInHand(hand).is(Items.FLINT) && PortConfig.FLINT.get()) {
            l.removeBlock(p,false);Block.popResource(l,p,new ItemStack(Items.FLINT_AND_STEEL));return InteractionResult.CONSUME;
        }
        if(l.getBlockEntity(p) instanceof DeviceEntity be) {
            if(be.interact(player,hand,selected(be,player))) return InteractionResult.CONSUME;
            if(kind.lid() && be.open) { be.toggle();return InteractionResult.CONSUME; }
        }
        return InteractionResult.PASS;
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving) {
        if(!s.is(next.getBlock()) && l.getBlockEntity(p) instanceof DeviceEntity be) be.dropContents();
        super.onRemove(s,l,p,next,moving);
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r) {
        if(!s.getValue(LIT)) return;
        l.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,p.getX()+.5,p.getY()+.8,p.getZ()+.5,0,.02,0);
        l.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,p.getX()+.5,p.getY()+.4,p.getZ()+.5,0,0,0);
        if(r.nextInt(24)==0) l.playLocalSound(p.getX()+.5,p.getY()+.5,p.getZ()+.5,net.minecraft.sounds.SoundEvents.FIRE_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,1,r.nextFloat()*.7f+.3f,false);
    }
}
