package id.oemah.nusantara.feature.cart.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CartLineEntity::class], version = 1, exportSchema = true)
abstract class CartDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
}
