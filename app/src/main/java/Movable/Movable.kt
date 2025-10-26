import kotlin.math.*
import kotlin.random.*

interface Movable {
    var x: Double
    var y: Double
    val currentSpeed: Double

    fun move()
    fun getPosition(): Pair<Double, Double>
<<<<<<< HEAD:app/src/main/java/Movable/Movable.kt
    fun getFullName(): String
}
=======
    fun getCurrentSpeed(): Double
    fun getFullName(): String 

}
>>>>>>> 051ff8a54643e93f871fc7516576028f56e660fd:src/Movable.kt
