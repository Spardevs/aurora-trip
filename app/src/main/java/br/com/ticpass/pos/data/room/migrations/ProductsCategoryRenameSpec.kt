package br.com.ticpass.pos.data.room.migrations

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec

@RenameColumn(
    tableName      = "products",
    fromColumnName = "category",
    toColumnName   = "categoryId"
)
class ProductsCategoryRenameSpec : AutoMigrationSpec
