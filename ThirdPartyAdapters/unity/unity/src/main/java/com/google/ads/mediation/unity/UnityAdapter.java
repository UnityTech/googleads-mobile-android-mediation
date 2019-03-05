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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.OnContextChangedListener;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import com.unity3d.ads.UnityAds;
import com.unity3d.services.UnitySdkListener;
import com.unity3d.services.monetization.mobileads.AdRequestException;
import com.unity3d.services.monetization.mobileads.BannerAd;
import com.unity3d.services.monetization.mobileads.BannerSize;
import com.unity3d.services.monetization.mobileads.InterstitialAd;
import com.unity3d.services.monetization.mobileads.RewardedVideoAd;

import java.lang.ref.WeakReference;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */
@Keep
public class UnityAdapter implements MediationRewardedVideoAdAdapter, MediationInterstitialAdapter,
		MediationBannerAdapter, OnContextChangedListener, UnitySdkListener {
    public static final String TAG = UnityAdapter.class.getSimpleName();

    /**
     * Key to obtain Game ID, required for loading Unity Ads.
     */
    private static final String KEY_GAME_ID = "gameId";

    /**
     * Key to obtain Placement ID, used to set the type of ad to be shown. Unity Ads has changed
     * the name from Zone ID to Placement ID in Unity Ads SDK 2.0.0. To maintain backwards
     * compatibility the key is not changed.
     */
    private static final String KEY_PLACEMENT_ID = "zoneId";

    /**
     * The current placementId.
     */
    private String mPlacementId;

    /**
     * An Android {@link Activity} weak reference used to show ads.
     */
    private WeakReference<Activity> mActivityWeakReference;

    /**
     * Adapter that proxies events between UnityAds's InterstitialAd and the Mediation Platform.
     */
    private UnityInterstitialAdMediationAdapter mInterstitialAdAdapter;
    /**
     * Adapter that proxies events between UnityAds's RewardedVideo and the Mediation Platform.
     */
    private UnityRewardedVideoMediationAdapter mRewardedVideoAdapter;

    /**
     * Adapter that proxies events between UnityAds's BannerAd and the Mediation Platform.
     */
    private UnityBannerMediationAdapter mBannerAdapter;

    /**
     * True if the UnityAds SDK has been initialized, else false.
     */
    private boolean mIsInitialized;

    /**
     * Checks whether or not the provided Unity Ads IDs are valid.
     *
     * @param gameId      Unity Ads Game ID to be verified.
     * @param placementId Unity Ads Placement ID to be verified.
     * @return {@code true} if all the IDs provided are valid.
     */
    private static boolean isValidIds(String gameId, String placementId) {
        if (TextUtils.isEmpty(gameId) || TextUtils.isEmpty(placementId)) {
            String ids = TextUtils.isEmpty(gameId) ? TextUtils.isEmpty(placementId)
                    ? "Game ID and Placement ID" : "Game ID" : "Placement ID";
            Log.w(TAG, ids + " cannot be empty.");

            return false;
        }

        return true;
    }

    /**
     * Unity Ads requires an Activity context to Initialize. This method will return false if
     * the context provided is either null or is not an Activity context.
     *
     * @param context to be checked if it is valid.
     * @return {@code true} if the context provided is valid, {@code false} otherwise.
     */
    private static boolean isValidContext(Context context) {
        if (context == null) {
            Log.w(TAG, "Context cannot be null.");
            return false;
        }

        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity. Unity Ads requires an Activity context to load "
                    + "ads.");
            return false;
        }
        return true;
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        String gameId = serverParameters.getString(KEY_GAME_ID);
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

        if (!isValidIds(gameId, mPlacementId)) {
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
        	// TODO Do we need to notify an error here?
            return;
        }
        // Storing a weak reference to the Activity.
        mActivityWeakReference = new WeakReference<>((Activity) context);
        mInterstitialAdAdapter = new UnityInterstitialAdMediationAdapter(mediationInterstitialListener);

        UnitySingleton.getInstance().initUnityAds(context, gameId, this);
    }

    @Override
    public void showInterstitial() {
        if (mInterstitialAdAdapter != null) {
            mInterstitialAdAdapter.show();
        }
    }

    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        String gameId = serverParameters.getString(KEY_GAME_ID);
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

        if (!isValidIds(gameId, mPlacementId)) {
            if (mediationRewardedVideoAdListener != null) {
                mediationRewardedVideoAdListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
            // TODO Do we need to notify an error here?
            return;
        }
        // Storing a weak reference to the Activity.
        mActivityWeakReference = new WeakReference<>((Activity) context);
        mRewardedVideoAdapter = new UnityRewardedVideoMediationAdapter(mediationRewardedVideoAdListener);

        UnitySingleton.getInstance().initUnityAds(context, gameId, this);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest,
                       Bundle serverParameters,
                       Bundle networkExtras) {
    	if (mRewardedVideoAdapter != null) {
    	    mRewardedVideoAdapter.loadAd();
        }
    }

    @Override
    public void showVideo() {
        if (mRewardedVideoAdapter != null) {
            mRewardedVideoAdapter.show();
        }
    }

    @Override
    public boolean isInitialized() {
    	return mIsInitialized;
    }

    @Override
    public void onDestroy() {
        if (mInterstitialAdAdapter != null) {
            mInterstitialAdAdapter.destroy();
        }
        if (mRewardedVideoAdapter != null) {
            mRewardedVideoAdapter.destroy();
        }
        if (mBannerAdapter != null) {
            mBannerAdapter.destroy();
        }
    }

    @Override
    public void onPause() {}

    @Override
    public void onResume() {}

    @Override
    public void onContextChanged(Context context) {
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity. Unity Ads requires an Activity context to show "
                    + "ads.");
            return;
        }

        // Storing a weak reference of the current Activity to be used when showing an ad.
        mActivityWeakReference = new WeakReference<>((Activity) context);
    }

    @Override
    public void onSdkInitialized() {
        mIsInitialized = true;
        if (mInterstitialAdAdapter != null) {
            mInterstitialAdAdapter.onSdkInitialized();
        } else if (mRewardedVideoAdapter != null) {
            mRewardedVideoAdapter.onSdkInitialized();
        }
    }

    @Override
    public void onSdkInitializationFailed(Exception e) {
        if (mInterstitialAdAdapter != null) {
            mInterstitialAdAdapter.onSdkInitializationFailed(e);
        } else if (mRewardedVideoAdapter != null) {
            mRewardedVideoAdapter.onSdkInitializationFailed(e);
        }
    }

	@Override
	public void requestBannerAd(Context context,
                                MediationBannerListener mediationBannerListener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        String gameId = serverParameters.getString(KEY_GAME_ID);
        mPlacementId = serverParameters.getString(KEY_PLACEMENT_ID);

        if (!isValidIds(gameId, mPlacementId)) {
            if (mediationBannerListener != null) {
                mediationBannerListener.onAdFailedToLoad(UnityAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!isValidContext(context)) {
            // TODO Do we need to notify an error here?
            return;
        }
        // Storing a weak reference to the Activity.
        mActivityWeakReference = new WeakReference<>((Activity) context);
        mBannerAdapter = new UnityBannerMediationAdapter(mediationBannerListener, adSize);

        UnitySingleton.getInstance().initUnityAds(context, gameId, this);
	}

    @Override
    public View getBannerView() {
        if (mBannerAdapter != null) {
            return mBannerAdapter.getView();
        }
        return null;
    }

	private class UnityInterstitialAdMediationAdapter extends InterstitialAd.Listener
            implements UnitySdkListener {

        private InterstitialAd mInterstitial;
        private MediationInterstitialListener mMediationInterstitialListener;

        private UnityInterstitialAdMediationAdapter(MediationInterstitialListener listener) {
        	mMediationInterstitialListener = listener;
        }

        public void show() {
        	if (mInterstitial != null && mInterstitial.isReady()) {
                mInterstitial.show();
            } else {
        	    if (mMediationInterstitialListener != null) {
                    mMediationInterstitialListener.onAdOpened(UnityAdapter.this);
                    mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
                }
            }
        }

        private void loadAd() {
            mInterstitial = new InterstitialAd(mActivityWeakReference.get(), mPlacementId);
            mInterstitial.setListener(this);
            mInterstitial.load();
        }

        @Override
        public void onInterstitialAdLoaded(InterstitialAd interstitialAd) {
        	if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onInterstitialAdFailedToLoad(InterstitialAd interstitial, AdRequestException e) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }

        @Override
        public void onInterstitialAdClicked(InterstitialAd interstitialAd) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClicked(UnityAdapter.this);
            }
        }

        @Override
        public void onInterstitialAdLeavingApplication(InterstitialAd interstitialAd) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdLeftApplication(UnityAdapter.this);
            }
        }

        @Override
        public void onInterstitialAdClosed(InterstitialAd interstitialAd, UnityAds.FinishState finishState) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdClosed(UnityAdapter.this);
            }
        }

        @Override
        public void onSdkInitialized() {
        	loadAd();
        }

        @Override
        public void onSdkInitializationFailed(Exception e) {
            if (mMediationInterstitialListener != null) {
                mMediationInterstitialListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }

        public void destroy() {
            if (mInterstitial != null) {
                mInterstitial.destroy();
            }
        }
    }

    private class UnityRewardedVideoMediationAdapter extends RewardedVideoAd.Listener
            implements UnitySdkListener {

        private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
        private RewardedVideoAd mRewardedVideo;

        private UnityRewardedVideoMediationAdapter(MediationRewardedVideoAdListener mediationRewardedVideoAdListener) {
            mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;
        }

        @Override
        public void onRewardedVideoAdLoaded(RewardedVideoAd rewardedVideo) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoAdFailedToLoad(RewardedVideoAd rewardedVideo, AdRequestException e) {
        	if (mRewardedVideo != null) {
        	    mRewardedVideo.destroy();
        	    mRewardedVideo = null;
            }
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }

        @Override
        public void onRewardedVideoAdStarted(RewardedVideoAd rewardedVideo) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onVideoStarted(UnityAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoAdClicked(RewardedVideoAd rewardedVideo) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdClicked(UnityAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoAdLeavingApplication(RewardedVideoAd rewardedVideo) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onAdLeftApplication(UnityAdapter.this);
            }
        }

        @Override
        public void onRewardedVideoAdReward(RewardedVideoAd rewardedVideo) {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onRewarded(UnityAdapter.this, new UnityReward());
            }
        }

        @Override
        public void onRewardedVideoAdClosed(RewardedVideoAd rewardedVideo, UnityAds.FinishState finishState) {
        	if (mMediationRewardedVideoAdListener != null) {
        	    if (finishState.equals(UnityAds.FinishState.COMPLETED)) {
                    mMediationRewardedVideoAdListener.onVideoCompleted(UnityAdapter.this);
                }
                mMediationRewardedVideoAdListener.onAdClosed(UnityAdapter.this);
            }

        }

        @Override
        public void onSdkInitialized() {
            if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onInitializationSucceeded(UnityAdapter.this);
            }
        }

        @Override
        public void onSdkInitializationFailed(Exception e) {
        	if (mMediationRewardedVideoAdListener != null) {
                mMediationRewardedVideoAdListener.onInitializationFailed(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }

        public void loadAd() {
        	if (mRewardedVideo != null) {
        		if (mRewardedVideo.isReady()) {
        			if (mMediationRewardedVideoAdListener != null) {
        			    mMediationRewardedVideoAdListener.onAdLoaded(UnityAdapter.this);
                    }
                }
            } else {
        	    mRewardedVideo = new RewardedVideoAd(mActivityWeakReference.get(), mPlacementId);
        	    mRewardedVideo.setListener(this);
        	    mRewardedVideo.load();
            }
        }

        public void show() {
            if (mRewardedVideo != null && mRewardedVideo.isReady()) {
				mRewardedVideo.show();
            } else {
            	mMediationRewardedVideoAdListener.onAdOpened(UnityAdapter.this);
                mMediationRewardedVideoAdListener.onAdClosed(UnityAdapter.this);
            }
        }

        public void destroy() {
            if (mRewardedVideo != null) {
                mRewardedVideo.destroy();
            }
        }
    }

    private class UnityBannerMediationAdapter extends BannerAd.Listener implements UnitySdkListener {

        private final MediationBannerListener mMediationBannerListener;
        private final AdSize mAdSize;
        private BannerAd mBannerAd;

        private UnityBannerMediationAdapter(MediationBannerListener mediationBannerListener, AdSize adSize) {
            mMediationBannerListener = mediationBannerListener;
            mAdSize = adSize;
        }

        public void loadAd() {
            if (mBannerAd != null) {
                mBannerAd = new BannerAd(mActivityWeakReference.get(), mPlacementId, new BannerSize(mAdSize.getWidth(), mAdSize.getHeight()));
                mBannerAd.setListener(this);
                mBannerAd.load();
            }
        }

        @Override
        public void onSdkInitialized() {
            loadAd();
        }

        public View getView() {
            if (mBannerAd != null) {
                return mBannerAd.getView();
            }
            return null;
        }

        public void destroy() {
            if (mBannerAd != null) {
                mBannerAd.destroy();
                mBannerAd = null;
            }
        }

        @Override
        public void onBannerAdLoaded(BannerAd ad) {
        	if (mMediationBannerListener != null) {
        	    mMediationBannerListener.onAdLoaded(UnityAdapter.this);
            }
        }

        @Override
        public void onBannerAdFailedToLoad(BannerAd ad, AdRequestException e) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }

        @Override
        public void onSdkInitializationFailed(Exception e) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(UnityAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        }

        @Override
        public void onBannerAdOpening(BannerAd ad) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdOpened(UnityAdapter.this);
            }
        }

        @Override
        public void onBannerAdClosing(BannerAd ad) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdClosed(UnityAdapter.this);
            }
        }

        @Override
        public void onBannerAdClick(BannerAd ad) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdClicked(UnityAdapter.this);
            }
        }

        @Override
        public void onBannerAdLeavingApplication(BannerAd ad) {
        	if (mMediationBannerListener != null) {
        	    mMediationBannerListener.onAdLeftApplication(UnityAdapter.this);
            }
        }
    }
}
