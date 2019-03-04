package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;

import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.services.UnitySdkListener;
import com.unity3d.services.UnityServices;
import com.unity3d.services.monetization.UnityMonetization;
import com.unity3d.services.monetization.mobileads.UnityMobileAds;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UnitySingleton implements UnitySdkListener {
	// The name of this mediation adapter.
    private static final String MEDIATION_ADAPTER_NAME = "AdMob";
    // The version of this mediation adapter.
    private static final String MEDIATION_ADAPTER_VERSION = "3.0.0";

    // The only instance of UnitySingleton
    private static UnitySingleton mInstance;

    /**
     * Returns the global instance of the UnitySingleton, creating it if it does not exist yet.
     * @return
     */
    public synchronized static UnitySingleton getInstance() {
        if (mInstance == null) {
            mInstance = new UnitySingleton();
        }
        return mInstance;
    }

    /**
     * State of the UnityAds SDK.
     */
    public enum UnitySdkInitState {
        NOT_INITIALIZED,
        INITIALIZING,
        INITIALIZED,
        ERROR
    }

    private static final String GAME_ID_KEY = "gameId";

    // A queue of sdk listeners to be notified
    private Set<WeakReference<UnitySdkListener>> mSdkListeners =
            Collections.synchronizedSet(new HashSet<WeakReference<UnitySdkListener>>());

    // Boolean to determine if the SDK is initialized or not. Will be set when SDK is finished
    // initializing.
    private UnitySdkInitState mInitState = UnitySdkInitState.NOT_INITIALIZED;

    // If the SDK initialized with an exception, we will record it to prevent reinitialization.
    // Assume this will be non-null if mInitState is InitState.ERROR
    private Exception mInitException;

    /**
     * Hidden constructor.
     */
    private UnitySingleton() { /**/ }

    /**
     * Initializes UnityAds.
     *
     * @param context The android context passed from the mediation provider.
     * @param sdkListener The SDK lifecycle listener, will be used to notify when SDK initializes or
     *                    fails to initialize.
     * @return True if initialization was called, false if it was already initialized.
     */
    public boolean initUnityAds(Context context,
                                String gameId,
                                UnitySdkListener sdkListener) {
        if (isState(UnitySdkInitState.INITIALIZED)) {
            // if already initialized, then just notify the SDK listener.
            sdkListener.onSdkInitialized();
            return false;
        } else if (isState(UnitySdkInitState.ERROR)) {
            // An error occurred while initializing, so just notify the SDK listener.
            sdkListener.onSdkInitializationFailed(mInitException);
            return false;
        } else if (isState(UnitySdkInitState.INITIALIZING)) {
            // If we are already initializing, we will just append the SDK listener to the set.
            mSdkListeners.add(new WeakReference<>(sdkListener));
            return false;
        } else {
            // Actually do the initialization.
            mSdkListeners.add(new WeakReference<>(sdkListener));
            if (!doInit(context, gameId)) {
                // An error occurred within the sanity check of UnityAds. Assume that the state is now errored.
                notifyListenersSdkInitializationDidFail();
            }
        }
        return true;
    }

    /**
     * Returns if the UnitySingleton is in a given state
     * @param testState The state to test against
     * @return True if in the given state, else false.
     */
    private boolean isState(UnitySdkInitState testState) {
        return mInitState.equals(testState);
    }

    /**
     * Actually does the initializing UnityAds.
     * @param context
     * @param gameId
     *
     * @return True if initialization was started, else false for error.
     */
    private boolean doInit(Context context, String gameId) {
        if (UnityAds.isInitialized()) {
            // In theory, this should never occur, but let's sanity check it anyway.
            return true;
        }

        mInitState = UnitySdkInitState.INITIALIZING;

        if (!UnityAds.isSupported()) {
            setErrorState(new IllegalStateException("Platform is not supported for Unity Ads"));
            return false;
        }

        // Validate the game ID
        if (gameId == null || gameId.isEmpty()) {
            setErrorState(new IllegalArgumentException(String.format(
                    "Server extras did not contain key for: \"%s\"", GAME_ID_KEY)));
            return false;
        }

        // Validate the context
        if (context == null || !(context instanceof Activity)) {
            setErrorState(new IllegalArgumentException("Context was not a valid Activity instance"));
            return false;
        }

        setMediationMetadata(context);

        UnityServices.addSdkListener(this);
        UnityMobileAds.enablePerPlacementLoading();
        UnityMonetization.initialize((Activity) context, gameId);
        return true;
    }

    @Override
    public void onSdkInitialized() {
        mInitState = UnitySdkInitState.INITIALIZED;

        Iterator<WeakReference<UnitySdkListener>> it = mSdkListeners.iterator();
        while (it.hasNext()) {
            try {
                UnitySdkListener listener = it.next().get();
                if (listener != null) {
                    listener.onSdkInitialized();
                }
            } finally {
                it.remove();
            }
        }
    }

    @Override
    public void onSdkInitializationFailed(Exception e) {
        setErrorState(e);
        notifyListenersSdkInitializationDidFail();
    }

    private void notifyListenersSdkInitializationDidFail() {
        Iterator<WeakReference<UnitySdkListener>> it = mSdkListeners.iterator();
        while (it.hasNext()) {
            try {
                UnitySdkListener listener = it.next().get();
                if (listener != null) {
                    listener.onSdkInitializationFailed(mInitException);
                }
            } finally {
                it.remove();
            }
        }
    }

    /**
     * Sets the singleton into the errored state with the given message.
     * @param e The exception to set as the init exception.
     */
    private void setErrorState(Exception e) {
        mInitState = UnitySdkInitState.ERROR;
        mInitException = e;
    }

    /**
     * Sets the Mediation metadata within UnityAds
     * @param context Context used to initialize UnityAds.
     */
    private void setMediationMetadata(Context context) {
        MediationMetaData mediationMetaData = new MediationMetaData(context);
        mediationMetaData.setName(MEDIATION_ADAPTER_NAME);
        mediationMetaData.setVersion(MEDIATION_ADAPTER_VERSION);
        mediationMetaData.commit();
    }

    /**
     * This class is needed for {@link UnityAds#initialize(Activity, String, IUnityAdsListener)},
     * but is not actually used under mediation.
     */
    private class NullUnityAdsListener implements IUnityAdsListener {
        @Override
        public void onUnityAdsReady(String s) { }

        @Override
        public void onUnityAdsStart(String s) { }

        @Override
        public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) { }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) { }
    }
}
