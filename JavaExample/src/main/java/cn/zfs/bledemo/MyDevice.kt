package cn.zfs.bledemo

import android.bluetooth.BluetoothDevice
import android.os.Parcel
import android.os.Parcelable
import com.snail.easyble.core.Device

/**
 * date: 2018/4/16 10:28
 * author: zengfansheng
 */
class MyDevice(originalDevice: BluetoothDevice) : Device(originalDevice), Parcelable {
    /**
     * 产品类别
     */
    var category: Int = 0
        internal set

    /** 产品系列  */
    var series = ""
        internal set

    /** 产品型号  */
    var model = ""
        internal set
    
    constructor(source: Parcel) : this(source.readParcelable<BluetoothDevice>(BluetoothDevice::class.java.classLoader)!!) {
        super.readFromParcel(source)
        category = source.readInt()
        series = source.readString()!!
        model = source.readString()!!
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        super.writeToParcel(dest, flags)
        writeInt(category)
        writeString(series)
        writeString(model)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<MyDevice> = object : Parcelable.Creator<MyDevice> {
            override fun createFromParcel(source: Parcel): MyDevice = MyDevice(source)
            override fun newArray(size: Int): Array<MyDevice?> = arrayOfNulls(size)
        }
    }
}
