package com.goldhider.copycats.block;

import com.goldhider.copycats.CCRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Letrero colgante copycat (solo de techo). Hereda de CeilingHangingSignBlock: rotación, "attached", formas,
 * soporte, waterlogging, edición de texto, tinte, brillo y cera son los de vanilla.
 * La tabla se dibuja con el material copiado y el texto lo dibuja CopycatHangingSignRenderer.
 * <p>
 * Material: clic derecho con un bloque completo para ponerlo; con la mano vacía y agachado para quitarlo
 * (con la mano vacía sin agacharse se abre el editor de texto, como en un letrero normal).
 */
public class CopycatHangingSignBlock extends CeilingHangingSignBlock {
	public CopycatHangingSignBlock(Properties properties) {
		super(WoodType.OAK, properties);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		InteractionResult result = CopycatBehavior.useItemOn(stack, level, pos, player, player.isShiftKeyDown());
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

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CopycatHangingSignBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, CCRegistry.COPYCAT_HANGING_SIGN_BE_TYPE, SignBlockEntity::tick);
	}
}
