package mail

import OEUVBot
import config
import db.BotDB
import db.EmailStatus
import desi.juan.email.api.Email
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeTraversor
import org.jsoup.select.NodeVisitor
import splitDiscordMessage
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

class Forwarder(val whitelistedFrom: List<String>, val client: IMAPReceiver, val bot: OEUVBot) : ListenerAdapter() {

    var latestMailId: Long = 0L

    var emailEditingInteraction: EmailEditingInteraction? = null

    val emailQueue = mutableListOf<Email>()

    var lastEmailCheck = 0L

    init {
        val latest = BotDB.emailStatus.filter { it.status == 3 }.maxByOrNull { it.received }
        if(latest != null){
            latestMailId = latest.id

            //Add saved queued emails again
            val email = client.getEmails()
            val queued = BotDB.emailStatus.filter { it.received > latest.received && it.status == 2 }.toList().map { it.id }
            if(queued.isNotEmpty()){
                emailQueue += email.filter { it.id in queued }
                openNextEmailEdit()
            }
        }

        val timeout = config().email.refreshInterval.inWholeMilliseconds

        bot.jda.addEventListener(object : ListenerAdapter(){
            override fun onGenericEvent(event: GenericEvent) {
                super.onGenericEvent(event)

                if(lastEmailCheck + timeout > System.currentTimeMillis()){
                    println("Checking for new emails (${System.currentTimeMillis()}), method 2")

                    checkNewMails()
                    lastEmailCheck = System.currentTimeMillis()
                }
            }
        })
    }

    fun startJob() {
        Thread {
            val timeout = config().email.refreshInterval

            while (true) {
                println(" Checking for new emails (${System.currentTimeMillis()})")

                checkNewMails()

                lastEmailCheck = System.currentTimeMillis()

                Thread.sleep(timeout.inWholeMilliseconds)
            }
        }.start()
    }

    fun checkNewMails() {

        val emails = client.getEmails()

        val latest = emails.maxByOrNull { it.receivedDate.orElse(LocalDateTime.MIN) }
        if (latest != null && latest.id != latestMailId) {
            if (latestMailId == 0L) {
                //Don't send on boot up
                latestMailId = latest.id
                println("Resuming at current id $latestMailId because no previous state was found")
            } else {
                //Send

                val previousLatest = emails.find { it.id == latestMailId }
                val newMails = if (previousLatest != null && previousLatest.receivedDate.isPresent) {

                    emails.filter {
                        val ts = it.receivedDate.orElse(LocalDateTime.MIN)
                        ts.isAfter(previousLatest.receivedDate.get())
                    }

                } else {
                    listOf(latest)
                }

                newMails.forEach {
                    newEmail(it)
                }

                println("Added ${newMails.size} emails to queue")

                latestMailId = latest.id

            }
            println("Set latest to ${latest.id}")
        }

    }

    val emailChannel = config().email.channel

    fun newEmail(email: Email) {

        if(!BotDB.emailStatus.any { it.id == email.id }){

            BotDB.emailStatus.add(
                EmailStatus(
                    email.id,
                    email.receivedDate.orElse(null)?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: 0L,
                    1 //queued
                )
            )

            emailQueue += email
            openNextEmailEdit()

        }


    }

    /*
    Status:
    1 = queued
    2 = editing
    3 = completed
     */
    fun updateEmailStatus(email: Email, status: Int) {

        BotDB.emailStatus.find { it.id == email.id }?.status = status
        BotDB.emailStatus.changed()

    }

    private fun openNextEmailEdit() {

        if (emailEditingInteraction == null && emailQueue.isNotEmpty()) {

            val email = emailQueue.removeAt(0)

            val emailRender = renderEmail(email)

            val fileUpload = FileUpload.fromData(emailRender.byteInputStream(), "email.txt")

            emailEditingInteraction = EmailEditingInteraction(bot, email, fileUpload)
            bot.jda.addEventListener(emailEditingInteraction)

            bot.admins.forEach {
                val user = bot.jda.getUserById(it)

                if (user != null) {

                    emailEditingInteraction!!.openEdit(user)

                }
            }

            updateEmailStatus(email, 2)
        }
    }

    private fun closeCurrentEdit(){

        updateEmailStatus(emailEditingInteraction!!.email, 3)

        emailEditingInteraction!!.close()
        bot.jda.removeEventListener(emailEditingInteraction)
        emailEditingInteraction = null

    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        super.onButtonInteraction(event)

        if(event.button.id?.startsWith("email") == true && emailEditingInteraction != null){

            val edit = emailEditingInteraction!!.getEditForUser(event.user)

            if(edit != null) {

                when (event.button.id!!) {
                    "email-admin-accept" -> {
                        val content = emailEditingInteraction!!.renderPreview(edit) //Renders it right actually
                        var channel = getChannelForEmail(emailEditingInteraction!!.email.from.firstOrNull() ?: "")

                        val contents = splitDiscordMessage(content)
                        contents.forEach {
                            channel.sendMessage(it).complete()
                        }

                        closeCurrentEdit()
                        openNextEmailEdit()
                        event.reply("Gesendet!").setEphemeral(true).queue()
                    }
                    "email-admin-decline" -> {

                        closeCurrentEdit()
                        openNextEmailEdit()

                        event.reply("Abgelehnt!").setEphemeral(true).queue()
                    }
                }
            }
        }
    }

    fun getChannelForEmail(email: String) : MessageChannel {

        println("Email from $email")

        val id = if(email.endsWith("@frisbeeverband.at")){
            config().channels.oefsvAnnouncements
        }else{
            emailChannel
        }
        return bot.jda.getNewsChannelById(id) ?: bot.jda.getTextChannelById(id)!!

    }

    fun renderEmail(email: Email) : String{

        val js = Jsoup.parse(email.body.content)

        val accum = StringUtil.borrowBuilder()

        NodeTraversor.traverse(object : NodeVisitor {

            override fun head(node: Node, depth: Int) {

                if (node is TextNode) {

                    accum.append(node.wholeText)

                } else if (node is Element) {

                    val name = node.tag().normalName()
                    if (name in listOf("h1", "h2", "h3", "h4")) accum.append("**")

                    when {
                        name == "a" -> {
                            val link = node.attr("href")
                            if(link.isNotBlank()){
                                accum.append("<$link>\n")
                            }
                        }
                        name in listOf("br", "p") -> {
                            accum.append("\n")
                        }

                    }
                }
            }

            override fun tail(node: Node, depth: Int) {
                if(node is Element){
                    if (node.tag().normalName() in listOf("h1", "h2", "h3", "h4")) accum.append("**\n")
                }
                super.tail(node, depth)
            }

        }, js.root())

        val text = StringUtil.releaseBuilder(accum)
            .replace("\u00a0","")

        return text

    }

}