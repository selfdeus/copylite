package com.goldhider.copycats.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Copycat de cubo completo. Clic derecho con un bloque para copiar su apariencia;
 * clic derecho con la mano vacía para quitarlo.
 */
public class CopycatBlock extends Block implements EntityBlock {
	public CopycatBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof CopycatBlockEntity copycat)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		@Nullable BlockState current = copycat.getMaterial();

		// Mano vacía: quitar el material actual.
		if (stack.isEmpty()) {
			if (current == null) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				player.getInventory().placeItemBackInInventory(new ItemStack(current.getBlock()), Prediction.SERVER_ONLY);
				copycat.setMaterial(null);
			}
			return InteractionResult.SUCCESS;
		}

		Block handBlock = Block.byItem(stack.getItem());
		if (!isValidMaterial(handBlock)) {
			return InteractionResult.PASS;
		}

		BlockState newMaterial = handBlock.defaultBlockState();
		if (newMaterial.equals(current)) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			if (!player.hasInfiniteMaterials()) {
				stack.consume(1, player);
			}
			if (current != null) {
				player.getInventory().placeItemBackInInventory(new ItemStack(current.getBlock()), Prediction.SERVER_ONLY);
			}
			copycat.setMaterial(newMaterial);
		}

		return InteractionResult.SUCCESS;
	}

	/** Solo bloques "normales": sin block entity, que no sean aire ni otro copycat. */
	private static boolean isValidMaterial(Block block) {
		if (block == Blocks.AIR) return false;
		if (block instanceof EntityBlock) return false;
		return true;
	}

	/** Al romper el copycat se devuelve también el material copiado. */
	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
		if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof CopycatBlockEntity copycat) {
			BlockState material = copycat.getMaterial();
			if (material != null) {
				drops.add(new ItemStack(material.getBlock()));
			}
		}
		return drops;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CopycatBlockEntity(pos, state);
	}
}
