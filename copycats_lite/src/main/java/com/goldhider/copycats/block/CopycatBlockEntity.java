package com.goldhider.copycats.block;

import com.goldhider.copycats.CCRegistry;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Guarda el BlockState del material copiado y lo expone al renderizador como "render data" inmutable.
 */
public class CopycatBlockEntity extends BlockEntity implements RenderDataBlockEntity, MaterialHolder {
	private static final String MATERIAL_KEY = "material";

	@Nullable
	private BlockState material = null;

	public CopycatBlockEntity(BlockPos pos, BlockState state) {
		super(CCRegistry.COPYCAT_BE_TYPE, pos, state);
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
	public BlockState getMaterial() {
		return this.material;
	}

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

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}
}
