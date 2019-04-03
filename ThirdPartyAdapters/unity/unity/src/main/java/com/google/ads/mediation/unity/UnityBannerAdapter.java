package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.unity3d.services.banners.IUnityBannerListener;
import com.unity3d.services.banners.UnityBanners;

@Keep
public class UnityBannerAdapter implements MediationBannerAdapter, IUnityBannerListener {
    private MediationBannerListener mMediationBannerListener;
    private View mBannerView;
    private String mPlacementId;

    @Override
    public void requestBannerAd(Context context,
                                MediationBannerListener listener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest adRequest,
                                Bundle mediationExtras) {
        mMediationBannerListener = listener;

        String gameId = serverParameters.getString(Utilities.KEY_GAME_ID);
        mPlacementId = serverParameters.getString(Utilities.KEY_PLACEMENT_ID);

        if (!Utilities.isValidIds(gameId, mPlacementId)) {
            if (mMediationBannerListener != null) {
                mMediationBannerListener.onAdFailedToLoad(UnityBannerAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        if (!Utilities.isValidContext(context)) {
            return;
        }

        UnityListenerMultiplexer.addBannerPlacementListener(mPlacementId, this);
        UnityBanners.loadBanner((Activity) context, mPlacementId);
    }

    @Override
    public View getBannerView() {
        return mBannerView;
    }

    @Override
    public void onDestroy() {
        UnityBanners.destroy();
        UnityListenerMultiplexer.removeBannerPlacementListener(mPlacementId, this);
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onUnityBannerLoaded(String placementId, View view) {
        mBannerView = view;

        if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdLoaded(this);
        }
    }

    @Override
    public void onUnityBannerUnloaded(String placementId) {
        mBannerView = null;

        if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdClosed(this);
        }
    }

    @Override
    public void onUnityBannerShow(String placementId) {
        if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdOpened(this);
        }
    }

    @Override
    public void onUnityBannerClick(String placementId) {
        if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdClicked(this);
        }
    }

    @Override
    public void onUnityBannerHide(String placementId) {
        if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdClosed(this);
        }
    }

    @Override
    public void onUnityBannerError(String message) {
        if (mMediationBannerListener != null) {
            mMediationBannerListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_NO_FILL);
        }
    }
}
