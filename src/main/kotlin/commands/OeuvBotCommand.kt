package commands

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData

interface OeuvBotCommand {
    fun setup() : List<CommandData>
    fun interaction(event: SlashCommandInteractionEvent)
}