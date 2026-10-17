package com.goldhider.copycats.client.model.core;

import com.goldhider.copycats.block.CopycatShelfBlock;
import com.goldhider.copycats.client.model.BoxBuilder;
import com.goldhider.copycats.client.model.CopycatBoxCore;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Repisa: dos tablas de 4 px (arriba y abajo, 5 px de fondo) y un panel trasero de 3 px. */
public class CopycatShelfModelCore extends CopycatBoxCore {

    @Override
    public void emitBoxes(BlockState state, BoxBuilder boxes) {
        // Se define con el frente hacia el norte y se gira según FACING (misma convención que Axis.YP).
        double yaw = switch (state.getValue(CopycatShelfBlock.FACING)) {
            case EAST -> -90;
            case SOUTH -> 180;
            case WEST -> 90;
            default -> 0;
        };
        boxes.yaw(yaw);

        boxes.box(0, 12, 11, 16, 16, 16);
        boxes.box(0, 0, 11, 16, 4, 16);
        // Las caras de arriba y abajo del panel quedan pegadas a las tablas.
        boxes.box(0, 4, 13, 16, 12, 16, Direction.UP, Direction.DOWN);
    }
}
