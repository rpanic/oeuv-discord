//object DB {
//
//    val db = Database.connect("jdbc:pgsql://rpanic.com:5432/oeuv", driver = "com.impossibl.postgres.jdbc.PGDriver",
//        user = "postgres", password = "voyager1!")
//
//    fun init(){
//
//        transaction {
//            SchemaUtils.createMissingTablesAndColumns(BotRoles)
//        }
//
//    }
//
//}
//
//object BotRoles : IntIdTable(){
//
//    val user = long("user")
//    val role = text("role")
//
//}