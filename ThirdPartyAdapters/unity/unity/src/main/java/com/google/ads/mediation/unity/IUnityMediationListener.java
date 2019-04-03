package com.google.ads.mediation.unity;

import com.unity3d.ads.UnityAds;

public interface IUnityMediationListener {
    void onUnityAdsLoaded(String placementId);
    void onUnityAdsLoadFailed(String placementId);
    void onUnityAdsStart(String placementId);
    void onUnityAdsClick(String placementId);
    void onUnityAdsFinish(String placementId, UnityAds.FinishState result);
}
