import kotlin.random.Random
import kotlin.math.*

// клас human
class Human(
    private val fullName: String, // ФИО человека
    private val age: Int, // возраст
    private var currentSpeed: Double // текущая скорость
) {
    // начальные координаты
    var x: Double = 0.0
    var y: Double = 0.0

    // Функция рандомного движения
    fun move() {
        val direction = Random.nextDouble(0.0, 2 * PI)
        // обновление координват
        x += currentSpeed * cos(direction)
        y += currentSpeed * sin(direction)
    }

    // получение значений
    fun getFullName(): String = fullName
    fun getCurrentSpeed(): Double = currentSpeed
    fun getPosition(): Pair<Double, Double> = Pair(x, y)

}

fun main() {
    // массив людей
    val humans = arrayOf(
        Human("Иванов Иван Иванович", 25, 1.5),
        Human("Петрова Анна Сергеевна", 30, 2.0),
        Human("Сидоров Алексей Петрович", 28, 1.8),

    )

    // время симуляции 10 сек
    val simulationTime = 10

    // цикл симуляции
    for (time in 1..simulationTime) {
        println("Шаг времени: $time")
        for ((index, human) in humans.withIndex()) {
            // двигаем людей
            human.move()
            // меняем координаты
            val (x, y) = human.getPosition()
            println("Человек ${index + 1} (${human.getFullName()}): позиция ($x, $y), скорость ${human.getCurrentSpeed()}")
        }
        println("---")
    }
}