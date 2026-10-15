package com.goldhider.copycats.client.model;

import com.goldhider.copycats.client.assembly.CopycatRenderContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Núcleo de modelo que dibuja cajas con {@link BoxBuilder} en vez de recortar piezas del cubo.
 * Sirve para formas con partes sueltas (valla, muro, viga, letrero).
 */
public abstract class CopycatBoxCore extends CopycatModelCore {

    public abstract void emitBoxes(BlockState state, BoxBuilder boxes);

    @Override
    public final void emitCopycatQuads(String key, BlockState state, CopycatRenderContext context, BlockState material) {
        // No se usa: CopycatBlockStateModel llama a emitBoxes directamente.
    }
}
