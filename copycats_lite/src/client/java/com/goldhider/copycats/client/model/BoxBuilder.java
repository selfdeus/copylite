package com.goldhider.copycats.client.model;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Dibuja cajas con la textura del cubo copiado (valla, muro, viga, letrero...).
 * <p>
 * Cada cara de la caja toma el sprite, el tinte, el sombreado y el material de render de las caras del cubo de origen
 * que miran en la misma dirección (así también salen las capas extra, como el borde de césped), y se calcula
 * su UV por posición, igual que un modelo JSON normal: la textura queda alineada con el mundo.
 * <p>
 * Todas las coordenadas van en píxeles (0..16). Con {@link #yaw} las cajas se giran alrededor del centro del bloque.
 */
public final class BoxBuilder {
    private final MeshView source;
    private final QuadEmitter dest;
    private final Supplier<@Nullable MeshView> overlaySource;
    private double yaw = 0.0;

    public BoxBuilder(MeshView source, QuadEmitter dest, Supplier<@Nullable MeshView> overlaySource) {
        this.source = source;
        this.dest = dest;
        this.overlaySource = overlaySource;
    }

    private static SpriteFinder spriteFinder(QuadView quad) {
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(quad.atlas().getId()).spriteFinder();
    }

    /** Gira lo que se dibuje después, en grados alrededor del eje Y del centro del bloque (convención de Axis.YP). */
    public BoxBuilder yaw(double degrees) {
        this.yaw = degrees;
        return this;
    }

    /** Dibuja una caja completa salvo las caras indicadas (por ejemplo las que quedan dentro de otra pieza). */
    public void box(double x1, double y1, double z1, double x2, double y2, double z2, Direction... skip) {
        for (Direction dir : Direction.values()) {
            boolean skipped = false;
            for (Direction s : skip) {
                if (s == dir) {
                    skipped = true;
                    break;
                }
            }
            if (!skipped) {
                face(dir, x1, y1, z1, x2, y2, z2);
            }
        }
    }

    private void face(Direction dir, double x1, double y1, double z1, double x2, double y2, double z2) {
        double[][] verts = vertices(dir, x1, y1, z1, x2, y2, z2);
        // Con giros de 90 grados la cara sigue estando en el borde del bloque, solo cambia de dirección.
        int turns = quarterTurns();
        Direction cull = turns >= 0 ? turn(cullFor(dir, x1, y1, z1, x2, y2, z2), turns) : null;
        Direction nominal = turns >= 0 ? turn(dir, turns) : dir;

        source.forEach(src -> {
            if (src.lightFace() != dir) {
                return;
            }
            TextureAtlasSprite sprite = spriteFinder(src).find(src);
            dest.copyFrom(src);
            for (int i = 0; i < 4; i++) {
                double[] p = verts[i];
                dest.uv(i, sprite.getU((float) (u(dir, p) / 16.0)), sprite.getV((float) (v(dir, p) / 16.0)));
                place(i, p[0], p[1], p[2]);
            }
            dest.nominalFace(nominal);
            dest.cullFace(cull);
            dest.emit();
        });
    }

    /**
     * Emite, con el mismo giro (yaw), los quads del modelo de superposición del bloque (por ejemplo las cadenas del
     * letrero, que vienen de un modelo JSON con su propia textura).
     */
    public void overlay() {
        MeshView overlay = overlaySource.get();
        if (overlay == null) {
            return;
        }
        double a = Math.toRadians(yaw);
        double cos = Math.cos(a);
        double sin = Math.sin(a);
        overlay.forEach(src -> {
            dest.copyFrom(src);
            for (int i = 0; i < 4; i++) {
                double dx = src.x(i) - 0.5;
                double dz = src.z(i) - 0.5;
                dest.pos(i, (float) (0.5 + dx * cos + dz * sin), src.y(i), (float) (0.5 - dx * sin + dz * cos));
            }
            dest.emit();
        });
    }

    /** Número de cuartos de vuelta si el giro es múltiplo de 90 grados; -1 si no lo es. */
    private int quarterTurns() {
        double q = yaw / 90.0;
        if (Math.abs(q - Math.rint(q)) > 1e-6) {
            return -1;
        }
        return (((int) Math.rint(q)) % 4 + 4) % 4;
    }

    private static @Nullable Direction turn(@Nullable Direction dir, int turns) {
        if (dir == null || dir.getAxis().isVertical()) {
            return dir;
        }
        for (int i = 0; i < turns; i++) {
            dir = dir.getCounterClockWise();
        }
        return dir;
    }

    private void place(int index, double x, double y, double z) {
        if (yaw != 0.0) {
            double a = Math.toRadians(yaw);
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            double dx = x - 8.0;
            double dz = z - 8.0;
            x = 8.0 + dx * cos + dz * sin;
            z = 8.0 - dx * sin + dz * cos;
        }
        dest.pos(index, (float) (x / 16.0), (float) (y / 16.0), (float) (z / 16.0));
    }

    /** Solo se marca para culling la cara que está justo en el borde del bloque. */
    private static @Nullable Direction cullFor(Direction dir, double x1, double y1, double z1, double x2, double y2, double z2) {
        boolean onEdge = switch (dir) {
            case DOWN -> y1 == 0.0;
            case UP -> y2 == 16.0;
            case NORTH -> z1 == 0.0;
            case SOUTH -> z2 == 16.0;
            case WEST -> x1 == 0.0;
            case EAST -> x2 == 16.0;
        };
        return onEdge ? dir : null;
    }

    /** Vértices de la cara en el mismo orden que los modelos de vanilla. */
    private static double[][] vertices(Direction dir, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (dir) {
            case DOWN -> new double[][]{{x1, y1, z2}, {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}};
            case UP -> new double[][]{{x1, y2, z1}, {x1, y2, z2}, {x2, y2, z2}, {x2, y2, z1}};
            case NORTH -> new double[][]{{x2, y2, z1}, {x2, y1, z1}, {x1, y1, z1}, {x1, y2, z1}};
            case SOUTH -> new double[][]{{x1, y2, z2}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}};
            case WEST -> new double[][]{{x1, y2, z1}, {x1, y1, z1}, {x1, y1, z2}, {x1, y2, z2}};
            case EAST -> new double[][]{{x2, y2, z2}, {x2, y1, z2}, {x2, y1, z1}, {x2, y2, z1}};
        };
    }

    /** UV por posición, como el UV automático de los modelos de vanilla (en 0..16). */
    private static double u(Direction dir, double[] p) {
        return switch (dir) {
            case DOWN, UP, SOUTH -> p[0];
            case NORTH -> 16.0 - p[0];
            case WEST -> p[2];
            case EAST -> 16.0 - p[2];
        };
    }

    private static double v(Direction dir, double[] p) {
        return switch (dir) {
            case DOWN -> 16.0 - p[2];
            case UP -> p[2];
            default -> 16.0 - p[1];
        };
    }
}
