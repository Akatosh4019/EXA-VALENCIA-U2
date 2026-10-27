package pe.upeu.agrosierra
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.upeu.agrosierra.data.local.AgroDatabase
internal actual fun baseEnMemoria(): AgroDatabase {
    val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AgroDatabase.Schema.create(driver)
    return AgroDatabase(driver)
}
