/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os.storage;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.IVold;

/**
 * Mount service local interface.
 *
 * @hide Only for use within the system server.
 */
public abstract class StorageManagerInternal {

    /**
     * Policy that influences how external storage is mounted and reported.
     */
    public interface ExternalStorageMountPolicy {
        /**
         * Gets the external storage mount mode for the given uid.
         *
         * @param uid The UID for which to determine mount mode.
         * @param packageName The package in the UID for making the call.
         * @return The mount mode.
         *
         * @see com.android.internal.os.Zygote#MOUNT_EXTERNAL_NONE
         * @see com.android.internal.os.Zygote#MOUNT_EXTERNAL_DEFAULT
         * @see com.android.internal.os.Zygote#MOUNT_EXTERNAL_READ
         * @see com.android.internal.os.Zygote#MOUNT_EXTERNAL_WRITE
         */
        public int getMountMode(int uid, String packageName);

        /**
         * Gets whether external storage should be reported to the given UID.
         *
         * @param uid The UID for which to determine whether it has external storage.
         * @param packageName The package in the UID for making the call.
         * @return Weather to report external storage.
         * @return True to report the state of external storage, false to
         *     report it as unmounted.
         */
        public boolean hasExternalStorage(int uid, String packageName);
    }

    /**
     * Adds a policy for determining how external storage is mounted and reported.
     * The mount mode is the most conservative result from querying all registered
     * policies. Similarly, the reported state is the most conservative result from
     * querying all registered policies.
     *
     * @param policy The policy to add.
     */
    public abstract void addExternalStoragePolicy(ExternalStorageMountPolicy policy);

    /**
     * Notify the mount service that the mount policy for a UID changed.
     * @param uid The UID for which policy changed.
     * @param packageName The package in the UID for making the call.
     */
    public abstract void onExternalStoragePolicyChanged(int uid, String packageName);

    /**
     * Gets the mount mode to use for a given UID as determined by consultin all
     * policies.
     *
     * @param uid The UID for which to get mount mode.
     * @param packageName The package in the UID for making the call.
     * @return The mount mode.
     */
    public abstract int getExternalStorageMountMode(int uid, String packageName);

    /**
     * Create storage sandbox for the given package.
     *
     * <p> This will involve calling into vold to setup appropriate bind mounts.
     *
     * @param packageName The package for which the sandbox needs to be created.
     * @param appId The appId for the given package.
     * @param sharedUserId The sharedUserId for given package if it specified
     *      {@code android:sharedUserId} in the manifest, otherwise {@code null}
     * @param userId The userId in which the sandbox needs to be created.
     */
    public abstract void prepareSandboxForApp(@NonNull String packageName, int appId,
            @Nullable String sharedUserId, int userId);

    /**
     * Delete storage sandbox for the given package.
     *
     * @param packageName The package for which the sandbox needs to be destroyed.
     * @param sharedUserId The sharedUserId if specified by the package.
     * @param userId The userId in which the sandbox needs to be destroyed.
     */
    public abstract void destroySandboxForApp(@NonNull String packageName,
            @Nullable String sharedUserId, int userId);

    /**
     * @return Labels of storage volumes that are visible to the given userId.
     */
    public abstract String[] getVisibleVolumesForUser(int userId);

    /**
     * A listener for reset events in the StorageManagerService.
     */
    public interface ResetListener {
        /**
         * A method that should be triggered internally by StorageManagerInternal
         * when StorageManagerService reset happens.
         *
         * @param vold The binder object to vold.
         */
        void onReset(IVold vold);
    }

    /**
     * Add a listener to listen to reset event in StorageManagerService.
     *
     * @param listener The listener that will be notified on reset events.
     */
    public abstract void addResetListener(ResetListener listener);

    /**
     * Return the sandboxId for the given package on external storage.
     */
    public abstract String getSandboxId(String packageName);
}
