package commands

import OEUVBot
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileProxy
import splitDiscordMessage

abstract class EditingInteraction(private val bot: OEUVBot, val numPreviewMessages: Int = 3) : ListenerAdapter() {

    val openEdits = mutableListOf<AbstractOpenEdit>()

    val PREVIEW_HEADER = "Vorschau:"

    abstract fun renderPreview(edit: AbstractOpenEdit) : String

    abstract fun populateEdit(edit: AbstractOpenEdit)

    fun updatePreview(edit: AbstractOpenEdit){

        val content = renderPreview(edit)

        val channel = edit.user.openPrivateChannel().complete()!!

        val msgIds = edit.previewMessages

        val split = splitDiscordMessage("$PREVIEW_HEADER\n\n$content")

        split.take(numPreviewMessages).forEachIndexed { i, s ->
            val msg = channel.retrieveMessageById(msgIds[i]).complete()
            msg.editMessage(s).queue()
        }

    }

    fun openEdit(user: User){

        val channel = user.openPrivateChannel().complete()

        startEdit(channel)

        val preview = channel.sendMessage("$PREVIEW_HEADER\n")
            .complete()

        val previewIds = mutableListOf<Long>(preview.idLong)

        for(i in 1 until numPreviewMessages){
            previewIds += channel.sendMessage(".")
                .complete().idLong
        }

        val openEdit = AbstractOpenEdit(
            user,
//            msg.idLong,
            previewIds,
            channel.idLong,
            mutableListOf(),
            mutableMapOf()
        )

        openEdits += openEdit

        populateEdit(openEdit)

        updatePreview(openEdit)

    }

    abstract fun startEdit(channel: MessageChannel)

    fun editCompleted(edit: AbstractOpenEdit){

        require(this.openEdits.remove(edit))

    }

//    abstract fun editingFinished(t: AbstractOpenEdit, status: Boolean)

    private fun getOpenEditForUserMessage(
        channel: MessageChannelUnion,
        authorId: Long
    ): AbstractOpenEdit? {
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

        if(event.author.isBot){
            return
        }

        val edit = getOpenEditForUserMessage(event.channel, event.author.idLong)
        if(edit != null){
            updateMessageContent(edit, event.message)
            updatePreview(edit)
        }

    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        println(event.toString())
        super.onMessageUpdate(event)

        val edit = getOpenEditForUserMessage(event.channel, event.author.idLong)
        if(edit != null){
            updateMessageContent(edit, event.message)
            updatePreview(edit)
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

                updatePreview(edit)
            }
        }
    }

    private fun updateMessageContent(edit: AbstractOpenEdit, msg: Message){

        var content = msg.contentRaw

        msg.attachments.forEach {
//            val inputstream = FileProxy(it.proxyUrl).download().join()
            val inputstream = it.retrieveInputStream().join()
            val s = String(inputstream.readBytes())
            println("Got file: $s")

            content += s
        }

        if(!edit.messages.contains(msg.idLong)){
            edit.messages.add(msg.idLong)
        }
        edit.fragments[msg.idLong] = content

        updatePreview(edit)

    }



}



class AbstractOpenEdit(
    val user: User,
//    val editMessage: Long,
    val previewMessages: MutableList<Long>,
    val channel: Long,
    val messages: MutableList<Long>,
    val fragments: MutableMap<Long, String>
)