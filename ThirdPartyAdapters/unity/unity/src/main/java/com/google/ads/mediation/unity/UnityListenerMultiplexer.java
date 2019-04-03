package com.google.ads.mediation.unity;

import android.view.View;

import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.services.banners.IUnityBannerListener;

import java.util.ArrayList;
import java.util.HashMap;

public class UnityListenerMultiplexer implements IUnityAdsExtendedListener, IUnityBannerListener {
    private static UnityListenerMultiplexer _instance;
    private static ArrayList<IUnityAdsExtendedListener> _globalListeners;
    private static HashMap<String, ArrayList<IUnityAdsExtendedListener>> _placementListeners;
    private static HashMap<String, ArrayList<IUnityBannerListener>> _placementBannerListeners;
    private static IUnityAdsListener _globalProxyListener;
    private static IUnityBannerListener _globalBannerProxyListener;

    public static UnityListenerMultiplexer getInstance() {
        if(_instance == null) {
            _instance = new UnityListenerMultiplexer();
        }

        return _instance;
    }

    public static void addGlobalListener(IUnityAdsExtendedListener listener) {
        if(_globalListeners == null) {
            _globalListeners = new ArrayList<>();
        }

        if(listener != null) {
            _globalListeners.add(listener);
        }
    }

    public static void removeGlobalListener(IUnityAdsExtendedListener listener) {
        if(_globalListeners == null || listener == null) {
            return;
        }

        _globalListeners.remove(listener);
    }

    public static void addPlacementListener(String placementId, IUnityAdsExtendedListener listener) {
        if(placementId == null) {
            return;
        }

        if(_placementListeners == null) {
            _placementListeners = new HashMap<>();
        }

        if(!_placementListeners.containsKey(placementId)) {
            _placementListeners.put(placementId, new ArrayList<IUnityAdsExtendedListener>());
        }

        if(listener != null) {
            _placementListeners.get(placementId).add(listener);
        }
    }

    public static void removePlacementListener(String placementId, IUnityAdsExtendedListener listener) {
        if(_placementListeners == null || placementId == null || listener == null) {
            return;
        }

        if(_placementListeners.containsKey(placementId)) {
            _placementListeners.get(placementId).remove(listener);
        }
    }

    public static void addBannerPlacementListener(String placementId, IUnityBannerListener listener) {
        if(placementId == null) {
            return;
        }

        if(_placementBannerListeners == null) {
            _placementBannerListeners = new HashMap<>();
        }

        if(!_placementBannerListeners.containsKey(placementId)) {
            _placementBannerListeners.put(placementId, new ArrayList<IUnityBannerListener>());
        }

        if(listener != null) {
            _placementBannerListeners.get(placementId).add(listener);
        }
    }

    public static void removeBannerPlacementListener(String placementId, IUnityBannerListener listener) {
        if(_placementListeners == null || placementId == null || listener == null) {
            return;
        }

        if(_placementListeners.containsKey(placementId)) {
            _placementListeners.get(placementId).remove(listener);
        }
    }

    public static void setGlobalProxyListener(IUnityAdsListener listener) {
        _globalProxyListener = listener;
    }

    public static void setGlobalProxyListener(IUnityBannerListener bannerListener) {
        _globalBannerProxyListener = bannerListener;
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        for(IUnityAdsExtendedListener listener : getPlacementListeners(placementId)) {
            listener.onUnityAdsClick(placementId);
        }
    }

