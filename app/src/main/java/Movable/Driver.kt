import kotlin.math.*
import kotlin.random.*

class Driver(
    fullName: String,
    age: Int,
    currentSpeed: Double
) : Human(fullName, age, currentSpeed) {

    private var directionAngle: Double = Random.nextDouble(0.0, 2 * PI)
    private var stepsDriver: Int = Random.nextInt(3, 8)

    override fun move() {
        stepsDriver--
        if (stepsDriver <= 0) {
            directionAngle = Random.nextDouble(0.0, 2 * PI)
            stepsDriver = Random.nextInt(3, 8)
        }
        x += currentSpeed * cos(directionAngle)
        y += currentSpeed * sin(directionAngle)
    }

    fun getDirectAngle(): Double = directionAngle
    override fun getFullName(): String = super.getFullName()
}