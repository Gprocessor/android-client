package com.mifos.objects.templates.clients

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.google.gson.annotations.SerializedName
import com.mifos.api.local.MifosBaseModel
import com.mifos.api.local.MifosDatabase
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.ModelContainer
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import kotlinx.parcelize.Parcelize

/*
 * This project is licensed under the open source MPL V2.
 * See https://github.com/openMF/android-client/blob/master/LICENSE.md
 */ /**
 * Created by rajan on 13/3/16.
 */
@Parcelize
@Table(database = MifosDatabase::class, name = "ClientTemplateSavingProductsOptions")
@ModelContainer
data class SavingProductOptions(
    @PrimaryKey
    var id: Int = 0,
    val name: String = "",
    val withdrawalFeeForTransfers: Boolean = false,
    val allowOverdraft: Boolean = false
) : MifosBaseModel(), Parcelable {
    override fun toString(): String {
        return "SavingProductOptions{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", withdrawalFeeForTransfers=" + withdrawalFeeForTransfers +
                ", allowOverdraft=" + allowOverdraft +
                '}'
    }
}
