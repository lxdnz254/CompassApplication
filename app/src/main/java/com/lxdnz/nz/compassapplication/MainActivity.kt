package com.lxdnz.nz.compassapplication

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.TextView

/**
 * Created by alex on 4/03/18.
 */

class MainActivity : Activity(), SensorEventListener, LocationListener {
    // Gravity for accelerometer data
    private val gravity = FloatArray(3)
    // magnetic data
    private val geomagnetic = FloatArray(3)
    // Rotation data
    private val rotation = FloatArray(9)
    // orientation (azimuth, pitch, roll)
    private val orientation = FloatArray(3)
    // smoothed values
    private var smoothed = FloatArray(3)
    // sensor manager
    private var sensorManager: SensorManager? = null
    // sensor gravity
    private var sensorGravity: Sensor? = null
    private var sensorMagnetic: Sensor? = null
    private var locationManager: LocationManager? = null
    private var currentLocation: Location? = null
    private var geomagneticField: GeomagneticField? = null
    private var bearing = 0.0
    private var textDirection: TextView? = null
    private var textLat: TextView? = null
    private var textLong: TextView? = null
    private var compassView: CompassView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textLat = findViewById(R.id.latitude)
        textLong = findViewById(R.id.longitude)
        textDirection = findViewById(R.id.text)
        compassView = findViewById(R.id.compass)
        // keep screen light on (wake lock light)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onStart() {
        super.onStart()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorGravity = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorMagnetic = sensorManager!!.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // listen to these sensors
        sensorManager!!.registerListener(this, sensorGravity,
                SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, sensorMagnetic,
                SensorManager.SENSOR_DELAY_NORMAL)

        // I forgot to get location manager from system service ... Ooops <img draggable="false" class="emoji" alt="😀" src="https://s.w.org/images/core/emoji/2.3/svg/1f600.svg">
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // request location data
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                LOCATION_MIN_TIME.toLong(), LOCATION_MIN_DISTANCE.toFloat(), this)

        // get last known position
        val gpsLocation = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (gpsLocation != null) {
            currentLocation = gpsLocation
        } else {
            // try with network provider
            val networkLocation = locationManager!!
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (networkLocation != null) {
                currentLocation = networkLocation
            } else {
                // Fix a position
                currentLocation = Location(FIXED)
                currentLocation!!.altitude = 1.0
                currentLocation!!.latitude = 43.296482
                currentLocation!!.longitude = 5.36978
            }

            // set current location
            onLocationChanged(currentLocation!!)
        }
    }

    override fun onStop() {
        super.onStop()
        // remove listeners
        sensorManager!!.unregisterListener(this, sensorGravity)
        sensorManager!!.unregisterListener(this, sensorMagnetic)
        locationManager!!.removeUpdates(this)
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = location
        // used to update location info on screen
        updateLocation(location)
        geomagneticField = GeomagneticField(
                currentLocation!!.latitude.toFloat(),
                currentLocation!!.longitude.toFloat(),
                currentLocation!!.altitude.toFloat(),
                System.currentTimeMillis())
    }

    private fun updateLocation(location: Location) {
        if (FIXED == location.provider) {
            textLat!!.text = NA
            textLong!!.text = NA
        }

        // better => make this creation outside method
        val dfs = DecimalFormatSymbols()
        dfs.decimalSeparator = '.'
        val formatter = DecimalFormat("#0.00", dfs)
        textLat!!.text = "Lat : " + formatter.format(location.latitude)
        textLong!!.text = "Long : " + formatter.format(location.longitude)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    override fun onSensorChanged(event: SensorEvent) {
        var accelOrMagnetic = false

        // get accelerometer data
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // we need to use a low pass filter to make data smoothed
            smoothed = LowPass().filter(event.values, gravity)
            gravity[0] = smoothed[0]
            gravity[1] = smoothed[1]
            gravity[2] = smoothed[2]
            accelOrMagnetic = true

        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            smoothed = LowPass().filter(event.values, geomagnetic)
            geomagnetic[0] = smoothed[0]
            geomagnetic[1] = smoothed[1]
            geomagnetic[2] = smoothed[2]
            accelOrMagnetic = true

        }

        // get rotation matrix to get gravity and magnetic data
        SensorManager.getRotationMatrix(rotation, null, gravity, geomagnetic)
        // get bearing to target
        SensorManager.getOrientation(rotation, orientation)
        // east degrees of true North
        bearing = orientation[0].toDouble()
        // convert from radians to degrees
        bearing = Math.toDegrees(bearing)

        // fix difference between true North and magnetical North
        if (geomagneticField != null) {
            bearing += geomagneticField!!.declination.toDouble()
        }

        // bearing must be in 0-360
        if (bearing < 0) {
            bearing += 360.0
        }

        // update compass view
        compassView!!.setBearing(bearing.toFloat())

        if (accelOrMagnetic) {
            compassView!!.postInvalidate()
        }

        updateTextDirection(bearing) // display text direction on screen
    }

    private fun updateTextDirection(bearing: Double) {
        val range = (bearing / (360f / 16f)).toInt()
        var dirTxt = ""

        if (range == 15 || range == 0)
            dirTxt = "N"
        if (range == 1 || range == 2)
            dirTxt = "NE"
        if (range == 3 || range == 4)
            dirTxt = "E"
        if (range == 5 || range == 6)
            dirTxt = "SE"
        if (range == 7 || range == 8)
            dirTxt = "S"
        if (range == 9 || range == 10)
            dirTxt = "SW"
        if (range == 11 || range == 12)
            dirTxt = "W"
        if (range == 13 || range == 14)
            dirTxt = "NW"

        textDirection!!.text = ("" + bearing.toInt() + 176.toChar() + " "
                + dirTxt) // char 176 ) = degrees ...
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            // manage fact that compass data are unreliable ...
            // toast ? display on screen ?
        }
    }

    companion object {

        val NA = "N/A"
        val FIXED = "FIXED"
        // location min time
        private val LOCATION_MIN_TIME = 30 * 1000
        // location min distance
        private val LOCATION_MIN_DISTANCE = 10
    }

}
