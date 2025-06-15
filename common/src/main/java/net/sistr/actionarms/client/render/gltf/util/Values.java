package net.sistr.actionarms.client.render.gltf.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Values {
    // 定数の追加
    public static final Position ZERO_POSITION = new Position(0, 0, 0);
    public static final Position UNIT_X = new Position(1, 0, 0);
    public static final Position UNIT_Y = new Position(0, 1, 0);
    public static final Position UNIT_Z = new Position(0, 0, 1);

    public static final Rotation IDENTITY_ROTATION = new Rotation(0, 0, 0, 1);
    public static final Scale UNIT_SCALE = new Scale(1, 1, 1);

    public static final RGBA WHITE = new RGBA(1, 1, 1, 1);
    public static final RGBA BLACK = new RGBA(0, 0, 0, 1);
    public static final RGBA TRANSPARENT = new RGBA(0, 0, 0, 0);

    public record Position(float x, float y, float z) {
        public static Position from(Vector3f vector) {
            return new Position(vector.x, vector.y, vector.z);
        }

        // 逆変換メソッド
        public Vector3f toVector3f() {
            return new Vector3f(x, y, z);
        }

        // 配列からの変換
        public static Position from(float @Nullable [] array) {
            if (array == null || array.length < 3) {
                throw new IllegalArgumentException("Array must have at least 3 elements");
            }
            return new Position(array[0], array[1], array[2]);
        }

        // 配列への変換
        public float[] toArray() {
            return new float[]{x, y, z};
        }

        // 加算
        public Position add(Position other) {
            return new Position(x + other.x, y + other.y, z + other.z);
        }

        // 減算
        public Position subtract(Position other) {
            return new Position(x - other.x, y - other.y, z - other.z);
        }

        // スカラー倍
        public Position multiply(float scalar) {
            return new Position(x * scalar, y * scalar, z * scalar);
        }

        // 距離計算
        public float distanceTo(Position other) {
            float dx = x - other.x;
            float dy = y - other.y;
            float dz = z - other.z;
            return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        // 長さ計算
        public float length() {
            return (float) Math.sqrt(x * x + y * y + z * z);
        }

        // 正規化
        public Position normalize() {
            float len = length();
            return len > 0 ? new Position(x / len, y / len, z / len) : this;
        }

        // 線形補間
        public Position lerp(Position other, float t) {
            return new Position(
                    x + (other.x - x) * t,
                    y + (other.y - y) * t,
                    z + (other.z - z) * t
            );
        }
    }

    public record Rotation(float x, float y, float z, float w) {
        public static Rotation from(Vector3f vector) {
            return new Rotation(vector.x, vector.y, vector.z, 1);
        }

        // Quaternionfへの変換
        public Quaternionf toQuaternionf() {
            return new Quaternionf(x, y, z, w);
        }

        // Quaternionfからの変換
        public static Rotation from(Quaternionf q) {
            return new Rotation(q.x, q.y, q.z, q.w);
        }

        // 配列からの変換
        public static Rotation from(float @Nullable [] array) {
            if (array == null || array.length < 4) {
                throw new IllegalArgumentException("Array must have at least 4 elements");
            }
            return new Rotation(array[0], array[1], array[2], array[3]);
        }

        // 配列への変換
        public float[] toArray() {
            return new float[]{x, y, z, w};
        }

        // 正規化
        public Rotation normalize() {
            float len = (float) Math.sqrt(x * x + y * y + z * z + w * w);
            return len > 0 ? new Rotation(x / len, y / len, z / len, w / len) : this;
        }

        // 球面線形補間
        public Rotation slerp(Rotation other, float t) {
            Quaternionf q1 = toQuaternionf();
            Quaternionf q2 = other.toQuaternionf();
            Quaternionf result = q1.slerp(q2, t);
            return from(result);
        }
    }

    public record Scale(float x, float y, float z) {
        public static Scale from(Vector3f vector) {
            return new Scale(vector.x, vector.y, vector.z);
        }

        // Vector3fへの変換
        public Vector3f toVector3f() {
            return new Vector3f(x, y, z);
        }

        // 配列からの変換
        public static Scale from(float @Nullable [] array) {
            if (array == null || array.length < 3) {
                throw new IllegalArgumentException("Array must have at least 3 elements");
            }
            return new Scale(array[0], array[1], array[2]);
        }

        // 配列への変換
        public float[] toArray() {
            return new float[]{x, y, z};
        }

        // 乗算
        public Scale multiply(Scale other) {
            return new Scale(x * other.x, y * other.y, z * other.z);
        }

        // スカラー乗算
        public Scale multiply(float scalar) {
            return new Scale(x * scalar, y * scalar, z * scalar);
        }

        // 線形補間
        public Scale lerp(Scale other, float t) {
            return new Scale(
                    x + (other.x - x) * t,
                    y + (other.y - y) * t,
                    z + (other.z - z) * t
            );
        }
    }

    public record RGBA(float r, float g, float b, float a) {
        public static RGBA from(Vector4f color) {
            return new RGBA(color.x, color.y, color.z, color.w);
        }

        public RGBA {
            // 値の範囲を[0,1]に制限
            if (r < 0 || r > 1 || g < 0 || g > 1 || b < 0 || b > 1 || a < 0 || a > 1) {
                throw new IllegalArgumentException("RGBA values must be in range [0, 1]");
            }
        }

        // Vector4fへの変換
        public Vector4f toVector4f() {
            return new Vector4f(r, g, b, a);
        }

        // RGBへの変換
        public RGB toRGB() {
            return new RGB(r, g, b);
        }

        // 配列からの変換
        public static RGBA from(float @Nullable [] array) {
            if (array == null || array.length < 4) {
                throw new IllegalArgumentException("Array must have at least 4 elements");
            }
            return new RGBA(array[0], array[1], array[2], array[3]);
        }

        // 配列への変換
        public float[] toArray() {
            return new float[]{r, g, b, a};
        }

        // アルファを変更した新しいRGBAを作成
        public RGBA withAlpha(float newAlpha) {
            return new RGBA(r, g, b, Math.max(0, Math.min(1, newAlpha)));
        }

        // 線形補間
        public RGBA lerp(RGBA other, float t) {
            return new RGBA(
                    r + (other.r - r) * t,
                    g + (other.g - g) * t,
                    b + (other.b - b) * t,
                    a + (other.a - a) * t
            );
        }

        // ガンマ補正
        public RGBA gammaCorrect(float gamma) {
            return new RGBA(
                    (float) Math.pow(r, gamma),
                    (float) Math.pow(g, gamma),
                    (float) Math.pow(b, gamma),
                    a
            );
        }

        @Override
        public @NotNull String toString() {
            return String.format("rgba(%.3f, %.3f, %.3f, %.3f)", r, g, b, a);
        }
    }

    public record RGB(float r, float g, float b) {
        public static RGB from(Vector3f color) {
            return new RGB(color.x, color.y, color.z);
        }

        public RGB {
            // 値の範囲を[0,1]に制限
            if (r < 0 || r > 1 || g < 0 || g > 1 || b < 0 || b > 1) {
                throw new IllegalArgumentException("RGB values must be in range [0, 1]");
            }
        }

        // Vector3fへの変換
        public Vector3f toVector3f() {
            return new Vector3f(r, g, b);
        }

        // RGBAへの変換
        public RGBA toRGBA(float alpha) {
            return new RGBA(r, g, b, alpha);
        }

        // 配列からの変換
        public static RGB from(float @Nullable [] array) {
            if (array == null || array.length < 3) {
                throw new IllegalArgumentException("Array must have at least 3 elements");
            }
            return new RGB(array[0], array[1], array[2]);
        }

        // 配列への変換
        public float[] toArray() {
            return new float[]{r, g, b};
        }

        // 線形補間
        public RGB lerp(RGB other, float t) {
            return new RGB(
                    r + (other.r - r) * t,
                    g + (other.g - g) * t,
                    b + (other.b - b) * t
            );
        }

        // ガンマ補正
        public RGB gammaCorrect(float gamma) {
            return new RGB(
                    (float) Math.pow(r, gamma),
                    (float) Math.pow(g, gamma),
                    (float) Math.pow(b, gamma)
            );
        }

        @Override
        public @NotNull String toString() {
            return String.format("rgb(%.3f, %.3f, %.3f)", r, g, b);
        }
    }

    public record Normal(float x, float y, float z) {
        public static Normal from(Vector3f vector) {
            return new Normal(vector.x, vector.y, vector.z);
        }

        // Vector3fへの変換
        public Vector3f toVector3f() {
            return new Vector3f(x, y, z);
        }

        // 配列からの変換
        public static Normal from(float @Nullable [] array) {
            if (array == null || array.length < 3) {
                throw new IllegalArgumentException("Array must have at least 3 elements");
            }
            return new Normal(array[0], array[1], array[2]);
        }

        // 配列への変換
        public float[] toArray() {
            return new float[]{x, y, z};
        }

        // 正規化
        public Normal normalize() {
            float len = (float) Math.sqrt(x * x + y * y + z * z);
            return len > 0 ? new Normal(x / len, y / len, z / len) : this;
        }

        // 内積計算
        public float dot(Normal other) {
            return x * other.x + y * other.y + z * other.z;
        }

        // 外積計算
        public Normal cross(Normal other) {
            return new Normal(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x
            );
        }
    }

    public record Matrix4x4(float[] data) {
        public Matrix4x4 {
            if (data == null || data.length != 16) {
                throw new IllegalArgumentException("Matrix4x4 data array must have exactly 16 elements");
            }
            // 防御的コピー
            data = data.clone();
        }

        public static Matrix4x4 from(Matrix4f matrix) {
            float[] data = new float[16];
            matrix.get(data);
            return new Matrix4x4(data);
        }

        // 単位行列の作成
        public static Matrix4x4 identity() {
            return new Matrix4x4(new float[]{
                    1, 0, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1
            });
        }

        // Matrix4fへの変換
        public Matrix4f toMatrix4f() {
            return new Matrix4f().set(data);
        }

        // 特定の要素へのアクセス
        public float get(int row, int col) {
            return data[row * 4 + col];
        }

        // 行列の乗算
        public Matrix4x4 multiply(Matrix4x4 other) {
            Matrix4f m1 = toMatrix4f();
            Matrix4f m2 = other.toMatrix4f();
            return from(m1.mul(m2));
        }

        // 変換行列の作成
        public static Matrix4x4 createTranslation(Position pos) {
            Matrix4f matrix = new Matrix4f().identity().translate(pos.x(), pos.y(), pos.z());
            return from(matrix);
        }

        public static Matrix4x4 createRotation(Rotation rot) {
            Matrix4f matrix = new Matrix4f().identity().rotate(rot.toQuaternionf());
            return from(matrix);
        }

        public static Matrix4x4 createScale(Scale scale) {
            Matrix4f matrix = new Matrix4f().identity().scale(scale.x(), scale.y(), scale.z());
            return from(matrix);
        }

        // TRSから変換行列を作成
        public static Matrix4x4 createTransform(Position translation, Rotation rotation, Scale scale) {
            Matrix4f matrix = new Matrix4f().identity()
                    .translate(translation.x(), translation.y(), translation.z())
                    .rotate(rotation.toQuaternionf())
                    .scale(scale.x(), scale.y(), scale.z());
            return from(matrix);
        }

        // 逆行列の計算
        public Matrix4x4 inverse() {
            Matrix4f matrix = toMatrix4f();
            Matrix4f inverted = matrix.invert(new Matrix4f());
            return from(inverted);
        }

        // 行列の転置
        public Matrix4x4 transpose() {
            Matrix4f matrix = toMatrix4f();
            Matrix4f transposed = matrix.transpose(new Matrix4f());
            return from(transposed);
        }

        @Override
        public @NotNull String toString() {
            return String.format("""
                            Matrix4x4[
                              [%.3f, %.3f, %.3f, %.3f]
                              [%.3f, %.3f, %.3f, %.3f]
                              [%.3f, %.3f, %.3f, %.3f]
                              [%.3f, %.3f, %.3f, %.3f]
                            ]""",
                    data[0], data[1], data[2], data[3],
                    data[4], data[5], data[6], data[7],
                    data[8], data[9], data[10], data[11],
                    data[12], data[13], data[14], data[15]);
        }
    }

}
