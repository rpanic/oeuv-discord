package commands

import OEUVBot
import config
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.buttons.Button
import splitDiscordMessage

class DrehscheibeCommands(val bot: OEUVBot) : OeuvBotCommand, EditingInteraction(bot){

    val jda = bot.jda

    val adminQueue = mutableListOf<PendingDrehscheibeRequest>()

    override fun setup(): List<CommandData> {
        return listOf(
            Commands.slash(
                "drehscheibe",
                "Sendet eine Nachricht an die Drehscheibe nachdem die Admins es kontrollieren"
            )
//            .addOption(
//                OptionType.STRING,
//                "text",
//                "Die Nachricht (markdown möglich)"
//            )
        )
    }

    override fun interaction(event: SlashCommandInteractionEvent) {

        println("Got ${event.name}")

        if(event.name == "drehscheibe"){

            event.reply("Der Bot meldet sich per PN bei dir mit weiteren Infos")
                .setEphemeral(true)
                .complete()

            openEdit(event.member!!.user)

        }

    }

    override fun renderPreview(edit: AbstractOpenEdit): String {

        val content = getHeaderFormatted(edit) + "\n\n" + getContent(edit)

        return "$PREVIEW_HEADER\n\n${content}\n\n-"
    }



    override fun startEdit(channel: MessageChannel){

        val msg = channel.sendMessage("""
            Um Nachrichten an die Drehscheibe zu senden, schreibe den Text, den du senden willst bitte in diesen Chat.
            Schreibe zuerst den Betreff und dann die restliche Nachricht (der Bot formatiert die erste Zeile deiner Nachricht automatisch zur Überschrift).
            In deinem Text kannst du Discord-Markdown verwenden (siehe FAQs).
            **Erst wenn du fertig bist, drücke den grünen Button**, danach wird die Nachricht an unsere Admins geschickt, die diese dann bestätigen.
            Du kannst auch mehrere Nachrichten schicken, diese werden dann zusammengefügt. Es gibt ein Zeichenlimit von 6000 Zeichen
            """.trimIndent())
            .addActionRow(
                Button.secondary("drehscheibe-confirm", Emoji.fromUnicode("U+2705")), //Check mark
                Button.secondary("drehscheibe-abort", Emoji.fromUnicode("U+274C"))
//                Button.secondary("drehscheibe-refresh", Emoji.fromUnicode("U+1F503"))
            )
            .complete()

        this.bot.reloadAdmins()

    }

    override fun populateEdit(edit: AbstractOpenEdit){

    }

