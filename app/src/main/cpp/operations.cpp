#include <jni.h>
#include <vector>
#include <cmath>
#include <stdexcept>

using namespace std;

// Type alias for 2D float matrix
using Matrix = vector<vector<float>>;

// Convert flat float array from Java to 2D matrix
Matrix convertToMatrix(JNIEnv* env, jfloatArray data, int rows, int cols) {
    jfloat* elements = env->GetFloatArrayElements(data, nullptr);
    Matrix matrix(rows, vector<float>(cols));

    for (int i = 0; i < rows; ++i)
        for (int j = 0; j < cols; ++j)
            matrix[i][j] = elements[i * cols + j];

    env->ReleaseFloatArrayElements(data, elements, JNI_ABORT);
    return matrix;
}

// Convert 2D matrix to flat float array for Java
jfloatArray convertToFlatArray(JNIEnv* env, const Matrix& matrix) {
    int rows = matrix.size();
    int cols = matrix[0].size();
    vector<jfloat> flat(rows * cols);

    for (int i = 0; i < rows; ++i)
        for (int j = 0; j < cols; ++j)
            flat[i * cols + j] = matrix[i][j];

    jfloatArray result = env->NewFloatArray(flat.size());
    env->SetFloatArrayRegion(result, 0, flat.size(), flat.data());
    return result;
}

// JNI: Matrix Addition
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_Calculations_addMatrices(
        JNIEnv* env, jobject, jfloatArray a, jfloatArray b, jint rows, jint cols) {
    Matrix A = convertToMatrix(env, a, rows, cols);
    Matrix B = convertToMatrix(env, b, rows, cols);
    Matrix result(rows, vector<float>(cols));

    for (int i = 0; i < rows; ++i)
        for (int j = 0; j < cols; ++j)
            result[i][j] = A[i][j] + B[i][j];

    return convertToFlatArray(env, result);
}

// JNI: Matrix Subtraction
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_Calculations_subtractMatrices(
        JNIEnv* env, jobject, jfloatArray a, jfloatArray b, jint rows, jint cols) {
    Matrix A = convertToMatrix(env, a, rows, cols);
    Matrix B = convertToMatrix(env, b, rows, cols);
    Matrix result(rows, vector<float>(cols));

    for (int i = 0; i < rows; ++i)
        for (int j = 0; j < cols; ++j)
            result[i][j] = A[i][j] - B[i][j];

    return convertToFlatArray(env, result);
}

// JNI: Matrix Multiplication
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_Calculations_multiplyMatrices(
        JNIEnv* env, jobject, jfloatArray a, jfloatArray b,
        jint r1, jint c1, jint c2) {
    Matrix A = convertToMatrix(env, a, r1, c1);
    Matrix B = convertToMatrix(env, b, c1, c2);
    Matrix result(r1, vector<float>(c2, 0.0f));

    for (int i = 0; i < r1; ++i)
        for (int j = 0; j < c2; ++j)
            for (int k = 0; k < c1; ++k)
                result[i][j] += A[i][k] * B[k][j];

    return convertToFlatArray(env, result);
}

// Gauss-Jordan matrix inversion
Matrix invertMatrix(const Matrix& input) {
    int n = input.size();
    Matrix mat = input;
    Matrix identity(n, vector<float>(n, 0.0f));

    for (int i = 0; i < n; ++i)
        identity[i][i] = 1.0f;

    for (int i = 0; i < n; ++i) {
        float diag = mat[i][i];
        if (std::abs(diag) < 1e-8f) {
            throw std::runtime_error("Matrix 2 is not invertible."); // Line changed
        }

        for (int j = 0; j < n; ++j) {
            mat[i][j] /= diag;
            identity[i][j] /= diag;
        }

        for (int k = 0; k < n; ++k) {
            if (k == i) continue;
            float factor = mat[k][i];
            for (int j = 0; j < n; ++j) {
                mat[k][j] -= factor * mat[i][j];
                identity[k][j] -= factor * identity[i][j];
            }
        }
    }

    return identity;
}

// JNI: Matrix Division (A x inverse(B))
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_example_matrixcalculator_Calculations_divideMatrices(
        JNIEnv* env, jobject, jfloatArray a, jfloatArray b,
        jint rA, jint cA, jint bSize) {
    Matrix A = convertToMatrix(env, a, rA, cA);
    Matrix B = convertToMatrix(env, b, bSize, bSize);
    try {
        Matrix invB = invertMatrix(B);
        Matrix result(rA, vector<float>(bSize, 0.0f));

        for (int i = 0; i < rA; ++i)
            for (int j = 0; j < bSize; ++j)
                for (int k = 0; k < cA; ++k)
                    result[i][j] += A[i][k] * invB[k][j];

        // Round to 2 decimal places
        for (auto& row : result)
            for (auto& val : row)
                val = std::round(val * 100.0f) / 100.0f;

        return convertToFlatArray(env, result);
    } catch (const std::runtime_error& e) {
        // Return a nullptr to signal an error back to Java
        return nullptr; // Line changed
    }
}