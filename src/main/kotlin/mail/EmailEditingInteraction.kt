package mail

import OEUVBot
import commands.AbstractOpenEdit
import commands.EditingInteraction
import desi.juan.email.api.Email
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.utils.FileUpload

class EmailEditingInteraction(val bot: OEUVBot, val email: Email, val file: FileUpload) : EditingInteraction(bot) {

    val emailInfo = """
            **Neue email: **
            
            **${email.subject}**
            
            Von ${email.from.firstOrNull() ?: "--"}
            To: ${email.to.firstOrNull() ?: "--"}
            """.trimIndent().trim()

    override fun renderPreview(edit: AbstractOpenEdit): String {

        val content = edit.messages.joinToString("\n") { edit.fragments[it] ?: "" }

        val text = renderEmailForDiscord(email, content)
        return text

    }

    override fun populateEdit(edit: AbstractOpenEdit) {


    }

    val reactionMessages = mutableListOf<Pair<MessageChannel, List<Long>>>()

    var closed = false

    override fun startEdit(channel: MessageChannel) {

        val msgs = mutableListOf<Long>()
        msgs +=
            channel.sendMessage(emailInfo).complete()
                .idLong

        msgs +=
            channel
            .sendMessage("Bitte checke die vorformattierte Nachricht auf Fehler und poste sie dann wieder hier in den Chat (gleich wie bei der Drehscheibe)")
            .addActionRow(
                Button.secondary("email-admin-accept", Emoji.fromUnicode("U+2705")),
                Button.secondary("email-admin-decline", Emoji.fromUnicode("U+274C"))
            )
            .complete()
            .idLong


        msgs +=
            channel.sendFiles(file).complete()
                .idLong

        reactionMessages += channel to msgs

    }

    fun getEditForUser(user: User): AbstractOpenEdit? {

        return openEdits.find { it.user.idLong == user.idLong }

    }

    fun close(){

        reactionMessages.forEach {
            it.second.forEach { msg ->
                it.first.deleteMessageById(msg).queue()
            }
        }

        closed = true

    }

}

fun renderEmailForDiscord(email: Email, content: String): String {

    return "**${email.subject}**\n" +
            "\n" +
            "@everyone\n" +
            "Von ${email.from.firstOrNull() ?: "--"}\n" +
            "\n" +
            content

//    """
//        **${email.subject}**
//
//        @everyone
//        Von ${email.from.firstOrNull() ?: "--"}
//
//        $content
//        """.trimIndent().trim()

}