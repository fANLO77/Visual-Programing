package com.example.android_project

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var display: TextView
    private lateinit var num0: Button
    private lateinit var num1: Button
    private lateinit var num2: Button
    private lateinit var num3: Button
    private lateinit var num4: Button
    private lateinit var num5: Button
    private lateinit var num6: Button
    private lateinit var num7: Button
    private lateinit var num8: Button
    private lateinit var num9: Button
    private lateinit var add: Button
    private lateinit var sub: Button
    private lateinit var mul: Button
    private lateinit var div: Button
    private lateinit var equal: Button
    private lateinit var clear: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display) ?: run {
            throw IllegalStateException("Display TextView not found in activity_main.xml")
        }
        num0 = findViewById(R.id.num0) ?: throw IllegalStateException("Button num0 not found")
        num1 = findViewById(R.id.num1) ?: throw IllegalStateException("Button num1 not found")
        num2 = findViewById(R.id.num2) ?: throw IllegalStateException("Button num2 not found")
        num3 = findViewById(R.id.num3) ?: throw IllegalStateException("Button num3 not found")
        num4 = findViewById(R.id.num4) ?: throw IllegalStateException("Button num4 not found")
        num5 = findViewById(R.id.num5) ?: throw IllegalStateException("Button num5 not found")
        num6 = findViewById(R.id.num6) ?: throw IllegalStateException("Button num6 not found")
        num7 = findViewById(R.id.num7) ?: throw IllegalStateException("Button num7 not found")
        num8 = findViewById(R.id.num8) ?: throw IllegalStateException("Button num8 not found")
        num9 = findViewById(R.id.num9) ?: throw IllegalStateException("Button num9 not found")
        add = findViewById(R.id.add) ?: throw IllegalStateException("Button add not found")
        sub = findViewById(R.id.sub) ?: throw IllegalStateException("Button sub not found")
        mul = findViewById(R.id.mul) ?: throw IllegalStateException("Button mul not found")
        div = findViewById(R.id.div) ?: throw IllegalStateException("Button div not found")
        equal = findViewById(R.id.equal) ?: throw IllegalStateException("Button equal not found")
        clear = findViewById(R.id.clear) ?: throw IllegalStateException("Button clear not found")

        num0.setOnClickListener { display.append("0") }
        num1.setOnClickListener { display.append("1") }
        num2.setOnClickListener { display.append("2") }
        num3.setOnClickListener { display.append("3") }
        num4.setOnClickListener { display.append("4") }
        num5.setOnClickListener { display.append("5") }
        num6.setOnClickListener { display.append("6") }
        num7.setOnClickListener { display.append("7") }
        num8.setOnClickListener { display.append("8") }
        num9.setOnClickListener { display.append("9") }
        add.setOnClickListener { display.append("+") }
        sub.setOnClickListener {
            val text = display.text.toString()
            if (text.isEmpty() || text.last() in "+-*/") {
                display.append("-")
            } else {
                display.append("-")
            }
        }
        mul.setOnClickListener { display.append("*") }
        div.setOnClickListener { display.append("/") }
        clear.setOnClickListener { display.text = "" }
        equal.setOnClickListener {
            val expression = display.text.toString().trim()
            if (expression.isEmpty() || !expression.contains(Regex("[+\\-*/]"))) {
                display.text = "Error: Invalid input"
                return@setOnClickListener
            }

            try {
                var operatorIndex = -1
                var i = if (expression[0] == '-') 1 else 0
                while (i < expression.length) {
                    if (expression[i] in "+-*/") {
                        operatorIndex = i
                        break
                    }
                    i++
                }
                if (operatorIndex == -1 || operatorIndex == expression.length - 1) {
                    display.text = "Error: Invalid format"
                    return@setOnClickListener
                }

                val aStr = expression.substring(0, operatorIndex).trim()
                val a = aStr.toDoubleOrNull() ?: throw NumberFormatException()

                val bStr = expression.substring(operatorIndex + 1).trim()
                val b = bStr.toDoubleOrNull() ?: throw NumberFormatException()

                val operator = expression[operatorIndex]
                val result = when (operator) {
                    '+' -> a + b
                    '-' -> a - b
                    '*' -> a * b
                    '/' -> if (b == 0.0) {
                        display.text = "Error: Div by 0"
                        return@setOnClickListener
                    } else a / b
                    else -> throw IllegalArgumentException()
                }
                display.text = result.toString()
            } catch (e: Exception) {
                display.text = "Error"
            }
        }
    }
}