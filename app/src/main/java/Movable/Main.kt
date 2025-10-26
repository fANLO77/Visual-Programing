import kotlin.concurrent.thread

fun main() {
    val humans = arrayOf(
        Human("Иванов Иван Иванович", 25, 1.5),
        Human("Петрова Анна Сергеевна", 30, 2.0),
        Human("Сидоров Алексей Петрович", 28, 1.8)
    )

    val drivers = arrayOf(
        Driver("Попович Антон Валерьевич", 33, 7.6)
    )

    val allMovingObject: List<Movable> = humans.toList() + drivers.toList()
    val simulationTime = 20

    for (time in 1..simulationTime) {
        println("Шаг времени: $time")
        val threads = mutableListOf<Thread>()

        for (obj in allMovingObject) {
            val movingThread = thread {
                obj.move()
                val (x, y) = obj.getPosition()
                val position = "(${"%.2f".format(x)}; ${"%.2f".format(y)})"

                if (obj is Driver) {
                    println("ВОДИТЕЛЬ ${obj.getFullName()}: позиция $position, скорость ${obj.currentSpeed}")
                } else {
                    println("Пешеход ${obj.getFullName()}: позиция $position, скорость ${obj.currentSpeed}")
                }
            }
            threads.add(movingThread)
        }
        threads.forEach { it.join() }
        println("---")
    }
}