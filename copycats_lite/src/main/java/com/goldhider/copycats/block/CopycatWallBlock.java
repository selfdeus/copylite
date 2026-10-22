package com.goldhider.copycats.block;

import com.goldhider.copycats.CCProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Muro copycat. Hereda de WallBlock de vanilla: conexiones, formas y waterlogging son las de un muro normal.
 * Está en la etiqueta de muros, así que conecta con otros muros copycat y con los de vanilla.
 */
public class CopycatWallBlock extends WallBlock implements EntityBlock {
	public CopycatWallBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(CCProperties.SEE_THROUGH, true));
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult result = CopycatBehavior.useItemOn(stack, level, pos, player);
		if (result.consumesAction()) {
			return result;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
		CopycatBehavior.addMaterialDrop(drops, builder);
		return drops;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CopycatBlockEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CCProperties.SEE_THROUGH);
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(CCProperties.SEE_THROUGH) ? Shapes.empty() : super.getOcclusionShape(state);
	}
}
