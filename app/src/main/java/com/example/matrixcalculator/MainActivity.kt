package com.example.matrixcalculator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.matrixcalculator.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val RowCount1 = findViewById<EditText>(R.id.RowCount1)
        val RowCount2 = findViewById<EditText>(R.id.RowCount2)
        val ColCount1 = findViewById<EditText>(R.id.ColCount1)
        val ColCount2 = findViewById<EditText>(R.id.ColCount2)
        val continueButton = findViewById<Button>(R.id.buttonContinue)

        continueButton.setOnClickListener {
            val row1 = RowCount1.text.toString().toIntOrNull()
            val row2 = RowCount2.text.toString().toIntOrNull()
            val col1 = ColCount1.text.toString().toIntOrNull()
            val col2 = ColCount2.text.toString().toIntOrNull()

            if ((row1 != null) && (row2 != null) && (col1 != null) && (col2 != null)) {
                val intent = Intent(this, Calculations::class.java).apply {
                    putExtra("rowCount1", row1)
                    putExtra("colCount1", col1)
                    putExtra("rowCount2", row2)
                    putExtra("colCount2", col2)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this , "Please specify all matrix dimensions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * A native method that is implemented by the 'matrixcalculator' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'matrixcalculator' library on application startup.
        init {
            System.loadLibrary("matrixcalculator")
        }
    }
}