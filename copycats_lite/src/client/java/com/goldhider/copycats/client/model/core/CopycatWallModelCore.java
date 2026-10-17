package com.goldhider.copycats.client.model.core;

import com.goldhider.copycats.client.model.BoxBuilder;
import com.goldhider.copycats.client.model.CopycatBoxCore;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WallSide;

/** Muro: poste de 8x8 (si UP) y brazos de 6 px de ancho, bajos (14 px) o altos (16 px), como el muro de vanilla. */
public class CopycatWallModelCore extends CopycatBoxCore {

    @Override
    public void emitBoxes(BlockState state, BoxBuilder boxes) {
        boolean up = state.getValue(BlockStateProperties.UP);
        if (up) {
            boxes.box(4, 0, 4, 12, 16, 12);
        }

        arm(boxes, Direction.NORTH, state.getValue(BlockStateProperties.NORTH_WALL), up);
        arm(boxes, Direction.SOUTH, state.getValue(BlockStateProperties.SOUTH_WALL), up);
        arm(boxes, Direction.WEST, state.getValue(BlockStateProperties.WEST_WALL), up);
        arm(boxes, Direction.EAST, state.getValue(BlockStateProperties.EAST_WALL), up);
    }

    /** Con poste, el brazo llega hasta su cara (4 px del borde); sin poste, hasta el centro (8 px). */
    private static void arm(BoxBuilder boxes, Direction dir, WallSide side, boolean up) {
        if (side == WallSide.NONE) {
            return;
        }
        double top = side == WallSide.TALL ? 16 : 14;
        double inner = up ? 4 : 8;
        switch (dir) {
            case NORTH -> boxes.box(5, 0, 0, 11, top, inner, Direction.SOUTH);
            case SOUTH -> boxes.box(5, 0, 16 - inner, 11, top, 16, Direction.NORTH);
            case WEST -> boxes.box(0, 0, 5, inner, top, 11, Direction.EAST);
            case EAST -> boxes.box(16 - inner, 0, 5, 16, top, 11, Direction.WEST);
            default -> {
            }
        }
    }
}
