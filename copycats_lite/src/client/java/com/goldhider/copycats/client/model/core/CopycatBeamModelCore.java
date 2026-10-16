package com.goldhider.copycats.client.model.core;

import com.goldhider.copycats.block.CopycatBeamBlock;
import com.goldhider.copycats.client.model.BoxBuilder;
import com.goldhider.copycats.client.model.CopycatBoxCore;
import net.minecraft.world.level.block.state.BlockState;

/** Viga: columna de 8x8 px a lo largo del eje del bloque. */
public class CopycatBeamModelCore extends CopycatBoxCore {

    @Override
    public void emitBoxes(BlockState state, BoxBuilder boxes) {
        switch (state.getValue(CopycatBeamBlock.AXIS)) {
            case X -> boxes.box(0, 4, 4, 16, 12, 12);
            case Y -> boxes.box(4, 0, 4, 12, 16, 12);
            case Z -> boxes.box(4, 4, 0, 12, 12, 16);
        }
    }
}
