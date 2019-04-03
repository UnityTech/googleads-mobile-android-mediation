// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import com.unity3d.ads.UnityAds;

import java.lang.ref.WeakReference;

import static android.content.ContentValues.TAG;

/**
 * The {@link UnityInterstitialVideoAdAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityInterstitialVideoAdAdapter implements MediationInterstitialAdapter, OnContextChangedListener {

    private MediationInterstitialListener mMediationInterstitialListener;
    private WeakReference<Activity> mActivityWeakReference;
    private UnityMediationAd mAd;


    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        mMediationInterstitialListener = mediationInterstitialListener;

        String gameId = serverParameters.getString(Utilities.KEY_GAME_ID);
        String placementId = serverParameters.getString(Utilities.KEY_PLACEMENT_ID);

        if (!Utilities.isValidIds(gameId, placementId)) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityInterstitialVideoAdAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!Utilities.isValidContext(context)) {
            return;
        }

        mAd = new UnityMediationAd(placementId, "INTERSTITIAL");

        mAd.setListener(new IUnityMediationListener() {
            @Override
            public void onUnityAdsLoaded(String placementId) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdLoaded(UnityInterstitialVideoAdAdapter.this);
                }
            }

            @Override
            public void onUnityAdsLoadFailed(String placementId) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdFailedToLoad(UnityInterstitialVideoAdAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                }
            }

            @Override
            public void onUnityAdsStart(String placementId) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdOpened(UnityInterstitialVideoAdAdapter.this);
                }
            }

            @Override
            public void onUnityAdsClick(String placementId) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdClicked(UnityInterstitialVideoAdAdapter.this);
                }
            }

            @Override
            public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {
                if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdClosed(UnityInterstitialVideoAdAdapter.this);
                }
            }
        });

        mActivityWeakReference = new WeakReference<>((Activity) context);
    }

    @Override
    public void showInterstitial() {
        Activity activity = mActivityWeakReference.get();
        if (activity == null) {
            // Activity is null, logging a warning and sending ad closed callback.
            Log.w(TAG, "An activity context is required to show Unity Ads, please call "
                    + "RewardedVideoAd#resume(Context) in your Activity's onResume.");
            mMediationInterstitialListener.onAdOpened(UnityInterstitialVideoAdAdapter.this);
            mMediationInterstitialListener.onAdClosed(UnityInterstitialVideoAdAdapter.this);
            return;
        }
        mAd.show(activity);
    }

    @Override
    public void onDestroy() {
        mAd.destroy();
        mAd = null;
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onContextChanged(Context context) {
        if (!Utilities.isValidContext(context)) {
            return;
        }

        // Storing a weak reference of the current Activity to be used when showing an ad.
        mActivityWeakReference = new WeakReference<>((Activity) context);
    }
}
