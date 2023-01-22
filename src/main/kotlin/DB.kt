import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DB {

    val db = Database.connect("jdbc:pgsql://rpanic.com:5432/oeuv", driver = "com.impossibl.postgres.jdbc.PGDriver",
        user = "postgres", password = "voyager1!")

    fun init(){

        transaction {
            SchemaUtils.createMissingTablesAndColumns(BotRoles)
        }

    }

}

object BotRoles : IntIdTable(){

    val user = long("user")
    val role = text("role")

}