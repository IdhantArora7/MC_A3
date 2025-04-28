package com.example.matrixcalculator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Calculations : AppCompatActivity() {

    private var matrixOneRows = 0
    private var matrixTwoRows = 0
    private var matrixOneCols = 0
    private var matrixTwoCols = 0

    private lateinit var layoutMatrixOne: LinearLayout
    private lateinit var layoutMatrixTwo: LinearLayout
    private lateinit var resultTextView: TextView

    private val inputsMatrixOne = mutableListOf<EditText>()
    private val inputsMatrixTwo = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.calculation_screen)

        layoutMatrixOne = findViewById(R.id.matrixOneLayout)
        layoutMatrixTwo = findViewById(R.id.matrixTwoLayout)
        resultTextView = findViewById(R.id.resultView)

        val btnAdd: Button = findViewById(R.id.btnAdd)
        val btnSubtract: Button = findViewById(R.id.btnSubtract)
        val btnMultiply: Button = findViewById(R.id.btnMultiply)
        val btnDivide: Button = findViewById(R.id.btnDivide)

        matrixOneRows = intent.getIntExtra("rowCount1", 0)
        matrixTwoRows = intent.getIntExtra("rowCount2", 0)
        matrixOneCols = intent.getIntExtra("colCount1", 0)
        matrixTwoCols = intent.getIntExtra("colCount2", 0)

        createMatrixInputFields(layoutMatrixOne, inputsMatrixOne, matrixOneRows, matrixOneCols)
        createMatrixInputFields(layoutMatrixTwo, inputsMatrixTwo, matrixTwoRows, matrixTwoCols)

        btnAdd.setOnClickListener {
            if (haveSameDimensions()) {
                val result = addMatrices(readMatrix(inputsMatrixOne, matrixOneRows, matrixOneCols),
                    readMatrix(inputsMatrixTwo, matrixTwoRows, matrixTwoCols),
                    matrixOneRows, matrixOneCols)
                showMatrix(result, matrixOneRows, matrixOneCols)
            } else showToast("Matrices don't have equal size. Addition not possible")
        }

        btnSubtract.setOnClickListener {
            if (haveSameDimensions()) {
                val result = subtractMatrices(readMatrix(inputsMatrixOne, matrixOneRows, matrixOneCols),
                    readMatrix(inputsMatrixTwo, matrixTwoRows, matrixTwoCols),
                    matrixOneRows, matrixOneCols)
                showMatrix(result, matrixOneRows, matrixOneCols)
            } else showToast("Matrices don't have equal size. Subtraction not possible")
        }

        btnMultiply.setOnClickListener {
            if (matrixOneCols == matrixTwoRows) {
                val result = multiplyMatrices(readMatrix(inputsMatrixOne, matrixOneRows, matrixOneCols),
                    readMatrix(inputsMatrixTwo, matrixTwoRows, matrixTwoCols),
                    matrixOneRows, matrixOneCols, matrixTwoCols)
                showMatrix(result, matrixOneRows, matrixTwoCols)
            } else showToast("Matrix 1 columns != Matrix 2 rows. Multiplication not possible")
        }

        btnDivide.setOnClickListener {
            if (matrixOneCols == matrixTwoRows && matrixTwoRows == matrixTwoCols) {
                try {
                    val result = divideMatrices(readMatrix(inputsMatrixOne, matrixOneRows, matrixOneCols),
                        readMatrix(inputsMatrixTwo, matrixTwoRows, matrixTwoCols),
                        matrixOneRows, matrixOneCols, matrixTwoCols)
                    showMatrix(result, matrixOneRows, matrixTwoCols)
                } catch (e: Exception) {
                    showToast("Division error: Matrix 2 can't be inverted. Division not possible")
                }
            }
            else showToast("For division: Matrix 2 must be square; Matrix 1's columns must match Matrix 2's rows.")
        }
    }

    private fun createMatrixInputFields(container: LinearLayout, inputs: MutableList<EditText>, rows: Int, cols: Int) {
        container.removeAllViews()
        inputs.clear()
        repeat(rows) { rowIndex ->
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            repeat(cols) { colIndex ->
                val inputField = EditText(this).apply {
                    hint = "[$rowIndex][$colIndex]"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = 8
                    }
                }
                rowLayout.addView(inputField)
                inputs.add(inputField)
            }
            container.addView(rowLayout)
        }
    }

    private fun readMatrix(inputs: List<EditText>, rows: Int, cols: Int): FloatArray {
        return FloatArray(rows * cols) { idx ->
            inputs[idx].text.toString().toFloatOrNull() ?: 0f
        }
    }

//    private fun showMatrix(matrix: FloatArray, rows: Int, cols: Int) {
//        val output = buildString {
//            for (row in 0 until rows) {
//                append(matrix.slice(row * cols until (row + 1) * cols).joinToString(" ", "[", "]")).append("\n")
//            }
//        }
//        resultTextView.text = output
//    }
private fun showMatrix(matrix: FloatArray, rows: Int, cols: Int) {
    val outputBuilder = StringBuilder()
    for (i in 0 until rows) {
        outputBuilder.append("| ")
        for (j in 0 until cols) {
            outputBuilder.append(String.format("%.2f\t", matrix[i * cols + j]))
            outputBuilder.append("| ")
        }
        outputBuilder.append("\n")
    }
    resultTextView.text = outputBuilder.toString()
}

    private fun haveSameDimensions() = matrixOneRows == matrixTwoRows && matrixOneCols == matrixTwoCols

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // JNI Native Functions
    external fun addMatrices(a: FloatArray, b: FloatArray, rows: Int, cols: Int): FloatArray
    external fun subtractMatrices(a: FloatArray, b: FloatArray, rows: Int, cols: Int): FloatArray
    external fun multiplyMatrices(a: FloatArray, b: FloatArray, r1: Int, c1: Int, c2: Int): FloatArray
    external fun divideMatrices(a: FloatArray, b: FloatArray, r1: Int, c1: Int, size: Int): FloatArray

    companion object {
        init {
            System.loadLibrary("matrixcalculator")
        }
    }
}
