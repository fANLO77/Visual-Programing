import kotlin.math.*
import kotlin.random.*

open class Human(
    private val fullName: String,
    private val age: Int,
    override val currentSpeed: Double
) : Movable {
    override var x: Double = 0.0
    override var y: Double = 0.0

    override fun move() {
        val direction = Random.nextDouble(0.0, 2 * PI)
        x += currentSpeed * cos(direction)
        y += currentSpeed * sin(direction)
    }

    override fun getFullName(): String = fullName
    override fun getPosition(): Pair<Double, Double> = Pair(x, y)
}