    fun removeActionRowsForMsgs(msgs: List<Message>){
        msgs.forEach {
            it.editMessageComponents().setComponents().complete()
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {

        super.onButtonInteraction(event)

        if(event.button.id?.startsWith("drehscheibe") == true){

            when(event.button.id!!){
                "drehscheibe-confirm" -> {

                    val edit = openEdits.find { it.user.idLong == event.user.idLong }!!

                    if(queueDrehscheibeMessage(getHeaderFormatted(edit), getContent(edit), event.user)){

                        editCompleted(edit)

                        event.reply("Nachricht an die Admins gesendet!").setEphemeral(true).complete()

                        //Remove action row
                        removeActionRowsForMsgs(listOf(event.message))

                    }else{
                        event.reply("Nachricht ist leer!").setEphemeral(true).complete()
                    }
                }
                "drehscheibe-abort" -> {

                    val edit = openEdits.find { it.user.idLong == event.user.idLong }

                    if(edit != null){
                        editCompleted(edit)

                        event.reply("Abgebrochen!").setEphemeral(true).complete()

                        //Remove action row
                        removeActionRowsForMsgs(listOf(event.message))
                    }else {
                        event.reply("Etwas ist schiefgelaufen").setEphemeral(true).queue()
                    }

                }
                "drehscheibe-admin-decline" -> {

                    val request = adminQueue.find { event.messageIdLong in it.adminMessages.map { x -> x.second.first() } }

                    if(request != null){
                        //Send user notification about declined status
                        val user = jda.retrieveUserById(request.user).complete()
                        val pn = user!!.openPrivateChannel().complete()
                        pn.sendMessage("""
                            Deine Drehscheibe Nachricht wurde leider von den Admins abgeleht.
                            Wenn du denkst dass das ein Irrtum war, schreibe bitte einem der Discord-Admins vom ÖUV-Server.
                        """.trimIndent()).complete()

                        adminQueue.remove(request)

                        event.reply("Done!").setEphemeral(true).queue()

                        removeActionRowsForMsgs(request.adminMessages.map {
                            jda.retrieveUserById(it.first).complete()
                                .openPrivateChannel()
                                    .complete()
                                .retrieveMessageById(it.second.first())
                                    .complete()
                        })
                    }else{
                        event.reply("Etwas ist schiefgelaufen...").setEphemeral(true).queue()
                    }

                }
                "drehscheibe-admin-accept" -> {

                    val request = adminQueue.find { event.messageIdLong in it.adminMessages.map { x -> x.second.first() } }

                    if(request != null){
                        //Send drehscheibe message
                        val user = jda.retrieveUserById(request.user).complete()

                        val formatted = this.getFormattedDrehscheibeMessage(request.header, request.content, user)

                        splitDiscordMessage(formatted).forEach {

                            bot.guild.getTextChannelById(bot.drehscheibeChannel)!!
                                .sendMessage(it)
                                .complete()
                        }

                        adminQueue.remove(request)

                        event.reply("Done!").setEphemeral(true).queue()

                        //Remove action row for all admins
                        removeActionRowsForMsgs(request.adminMessages.map {
                            jda.retrieveUserById(it.first).complete()
                                .openPrivateChannel()
                                .complete()
                                .retrieveMessageById(it.second.first())
                                .complete()
                        })
                    }else{
                        event.reply("Etwas ist schiefgelaufen...").setEphemeral(true).queue()
                    }

                }
                "drehscheibe-send" -> {

                    event.reply("Der Bot meldet sich per PN bei dir mit weiteren Infos")
                        .setEphemeral(true)
                        .complete()

                    openEdit(event.member!!.user)

                }
            }

        }

    }

    private fun queueDrehscheibeMessage(header: String, content: String, user: User) : Boolean{

        if(content.trim().isBlank()){
            return false
        }

        val messageContent = this.getFormattedDrehscheibeMessage(header, content, user)

        val msgs = bot.admins.map {
            val adminUser = jda.retrieveUserById(it).complete()!! //Should be cached though
            val channel = adminUser.openPrivateChannel().complete()

            val sendActionRowMsg = { text: String ->
                channel.sendMessage(text)
                    .addActionRow(
                        Button.secondary("drehscheibe-admin-accept", Emoji.fromUnicode("U+2705")),
                        Button.secondary("drehscheibe-admin-decline", Emoji.fromUnicode("U+274C"))
                    )
                    .complete()
            }

            val msgIds = if(messageContent.length < 1000){

                val headerMsg = sendActionRowMsg("${user.asTag} möchte folgendes in die Drehscheibe posten:\n${messageContent}")
                listOf(headerMsg.idLong)

            }else{

                val headerMsg = sendActionRowMsg("${user.asTag} möchte folgendes in die Drehscheibe posten:")

                val splits = splitDiscordMessage(messageContent)

                val ids = splits.mapIndexed { index, s ->
                    val msg = channel.sendMessage(s)
                        .complete()
                    msg.idLong
                }

                listOf(headerMsg.idLong) + ids

            }

            it to msgIds
        }

        this.adminQueue += PendingDrehscheibeRequest(
            header,
            content,
            user.idLong,
            msgs
        )

        return true
    }

    fun resendInfoChannelMessage(){

        val channel = bot.guild.getTextChannelById(config().channels.infos)!!
        channel.sendMessage("""
            **Drehscheibe**
            Die neue Drehscheibe ist sehr ähnlich wie die alte Drehscheibe strukturiert und soll auch die selben Informationen beinhalten. 
            Um eine Ausschreibung an die Drehscheibe zu schicken, kannst du entweder `/drehscheibe` in irgendeinen Kanal schreiben oder auf das :envelope: unter dieser Nachricht klicken. Danach meldet sich unser Bot mit allen weiteren Infos.
        """.trimIndent())
            .setActionRow(
                Button.secondary("drehscheibe-send", Emoji.fromUnicode("U+2709"))
            ).queue()
        //${Emoji.fromUnicode("U+2709")}

    }

    fun getFormattedDrehscheibeMessage(header: String, content: String, user: User) : String{
        return "${header}\n\n@everyone \nVon <@${user.idLong}>: \n\n" + content
    }

    companion object {
        const val FRAGMENT_SPLIT = "\n"
    }

    private fun getHeaderFormatted(edit: AbstractOpenEdit) : String{
        val header = edit.messages.firstOrNull()?.let {
            (edit.fragments[it] ?: "").split(FRAGMENT_SPLIT).first()
        } ?: ""
        return if(header.isBlank() ) "" else "**${header.replace("**", "")}**"
    }

    private fun getContent(edit: AbstractOpenEdit) : String{
        val content = edit.messages.mapIndexed { i, it ->
            val fragment = edit.fragments[it] ?: ""
            if(i == 0){
                val split = fragment.split(FRAGMENT_SPLIT)
                if(split.size > 1){
                    split.drop(1).joinToString(FRAGMENT_SPLIT)
                }else{
                    ""
                }
            }else{
                fragment
            }
        }.reduceOrNull { a, b -> "$a\n$b" } ?: ""
        return content.trim()
    }

}

data class PendingDrehscheibeRequest(
    val header: String,
    val content: String,
    val user: Long,
    val adminMessages: List<Pair<Long, List<Long>>>
)