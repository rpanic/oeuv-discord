package commands

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandListener(
    jda: JDA,
    val commands: List<OeuvBotCommand>
) : ListenerAdapter() {

    init {
        commands.forEach {
            if(it is ListenerAdapter){
                println("Adding ${it.javaClass.simpleName} to listeners")
                jda.addEventListener(it)
            }
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        //TODO Filter execution by class (extract from setup())

        commands.forEach {
            it.interaction(event)
        }

    }
}