import commands.CommandListener
import commands.DrehscheibeCommands
import db.BotDB
import db.EmailStatus
import desi.juan.email.api.Email
import mail.Forwarder
import mail.IMAPReceiver
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
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

        BotDB.init()

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

        reloadAdmins()

        initCommands()

        Thread{
            Thread.sleep(3000L)

            val receiver = IMAPReceiver(config().email)
            val forwarder = Forwarder(config().email.whitelist, receiver, this)

            jda.addEventListener(forwarder)

//            val email = receiver.getEmails().last()
//            println()
//            forwarder.newEmail(email)

            forwarder.startJob()

        }.start()

    }

    fun reloadAdmins(callback: (Boolean) -> Unit = {}){
        val adminRole = guild.getRolesByName("Discord Admin", true).first()
        guild.loadMembers().onSuccess {

            val adminUsers = guild.getMembersWithRoles(adminRole)
            admins.clear()
            admins += adminUsers.map { it.idLong }

            println("Loaded Admins ${adminUsers.map { it.effectiveName }}")
            callback(true)

        }.onError { callback(false) }

    }

    fun initCommands(){

        val providers = listOf(
            DrehscheibeCommands(this),
        )

//        val commands = providers.map { it.setup() }.flatten()
//        this.jda.updateCommands().addCommands(commands)
//            .complete()

        jda.addEventListener(CommandListener(jda, providers))

//        (providers[0] as DrehscheibeCommands).resendInfoChannelMessage()

    }

    fun initFromDB(){

    }

}

fun splitDiscordMessage(text: String) : List<String>{

    val agg = mutableListOf<MutableList<String>>(mutableListOf())
    var c = 0

    val limit = 2000

    text.split("\n").forEach {

        if(it.length > limit) return@forEach //Discard line in case one line is so big

        var currSplit = agg.last()

        if(c + it.length + currSplit.size + 1 > limit){ //every entry is a \n, counts as char
            agg.add(mutableListOf())
            c = 0
        }
        agg.last().add(it)
        c += it.length
    }

    return agg.map {
        it.joinToString("\n")
    }
}

//fun TextChannel.sendLargeMessage(text: String) : List<Message>{
//
//    val agg = mutableListOf<MutableList<String>>(mutableListOf())
//    var c = 0
//
//    val limit = 2000
//
//    text.split("\n").forEach {
//
//        if(it.length > limit) return@forEach //Discard line in case one line is so big
//
//        if(c + it.length > limit){
//            agg.add(mutableListOf())
//            c = 0
//        }
//        agg.last().add(it)
//        c += it.length
//    }
//
//    return agg.map {
//        val msg = it.joinToString("\n")
//        this.sendMessage(msg).complete()
//    }
//
//}