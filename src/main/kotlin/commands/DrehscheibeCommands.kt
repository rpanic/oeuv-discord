package commands

import OEUVBot
import config
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
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

class DrehscheibeCommands(val bot: OEUVBot) : OeuvBotCommand, ListenerAdapter(){

    val jda = bot.jda

    val openEdits = mutableListOf<OpenEdit>()

    val adminQueue = mutableListOf<PendingDrehscheibeRequest>()

    val PREVIEW_HEADER = "Vorschau:"

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

            openEdit(event.member!!)

        }

    }

    private fun openEdit(member: Member){

        val user = member.user

        val channel = user.openPrivateChannel().complete()

        val msg = channel.sendMessage("""
            Um Nachrichten an die Drehscheibe zu senden, schreibe den Text, den du send willst bitte in diesen Chat.
            **Erst wenn du fertig bist, drücke den grünen Button**, danach wird die Nachricht an unsere Admins geschickt, die diese dann bestätigen.
            Du kannst auch mehrere Nachrichten schicken, diese werden dann zusammengefügt.
            """.trimIndent())
            .addActionRow(
                Button.secondary("drehscheibe-confirm", Emoji.fromUnicode("U+2705")) //Check mark
//                Button.secondary("drehscheibe-refresh", Emoji.fromUnicode("U+1F503"))
            )
            .complete()

        val preview = channel.sendMessage(PREVIEW_HEADER)
            .complete()

        openEdits += OpenEdit(
            user,
            msg.idLong,
            preview.idLong,
            channel.idLong,
            mutableListOf(),
            mutableMapOf()
        )

    }

    private fun getOpenEditForUserMessage(
        channel: MessageChannelUnion,
        authorId: Long
    ): OpenEdit? {
        if(channel.type == ChannelType.PRIVATE){
            val edit = openEdits.find { it.channel == channel.idLong }
            if(edit != null && edit.user.idLong == authorId){
                return edit
            }
        }
        return null
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        println(event.toString())
        println(event.channel.type)

        val edit = getOpenEditForUserMessage(event.channel, event.author.idLong)
        if(edit != null){
            updateMessageContent(edit, event.message)
        }

    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        println(event.toString())
        super.onMessageUpdate(event)

        val edit = getOpenEditForUserMessage(event.channel, event.author.idLong)
        if(edit != null){
            updateMessageContent(edit, event.message)
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        println(event.toString())
        super.onMessageDelete(event)

        if(event.channel.type == ChannelType.PRIVATE){
            val edit = getOpenEditForUserMessage(event.channel, event.channel.asPrivateChannel().user?.idLong ?: 0)
            if(edit != null){
                edit.messages.remove(event.messageIdLong)
                edit.fragments.remove(event.messageIdLong)

                updatePreview(edit, event.channel.asPrivateChannel())
            }
        }
    }

    private fun updateMessageContent(edit: OpenEdit, msg: Message){

        val content = msg.contentRaw
        if(!edit.messages.contains(msg.idLong)){
            edit.messages.add(msg.idLong)
        }
        edit.fragments[msg.idLong] = content

        updatePreview(edit, msg.channel.asPrivateChannel())

    }

    private fun updatePreview(edit: OpenEdit, channel: PrivateChannel) {

        val content = edit.getContent()

        val msg = channel.retrieveMessageById(edit.previewMessage).complete()

        msg.editMessage("$PREVIEW_HEADER\n\n $content\n\n-").complete()

    }

    fun removeActionRowsForMsgs(msgs: List<Message>){
        msgs.forEach {
            it.editMessageComponents().setComponents().complete()
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {

        if(event.button.id?.startsWith("drehscheibe") == true){

            when(event.button.id!!){
                "drehscheibe-confirm" -> {

                    val edit = openEdits.find { it.user.idLong == event.user.idLong }!!

                    if(queueDrehscheibeMessage(edit.getContent(), event.user)){

                        this.openEdits.remove(edit)

                        event.reply("Nachricht an die Admins gesendet!").setEphemeral(true).complete()

                        //Remove action row
                        removeActionRowsForMsgs(listOf(event.message))

                    }else{
                        event.reply("Nachricht ist leer!").setEphemeral(true).complete()
                    }
                }
                "drehscheibe-admin-decline" -> {

                    val request = adminQueue.find { event.messageIdLong in it.adminMessages.map { x -> x.second } }

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
                                .retrieveMessageById(it.second)
                                    .complete()
                        })
                    }else{
                        event.reply("Etwas ist schiefgelaufen...").setEphemeral(true).queue()
                    }

                }
                "drehscheibe-admin-accept" -> {

                    val request = adminQueue.find { event.messageIdLong in it.adminMessages.map { x -> x.second } }

                    if(request != null){
                        //Send drehscheibe message
                        val user = jda.retrieveUserById(request.user).complete()
                        bot.guild.getTextChannelById(bot.drehscheibeChannel)!!
                            .sendMessage("@everyone \n<@${user.idLong}>: \n\n" + request.content)
                            .complete()

                        adminQueue.remove(request)

                        event.reply("Done!").setEphemeral(true).queue()

                        //Remove action row for all admins
                        removeActionRowsForMsgs(request.adminMessages.map {
                            jda.retrieveUserById(it.first).complete()
                                .openPrivateChannel()
                                .complete()
                                .retrieveMessageById(it.second)
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

                    openEdit(event.member!!)

                }
            }

        }

    }

    private fun queueDrehscheibeMessage(content: String, user: User) : Boolean{

        if(content.trim().isBlank()){
            return false
        }

        val msgs = bot.admins.map {
            val adminUser = jda.retrieveUserById(it).complete()!! //Should be cached though
            val channel = adminUser.openPrivateChannel().complete()
            val msg = channel.sendMessage("""
                ${user.asTag} möchte folgendes in die Drehscheibe posten:
                ${content}
            """.trimIndent())
                .addActionRow(
                    Button.secondary("drehscheibe-admin-accept", Emoji.fromUnicode("U+2705")),
                    Button.secondary("drehscheibe-admin-decline", Emoji.fromUnicode("U+274C"))
                )
                .complete()
            it to msg.idLong
        }

        this.adminQueue += PendingDrehscheibeRequest(
            content,
            user.idLong,
            msgs
        )

        return true
    }

    fun resendInfoChannelMessage(){

        val channel = bot.guild.getTextChannelById(config().channels.infos)!!
        channel.sendMessage("""
            Willst du etwas in die Drehscheibe posten?
            Klicke auf das :envelope:
        """.trimIndent())
            .setActionRow(
                Button.secondary("drehscheibe-send", Emoji.fromUnicode("U+2709"))
            ).queue()
        //${Emoji.fromUnicode("U+2709")}

    }

}

data class PendingDrehscheibeRequest(
    val content: String,
    val user: Long,
    val adminMessages: List<Pair<Long, Long>>
)

data class OpenEdit(
    val user: User,
    val editMessage: Long,
    val previewMessage: Long,
    val channel: Long,
    val messages: MutableList<Long>,
    val fragments: MutableMap<Long, String>
){
    fun getContent() : String{
        return this.messages.map {
            this.fragments[it] ?: ""
        }.reduceOrNull { a, b -> "$a\n$b" } ?: ""
    }
}