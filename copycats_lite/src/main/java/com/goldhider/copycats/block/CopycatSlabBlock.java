package com.goldhider.copycats.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
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
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Losa copycat de un solo material. Se puede colocar sobre cualquiera de los tres ejes.
 * SlabType.BOTTOM es la mitad del lado negativo del eje (abajo, norte u oeste) y TOP la del lado positivo.
 */
public class CopycatSlabBlock extends Block implements EntityBlock {
	public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;
	public static final EnumProperty<SlabType> SLAB_TYPE = BlockStateProperties.SLAB_TYPE;

	// [eje][0 = BOTTOM, 1 = TOP]
	private static final VoxelShape[][] SHAPES = {
			{Block.box(0, 0, 0, 8, 16, 16), Block.box(8, 0, 0, 16, 16, 16)}, // X
			{Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 16, 16, 16)}, // Y
			{Block.box(0, 0, 0, 16, 16, 8), Block.box(0, 0, 8, 16, 16, 16)}  // Z
	};

	public CopycatSlabBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(AXIS, Axis.Y)
				.setValue(SLAB_TYPE, SlabType.BOTTOM));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, SLAB_TYPE);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return CopycatBehavior.useItemOn(stack, level, pos, player);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
		// Una losa doble son dos losas: la tabla de loot da una y aquí se añade la otra.
		if (state.getValue(SLAB_TYPE) == SlabType.DOUBLE) {
			drops.add(new ItemStack(this));
		}
		CopycatBehavior.addMaterialDrop(drops, builder);
		return drops;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		SlabType type = state.getValue(SLAB_TYPE);
		if (type == SlabType.DOUBLE) {
			return Shapes.block();
		}
		return SHAPES[state.getValue(AXIS).ordinal()][type == SlabType.BOTTOM ? 0 : 1];
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		BlockState existing = context.getLevel().getBlockState(pos);
		if (existing.is(this)) {
			return existing.setValue(SLAB_TYPE, SlabType.DOUBLE);
		}

		// Como una losa normal: eje Y por defecto. Agachado (shift), se coloca vertical según hacia dónde mires.
		Axis axis = Axis.Y;
		if (context.isSecondaryUseActive()) {
			for (Direction direction : context.getNearestLookingDirections()) {
				if (direction.getAxis().isHorizontal()) {
					axis = direction.getAxis();
					break;
				}
			}
		}
		boolean upperHalfClicked = isUpperHalf(axis, context);
		Direction clickedFace = context.getClickedFace();
		boolean bottom = clickedFace == Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE)
				|| clickedFace.getAxis() != axis && !upperHalfClicked;
		return defaultBlockState()
				.setValue(AXIS, axis)
				.setValue(SLAB_TYPE, bottom ? SlabType.BOTTOM : SlabType.TOP);
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		SlabType type = state.getValue(SLAB_TYPE);
		if (type == SlabType.DOUBLE || !context.getItemInHand().is(this.asItem())) {
			return false;
		}
		Axis axis = state.getValue(AXIS);
		boolean upperHalfClicked = isUpperHalf(axis, context);
		Direction clickedFace = context.getClickedFace();
		if (type == SlabType.BOTTOM) {
			return clickedFace == Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE) || upperHalfClicked;
		}
		return clickedFace == Direction.fromAxisAndDirection(axis, AxisDirection.NEGATIVE) || !upperHalfClicked;
	}

	private static boolean isUpperHalf(Axis axis, BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		return switch (axis) {
			case X -> context.getClickLocation().x - pos.getX() > 0.5D;
			case Y -> context.getClickLocation().y - pos.getY() > 0.5D;
			case Z -> context.getClickLocation().z - pos.getZ() > 0.5D;
		};
	}

	/** Dirección hacia la que "mira" la losa: negativa para BOTTOM, positiva para TOP. */
	public static Direction getApparentDirection(BlockState state) {
		return Direction.fromAxisAndDirection(state.getValue(AXIS),
				state.getValue(SLAB_TYPE) == SlabType.BOTTOM ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CopycatBlockEntity(pos, state);
	}
}
