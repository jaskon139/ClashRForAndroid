package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.common.serialization.MergedParcels
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroupWrapper(val list: List<ProxyGroup>) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        MergedParcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProxyGroupWrapper> {
        override fun createFromParcel(parcel: Parcel): ProxyGroupWrapper {
            return MergedParcels.load(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<ProxyGroupWrapper?> {
            return arrayOfNulls(size)
        }
    }
}