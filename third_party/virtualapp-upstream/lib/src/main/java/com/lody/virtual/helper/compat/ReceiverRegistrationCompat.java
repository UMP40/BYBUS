package com.lody.virtual.helper.compat;

/** Locate nullable registration arguments by the Binder signature, never by their current value. */
public final class ReceiverRegistrationCompat {
    private ReceiverRegistrationCompat() { }

    public static void clearRequiredPermission(Class<?>[] parameterTypes, Object[] arguments) {
        if (parameterTypes == null || arguments == null || parameterTypes.length != arguments.length) return;
        for (int i = 0; i + 1 < parameterTypes.length; i++) {
            if (parameterTypes[i] != null
                    && "android.content.IntentFilter".equals(parameterTypes[i].getName())
                    && parameterTypes[i + 1] == String.class) {
                // In the modern ABI, index 4 is the receiver, NOT the permission. A null
                // permission is legitimate and must not trigger the legacy-index fallback.
                if (arguments[i + 1] == null || arguments[i + 1] instanceof String) {
                    arguments[i + 1] = null;
                }
                return;
            }
        }
    }

    /**
     * Preserve targetSdk < 33 microG's implicit-export contract when Android 14+ enforces
     * the physical host's newer target SDK. Explicit flags and sticky queries are untouched.
     */
    public static void applyLegacyMicrogExportFlag(int apiLevel, String packageName, int targetSdk,
                                                   Class<?>[] parameterTypes, Object[] arguments) {
        if (apiLevel < 34 || targetSdk <= 0 || targetSdk >= 33
                || !"com.google.android.gms".equals(packageName)
                || parameterTypes == null || arguments == null
                || parameterTypes.length != arguments.length) return;
        for (int i = 1; i + 3 < parameterTypes.length; i++) {
            if (parameterTypes[i] == null
                    || !"android.content.IntentFilter".equals(parameterTypes[i].getName())
                    || parameterTypes[i + 1] != String.class
                    || parameterTypes[i + 2] != int.class
                    || parameterTypes[i + 3] != int.class
                    || i + 4 != parameterTypes.length) continue;
            if (arguments[i - 1] != null && arguments[i] != null
                    && arguments[i + 3] instanceof Integer) {
                int flags = (Integer) arguments[i + 3];
                // RECEIVER_EXPORTED=2, RECEIVER_NOT_EXPORTED=4 (API 33 constants).
                if ((flags & 6) == 0) {
                    arguments[i + 3] = flags | 2;
                }
            }
            return;
        }
    }
}
