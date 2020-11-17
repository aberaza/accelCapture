package com.aberaza.accelcapture

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener

class TransitionRecognition  {
    private val TAG = TransitionRecognition::class.java.getSimpleName()
    lateinit var mContext: Context
    lateinit var mPendingIntent: PendingIntent

    fun startTracking(context: Context) {
        mContext = context
        launchTransitionsTracker()
    }

    fun stopTracking() {
        //if (mContext != null && mPendingIntent != null) {
            ActivityRecognition.getClient(mContext).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(OnSuccessListener<Void> {
                    mPendingIntent.cancel()
                })
                .addOnFailureListener(OnFailureListener { e -> Log.e(TAG, "Transitions could not be unregistered: $e") })
        //}
    }

    /***********************************************************************************************
     * LAUNCH TRANSITIONS TRACKER
     **********************************************************************************************/
    private fun launchTransitionsTracker() {

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

        val intent = Intent(mContext, TransitionRecognitionReceiver::class.java)
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0)

        val task = ActivityRecognition.getClient(mContext).requestActivityTransitionUpdates(request, mPendingIntent)

        task.addOnSuccessListener {
            Log.v(TAG, "Registered for activity updates")
        }

        task.addOnFailureListener { exception -> Log.e(TAG, exception.message?:exception.toString()) }

    }
}




class TransitionRecognitionReceiver : BroadcastReceiver() {
    private val TAG = TransitionRecognitionReceiver::class.java.getSimpleName()
    lateinit var mContext: Context

    override fun onReceive(context: Context?, intent: Intent?) {
        mContext = context!!

        if (ActivityTransitionResult.hasResult(intent)) {
            var result = ActivityTransitionResult.extractResult(intent)
            if (result != null) {
                Log.v(TAG, "Activity recognition got result ${result.toString()}")
                processTransitionResult(result)
            }else{
                Log.w(TAG, "ActivityRecognition transition reviver got a null activityresult")
            }
        }
    }

    fun processTransitionResult(result: ActivityTransitionResult) {
        for (event in result.transitionEvents) {
            onDetectedTransitionEvent(event)
        }
    }

    private fun onDetectedTransitionEvent(activity: ActivityTransitionEvent) {
        when (activity.activityType) {
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING -> {
                // Make whatever you want with the activity
                Log.v(TAG, "Recognized activity ${activity.toString()}")
                setActivity(mContext, activity.toString())
            }
            else -> {
                Log.v(TAG, "Recognized other activity ${activity.toString()}")
                setActivity(mContext, activity.toString())
            }
        }
    }
}