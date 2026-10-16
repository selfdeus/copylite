package com.goldhider.copycats.block;

import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Block entity que guarda el material copiado (la usan CopycatBlockEntity y el letrero colgante). */
public interface MaterialHolder {
	@Nullable
	BlockState getMaterial();

	void setMaterial(@Nullable BlockState material);
}
