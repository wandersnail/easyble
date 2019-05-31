# A framework for Android BLE

[中文文档](https://github.com/wandersnail/easyble/blob/master/README.md)

## Lastest version
[![](https://jitpack.io/v/wandersnail/easyble.svg)](https://jitpack.io/#wandersnail/easyble)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.wandersnail/easyble/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.wandersnail/easyble)
[![Download](https://api.bintray.com/packages/wandersnail/android/easyble/images/download.svg) ](https://bintray.com/wandersnail/android/easyble/_latestVersion)

## Features
- Support multi-device simultaneous connection
- Support scan connected devices
- Support scan settings
- Support for custom scan filters
- Support automatic reconnection, maximum reconnection limit, direct reconnection or scan and reconnection
- Support request delay and send delay settings
- Support packet size setting, maximum transmission unit setting
- ......

## Configuration

1. Adding dependencies and configurations
```
dependencies {
	...
	implementation 'com.github.wandersnail:easyble:1.1.10'
}
```

2. If sync project failed, add following configurations and sync again
```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
		maven { url 'https://dl.bintray.com/wandersnail/android/' }
	}
}
```

## Usage(kotlin)

1. Initialize the SDK
```
Ble.instance.initialize(application)//It is recommended to initialize in the Application
BleLogger.logEnabled = true//Only control log printing, do not control log callback 
BleLogger.logCallback = object : LogCallback {
	override fun onLog(priority: Int, log: String) {
		when (priority) {
			Log.VERBOSE -> TODO()
			Log.INFO -> TODO()
			Log.DEBUG -> TODO()
			Log.WARN -> TODO()
			Log.ERROR -> TODO()
			Log.ASSERT -> TODO()
		}
	}
}
```

2. Scan bluetooth devices
```
//1. Instance listener 
private val scanListener = object : ScanListener {
	override fun onScanStart() {
		
	}

	override fun onScanStop() {
		
	}

	override fun onScanResult(device: Device) {
		
	}

	override fun onScanError(errorCode: Int, errorMsg: String) {
		when (errorCode) {
			ScanListener.ERROR_LACK_LOCATION_PERMISSION -> {
				
			}
			ScanListener.ERROR_LOCATION_SERVICE_CLOSED -> {
				
			}
		}
	}
}

//2. add listener
Ble.instance.addScanListener(scanListener)

//3. scan settings
Ble.instance.bleConfig.scanConfig.setScanPeriodMillis(30000)
		.setUseBluetoothLeScanner(true)
		.setAcceptSysConnectedDevice(true)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
	Ble.instance.bleConfig.scanConfig.setScanSettings(ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build())
}

//4. start
Ble.instance.startScan()

//stop scan
Ble.instance.stopScan()

```

3. Connect
```

val config = ConnectionConfig()
config.setDiscoverServicesDelayMillis(500)
config.setAutoReconnect(autoReconnect)
...
//create connection
Ble.instance.connect(device!!, config, object : ConnectionStateChangeListener {
	override fun onConnectionStateChanged(device: Device) {
		when (device.connectionState) {
			IConnection.STATE_SCANNING -> {

			}
			IConnection.STATE_CONNECTING -> {

			}
			IConnection.STATE_CONNECTED -> {

			}
			IConnection.STATE_DISCONNECTED -> {

			}
			IConnection.STATE_SERVICE_DISCOVERING -> {

			}
			IConnection.STATE_SERVICE_DISCOVERED -> {

			}
			IConnection.STATE_RELEASED -> {

			}
		}
	}

	override fun onConnectFailed(device: Device?, type: Int) {
		when (type) {
			IConnection.CONNECT_FAIL_TYPE_NON_CONNECTABLE -> {}
			IConnection.CONNECT_FAIL_TYPE_UNSPECIFIED_ADDRESS -> {}
			IConnection.CONNECT_FAIL_TYPE_MAXIMUM_RECONNECTION -> {}
		}
	}

	override fun onConnectTimeout(device: Device, type: Int) {
		when (type) {
			IConnection.TIMEOUT_TYPE_CANNOT_CONNECT -> {}
			IConnection.TIMEOUT_TYPE_CANNOT_DISCOVER_SERVICES -> {}
		}
	}
})

//disconnect one
Ble.instance.disconnectConnection(device)
//disconnect all
Ble.instance.disconnectAllConnections()

//release one
Ble.instance.releaseConnection(device)
//release all
Ble.instance.releaseAllConnections()
```

4. Read or write Characteristic, enable Notification, read rssi...
```
//observer mode：
//1. Instance observer
val eventObserver = object : EventObserver {
	override fun onBluetoothStateChanged(state: Int) {
	}

	override fun onLogChanged(log: String, level: Int) {
	}

	override fun onCharacteristicRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
	}

	override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
	}

	override fun onCharacteristicWrite(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
	}

	override fun onRemoteRssiRead(device: Device, tag: String, rssi: Int) {
	}

	override fun onDescriptorRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, value: ByteArray) {
	}

	override fun onNotificationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {
	}

	override fun onIndicationChanged(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, descriptorUuid: UUID, isEnabled: Boolean) {
	}

	override fun onMtuChanged(device: Device, tag: String, mtu: Int) {
	}

	override fun onPhyRead(device: Device, tag: String, txPhy: Int, rxPhy: Int) {
	}

	override fun onPhyUpdate(device: Device, tag: String, txPhy: Int, rxPhy: Int) {
	}

	override fun onConnectionStateChanged(device: Device) {
	}

	override fun onConnectFailed(device: Device?, type: Int) {
	}

	override fun onConnectTimeout(device: Device, type: Int) {
	}

	override fun onCharacteristicChanged(device: Device, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
	}
}
//2. register observer
Ble.instance.registerObserver(eventObserver)

val connection = Ble.instance.getConnection(device)
connection?.readCharacteristic(tag, service, characteristic)
connection?.enableNotification(tag, service, characteristic)
connection?.disableNotification(tag, service, characteristic)
connection?.writeCharacteristic(tag, service, characteristic, byteArrayOf(0x05, 0x06))
connection?.readRssi(tag)
...
//unregister observer
Ble.instance.unregisterObserver(eventObserver)

//callback mode：
val connection = Ble.instance.getConnection(device)
connection?.readCharacteristic(tag, service, characteristic, object : CharacteristicReadCallback {
	@InvokeThread(RunOn.MAIN)
	override fun onCharacteristicRead(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
	}

	@InvokeThread(RunOn.BACKGROUND)
	override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
	}
})
connection?.enableNotification(tag, service, characteristic, object : CharacteristicWriteCallback {
	override fun onCharacteristicWrite(device: Device, tag: String, serviceUuid: UUID, characteristicUuid: UUID, value: ByteArray) {
	}

	override fun onRequestFailed(device: Device, tag: String, requestType: Request.RequestType, failType: Int, src: ByteArray?) {
	}
})
...
```

5. Release SDK
```
Ble.instance.release()
```	

## App based on this library
[![](https://img.shields.io/badge/Download-App%20Store-yellow.svg)](http://app.mi.com/details?id=cn.zfs.bledebugger)
[![](https://img.shields.io/badge/Download-APK-blue.svg)](https://raw.githubusercontent.com/wandersnail/myapks/master/bleutility-v2.1.apk)

![image](https://github.com/wandersnail/easyble/blob/master/screenshot/demo.gif)
