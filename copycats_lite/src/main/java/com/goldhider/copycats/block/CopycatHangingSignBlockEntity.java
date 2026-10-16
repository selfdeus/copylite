package com.goldhider.copycats.block;

import com.goldhider.copycats.CCRegistry;
import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Block entity del letrero colgante copycat: es un letrero normal (texto delantero y trasero, tinte, cera)
 * que además guarda el material copiado, igual que CopycatBlockEntity.
 * La sincronización con el cliente la hace SignBlockEntity (getUpdateTag guarda todo lo "custom", incluido el material).
 */
public class CopycatHangingSignBlockEntity extends SignBlockEntity implements RenderDataBlockEntity, MaterialHolder {
	// Valores del letrero colgante de vanilla (en 26.3 son privados en HangingSignBlockEntity).
	private static final int TEXT_LINE_HEIGHT = 9;
	private static final int MAX_TEXT_LINE_WIDTH = 60;

	private static final String MATERIAL_KEY = "material";

	@Nullable
	private static Method GET_TEXT = null;
	private static Object[] SLOTS = new Object[0];

	@Nullable
	private BlockState material = null;

	public CopycatHangingSignBlockEntity(BlockPos pos, BlockState state) {
		super(CCRegistry.COPYCAT_HANGING_SIGN_BE_TYPE, pos, state);
	}

	/**
	 * Texto de la cara delantera o trasera. En 26.3 SignBlockEntity#getText recibe un enum (SignTextSlot) en vez de un
	 * boolean; se busca ese método por reflexión para no depender del nombre exacto del tipo ni de sus constantes
	 * (FRONT / BACK, o por orden si se llamaran de otra forma).
	 */
	public SignText getSideText(boolean front) {
		try {
			if (GET_TEXT == null) {
				for (Method method : SignBlockEntity.class.getDeclaredMethods()) {
					if (method.getName().equals("getText") && method.getParameterCount() == 1 && method.getParameterTypes()[0].isEnum()) {
						method.setAccessible(true);
						GET_TEXT = method;
						SLOTS = method.getParameterTypes()[0].getEnumConstants();
						break;
					}
				}
			}
			if (GET_TEXT != null) {
				String wanted = front ? "FRONT" : "BACK";
				Object slot = Arrays.stream(SLOTS).filter(s -> ((Enum<?>) s).name().equalsIgnoreCase(wanted)).findFirst()
						.orElse(SLOTS[Math.min(front ? 0 : 1, SLOTS.length - 1)]);
				return (SignText) GET_TEXT.invoke(this, slot);
			}
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			// Cae al texto vacío más abajo.
		}
		return new SignText();
	}

	@Override
	public int getTextLineHeight() {
		return TEXT_LINE_HEIGHT;
	}

	@Override
	public int getMaxTextLineWidth() {
		return MAX_TEXT_LINE_WIDTH;
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