    @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
        for(IUnityAdsExtendedListener listener : getPlacementListeners(placementId)) {
            listener.onUnityAdsPlacementStateChanged(placementId, oldState, newState);
        }
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if (_globalProxyListener != null) {
            _globalProxyListener.onUnityAdsReady(placementId);
        }
        for(IUnityAdsExtendedListener listener : getPlacementListeners(placementId)) {
            listener.onUnityAdsReady(placementId);
        }
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        if (_globalProxyListener != null) {
            _globalProxyListener.onUnityAdsStart(placementId);
        }
        for(IUnityAdsExtendedListener listener : getPlacementListeners(placementId)) {
            listener.onUnityAdsStart(placementId);
        }
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {
        if (_globalProxyListener != null) {
            _globalProxyListener.onUnityAdsFinish(placementId, result);
        }
        for(IUnityAdsExtendedListener listener : getPlacementListeners(placementId)) {
            listener.onUnityAdsFinish(placementId, result);
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError error, String message) {
        if (_globalProxyListener != null) {
            _globalProxyListener.onUnityAdsError(error, message);
        }
        if(_globalListeners != null) {
            for(IUnityAdsExtendedListener listener : _globalListeners) {
                listener.onUnityAdsError(error, message);
            }
        }
    }

    private ArrayList<IUnityAdsExtendedListener> getPlacementListeners(String placementId) {
        ArrayList<IUnityAdsExtendedListener> listeners = new ArrayList<>();

        if(_globalListeners != null) {
            listeners.addAll(_globalListeners);
        }

        if(_placementListeners != null) {
            if(_placementListeners.containsKey(placementId)) {
                listeners.addAll(_placementListeners.get(placementId));
            }
        }

        return listeners;
    }

    @Override
    public void onUnityBannerLoaded(String placementId, View view) {
        if (_globalBannerProxyListener != null) {
            _globalBannerProxyListener.onUnityBannerLoaded(placementId, view);
        }

        if(_placementBannerListeners != null) {
            if(_placementBannerListeners.containsKey(placementId)) {
                for(IUnityBannerListener listener : _placementBannerListeners.get(placementId)) {
                    listener.onUnityBannerLoaded(placementId, view);
                }
            }
        }
    }

    @Override
    public void onUnityBannerUnloaded(String placementId) {
        if (_globalBannerProxyListener != null) {
            _globalBannerProxyListener.onUnityBannerUnloaded(placementId);
        }

        if(_placementBannerListeners != null) {
            if(_placementBannerListeners.containsKey(placementId)) {
                for(IUnityBannerListener listener : _placementBannerListeners.get(placementId)) {
                    listener.onUnityBannerUnloaded(placementId);
                }
            }
        }
    }

    @Override
    public void onUnityBannerShow(String placementId) {
        if (_globalBannerProxyListener != null) {
            _globalBannerProxyListener.onUnityBannerShow(placementId);
        }

        if(_placementBannerListeners != null) {
            if(_placementBannerListeners.containsKey(placementId)) {
                for(IUnityBannerListener listener : _placementBannerListeners.get(placementId)) {
                    listener.onUnityBannerShow(placementId);
                }
            }
        }
    }

    @Override
    public void onUnityBannerClick(String placementId) {
        if (_globalBannerProxyListener != null) {
            _globalBannerProxyListener.onUnityBannerClick(placementId);
        }

        if(_placementBannerListeners != null) {
            if(_placementBannerListeners.containsKey(placementId)) {
                for(IUnityBannerListener listener : _placementBannerListeners.get(placementId)) {
                    listener.onUnityBannerClick(placementId);
                }
            }
        }
    }

    @Override
    public void onUnityBannerHide(String placementId) {
        if (_globalBannerProxyListener != null) {
            _globalBannerProxyListener.onUnityBannerHide(placementId);
        }

        if(_placementBannerListeners != null) {
            if(_placementBannerListeners.containsKey(placementId)) {
                for(IUnityBannerListener listener : _placementBannerListeners.get(placementId)) {
                    listener.onUnityBannerHide(placementId);
                }
            }
        }
    }

    @Override
    public void onUnityBannerError(String message) {
        if (_globalBannerProxyListener != null) {
            _globalBannerProxyListener.onUnityBannerError(message);
        }

        if(_placementBannerListeners != null) {
            String placementId = Utilities.extractPlacementNameFromError(message);
            if(_placementBannerListeners.containsKey(placementId)) {
                for(IUnityBannerListener listener : _placementBannerListeners.get(placementId)) {
                    listener.onUnityBannerError(placementId);
                }
            }
        }
    }
}