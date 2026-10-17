package com.goldhider.copycats.block;

import com.goldhider.copycats.CCProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Lógica común a todos los copycats: poner/quitar el material con clic derecho y devolverlo al romper.
 * Está separada porque algunos bloques heredan de clases de vanilla (StairBlock) y no de CopycatBlock.
 */
public final class CopycatBehavior {

	public static InteractionResult useItemOn(ItemStack stack, Level level, BlockPos pos, Player player) {
		return useItemOn(stack, level, pos, player, true);
	}

	/**
	 * @param removeWithEmptyHand si es false, la mano vacía no quita el material (para bloques como el letrero,
	 *                            donde la mano vacía abre el editor de texto).
	 */
	public static InteractionResult useItemOn(ItemStack stack, Level level, BlockPos pos, Player player, boolean removeWithEmptyHand) {
		if (!(level.getBlockEntity(pos) instanceof MaterialHolder copycat)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		@Nullable BlockState current = copycat.getMaterial();

		// Mano vacía: quitar el material actual.
		if (stack.isEmpty()) {
			if (!removeWithEmptyHand || current == null) {
				return InteractionResult.PASS;
			}
			if (!level.isClientSide()) {
				player.getInventory().placeItemBackInInventory(new ItemStack(current.getBlock()), Prediction.SERVER_ONLY);
				copycat.setMaterial(null);
				applySeeThrough(level, pos, null);
			}
			return InteractionResult.SUCCESS;
		}

		Block handBlock = Block.byItem(stack.getItem());
		BlockState newMaterial = handBlock.defaultBlockState();
		if (!isValidMaterial(handBlock, newMaterial, level, pos)) {
			return InteractionResult.PASS;
		}
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
			applySeeThrough(level, pos, newMaterial);
		}

		return InteractionResult.SUCCESS;
	}

	/**
	 * Ajusta el estado see_through: si no hay material (cajón con huecos) o el material no tapa la vista (cristal, hojas...), el copycat deja de ocluir a
	 * sus vecinos para que el terreno de al lado no desaparezca. Cambiar el estado conserva la block entity.
	 */
	public static void applySeeThrough(Level level, BlockPos pos, @Nullable BlockState material) {
		BlockState state = level.getBlockState(pos);
		if (!state.hasProperty(CCProperties.SEE_THROUGH)) {
			return;
		}
		// Sin material, el cajón base tiene huecos (textura con transparencia), así que también es transparente.
		boolean seeThrough = material == null || !material.canOcclude();
		if (state.getValue(CCProperties.SEE_THROUGH) != seeThrough) {
			level.setBlock(pos, state.setValue(CCProperties.SEE_THROUGH, seeThrough), Block.UPDATE_ALL);
		}
	}

	/** Solo cubos completos sin block entity (los copycats no pueden copiarse entre sí). */
	private static boolean isValidMaterial(Block block, BlockState state, Level level, BlockPos pos) {
		if (block instanceof EntityBlock) return false;
		return state.isCollisionShapeFullBlock(level, pos);
	}

	/** Añade el material copiado a los drops al romper el copycat. */
	public static void addMaterialDrop(List<ItemStack> drops, LootParams.Builder builder) {
		if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MaterialHolder copycat) {
			BlockState material = copycat.getMaterial();
			if (material != null) {
				drops.add(new ItemStack(material.getBlock()));
			}
		}
	}

	private CopycatBehavior() {
	}
}
