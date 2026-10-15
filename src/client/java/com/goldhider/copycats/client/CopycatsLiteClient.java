package com.goldhider.copycats.client;

import com.goldhider.copycats.CopycatsLite;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;

public class CopycatsLiteClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// El id debe coincidir con "fabric:type" en los blockstates JSON.
		CustomUnbakedBlockStateModel.register(CopycatsLite.id("copycat"), CopycatBlockStateModel.Unbaked.CODEC);
	}
}
