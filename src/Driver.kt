import kotlin.math.*
import kotlin.random.*

class Driver(
    fullName: String,
    age: Int,
    currentSpeed: Double
) : Human(fullName, age, currentSpeed) {

    // Случайный угол при создании объекта для прямолинейного движения
    private var directionAngle: Double = Random.nextDouble(0.0, 2 * PI)

    // Водитель меняет направление каждые 3-7 шагов
    private var stepsDriver: Int = Random.nextInt(3, 8)

    // Переопределение функции движения для прямолинейного движения
    override fun move() {
        stepsDriver-- // уменьшаем счётчик шагов
        // Проверка на смену направления
        if (stepsDriver <= 0) {
            directionAngle = Random.nextDouble(0.0, 2 * PI)
            stepsDriver = Random.nextInt(3, 8) // обновляем счётчик
        }

        // Двигаемся по текущему направлению
        x += currentSpeed * cos(directionAngle)
        y += currentSpeed * sin(directionAngle)
    }

    // Получаем текущий угол
    fun getDirectAngle(): Double = directionAngle

    // Переопределяем getFullName
    override fun getFullName(): String = super.getFullName()
}