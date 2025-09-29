import kotlin.math.*
import kotlin.random.*

open class Human(
    private val fullName: String, // ФИО человека
    private val age: Int, // возраст
    override val currentSpeed: Double // текущая скорость
) : Movable {
    // начальные координаты
    override var x: Double = 0.0
    override var y: Double = 0.0

    // Функция рандомного движения
    override fun move() {
        val direction = Random.nextDouble(0.0, 2 * PI)
        // обновление координват
        x += currentSpeed * cos(direction)
        y += currentSpeed * sin(direction)
    }

    // получение значений
    override fun getFullName(): String = fullName // Переопределяем с override
    override fun getCurrentSpeed(): Double = currentSpeed
    override fun getPosition(): Pair<Double, Double> = Pair(x, y)
}