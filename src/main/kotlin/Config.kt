import com.sksamuel.hoplite.ConfigLoader
import java.io.File
import kotlin.time.Duration

data class Config(

    val channels: ChannelConfig,
    val bot: BotConfig,
    val email: EmailConfig,
//    val admins: List<Long>,

) {

    data class EmailConfig(
        val channel: Long,
        val host: String,
        val username: String,
        val password: String,
        val refreshInterval: Duration,
        val whitelist: List<String>,
    )

    data class ChannelConfig(
        val drehscheibe: Long,
        val oefsvAnnouncements: Long,
        val infos: Long
    )

    data class BotConfig(
        val playing: String = "Ultimate Frisbee",
        val token: String
    )

    companion object{

        val config by lazy {
            loadFromFile("config.yml")
        }

        fun loadFromFile(name: String): Config {
            val c =
                ConfigLoader().loadConfigOrThrow<Config>(File(System.getProperty("user.dir") + File.separator + name))
            return c
        }
    }

}

fun config() : Config{
    return Config.config
}