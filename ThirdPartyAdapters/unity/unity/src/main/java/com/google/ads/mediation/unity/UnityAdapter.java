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
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.services.banners.UnityBanners;

import java.lang.ref.WeakReference;

import static android.content.ContentValues.TAG;

/**
 * The {@link UnityAdapter} is used to load Unity ads and mediate the callbacks between Google
 * Mobile Ads SDK and Unity Ads SDK.
 */

public class UnityAdapter implements MediationRewardedVideoAdAdapter, MediationInterstitialAdapter,
        OnContextChangedListener, MediationBannerAdapter {

    private UnityRewardedVideoAdAdapter mRewardedAdapter = new UnityRewardedVideoAdAdapter();
    private UnityInterstitialVideoAdAdapter mInterstitialAdapter = new UnityInterstitialVideoAdAdapter();
    private UnityBannerAdapter mBannerAdapter = new UnityBannerAdapter();

    @Override
    public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener, Bundle bundle, AdSize adSize, MediationAdRequest mediationAdRequest, Bundle bundle1) {
        mBannerAdapter.requestBannerAd(context, mediationBannerListener, bundle, adSize, mediationAdRequest, bundle1);
    }

    @Override
    public View getBannerView() {
        return mBannerAdapter.getBannerView();
    }

    @Override
    public void initialize(Context context, MediationAdRequest mediationAdRequest, String s, MediationRewardedVideoAdListener mediationRewardedVideoAdListener, Bundle bundle, Bundle bundle1) {
        mRewardedAdapter.initialize(context, mediationAdRequest, s, mediationRewardedVideoAdListener, bundle, bundle1);
    }

    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle bundle, Bundle bundle1) {
        mRewardedAdapter.loadAd(mediationAdRequest, bundle, bundle1);
    }

    @Override
    public void showVideo() {
        mRewardedAdapter.showVideo();
    }

    @Override
    public boolean isInitialized() {
        return mRewardedAdapter.isInitialized();
    }

    @Override
    public void requestInterstitialAd(Context context, MediationInterstitialListener mediationInterstitialListener, Bundle bundle, MediationAdRequest mediationAdRequest, Bundle bundle1) {
        mInterstitialAdapter.requestInterstitialAd(context, mediationInterstitialListener, bundle, mediationAdRequest, bundle1);
    }

    @Override
    public void showInterstitial() {
        mInterstitialAdapter.showInterstitial();
    }

    @Override
    public void onDestroy() {
        mRewardedAdapter.onDestroy();
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onContextChanged(Context context) {
        mInterstitialAdapter.onContextChanged(context);
        mRewardedAdapter.onContextChanged(context);
    }
}
