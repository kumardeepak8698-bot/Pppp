package com.example

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class EnclaveDeviceAdminReceiver : android.app.admin.DeviceAdminReceiver() {

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        Log.d("EnclaveDPC", "Work Profile provisioning complete")
        Toast.makeText(context, "Enclave Work Profile Created Successfully", Toast.LENGTH_LONG).show()
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Log.d("EnclaveDPC", "Enclave Device Admin Enabled")
        Toast.makeText(context, "Enclave Device Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.d("EnclaveDPC", "Enclave Device Admin Disabled!")
        Toast.makeText(context, "Enclave Device Admin Disabled!", Toast.LENGTH_SHORT).show()
    }
}
