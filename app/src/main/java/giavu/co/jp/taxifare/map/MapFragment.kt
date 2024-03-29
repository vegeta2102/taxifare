package giavu.co.jp.taxifare.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.google.android.gms.maps.SupportMapFragment
import giavu.co.jp.taxifare.R
import giavu.co.jp.taxifare.activity.MainViewModel
import kotlinx.android.synthetic.main.layout_fragment_map.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import timber.log.Timber

/**
 * @Author: Hoang Vu
 * @Date:   2019-10-05
 */
class MapFragment : SupportMapFragment() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    private enum class CenterPinType(val value: Int) {
        PICKUP(0),
        DROPOFF(1);

        companion object {
            fun from(value: Int): CenterPinType = values().find { it.value == value } ?: PICKUP
        }
    }

    private val viewModel: MainViewModel by sharedViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getMapAsync { googleMap ->
            viewModel.initialize(googleMap)
            viewModel.moveMyLocation()
            observeMap()
        }
    }

    private fun onChangedCameraState(state: MainViewModel.CameraState) {
        Timber.d("onChangedCameraState:$state")
        center_pin.apply {
            when (state) {
                MainViewModel.CameraState.MOVE -> {
                    pauseAnimation()
                }
                MainViewModel.CameraState.IDLE -> {
                    setAnimation("pin_location_animation.json")
                    playAnimation()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?
    ): View? {
        val origin = super.onCreateView(layoutInflater, viewGroup, bundle) as ViewGroup
        return inflater.inflate(R.layout.layout_fragment_map, origin)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                for (i in permissions.indices) {
                    if (Manifest.permission.ACCESS_FINE_LOCATION == permissions[i] &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.moveMyLocation()
                        viewModel.requestMyLocation()
                    } else {
                        requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_LOCATION_PERMISSION
                        )
                    }
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        }
    }

    private fun observeMap() {
        with(viewModel) {
            cameraState.observe(
                this@MapFragment,
                Observer { it?.also { state -> onChangedCameraState(state) } }
            )
        }
    }
}