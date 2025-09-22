import kotlin.random.Random
import kotlin.math.*
import kotlin.concurrent.thread
import kotlin.random.nextInt

// клас human
open class Human(
    private val fullName: String, // ФИО человека
    private val age: Int, // возраст
    private var currentSpeed: Double // текущая скорость
) {
    // начальные координаты
    var x: Double = 0.0
    var y: Double = 0.0

    // Функция рандомного движения
    open fun move() {
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

// Класс наследник Driver, получает все свойства и функции класса Human
class Driver(
    fullName: String,
    age: Int,
    currentSpeed: Double
) : Human(fullName, age, currentSpeed) {

    //Случайный угол при создании объекта для прямолиненого движения
    private var directionAngle: Double = Random.nextDouble(0.0, 2 * PI)

    //водитель меняет направление каждые 3-7 шагов
    private var stepsDriver: Int = Random.nextInt(3,8)

    // Переопределение функции движения для прямолинейного движения
    override fun move() {

        stepsDriver-- // уменьшаем счётчик шагров
        // поверка на смену направления
        if (stepsDriver <= 0) {
            directionAngle = Random.nextDouble(0.0, 2 * PI)
            stepsDriver = Random.nextInt(3,8) // обновляем счётчик
        }

        // Двигаемся по текущему направлению
        x += getCurrentSpeed() * cos(directionAngle)
        y += getCurrentSpeed() * sin(directionAngle)
    }

    // получем текущий угол
    fun getDirectAngle(): Double = directionAngle

}

fun main() {
    // массив пешеходов
    val humans = arrayOf(
        Human("Иванов Иван Иванович", 25, 1.5),
        Human("Петрова Анна Сергеевна", 30, 2.0),
        Human("Сидоров Алексей Петрович", 28, 1.8),

        )
    // Водитель класса Driver
    val drivers = Driver("Попович Антон Валерьевич", 33, 7.6)

    // Все люди для симуляции
    val allMovingObject = humans + drivers

    // время симуляции 20 сек
    val simulationTime = 20

    // цикл симуляции
    for (time in 1..simulationTime) {
        println("Шаг времени: $time")

        //создаём список для потоков
        val threads = mutableListOf<Thread>()

        //создаём и запускаем потоки для каждого человека
        for (objects in allMovingObject) {
            //создаём и запускаем поток
            val movingThread = thread {
                objects.move()
                // меняем координаты
                val (x, y) = objects.getPosition()

                // строка с координатами округленных до 2-х знаков
                val position = "(${"%.2f".format(x)};  ${"%.2f".format(y)})"

                // фильтруем водителей и пешеходов
                if (objects is Driver) {
                    println("ВОДИТЕЛЬ ${objects.getFullName()}: позиция $position, скорость ${objects.getCurrentSpeed()}")
                } else {
                    println("Пешеход ${objects.getFullName()}: позиция $position, скорость ${objects.getCurrentSpeed()}")
                }
            }
            // Добавляем тольбкочто созданный и  запущеный поток
            threads.add(movingThread)
        }
        // Ждём пока все потоки завершат  работу
        threads.forEach { it.join() }
        println("---")
    }
}