package com.goldhider.copycats.client.model;

import com.goldhider.copycats.client.model.core.CopycatBeamModelCore;
import com.goldhider.copycats.client.model.core.CopycatFenceModelCore;
import com.goldhider.copycats.client.model.core.CopycatHangingSignModelCore;
import com.goldhider.copycats.client.model.core.CopycatLayerModelCore;
import com.goldhider.copycats.client.model.core.CopycatShelfModelCore;
import com.goldhider.copycats.client.model.core.CopycatSlabModelCore;
import com.goldhider.copycats.client.model.core.CopycatStairsModelCore;
import com.goldhider.copycats.client.model.core.CopycatWallModelCore;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Asocia el nombre de forma ("shape" en el blockstate JSON) con su lógica de modelo.
 * Una forma sin entrada (por ejemplo "block") copia el cubo del material tal cual.
 */
public final class CopycatCores {
    private static final Map<String, CopycatModelCore> CORES = Map.of(
            "slab", new CopycatSlabModelCore(),
            "stairs", new CopycatStairsModelCore(),
            "layer", new CopycatLayerModelCore(),
            "fence", new CopycatFenceModelCore(),
            "wall", new CopycatWallModelCore(),
            "beam", new CopycatBeamModelCore(),
            "hanging_sign", new CopycatHangingSignModelCore(),
            "shelf", new CopycatShelfModelCore()
    );

    @Nullable
    public static CopycatModelCore get(String shape) {
        return CORES.get(shape);
    }

    private CopycatCores() {
    }
}
