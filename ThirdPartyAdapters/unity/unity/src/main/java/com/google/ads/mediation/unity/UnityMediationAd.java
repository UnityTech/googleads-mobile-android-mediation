package com.google.ads.mediation.unity;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;

import com.unity3d.ads.UnityAds;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.core.log.DeviceLog;

import org.json.JSONException;
import org.json.JSONObject;

public class UnityMediationAd implements IUnityAdsExtendedListener {
    private static long _loadId = 1;

    private enum AdState { NOT_LOADED, LOADING, LOADED, SHOWING, FINISHED, FAILED, INVALIDATED };

    private String _placementId;
    private String _type;
    private IUnityMediationListener _listener;
    private AdState _state;
    private long _loadTimestamp;
    private long _timeout = 30000; // 30 seconds

    public UnityMediationAd(String placementId, String type) {
        _placementId = placementId;
        _type = type;
        _state = AdState.NOT_LOADED;
    }

    public void setListener(IUnityMediationListener listener) {
        _listener = listener;
    }

    public void load(Context context) {
        DeviceLog.debug("Unity Ads mediation loading placement " + _placementId);

        _loadTimestamp = SystemClock.elapsedRealtime();

        sendLoadMetadata(context);

        if(_state == AdState.NOT_LOADED) {
            UnityAds.PlacementState placementState = UnityAds.getPlacementState(_placementId);
            if(placementState == UnityAds.PlacementState.NO_FILL || placementState == UnityAds.PlacementState.DISABLED) {
                sendLoadFailed();
                return;
            }

            UnityListenerMultiplexer.addPlacementListener(_placementId, this);

            if(placementState == UnityAds.PlacementState.READY) {
                sendLoaded();
            } else {
                _state = AdState.LOADING;
            }
        } else {
            sendLoadFailed();
        }
    }

    public boolean isLoaded() {
        return _state == AdState.LOADED;
    }

    public void show(Activity activity) {
        if(_state == AdState.LOADED) {
            _state = AdState.SHOWING;
            UnityAds.show(activity, _placementId);
        } else {
            if(_listener != null) {
                _listener.onUnityAdsFinish(_placementId, UnityAds.FinishState.ERROR);
            }
        }
    }

    public void destroy() {
        _state = AdState.INVALIDATED;
        UnityListenerMultiplexer.removePlacementListener(_placementId, this);
    }

    @Override
    public void onUnityAdsClick(String placementId) {
        if(_state == AdState.SHOWING && _listener != null) {
            _listener.onUnityAdsClick(_placementId);
        }
    }

    @Override
    public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {
        if(_state == AdState.LOADING && SystemClock.elapsedRealtime() - _loadTimestamp < _timeout) {
            if(newState == UnityAds.PlacementState.READY) {
                sendLoaded();
            } else if(newState == UnityAds.PlacementState.NO_FILL || newState == UnityAds.PlacementState.DISABLED) {
                sendLoadFailed();
            }
        } else if(_state == AdState.LOADED && oldState == UnityAds.PlacementState.READY && newState != UnityAds.PlacementState.READY) {
            _state = AdState.INVALIDATED;
        }
    }

    @Override
    public void onUnityAdsReady(String placementId) {
        if(_state == AdState.LOADING && SystemClock.elapsedRealtime() - _loadTimestamp < _timeout) {
            sendLoaded();
        }
    }

    @Override
    public void onUnityAdsStart(String placementId) {
        if(_state == AdState.SHOWING && _listener != null) {
            _listener.onUnityAdsStart(_placementId);
        }
    }

    @Override
    public void onUnityAdsFinish(String placementId, UnityAds.FinishState result) {
        if(_state == AdState.SHOWING && _listener != null) {
            _listener.onUnityAdsFinish(_placementId, result);
            _state = AdState.FINISHED;
        }
    }

    @Override
    public void onUnityAdsError(UnityAds.UnityAdsError error, String message) {

    }

    private void sendLoadFailed() {
        DeviceLog.debug("Unity Ads mediation placement " + _placementId + " loading failed");

        _state = AdState.FAILED;

        if(_listener != null) {
            _listener.onUnityAdsLoadFailed(_placementId);
        }
    }

    private void sendLoaded() {
        DeviceLog.debug("Unity Ads mediation placement " + _placementId + " successfully loaded");

        _state = AdState.LOADED;

        if(_listener != null) {
            _listener.onUnityAdsLoaded(_placementId);
        }
    }

    private void sendLoadMetadata(Context context) {
        JSONObject data = new JSONObject();
        try {
            data.put("placementId", _placementId);
            data.put("type", _type);
        } catch(JSONException e) {
            DeviceLog.error("Unity Ads failed to construct mediation metadata object");
            return;
        }

        MetaData metadata = new MetaData(context);
        metadata.setCategory("load");
        metadata.set(String.valueOf(_loadId++), data.toString());
        metadata.commit();
    }
}