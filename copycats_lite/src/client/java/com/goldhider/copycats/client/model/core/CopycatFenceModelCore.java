package com.goldhider.copycats.client.model.core;

import com.goldhider.copycats.client.model.BoxBuilder;
import com.goldhider.copycats.client.model.CopycatBoxCore;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Valla: poste de 4x4 y dos barras de 2 px por cada lado conectado, con las medidas de la valla de vanilla. */
public class CopycatFenceModelCore extends CopycatBoxCore {

    @Override
    public void emitBoxes(BlockState state, BoxBuilder boxes) {
        boxes.box(6, 0, 6, 10, 16, 10);

        if (state.getValue(BlockStateProperties.NORTH)) {
            arms(boxes, Direction.NORTH);
        }
        if (state.getValue(BlockStateProperties.SOUTH)) {
            arms(boxes, Direction.SOUTH);
        }
        if (state.getValue(BlockStateProperties.WEST)) {
            arms(boxes, Direction.WEST);
        }
        if (state.getValue(BlockStateProperties.EAST)) {
            arms(boxes, Direction.EAST);
        }
    }

    private static void arms(BoxBuilder boxes, Direction dir) {
        bar(boxes, dir, 6, 9);
        bar(boxes, dir, 12, 15);
    }

    /** Barra desde el borde del bloque hasta la cara del poste; la cara que toca el poste no se dibuja. */
    private static void bar(BoxBuilder boxes, Direction dir, double y1, double y2) {
        switch (dir) {
            case NORTH -> boxes.box(7, y1, 0, 9, y2, 6, Direction.SOUTH);
            case SOUTH -> boxes.box(7, y1, 10, 9, y2, 16, Direction.NORTH);
            case WEST -> boxes.box(0, y1, 7, 6, y2, 9, Direction.EAST);
            case EAST -> boxes.box(10, y1, 7, 16, y2, 9, Direction.WEST);
            default -> {
            }
        }
    }
}
