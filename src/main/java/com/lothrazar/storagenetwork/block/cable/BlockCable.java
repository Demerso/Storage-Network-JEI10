package com.lothrazar.storagenetwork.block.cable;

import java.util.Locale;
import java.util.Map;
import com.google.common.collect.Maps;
import com.lothrazar.storagenetwork.StorageNetwork;
import com.lothrazar.storagenetwork.block.master.TileMaster;
import com.lothrazar.storagenetwork.registry.SsnRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraftforge.items.CapabilityItemHandler;

public class BlockCable extends Block {

  public BlockCable(String registryName) {
    super(Block.Properties.create(Material.ROCK).hardnessAndResistance(0.2F));
    setRegistryName(registryName);
    setDefaultState(stateContainer.getBaseState()
        .with(NORTH, EnumConnectType.NONE).with(EAST, EnumConnectType.NONE)
        .with(SOUTH, EnumConnectType.NONE).with(WEST, EnumConnectType.NONE)
        .with(UP, EnumConnectType.NONE).with(DOWN, EnumConnectType.NONE));
  }

  public static BlockState cleanBlockState(BlockState state) {
    for (Direction d : Direction.values()) {
      EnumProperty prop = FACING_TO_PROPERTY_MAP.get(d);
      if (state.get(prop) == EnumConnectType.INVENTORY) {
        //dont replace cable types only inv types
        state = state.with(prop, EnumConnectType.NONE);
      }
    }
    return state;
  }

  public enum EnumConnectType implements IStringSerializable {

    NONE, CABLE, INVENTORY, BLOCKED;

    public boolean isHollow() {
      return this == NONE || this == BLOCKED;
    }

    @Override
    public String getName() {
      return name().toLowerCase(Locale.ENGLISH);
    }
  }

  private static final EnumProperty<EnumConnectType> DOWN = EnumProperty.create("down", EnumConnectType.class);
  private static final EnumProperty<EnumConnectType> UP = EnumProperty.create("up", EnumConnectType.class);
  private static final EnumProperty<EnumConnectType> NORTH = EnumProperty.create("north", EnumConnectType.class);
  private static final EnumProperty<EnumConnectType> SOUTH = EnumProperty.create("south", EnumConnectType.class);
  private static final EnumProperty<EnumConnectType> WEST = EnumProperty.create("west", EnumConnectType.class);
  private static final EnumProperty<EnumConnectType> EAST = EnumProperty.create("east", EnumConnectType.class);
  public static final Map<Direction, EnumProperty<EnumConnectType>> FACING_TO_PROPERTY_MAP = Util.make(Maps.newEnumMap(Direction.class), (p) -> {
    p.put(Direction.NORTH, NORTH);
    p.put(Direction.EAST, EAST);
    p.put(Direction.SOUTH, SOUTH);
    p.put(Direction.WEST, WEST);
    p.put(Direction.UP, UP);
    p.put(Direction.DOWN, DOWN);
  });
  private static final double top = 16;
  private static final double bot = 0;
  private static final double C = 8;
  private static final double w = 2;
  private static final double sm = C - w;
  private static final double lg = C + w;
  //TODO PRE COMPUTE ALL POSSIBLE COMBINATIONS OF ALL 6 DIRS
  //(double x1, double y1, double z1, double x2, double y2, double z2)
  private static final VoxelShape AABB = Block.makeCuboidShape(sm, sm, sm, lg, lg, lg);
  //Y for updown
  private static final VoxelShape AABB_UP = Block.makeCuboidShape(sm, sm, sm, lg, top, lg);
  private static final VoxelShape AABB_DOWN = Block.makeCuboidShape(sm, bot, sm, lg, lg, lg);
  //Z for n-s
  private static final VoxelShape AABB_NORTH = Block.makeCuboidShape(sm, sm, bot, lg, lg, lg);
  private static final VoxelShape AABB_SOUTH = Block.makeCuboidShape(sm, sm, sm, lg, lg, top);
  //X for e-w
  private static final VoxelShape AABB_WEST = Block.makeCuboidShape(bot, sm, sm, lg, lg, lg);
  private static final VoxelShape AABB_EAST = Block.makeCuboidShape(sm, sm, sm, top, lg, lg);

  private boolean shapeConnects(BlockState state, EnumProperty<EnumConnectType> dirctionProperty) {
    return state.get(dirctionProperty).equals(EnumConnectType.CABLE)
        || state.get(dirctionProperty).equals(EnumConnectType.INVENTORY);
  }

  @Override
  public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
    VoxelShape shape = AABB;
    if (shapeConnects(state, UP)) {
      shape = VoxelShapes.combine(shape, AABB_UP, IBooleanFunction.OR);
    }
    if (shapeConnects(state, DOWN)) {
      shape = VoxelShapes.combine(shape, AABB_DOWN, IBooleanFunction.OR);
    }
    if (state.get(WEST).equals(EnumConnectType.CABLE)) {
      shape = VoxelShapes.combine(shape, AABB_WEST, IBooleanFunction.OR);
    }
    if (state.get(EAST).equals(EnumConnectType.CABLE)) {
      shape = VoxelShapes.combine(shape, AABB_EAST, IBooleanFunction.OR);
    }
    if (state.get(NORTH).equals(EnumConnectType.CABLE)) {
      shape = VoxelShapes.combine(shape, AABB_NORTH, IBooleanFunction.OR);
    }
    if (state.get(SOUTH).equals(EnumConnectType.CABLE)) {
      shape = VoxelShapes.combine(shape, AABB_SOUTH, IBooleanFunction.OR);
    }
    return shape;
  }

  @Override
  public BlockRenderType getRenderType(BlockState p_149645_1_) {
    return BlockRenderType.MODEL;
  }

  @Override
  public boolean hasTileEntity(BlockState state) {
    return true;
  }

  @Override
  public TileEntity createTileEntity(BlockState state, IBlockReader world) {
    return new TileCable();
  }

  @Override
  public BlockState getExtendedState(BlockState state, IBlockReader world, BlockPos pos) {
    return super.getExtendedState(state, world, pos);
  }

  @Override
  protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
    super.fillStateContainer(builder);
    builder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
  }

  @Override
  public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos) {
    EnumProperty<EnumConnectType> property = FACING_TO_PROPERTY_MAP.get(facing);
    //TODO: api should come back here
    if (facingState.getBlock() instanceof BlockCable
        || facingState.getBlock() == SsnRegistry.inventory
        || facingState.getBlock() == SsnRegistry.master
        || facingState.getBlock() == SsnRegistry.request) {
      //dont set self to self
      return stateIn.with(property, EnumConnectType.CABLE);
    }
    else if (isInventory(stateIn, facing, facingState, world, currentPos, facingPos)) {
      StorageNetwork.log(" post placement: inventory ");
      return stateIn.with(property, EnumConnectType.INVENTORY);
    }
    else {
      return stateIn.with(property, EnumConnectType.NONE);
    }
  }

  private static boolean isInventory(BlockState stateIn, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos) {
    if (facing == null) {
      return false;
    }
    if (!TileMaster.isTargetAllowed(facingState)) {
      return false;
    }
    TileEntity neighbor = world.getTileEntity(facingPos);
    if (neighbor != null
        && neighbor.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing.getOpposite()).orElse(null) != null) {
      return true;
    }
    return false;
  }
}
