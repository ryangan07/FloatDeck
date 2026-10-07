package app.floatdeck.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * 设备姿态传感器监听器。
 *
 * 优先使用旋转矢量传感器（TYPE_ROTATION_VECTOR），回退到游戏旋转矢量，
 * 最后回退到加速度计。输出 rollX（左右倾斜）和 pitchY（前后倾斜）归一化值。
 */
class SensorHandler(
    private val context: Context,
) : SensorEventListener {
    companion object {
        /**
         * 把旋转矢量传感器返回的 values 截断到前 4 个元素。
         * 部分厂商返回长度为 5（含 heading），会导致
         * [SensorManager.getRotationMatrixFromVector] 抛 IllegalArgumentException。
         */
        internal fun safeRotationValues(values: FloatArray): FloatArray =
            if (values.size > 4) values.copyOf(4) else values
    }

    /** 左右倾斜值（roll），约 -1 ~ 1 */
    var rollX = 0f
        private set

    /** 前后倾斜值（pitch），约 -1 ~ 1 */
    var pitchY = 0f
        private set

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var registered = false

    /** Relative tilt for depth mode, fed by the gyroscope (stays 0 on devices without one). */
    val tiltTracker = TiltTracker()

    /** Horizontal relative tilt, -1 ~ 1. */
    val tiltX: Float get() = tiltTracker.normalizedHorizontal

    /**
     * Vertical relative tilt, -1 ~ 1. Negated so that tilting the top edge
     * toward the user moves the background up (screen Y points down), the same
     * "looking through a window" feel as the horizontal axis.
     */
    val tiltY: Float get() = -tiltTracker.normalizedVertical

    private var lastGyroTimestampNanos = 0L

    /** 注册传感器监听，按优先级依次尝试旋转矢量 → 游戏旋转矢量 → 加速度计。 */
    fun register() {
        if (registered) return

        // Gyroscope drives the depth-mode tilt; registered in addition to the
        // orientation sensor below. Start centered on whatever pose the phone
        // is held in right now.
        tiltTracker.reset()
        lastGyroTimestampNanos = 0L
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let { gyro ->
            sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
            registered = true
        }

        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
            registered = true
            return
        }

        val gameRotation = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        if (gameRotation != null) {
            sensorManager.registerListener(this, gameRotation, SensorManager.SENSOR_DELAY_UI)
            registered = true
            return
        }

        // 最后回退：仅用加速度计估算倾斜（无陀螺仪的设备）
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            registered = true
        }
    }

    /** 取消传感器监听。 */
    fun unregister() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        registered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val last = lastGyroTimestampNanos
                lastGyroTimestampNanos = event.timestamp
                if (last != 0L) {
                    val dt = (event.timestamp - last) / 1_000_000_000f
                    tiltTracker.onGyro(event.values[0], event.values[1], dt)
                }
            }
            // 旋转矢量 / 游戏旋转矢量：通过旋转矩阵 → 欧拉角提取 roll 和 pitch
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(
                    rotationMatrix,
                    safeRotationValues(event.values),
                )
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                rollX = orientation[2] // 弧度制 roll
                pitchY = orientation[1] // 弧度制 pitch
            }
            // 加速度计回退：用重力方向归一化估算倾斜
            Sensor.TYPE_ACCELEROMETER -> {
                val g = event.values
                val norm = sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2])
                if (norm > 0.1f) {
                    rollX = g[0] / norm
                    pitchY = g[1] / norm
                }
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) {}
}
