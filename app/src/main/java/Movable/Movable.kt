import kotlin.math.*
import kotlin.random.*

interface Movable {
    var x: Double
    var y: Double
    val currentSpeed: Double

    fun move()
    fun getPosition(): Pair<Double, Double>
    fun getFullName(): String
}