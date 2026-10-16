package com.goldhider.copycats.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Capa copycat de un solo material. Funciona como la nieve: cada item añade una capa de 2 px (hasta 8).
 * FACING es el lado abierto de la capa: se pega a la cara donde se hace clic (clic en el suelo = capa en el suelo).
 */
public class CopycatLayerBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
	public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
	public static final int MAX_LAYERS = 8;

	// [dirección 3D de FACING][capas 1..8]
	private static final VoxelShape[][] SHAPES = new VoxelShape[6][MAX_LAYERS + 1];

	static {
		for (Direction facing : Direction.values()) {
			for (int layers = 1; layers <= MAX_LAYERS; layers++) {
				SHAPES[facing.get3DDataValue()][layers] = makeShape(facing, layers * 2.0D);
			}
		}
	}

	private static VoxelShape makeShape(Direction facing, double t) {
		return switch (facing) {
			case UP -> Block.box(0, 0, 0, 16, t, 16);
			case DOWN -> Block.box(0, 16 - t, 0, 16, 16, 16);
			case NORTH -> Block.box(0, 0, 16 - t, 16, 16, 16);
			case SOUTH -> Block.box(0, 0, 0, 16, 16, t);
			case EAST -> Block.box(0, 0, 0, t, 16, 16);
			case WEST -> Block.box(16 - t, 0, 0, 16, 16, 16);
		};
	}

	public CopycatLayerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(FACING, Direction.UP)
				.setValue(LAYERS, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LAYERS);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return CopycatBehavior.useItemOn(stack, level, pos, player);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
		// La tabla de loot da una capa; cada capa extra es otro item.
		for (int i = 1; i < state.getValue(LAYERS); i++) {
			drops.add(new ItemStack(this));
		}
		CopycatBehavior.addMaterialDrop(drops, builder);
		return drops;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING).get3DDataValue()][state.getValue(LAYERS)];
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
		if (existing.is(this)) {
			return existing.setValue(LAYERS, Math.min(MAX_LAYERS, existing.getValue(LAYERS) + 1));
		}
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		if (!context.getItemInHand().is(this.asItem()) || state.getValue(LAYERS) >= MAX_LAYERS) {
			return false;
		}
		// Solo se apila si se hace clic por el lado abierto de la capa.
		return context.getClickedFace() == state.getValue(FACING);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CopycatBlockEntity(pos, state);
	}
}
