package com.android.seandroid_admin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.ByteStreams;

import com.android.seandroid_admin.R;

/**
 * Mock policy updates using new ConfigUpdateInstallReceiver mechanism.
 * Code will help explore the new ConfigUpdate mechanism regarding mmac
 * and kernel policies. Ideally, you'll want to use the host based build*bundle
 * tool to first generate the zip file that will contain both the
 * approved OTA bundle format as well as the metadata file. The zip
 * is expected to be pushed to /sdcard. In a production environment
 * you'll clearly want to change the location.
 */
public class ConfigUpdateFragment extends PreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String TAG = "SEAdminConfigUpdateFragment";

    private static final String AUTHORITY = "com.android.seandroid_admin.fileprovider";

    // Connection back out to the main fragment.
    protected SEAndroidAdminActivity mActivity;
    private File mPolicyLocation;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (SEAndroidAdminActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addPreferencesFromResource(R.xml.config_update_mmac_fragment);

        // Create location for policy bundles
        mPolicyLocation = new File(mActivity.getFilesDir(), "policy");
        mPolicyLocation.mkdir();

        new EopsConfigUpdate();
        new IfwConfigUpdate();
        new SELinuxUpdate();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return true;
    }

    private class ConfigUpdate {

        private File mBundleFile = null;
        private File mMetadataFile = null;
        private File mZipFile = null;

        private final Preference mReload;
        private final String mKeyReload;
        private final String mIntent;

        private final String mBundleFilePath;
        private final String mMetadataFilePath;
        private final String mBundleZipPath;

        private static final String ZIP_METADATA_ENTRY = "update_bundle_metadata";
        private static final String ZIP_BUNDLE_ENTRY = "update_bundle";

        private final Map<String, File> mZipMap = new HashMap<>(2);

        ConfigUpdate(final String name, final String intent) {
            mKeyReload = "key_" + name + "_reload";
            mBundleFilePath = name + "_bundle";
            mMetadataFilePath = name + "_bundle_metadata";
            mBundleZipPath = name + "_bundle.zip";

            mIntent = "android.intent.action." + intent;

            mBundleFile = new File(mPolicyLocation, mBundleFilePath);
            mMetadataFile = new File(mPolicyLocation, mMetadataFilePath);

            mZipMap.put(ZIP_BUNDLE_ENTRY, mBundleFile);
            mZipMap.put(ZIP_METADATA_ENTRY, mMetadataFile);

            final File extDir = Environment.getExternalStorageDirectory();
            if (extDir != null) {
                mZipFile = new File(extDir, mBundleZipPath);
            } else {
                Log.e(TAG, "External storage directory not found. " +
                      "Policy updates won't work.");
            }

            mReload = getPreferenceScreen().findPreference(mKeyReload);
            mReload.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.d(TAG, "Loading of policy bundle requested.");

                    FileInputStream fis = null;
                    ZipInputStream zis = null;
                    try {
                        fis = new FileInputStream(mZipFile);
                        zis = new ZipInputStream(fis);
                        ZipEntry ze;
                        while ((ze = zis.getNextEntry()) != null) {
                            String name = ze.getName();
                            if (mZipMap.containsKey(name)) {
                                File output = mZipMap.get(name);
                                try {
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    byte[] data = new byte[1024];
                                    int n;
                                    while ((n = zis.read(data)) != -1) {
                                        baos.write(data, 0, n);
                                    }
                                    atomicWriteToFile(mPolicyLocation, output, baos.toByteArray());
                                } catch (IOException ioex) {
                                    throw new Exception("Failed extracting policy zip. " + ioex);
                                } finally {
                                    zis.closeEntry();
                                }
                            }
                        }

                        // Open the metadata file.
                        Scanner scan = new Scanner(mMetadataFile);
                        scan.useDelimiter(":");
                        String requiredHash = scan.next();
                        String signature = scan.next();
                        String version = scan.next();

                        // BUild the URI to the private policy files
                        File policyPath = new File(mActivity.getFilesDir(), "policy");
                        File policy = new File(policyPath, mBundleFilePath);
                        Uri contentUri = FileProvider.getUriForFile(mActivity.getApplicationContext(),
                                AUTHORITY, policy);

                        // Build the intent to broadcast.
                        Intent i = new Intent(mIntent);
                        i.putExtra("REQUIRED_HASH", requiredHash);
                        i.putExtra("SIGNATURE", signature);
                        i.putExtra("VERSION", version);
                        i.setData(contentUri);

                        Log.d(TAG, mIntent + " being broadcast. " + i + " Extras: " + i.getExtras());
                        mActivity.sendBroadcast(i);

                    } catch (Exception ex) {
                        Toast.makeText(mActivity, ex.toString(), Toast.LENGTH_SHORT).show();
                        mBundleFile.delete();
                        mMetadataFile.delete();
                        Log.e(TAG, "Exception loading policy.", ex);
                    } finally {
                        if (zis != null) Closeables.closeQuietly(zis);
                        if (fis != null) Closeables.closeQuietly(fis);
                    }

                    return true;
                }
            });

            String mes = "Load from: ";
            if (extDir == null) {
                mes += "Can't locate sdcard. Policy location error.";
                mReload.setEnabled(false);
                mReload.setSelectable(false);
            } else {
                mes += mZipFile.getPath();
            }
            mReload.setSummary(mes);
        }

        private void atomicWriteToFile(File dir, File file, byte[] content) throws IOException {
            FileOutputStream out = null;
            File tmp = null;
            try {
                tmp = File.createTempFile("journal", "", dir);
                tmp.setReadable(true);
                out = new FileOutputStream(tmp);
                out.write(content);
                out.getFD().sync();
                if (!tmp.renameTo(file)) {
                    throw new IOException("Failed to atomically rename " + file.getCanonicalPath());
                }
            } finally {
                if (tmp != null) {
                    tmp.delete();
                }
                Closeables.closeQuietly(out);
            }
        }
    }

    private class EopsConfigUpdate extends ConfigUpdate {
        EopsConfigUpdate() {
            super("eops", "UPDATE_EOPS");
        }
    }

    private class IfwConfigUpdate extends ConfigUpdate {
        IfwConfigUpdate() {
            super("ifw", "UPDATE_INTENT_FIREWALL");
        }
    }

    private class SELinuxUpdate extends ConfigUpdate {
        SELinuxUpdate() {
            super("selinux", "UPDATE_SEPOLICY");
        }
    }
}
