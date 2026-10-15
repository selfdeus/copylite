package com.goldhider.copycats.client.model;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sustituye a BakedModelHelper.cropAndMove de Create.
 * <p>
 * Recorta un quad plano alineado a los ejes contra una caja: cada vértice se limita ("clamp") a la caja y los UV
 * se recalculan por interpolación afín, de modo que la textura se recorta en vez de estirarse. El plano del quad
 * también se limita a la caja, lo que equivale a "mover" la cara hacia dentro. Después se aplica el desplazamiento.
 */
final class CropUtil {
    private static final double EPS = 1.0E-9;

    /**
     * @return false si el quad queda degenerado (área cero) y debe descartarse.
     */
    static boolean cropAndMove(MutableQuadView q, AABB crop, Vec3 move) {
        double[][] p = new double[4][3];
        double[] u = new double[4];
        double[] v = new double[4];
        for (int i = 0; i < 4; i++) {
            p[i][0] = q.x(i);
            p[i][1] = q.y(i);
            p[i][2] = q.z(i);
            u[i] = q.u(i);
            v[i] = q.v(i);
        }

        double[] min = {crop.minX, crop.minY, crop.minZ};
        double[] max = {crop.maxX, crop.maxY, crop.maxZ};

        // Eje normal del plano del quad: el de su cara de luz.
        Direction.Axis normal = q.lightFace().getAxis();
        int n = normal.ordinal();
        int a = (n + 1) % 3;
        int b = (n + 2) % 3;

        // Aristas adyacentes desde el vértice 0: e1 = p1-p0, e2 = p3-p0 (en los ejes del plano).
        double e1a = p[1][a] - p[0][a];
        double e1b = p[1][b] - p[0][b];
        double e2a = p[3][a] - p[0][a];
        double e2b = p[3][b] - p[0][b];
        double det = e1a * e2b - e1b * e2a;
        if (Math.abs(det) < EPS) {
            return false;
        }

        double du1 = u[1] - u[0];
        double dv1 = v[1] - v[0];
        double du2 = u[3] - u[0];
        double dv2 = v[3] - v[0];

        double[][] np = new double[4][3];
        double[] nu = new double[4];
        double[] nv = new double[4];
        for (int i = 0; i < 4; i++) {
            for (int k = 0; k < 3; k++) {
                np[i][k] = Math.max(min[k], Math.min(max[k], p[i][k]));
            }
            double da = np[i][a] - p[0][a];
            double db = np[i][b] - p[0][b];
            double s = (da * e2b - db * e2a) / det;
            double t = (e1a * db - e1b * da) / det;
            nu[i] = u[0] + s * du1 + t * du2;
            nv[i] = v[0] + s * dv1 + t * dv2;
        }

        // Área en el plano tras recortar (descarta quads colapsados).
        double area = Math.abs(
                (np[1][a] - np[0][a]) * (np[3][b] - np[0][b]) - (np[1][b] - np[0][b]) * (np[3][a] - np[0][a]));
        double areaFull = Math.abs(det);
        if (area < 1.0E-7 * Math.max(1.0, areaFull)) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            q.pos(i,
                    (float) (np[i][0] + move.x),
                    (float) (np[i][1] + move.y),
                    (float) (np[i][2] + move.z));
            q.uv(i, (float) nu[i], (float) nv[i]);
        }
        return true;
    }

    private CropUtil() {
    }
}
