package com.goldhider.copycats.client;

import com.goldhider.copycats.client.assembly.CopycatRenderContext;
import com.goldhider.copycats.client.model.BoxBuilder;
import com.goldhider.copycats.client.model.CopycatBoxCore;
import com.goldhider.copycats.client.model.CopycatCores;
import com.goldhider.copycats.client.model.CopycatModelCore;
import com.goldhider.copycats.client.model.MeshRenderContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Modelo del copycat. Toma los quads del modelo del material copiado (o del modelo base si no hay material)
 * y, si el bloque tiene una forma ("shape"), los recorta y reensambla con el motor de piezas.
 *
 * El tinte de bioma del material se calcula en {@link CopycatTint}.
 */
public class CopycatBlockStateModel implements BlockStateModel {
	private final BlockStateModel baseModel;
	private final String shape;
	@Nullable
	private final CopycatModelCore core;
	/** Modelo extra que se dibuja encima de la forma (cadenas del letrero), con su propia textura. */
	@Nullable
	private final BlockStateModel overlayModel;

	public CopycatBlockStateModel(BlockStateModel baseModel, String shape, @Nullable BlockStateModel overlayModel) {
		this.overlayModel = overlayModel;
		this.baseModel = baseModel;
		this.shape = shape;
		this.core = CopycatCores.get(shape);
	}

	/** Lee el material desde los datos de render (inmutables) de la block entity. */
	private static @Nullable BlockState getMaterial(BlockAndTintGetter level, BlockPos pos) {
		if (((FabricBlockGetter) level).getBlockEntityRenderData(pos) instanceof BlockState material) {
			return material;
		}
		return null;
	}

	private static BlockStateModel modelOf(BlockState state) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		BlockState material = getMaterial(level, pos);
		int skipMask = material == null ? 0 : skipMask(level, pos, material);
		if (skipMask == 0) {
			emitQuadsInto(emitter, level, pos, state, random, cullTest, material);
			return;
		}

