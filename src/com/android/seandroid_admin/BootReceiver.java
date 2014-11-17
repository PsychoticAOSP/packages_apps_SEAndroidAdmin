package com.android.seandroid_admin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;

/**
 * Receive BOOT_COMPLETE so we can insert OTA cert into db.
 * Needed because the ConfigUpdate code requires an approved
 * cert in the Settings.Secure database when checking policy
 * updates.
 */
public class BootReceiver extends BroadcastReceiver {

    private static String TAG = "SEAndroidAdminBootReceiver";
    private static final String OTA_CERTS = "/system/etc/security/otacerts.zip";

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        if (arg1.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            try {
                insertOTACert(arg0);
            } catch (Exception ex) {
                Log.d(TAG, "Error inserting OTA cert.", ex);
            }
        }
    }

    // Just install the very first OTA returned by zip.entries().
    // The config_update_certificate only accepts one entry for now
    // and at least AOSP builds typically only have one cert in
    // otacerts.zip
    private void insertOTACert(Context ctx) throws IOException {

        ZipFile zip = new ZipFile(OTA_CERTS);
        try {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            if (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream is = zip.getInputStream(entry);
                InputStreamReader isr = new InputStreamReader(is);
                try {
                    String cert = CharStreams.toString(isr);
                    cert = cert.replace("-----BEGIN CERTIFICATE-----", "");
                    cert = cert.replace("-----END CERTIFICATE-----", "");
                    cert = cert.replace("\r","").replace("\n","");
                    Settings.Secure.putString(ctx.getContentResolver(),
                                              "config_update_certificate", cert);
                } finally {
                    Closeables.closeQuietly(is);
                    Closeables.closeQuietly(isr);
                }
            }
        } finally {
            zip.close();
        }
    }
}
