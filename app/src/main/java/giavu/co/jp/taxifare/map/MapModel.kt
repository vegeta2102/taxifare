package giavu.co.jp.taxifare.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import giavu.co.jp.taxifare.helper.ImageUtils
import timber.log.Timber

/**
 * @Author: Hoang Vu
 * @Date:   2019-10-05
 */
class MapModel(
    private val context: Context,
    private val map: GoogleMap,
    private val initialLocation: LatLng? = null,
    private val initialZoomLevel: Float? = null
) {
    companion object {
        private val TOKYO_STATION_LOCATION = LatLng(35.681167, 139.767052)
        private const val DEFAULT_ZOOM_LEVEL = 15.0f
        private const val ADJUST_ZOOM_LEVEL = 18.0f
    }

    private val _centerLocation = MutableLiveData<LatLng>()
    val centerLocation: LiveData<LatLng>
        get() = _centerLocation

    private val _zoomLevel = MutableLiveData<Float>()
    val zoomLevel: LiveData<Float>
        get() = _zoomLevel

    private val _idleCameraEvent = MutableLiveData<Unit>()
    val idleCameraEvent: LiveData<Unit>
        get() = _idleCameraEvent


    private val _startCameraEvent = MutableLiveData<Unit>()
    val startCameraEvent: LiveData<Unit>
        get() = _startCameraEvent

    fun initialize() {
        Timber.d("initialize")
        map.apply {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            isBuildingsEnabled = false
            isIndoorEnabled = false
            isTrafficEnabled = false
        }

        map.uiSettings.apply {
            isRotateGesturesEnabled = false
            isCompassEnabled = false
            isIndoorLevelPickerEnabled = false
            isMapToolbarEnabled = false
            isMyLocationButtonEnabled = false
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = false
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = true
            isScrollGesturesEnabledDuringRotateOrZoom = false
        }

        map.setOnCameraIdleListener(::onCameraIdle)
        map.setOnCameraMoveStartedListener(::onCameraStart)

        val location = initialLocation ?: TOKYO_STATION_LOCATION
        val zoom = initialZoomLevel ?: DEFAULT_ZOOM_LEVEL

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoom))

        _zoomLevel.value = map.cameraPosition.zoom
        requestMyLocation()
    }

    fun moveCamera(latLng: LatLng) {
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    fun moveCamera(bounds: LatLngBounds, padding: Int) {
        map.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                padding
            )
        )
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        map.setPadding(left, top, right, bottom)
    }

    fun addMarker(@DrawableRes resourceId: Int, location: LatLng): Marker {
        val markerOptions = MarkerOptions().apply {
            position(location)
            icon(ImageUtils.getBitmapDescriptor(context, resourceId))

        }
        return addMarker(markerOptions)
    }

    private fun addMarker(option: MarkerOptions): Marker {
        return map.addMarker(option)
    }

    private fun onCameraIdle() {
        val centerLatLon = map.projection.visibleRegion.latLngBounds.center
        Timber.d("onCameraIdle:$centerLatLon")
        _centerLocation.value = centerLatLon
        _idleCameraEvent.postValue(Unit)
    }

    private fun onCameraStart(status: Int) {
        Timber.d("onCameraStart $status")
        _startCameraEvent.postValue(Unit)
    }

    fun requestMyLocation() {
        if (hasLocationPermission()) {
            @SuppressLint("MissingPermission")
            map.isMyLocationEnabled = true
        }
    }

    private fun animateCameraInner(
        cameraUpdate: CameraUpdate,
        animationEndCallback: (() -> Unit)? = null
    ) {
        lockGestures()
        map.animateCamera(
            cameraUpdate,
            object : GoogleMap.CancelableCallback {
                override fun onFinish() {
                    unlockGestures()
                    animationEndCallback?.invoke()
                }

                override fun onCancel() {
                    unlockGestures()
                    animationEndCallback?.invoke()
                }
            }
        )
    }

    private fun lockGestures() {
        Timber.d("lockGestures")
        map.uiSettings.apply {
            isZoomGesturesEnabled = false
            isScrollGesturesEnabled = false
        }
    }

    private fun unlockGestures() {
        Timber.d("unlockGestures")
        map.uiSettings.apply {
            isZoomGesturesEnabled = true
            isScrollGesturesEnabled = true
        }
    }

    fun animateCamera(latLng: LatLng, animationEndCallback: (() -> Unit)? = null) {
        animateCameraInner(
            cameraUpdate = CameraUpdateFactory.newLatLng(latLng),
            animationEndCallback = animationEndCallback
        )
    }

    fun animateCamera(bounds: LatLngBounds, padding: Int, animationEndCallback: (() -> Unit)? = null) {
        animateCameraInner(
            cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                bounds,
                padding
            ),
            animationEndCallback = animationEndCallback
        )
    }

    fun animateCameraWithAdjustZoom(latLng: LatLng, animationEndCallback: (() -> Unit)? = null) {
        animateCameraInner(
            cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, ADJUST_ZOOM_LEVEL),
            animationEndCallback = animationEndCallback
        )
    }

    fun animateCameraWithAdjustZoom(animationEndCallback: () -> Unit) {
        animateCameraInner(
            cameraUpdate = CameraUpdateFactory.zoomTo(ADJUST_ZOOM_LEVEL),
            animationEndCallback = animationEndCallback
        )
    }

    fun animateCameraWithDefaultZoom(latLng: LatLng, animationEndCallback: () -> Unit) {
        animateCameraInner(
            cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL),
            animationEndCallback = animationEndCallback
        )
    }

    fun animateCameraWithDefaultZoom(animationEndCallback: () -> Unit) {
        animateCameraInner(
            cameraUpdate = CameraUpdateFactory.zoomTo(DEFAULT_ZOOM_LEVEL),
            animationEndCallback = animationEndCallback
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}