		// Hay caras que el material (p. ej. un cristal) no dibuja contra un vecino igual: se arma todo en una malla
		// temporal y se pasan al emisor real solo los quads que no tocan esas caras.
		MutableMesh filtered = Renderer.get().mutableMesh();
		emitQuadsInto(filtered.emitter(), level, pos, state, random, cullTest, material);
		filtered.forEach(quad -> {
			Direction cullFace = quad.cullFace();
			if (cullFace != null && (skipMask & (1 << cullFace.ordinal())) != 0) {
				return;
			}
			emitter.copyFrom(quad);
			emitter.emit();
		});
	}

	/**
	 * Caras (bit por Direction.ordinal) que el material no dibuja porque el vecino cuenta como "el mismo bloque",
	 * igual que el cristal de vanilla. Se compara con el material del vecino si es un copycat, o con su estado si no.
	 */
	private static int skipMask(BlockAndTintGetter level, BlockPos pos, BlockState material) {
		// Atajo: casi ningún material oculta caras contra vecinos (solo cristal, hielo, slime...), y eso no cambia
		// nunca para un mismo estado, así que se averigua una vez y se recuerda.
		if (!MAY_SKIP.computeIfAbsent(material, CopycatBlockStateModel::computeMaySkip)) {
			return 0;
		}
		int mask = 0;
		for (Direction direction : Direction.values()) {
			BlockPos neighborPos = pos.relative(direction);
			BlockState neighborMaterial = getMaterial(level, neighborPos);
			BlockState neighbor = neighborMaterial != null ? neighborMaterial : level.getBlockState(neighborPos);
			if (material.skipRendering(neighbor, direction)) {
				mask |= 1 << direction.ordinal();
			}
		}
		return mask;
	}

	private static final Map<BlockState, Boolean> MAY_SKIP = new ConcurrentHashMap<>();

	/** Los materiales que ocultan caras lo hacen contra un vecino del mismo bloque: se prueba contra sí mismo. */
	private static boolean computeMaySkip(BlockState material) {
		for (Direction direction : Direction.values()) {
			if (material.skipRendering(material, direction)) {
				return true;
			}
		}
		return false;
	}

	private void emitQuadsInto(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest, @Nullable BlockState material) {
		BlockStateModel source = material == null ? baseModel : modelOf(material);
		BlockState sourceState = material == null ? state : material;

		if (core == null) {
			// Cubo completo: se emite el modelo de origen sin cambios.
			source.emitQuads(emitter, level, pos, sourceState, random, cullTest);
			return;
		}

		// Se capturan los quads del cubo de origen (sin descartar ninguno) y se reensamblan con la forma del bloque.
		MutableMesh mesh = Renderer.get().mutableMesh();
		source.emitQuads(mesh.emitter(), level, pos, sourceState, random, face -> false);

		if (core instanceof CopycatBoxCore boxCore) {
			// Formas hechas de cajas (valla, muro, viga, letrero); la malla de la cadena solo se crea si hace falta.
			boxCore.emitBoxes(state, new BoxBuilder(mesh, emitter, () -> overlayMesh(level, pos, state, random)));
			return;
		}

		CopycatRenderContext context = new MeshRenderContext(mesh, emitter, CopycatModelCore.MATERIAL_KEY);
		core.emitCopycatQuads(CopycatModelCore.MATERIAL_KEY, state, context, sourceState);
	}

	/** Quads del modelo de superposición (cadenas del letrero); null si el bloque no tiene. */
	private @Nullable MeshView overlayMesh(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		if (overlayModel == null) {
			return null;
		}
		MutableMesh overlayMesh = Renderer.get().mutableMesh();
		overlayModel.emitQuads(overlayMesh.emitter(), level, pos, state, random, face -> false);
		return overlayMesh;
	}

	/** skipMask: el resultado depende de qué caras se ocultan contra los vecinos, así que forma parte de la clave. */
	private record GeometryKey(Object sub, BlockState state, int skipMask) {
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return new GeometryKey(this, state, 0);
		}

		Object subkey = modelOf(material).createGeometryKey(level, pos, material, random);
		if (subkey == null) {
			return null;
		}
		return new GeometryKey(subkey, state, skipMask(level, pos, material));
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return baseModel.particleMaterial(level, pos, state);
		}
		return modelOf(material).particleMaterial(level, pos, material);
	}

	@Override
	@BakedQuad.MaterialFlags
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		@BakedQuad.MaterialFlags int flags = baseModel.materialFlags(level, pos, state, random);

		BlockState material = getMaterial(level, pos);
		if (material == null) {
			return flags;
		}
		return flags | modelOf(material).materialFlags(level, pos, material, random);
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
		// La Renderer API de Fabric hace esto innecesario: no se añaden partes.
	}

	@Override
	public Material.Baked particleMaterial() {
		return baseModel.particleMaterial();
	}

	@Override
	public @BakedQuad.MaterialFlags int materialFlags() {
		// Puede renderizar cualquier submodelo (animado o translúcido), así que se declara el caso general.
		return BakedQuad.FLAG_ANIMATED | BakedQuad.FLAG_TRANSLUCENT;
	}

	public record Unbaked(BlockStateModel.Unbaked baseModel, String shape, Optional<BlockStateModel.Unbaked> overlayModel) implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				BlockStateModel.Unbaked.CODEC.fieldOf("base_model").forGetter(Unbaked::baseModel),
				Codec.STRING.optionalFieldOf("shape", "block").forGetter(Unbaked::shape),
				BlockStateModel.Unbaked.CODEC.optionalFieldOf("overlay_model").forGetter(Unbaked::overlayModel)
		).apply(instance, Unbaked::new));

		@Override
		public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
			return CODEC;
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			baseModel.resolveDependencies(resolver);
			overlayModel.ifPresent(model -> model.resolveDependencies(resolver));
		}

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			return new CopycatBlockStateModel(baseModel.bake(baker), shape, overlayModel.map(model -> model.bake(baker)).orElse(null));
		}
	}
}
