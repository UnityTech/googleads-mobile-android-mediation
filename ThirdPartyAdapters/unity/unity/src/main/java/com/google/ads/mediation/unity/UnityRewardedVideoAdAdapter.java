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

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.services.banners.UnityBanners;

import java.lang.ref.WeakReference;

import static android.content.ContentValues.TAG;

/**
 * The {@link UnityRewardedVideoAdAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityRewardedVideoAdAdapter implements MediationRewardedVideoAdAdapter, OnContextChangedListener {

    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private WeakReference<Activity> mActivityWeakReference;
    private UnityMediationAd mAd;
    private boolean mInitialized = false;

    @Override
    public void initialize(Context context, MediationAdRequest mediationAdRequest, String userId, MediationRewardedVideoAdListener mediationRewardedVideoAdListener, Bundle serverParameters, Bundle networkExtras) {
        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;

        MediationMetaData mediationMetadata = new MediationMetaData(context);
        mediationMetadata.setName("AdMob");
        mediationMetadata.commit();


        String gameId = serverParameters.getString(Utilities.KEY_GAME_ID);
        String placementId = serverParameters.getString(Utilities.KEY_PLACEMENT_ID);

        if (!Utilities.isValidIds(gameId, placementId)) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onInitializationFailed(UnityRewardedVideoAdAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!Utilities.isValidContext(context)) {
            return;
        }

        UnityListenerMultiplexer.setGlobalProxyListener(UnityBanners.getBannerListener());
        UnityListenerMultiplexer.setGlobalProxyListener(UnityAds.getListener());
        UnityListenerMultiplexer.addGlobalListener(new IUnityAdsExtendedListener() {
            @Override
            public void onUnityAdsClick(String placementId) {

            }

            @Override
            public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
                if (!mInitialized && newState == UnityAds.PlacementState.WAITING) {
                    mInitialized = true;
                    if (mMediationRewardedVideoAdListener != null) {
                        mMediationRewardedVideoAdListener.onInitializationSucceeded(UnityRewardedVideoAdAdapter.this);
                    }
                }
            }

            @Override
            public void onUnityAdsReady(String placementId) {

            }

            @Override
            public void onUnityAdsStart(String placementId) {

            }

            @Override
            public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {

            }

            @Override
            public void onUnityAdsError(UnityAds.UnityAdsError error, String message) {
                if (error == UnityAds.UnityAdsError.INITIALIZE_FAILED ||
                        error == UnityAds.UnityAdsError.AD_BLOCKER_DETECTED) {
                    if (mMediationRewardedVideoAdListener != null) {
                        mMediationRewardedVideoAdListener.onInitializationFailed(UnityRewardedVideoAdAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    }
                }
            }
        });

        UnityBanners.setBannerListener(UnityListenerMultiplexer.getInstance());
        UnityAds.initialize((Activity) context, gameId, UnityListenerMultiplexer.getInstance());

        mAd = new UnityMediationAd(placementId, "REWARDED");

        mAd.setListener(new IUnityMediationListener() {
            @Override
            public void onUnityAdsLoaded(String placementId) {
                if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdLoaded(UnityRewardedVideoAdAdapter.this);
                }
            }

            @Override
            public void onUnityAdsLoadFailed(String placementId) {
                if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdFailedToLoad(UnityRewardedVideoAdAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                }
            }

            @Override
            public void onUnityAdsStart(String placementId) {
                if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdOpened(UnityRewardedVideoAdAdapter.this);
                    mMediationRewardedVideoAdListener.onVideoStarted(UnityRewardedVideoAdAdapter.this);
                }
            }

            @Override
            public void onUnityAdsClick(String placementId) {
                if (mMediationRewardedVideoAdListener != null) {
                    mMediationRewardedVideoAdListener.onAdClicked(UnityRewardedVideoAdAdapter.this);
                }
            }

            @Override
            public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {
                if (mMediationRewardedVideoAdListener != null) {
                    if (result == UnityAds.FinishState.COMPLETED) {
                        mMediationRewardedVideoAdListener.onRewarded(UnityRewardedVideoAdAdapter.this, new UnityReward());
                    }
                    mMediationRewardedVideoAdListener.onVideoCompleted(UnityRewardedVideoAdAdapter.this);
                    mMediationRewardedVideoAdListener.onAdClosed(UnityRewardedVideoAdAdapter.this);
                }
            }
        });

        mActivityWeakReference = new WeakReference<>((Activity) context);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle bundle, Bundle bundle1) {
        Activity activity = mActivityWeakReference.get();
        if (activity == null) {
            // Activity is null, logging a warning and sending ad closed callback.
            Log.w(TAG, "An activity context is required to show Unity Ads, please call "
                    + "RewardedVideoAd#resume(Context) in your Activity's onResume.");
            mMediationRewardedVideoAdListener.onAdFailedToLoad(UnityRewardedVideoAdAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        mAd.load(activity);
    }

    @Override
    public void showVideo() {
        Activity activity = mActivityWeakReference.get();
        if (activity == null) {
            // Activity is null, logging a warning and sending ad closed callback.
            Log.w(TAG, "An activity context is required to show Unity Ads, please call "
                    + "RewardedVideoAd#resume(Context) in your Activity's onResume.");
            mMediationRewardedVideoAdListener.onAdOpened(UnityRewardedVideoAdAdapter.this);
            mMediationRewardedVideoAdListener.onAdClosed(UnityRewardedVideoAdAdapter.this);
            return;
        }
        mAd.show(activity);
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
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
