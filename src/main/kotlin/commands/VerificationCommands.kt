package commands

import OEUVBot
import db.BotDB
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import kotlin.math.log

class VerificationCommands(val bot: OEUVBot) : OeuvBotCommand {

    override fun setup(): List<CommandData> {

        return listOf(
            Commands.slash("nudgeunverified", "nudged unverified members (only admin)")
        )

    }

    override fun interaction(event: SlashCommandInteractionEvent) {

        if(event.name == "nudgeunverified"){
            val isAdmin = event.member?.roles?.any { it.name.equals(bot.adminRoleName, true) } ?: false
            if(isAdmin){
                nudgeNonVerified();
            }
            event.reply("Done").setEphemeral(true).queue()
        }

    }

    val msgText = """Hallo, du bist in letzter Zeit dem ÖUV Discord-Server beigetreten.
        |
        |Allerdings bist du noch nicht verifiziert und kannst daher noch nicht alle Kanäle sehen, die die Platform zu bieten hat.
        |Um verifiziert zu werden musst du deinen Nicknamen (nur auf dem ÖUV Server sichtbar) in **Vor- und Nachname** ändern. 
        |Genauere Infos und Erklärungen findest du im ÖUV-Server im Channel **Willkommen**
    """.trimMargin()

    private fun nudgeNonVerified() {

        val sentMsgs = BotDB.sentVerificationNotifications.toList().map { it.id }

        bot.guild.loadMembers().onSuccess {
            it.filter { user -> user.roles.none { role -> role.name == "ÖUV" } }
                .filter { user -> user.idLong !in sentMsgs }
                .forEach { member ->

                    println("Nudging ${member.nickname}")
                    member.user.openPrivateChannel().complete()
                        .sendMessage(msgText)
                        .queue()
                }
        }

    }

}

@Serializable
data class UserIdDC (val id: Long)