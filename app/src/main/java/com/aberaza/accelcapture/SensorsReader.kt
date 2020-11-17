package com.aberaza.accelcapture

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

class SensorsReader : Service(), SensorEventListener {
    private val TAG = "SensorsReader"

    private var serviceStarted = false
    val isRunning : Boolean
        get() = serviceStarted
    private val AWS_API_URL = BuildConfig.API_URL

    //private val CONFIDENCE_TRESHOLD = 0.5
    private val MOTION_DURATION = 5 //seconds
    private var MOTION_SAMPLES : Int = 0

    private lateinit var sensingAccelXBuffer: RingBuffer<Float>
    private lateinit var sensingAccelYBuffer: RingBuffer<Float>
    private lateinit var sensingAccelZBuffer: RingBuffer<Float>
    private lateinit var sensingAccelDiffBuffer: RingBuffer<Float>

    private var sessionTriggerMethod: String = ""
    private var sensorSamplingRate: Int = 1
    private var sensorMaxRange: Float = 0.0f
    private var sensorResolution: Float = 0.0f

    private val MOTION_TRESHOLD_START = 0.3f
    private val MOTION_TRESHOLD_STOP  = 0.2f
    private val MOTION_SUDDEN_TRESHOLD: Float = 3.0f
    private var motionAccelCurrent = SensorManager.GRAVITY_EARTH
    private var motionCaptureStartTime: Long = 0

    //private var MOTION_SESSION_VALUES: MutableList<Array<Float>>? = null
    private var MOTION_SESSION_VALUES_ACCEL_X : MutableList<Float> = mutableListOf<Float>()
    private var MOTION_SESSION_VALUES_ACCEL_Y: MutableList<Float> = mutableListOf<Float>()
    private var MOTION_SESSION_VALUES_ACCEL_Z: MutableList<Float> = mutableListOf<Float>()
    private var sessionId:String? = null
    var sessionUserId: String? = null
    var sessionActivity:String? = null

    private lateinit var sensorManager: SensorManager // = getSystemService(Context.SENSOR_SERVICE) as SensorManager;
    private var accelSensor: Sensor? = null

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): SensorsReader = this@SensorsReader
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.v(TAG, "onBind service")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(TAG, "onStartCommand called")
        if(intent!=null) {
            val extras = intent?.extras
            if(extras != null) sessionUserId = extras.get("uid") as String?

            when (intent.action) {
                Action.START.name -> startService()
                Action.STOP.name -> stopService()
                else -> {
                    startService()
                    Log.w(TAG, "This should never happen. No action in the received intent")
                }
            }
        } else {
            Log.i(TAG, "Null intent, restarted by system")
        }

        //Toast.makeText(this, "SensorsRead  Started", Toast.LENGTH_SHORT).show()

        return START_STICKY
    }

    override fun onCreate() {
        Log.v(TAG, "onCreate called")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //Log.v(TAG, "AVAILABLE SENSORS:::\n ${sensorManager.getSensorList(Sensor.TYPE_ALL).toString()}")

        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //results in m/s^2

        val minDelay = accelSensor?.minDelay
        sensorSamplingRate = floor (1000000.0 / minDelay!!).toInt()
        sensorMaxRange = accelSensor!!.maximumRange
        sensorResolution = accelSensor!!.resolution

        MOTION_SAMPLES = MOTION_DURATION * sensorSamplingRate

        sensingAccelDiffBuffer = RingBuffer<Float>(MOTION_SAMPLES)
        sensingAccelXBuffer = RingBuffer<Float>(MOTION_SAMPLES)
        sensingAccelYBuffer = RingBuffer<Float>(MOTION_SAMPLES)
        sensingAccelZBuffer = RingBuffer<Float>(MOTION_SAMPLES)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy called")
        Toast.makeText(this, "SensorsRead Stopped", Toast.LENGTH_SHORT).show()

        this.stopService()
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // TODO("Not yet implemented")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val accelLast = motionAccelCurrent
                motionAccelCurrent = sqrt(x * x + y * y + z * z)
                val accelDiff = abs(motionAccelCurrent - accelLast)
                sensingAccelDiffBuffer.enqueue(accelDiff)

                if(sessionId != null){
                    MOTION_SESSION_VALUES_ACCEL_X.add(x)
                    MOTION_SESSION_VALUES_ACCEL_Y.add(y)
                    MOTION_SESSION_VALUES_ACCEL_Z.add(z)
                    if(sensingAccelDiffBuffer.averageSum() < MOTION_TRESHOLD_STOP && sensingAccelDiffBuffer.isFull()){
                        Log.v(TAG, "STOP ${sensingAccelDiffBuffer.toString()}")
                        stopSession()
                    }
                }else{
                    sensingAccelXBuffer.enqueue(x)
                    sensingAccelYBuffer.enqueue(y)
                    sensingAccelZBuffer.enqueue(z)
                    if(sensingAccelDiffBuffer.averageSum() >= MOTION_TRESHOLD_START){
                        Log.v(TAG, "START average ${sensingAccelDiffBuffer.toString()}")
                        startSession("accumulated")
                    }else if(accelDiff >= MOTION_SUDDEN_TRESHOLD){
                        Log.v(TAG, "START sudden $accelDiff")
                        startSession("sudden")
                    }
                }
            }
        }
    }

    fun startService() {
        if(!serviceStarted) {
            Toast.makeText(this, "SensorsRead LOGGING STARTED", Toast.LENGTH_SHORT).show()
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
            serviceStarted = true
            setServiceState(this, ServiceState.STARTED)
        }
    }

    fun stopService() {
        if(serviceStarted) {
            Toast.makeText(this, "SensorsRead LOGGING STOPPED", Toast.LENGTH_SHORT).show()
            sensorManager.unregisterListener(this, this.accelSensor)
            serviceStarted = false
            setServiceState(this, ServiceState.STOPPED)

            if(sessionId !== null) stopSession()
        }
    }


