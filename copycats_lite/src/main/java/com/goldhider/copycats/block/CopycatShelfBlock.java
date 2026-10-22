package com.goldhider.copycats.block;

import com.goldhider.copycats.CCProperties;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Repisa copycat: solo forma. No guarda ni muestra items, no tiene redstone ni conexión entre repisas.
 * (La block entity es únicamente la que guarda el material copiado, como en el resto de copycats.)
 * FACING es hacia donde mira el frente abierto; la parte trasera queda contra la pared.
 */
public class CopycatShelfBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape SHAPE_NORTH = makeShape(Direction.NORTH);
	private static final VoxelShape SHAPE_SOUTH = makeShape(Direction.SOUTH);
	private static final VoxelShape SHAPE_EAST = makeShape(Direction.EAST);
	private static final VoxelShape SHAPE_WEST = makeShape(Direction.WEST);

	/** Tablas de arriba y abajo (4 px de alto, 5 de fondo) y panel trasero de 3 px, medidas del shelf de vanilla. */
	private static VoxelShape makeShape(Direction facing) {
		return Shapes.or(
				rotated(facing, 0, 12, 11, 16, 16, 16),
				rotated(facing, 0, 0, 11, 16, 4, 16),
				rotated(facing, 0, 4, 13, 16, 12, 16)
		);
	}

	/** Caja definida con el frente hacia el norte (parte trasera en z=16), girada a la dirección pedida. */
	private static VoxelShape rotated(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
		return switch (facing) {
			case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
			case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
			case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
			default -> Block.box(x1, y1, z1, x2, y2, z2);
		};
	}

	public CopycatShelfBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
		registerDefaultState(defaultBlockState().setValue(CCProperties.SEE_THROUGH, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		builder.add(CCProperties.SEE_THROUGH);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return CopycatBehavior.useItemOn(stack, level, pos, player);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
		CopycatBehavior.addMaterialDrop(drops, builder);
		return drops;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SHAPE_SOUTH;
			case EAST -> SHAPE_EAST;
			case WEST -> SHAPE_WEST;
			default -> SHAPE_NORTH;
		};
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CopycatBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(CCProperties.SEE_THROUGH) ? Shapes.empty() : super.getOcclusionShape(state);
	}
}
