package com.goldhider.copycats.block;

import com.goldhider.copycats.CCProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Viga copycat: columna de 8x8 px a lo largo de un eje. El eje es el de la cara donde haces clic
 * (clic en el suelo = viga vertical, clic en una pared = viga horizontal hacia dentro de la pared).
 */
public class CopycatBeamBlock extends Block implements EntityBlock {
	public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;

	private static final VoxelShape SHAPE_X = Block.box(0, 4, 4, 16, 12, 12);
	private static final VoxelShape SHAPE_Y = Block.box(4, 0, 4, 12, 16, 12);
	private static final VoxelShape SHAPE_Z = Block.box(4, 4, 0, 12, 12, 16);

	public CopycatBeamBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AXIS, Axis.Y));
		registerDefaultState(defaultBlockState().setValue(CCProperties.SEE_THROUGH, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
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
		return switch (state.getValue(AXIS)) {
			case X -> SHAPE_X;
			case Y -> SHAPE_Y;
			case Z -> SHAPE_Z;
		};
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clicked = context.getClickedFace();
		return defaultBlockState().setValue(AXIS, clicked.getAxis());
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
