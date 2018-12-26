package com.snail.easyble.event

/**
 * Make the subscription method easier to write, let the owner class implement the interface and add @Subscribe to the method
 *
 * date: 2018/12/26 11:36
 * author: zengfansheng
 */

interface IBluetoothStateChangedEvent {    
    fun onBluetoothStateChanged(event: Events.BluetoothStateChanged)
}

interface ICharacteristicChangedEvent {
    fun onCharacteristicChanged(event: Events.CharacteristicChanged)
}

interface ICharacteristicReadEvent {
    fun onCharacteristicRead(event: Events.CharacteristicRead)
}

interface ICharacteristicWriteEvent {
    fun onCharacteristicWrite(event: Events.CharacteristicWrite)
}

interface IConnectFailedEvent {
    fun onConnectFailed(event: Events.ConnectFailed)
}

interface IConnectionStateChangedEvent {
    fun onConnectionStateChanged(event: Events.ConnectionStateChanged)
}

interface IConnectTimeoutEvent {
    fun onConnectTimeout(event: Events.ConnectTimeout)
}

interface IDescriptorReadEvent {
    fun onDescriptorRead(event: Events.DescriptorRead)
}

interface IIndicationChangedEvent {
    fun onIndicationChanged(event: Events.IndicationChanged)
}

interface ILogEvent {
    fun onLogChanged(event: Events.LogChanged)
}

interface IMtuChangedEvent {
    fun onMtuChanged(event: Events.MtuChanged)
}

interface INotificationChangedEvent {
    fun onNotificationChanged(event: Events.NotificationChanged)
}

interface IReadPhyEvent {
    fun onPhyRead(event: Events.PhyRead)
}

interface IRemoteRssiReadEvent {
    fun onRemoteRssiRead(event: Events.RemoteRssiRead)
}

interface IRequestFailedEvent {
    fun onRequestFailed(event: Events.RequestFailed)
}