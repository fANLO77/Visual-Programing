import kotlin.concurrent.thread

fun main() {
    val humans = arrayOf(
        Human("Иванов Иван Иванович", 25, 1.5),
        Human("Петрова Анна Сергеевна", 30, 2.0),
        Human("Сидоров Алексей Петрович", 28, 1.8),
        )
    val drivers = arrayOf(
        Driver("Попович Антон Валерьевич", 33, 7.6)
    )

    val allMovingObject: List<Movable> = humans.toList() + drivers.toList()

    val simulationTime = 20

    for (time in 1..simulationTime) {
        println("Шаг времени: $time")
        val threads = mutableListOf<Thread>()
        for (objects in allMovingObject) {
            val movingThread = thread {
                objects.move()
                val (x, y) = objects.getPosition()
                val position = "(${"%.2f".format(x)};  ${"%.2f".format(y)})"
                if (objects is Driver) {
                    println("ВОДИТЕЛЬ ${objects.getFullName()}: позиция $position, скорость ${objects.currentSpeed}")
                } else {
                    println("Пешеход ${objects.getFullName()}: позиция $position, скорость ${objects.currentSpeed}")
                }
            }
            threads.add(movingThread)
        }
        threads.forEach { it.join() }
        println("---")
    }
}