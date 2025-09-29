package com.freehands.assistant.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor() {
    
    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val VOICE_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        
        val CONTACT_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
        
        val STORAGE_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasPermissions(context, *REQUIRED_PERMISSIONS)
    }
    
    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestPermissions(
        activity: FragmentActivity,
        permissions: Array<String> = REQUIRED_PERMISSIONS,
        onGranted: () -> Unit = {},
        onDenied: (List<String>) -> Unit = {}
    ) {
        val launcher = activity.activityResultRegistry.register(
            "permission_launcher",
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            val deniedPermissions = permissionsMap.filter { !it.value }.keys.toList()
            
            if (deniedPermissions.isEmpty()) {
                onGranted()
            } else {
                onDenied(deniedPermissions)
            }
        }
        
        launcher.launch(permissions)
    }
    
    fun requestPermissions(
        fragment: Fragment,
        permissions: Array<String> = REQUIRED_PERMISSIONS,
        onGranted: () -> Unit = {},
        onDenied: (List<String>) -> Unit = {}
    ) {
        val launcher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            val deniedPermissions = permissionsMap.filter { !it.value }.keys.toList()
            
            if (deniedPermissions.isEmpty()) {
                onGranted()
            } else {
                onDenied(deniedPermissions)
            }
        }
        
        launcher.launch(permissions)
    }
    
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }
    
    fun openAppSettings(context: Context) {
        val intent = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS.apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}
