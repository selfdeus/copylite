package com.goldhider.copycats;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class CCProperties {
	/**
	 * true cuando el material copiado no tapa la vista (cristal, hojas, hielo...). Con este estado el bloque no ocluye
	 * a sus vecinos, así que el terreno de al lado sí dibuja sus caras (si no, se ve el cielo a través del cristal).
	 * Lo ajusta CopycatBehavior al poner o quitar el material.
	 */
	public static final BooleanProperty SEE_THROUGH = BooleanProperty.create("see_through");

	private CCProperties() {
	}
}
