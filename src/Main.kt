import kotlin.concurrent.thread

fun main() {
    // массив пешеходов
    val humans = arrayOf(
        Human("Иванов Иван Иванович", 25, 1.5),
        Human("Петрова Анна Сергеевна", 30, 2.0),
        Human("Сидоров Алексей Петрович", 28, 1.8),

        )
    // Водитель класса Driver
    val drivers = arrayOf(
        Driver("Попович Антон Валерьевич", 33, 7.6)
    )

    // Все люди для симуляции
    val allMovingObject: List<Movable> = humans.toList() + drivers.toList()

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