/*

    fun setUserId(uid: String){
        sessionUserId = uid
    }

    fun setActivity(activity: String) {
        sessionActivity = activity
    }*/






    private fun startSession(triggerMethod: String) {
        if(sessionId == null) {
            Toast.makeText(this, "New Session!", Toast.LENGTH_SHORT).show()
            motionCaptureStartTime = System.currentTimeMillis()
            sessionId = System.currentTimeMillis().toString() + sessionUserId
            sessionTriggerMethod = triggerMethod
            sensingAccelDiffBuffer.clear()
            //SESSION_ACTIVITY = "Unknown"
        }
    }

    private fun stopSession() {
        if (sessionId != null){
            Toast.makeText(this, "Session Ended!", Toast.LENGTH_SHORT).show()
            sessionActivity = getActivity(this)
            saveSession()
            sessionId = null
        }
        sensingAccelXBuffer.clear()
        sensingAccelYBuffer.clear()
        sensingAccelZBuffer.clear()
    }

    private fun saveSession() {
        Toast.makeText(this, "Save Session $sessionId", Toast.LENGTH_SHORT).show()
        val payload = Session(sessionUserId?:"unknown",
            sessionId?:"sessionid",
            sensorResolution,
            sensorMaxRange,
            motionCaptureStartTime,
            MOTION_SESSION_VALUES_ACCEL_X.size,
            sensingAccelDiffBuffer.averageSum(),
            sessionTriggerMethod,
            SessionData(System.currentTimeMillis() - motionCaptureStartTime,
                sessionActivity?: "Unknown",
                sensorSamplingRate,
                MOTION_SESSION_VALUES_ACCEL_X,
                MOTION_SESSION_VALUES_ACCEL_Y,
                MOTION_SESSION_VALUES_ACCEL_Z))
        val serializedPayload = Json(JsonConfiguration.Stable).stringify(Session.serializer(), payload)
        Log.v(TAG, serializedPayload)

        sendPost(serializedPayload)
    }

    private fun sendPost(data: String) {
        try {
            Fuel.post(AWS_API_URL)
                .jsonBody(data)
                .response{ result -> }
        }catch(e: Exception) {
            Log.e(TAG, e.message.toString())
        }
    }
}
