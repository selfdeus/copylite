package com.goldhider.copycats.block;

import com.goldhider.copycats.CCRegistry;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Block entity del letrero colgante copycat: es un letrero normal (texto delantero y trasero, tinte, cera)
 * que además guarda el material copiado, igual que CopycatBlockEntity.
 * La sincronización con el cliente la hace SignBlockEntity (getUpdateTag guarda todo lo "custom", incluido el material).
 */
public class CopycatHangingSignBlockEntity extends SignBlockEntity implements RenderDataBlockEntity, MaterialHolder {
	private static final String MATERIAL_KEY = "material";

	@Nullable
	private BlockState material = null;

	public CopycatHangingSignBlockEntity(BlockPos pos, BlockState state) {
		super(CCRegistry.COPYCAT_HANGING_SIGN_BE_TYPE, pos, state);
	}

	@Override
	public int getTextLineHeight() {
		return HangingSignBlockEntity.TEXT_LINE_HEIGHT;
	}

	@Override
	public int getMaxTextLineWidth() {
		return HangingSignBlockEntity.MAX_TEXT_LINE_WIDTH;
	}

	@Override
	public SoundEvent getSignInteractionFailedSoundEvent() {
		return SoundEvents.WAXED_HANGING_SIGN_INTERACT_FAIL;
	}

	@Override
	public void loadAdditional(ValueInput data) {
		super.loadAdditional(data);

		material = data.read(MATERIAL_KEY, BlockState.CODEC).orElse(null);
		if (material != null && material.isAir()) {
			material = null;
		}

		if (this.getLevel() != null && this.getLevel().isClientSide()) {
			// Fuerza un re-mesh del chunk en el cliente.
			level.sendBlockUpdated(worldPosition, null, null, 0);
		}
	}

	@Override
	public void saveAdditional(ValueOutput data) {
		super.saveAdditional(data);

		// Siempre hay que guardar algo, si no el paquete de sincronización no se aplica.
		data.store(MATERIAL_KEY, BlockState.CODEC, material != null ? material : Blocks.AIR.defaultBlockState());
	}

	@Override
	public void setChanged() {
		super.setChanged();

		if (this.hasLevel() && !this.getLevel().isClientSide()) {
			((ServerLevel) level).getChunkSource().blockChanged(getBlockPos());
		}
	}

	@Nullable
	@Override
	public BlockState getMaterial() {
		return this.material;
	}

	@Override
	public void setMaterial(@Nullable BlockState material) {
		if (material != null && material.isAir()) {
			material = null;
		}

		this.material = material;
		this.setChanged();
	}

	@Nullable
	@Override
	public BlockState getRenderData() {
		return this.material;
	}
}
