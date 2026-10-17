package com.goldhider.copycats.client.model;

import com.goldhider.copycats.client.assembly.CopycatRenderContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lógica de modelo específica de cada bloque, independiente de la plataforma.
 * Cada subclase ensambla piezas del cubo copiado usando la API de {@link CopycatRenderContext}.
 */
public abstract class CopycatModelCore {
    /** Clave del modelo del material copiado. */
    public static final String MATERIAL_KEY = "material";

    /** Si se usan las versiones "mejoradas" del modelo (más piezas para que la textura se vea mejor). */
    public boolean enhanced = true;

    /**
     * @param key      clave del modelo (siempre "material" en esta versión de un solo material)
     * @param state    estado del bloque copycat
     * @param context  contexto con el que ensamblar los quads
     * @param material estado del bloque copiado
     */
    public abstract void emitCopycatQuads(String key, BlockState state, CopycatRenderContext context, BlockState material);
}
