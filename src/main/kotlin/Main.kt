import commands.CommandListener
import commands.DrehscheibeCommands
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy

fun main(){

    val token = config().bot.token
    val bot = OEUVBot(token)

}

class OEUVBot(token: String){

    val jda: JDA
    val guild: Guild

    val admins = mutableListOf<Long>()

    val drehscheibeChannel = config().channels.drehscheibe

    init{

        val builder = JDABuilder.createDefault(token)

        val playing = config().bot.playing
        builder.setActivity(Activity.playing(playing));

        builder
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .enableIntents(GatewayIntent.DIRECT_MESSAGES)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
//            .setChunkingFilter(ChunkingFilter.ALL)

        jda = builder.build();
        jda.awaitReady()

        guild = jda.guilds.first()

        initFromDB()

        val adminRole = guild.getRolesByName("Discord Admin", true).first()
        guild.loadMembers().onSuccess {
            println(it)
            val adminUsers = guild.getMembersWithRoles(adminRole)
            admins += adminUsers.map { it.idLong }
        }

        initCommands()

    }

    fun initCommands(){

        val providers = listOf(
            DrehscheibeCommands(this)
        )

        val commands = providers.map { it.setup() }.flatten()
        this.jda.updateCommands().addCommands(commands)
            .complete()

        jda.addEventListener(CommandListener(jda, providers))

//        (providers[0] as DrehscheibeCommands).resendInfoChannelMessage()

    }

    fun initFromDB(){

    }

}