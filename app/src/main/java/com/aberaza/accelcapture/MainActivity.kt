package com.aberaza.accelcapture

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity


// nurar https://robertohuertas.com/2019/06/29/android_foreground_services/
class MainActivity : WearableActivity() {
    private val TAG = "MainActivity"

    private lateinit var accelService : SensorsReader
    private var serviceBound = false
    private val connection = object: ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as SensorsReader.LocalBinder
            accelService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enables Always-on
        setAmbientEnabled()

        // Start Service Button
        val toggleServiceButton : ToggleButton = findViewById(R.id.toggleServiceButton)
        toggleServiceButton.isChecked = isServiceRunning() &&  getServiceState(this) == ServiceState.STARTED
        toggleServiceButton.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                if(!serviceBound) bindToAccelService()
                runAccelService(Action.START)
            }else{
                runAccelService(Action.STOP)
            }
        }



        val userTextNameView : EditText = findViewById(R.id.editTextTextPersonName)
        userTextNameView.setText(getUserName(this))
        /*
        userTextNameView.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                setUser( s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
        })

         */

        userTextNameView.setOnEditorActionListener { v, actionId, event ->
                Log.v(TAG, "ACTION ID::: $actionId")
                if(actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL){
                    setUser(v.text.toString())
                    true
                } else {
                    false
                }
            }

        when(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)){
            ConnectionResult.SUCCESS -> {
                Log.v(TAG, "GOOGLE API SERVICES AVAILABLE")

                val mTransitionRecognition = TransitionRecognition()
                mTransitionRecognition.startTracking(this)
            }
            else -> Log.w(TAG, "GOOGLE API SERVICES NOT NOT NOT AVAILABLE")

        }
    }

    override fun onResume() {
        super.onResume()

        val toggleServiceButton : ToggleButton = findViewById(R.id.toggleServiceButton)
        toggleServiceButton.isChecked = isServiceRunning() && getServiceState(this) == ServiceState.STARTED

    }

    private fun setUser(name: String){
        Log.v(TAG, "SET USER TO $name SERVICE BOUND $serviceBound")
        setUserName(this, name)
        if(serviceBound) accelService.sessionUserId = name
    }

    private fun setActivity(activity: String){
        if(serviceBound) accelService.sessionActivity = activity
    }

    private fun runAccelService(action: Action) {
        val userTextView : TextView = findViewById(R.id.editTextTextPersonName)
        var userId = "default"
        if(userTextView.text.isNotBlank()){
            userId = userTextView.text.toString()
        }
        Log.v(TAG, "runAccelService with user $userId")

        Intent(this, SensorsReader::class.java).also { intent ->
            intent.action = action.name
            intent.putExtra("uid", userId)
            startService(intent)
        }
    }

    private fun bindToAccelService(){
        Log.v(TAG, "BIND AccelService")
        Intent(this, SensorsReader::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun isServiceRunning(): Boolean {
        val serviceClass : Class<*> = SensorsReader::class.java
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun registerToActivity() {
        val transitions = mutableListOf<ActivityTransition>()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()

        transitions +=
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()


        val request = ActivityTransitionRequest(transitions)



    }
}
