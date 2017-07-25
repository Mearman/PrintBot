

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.decyg.command.CommandCore
import com.github.decyg.core.DiscordCore
import com.github.decyg.core.indicateSuccess
import com.github.decyg.core.sendInfoEmbed
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.util.RequestBuffer

data class PollPOKO(var pollChannels : MutableList<String>)

CommandCore.command {
    commandAliases = listOf("polltoggle")
    prettyName = "Poll"
    description = "Designates specific channels as poll channels, run in channel to toggle"
    requiredPermission = Permissions.MANAGE_CHANNEL
    argumentParams = listOf()
    behaviour = end@{ event, tokens ->

        val curChannelID = event.channel.stringID
        val poko = DiscordCore.getConfigForGuild(event.guild)
        if(!poko.pluginSettings.containsKey(this.prettyName)) {

            poko.pluginSettings[this.prettyName] = DiscordCore.mapper.writeValueAsString(PollPOKO(mutableListOf()))

        }

        val curPOKO = DiscordCore.mapper.readValue<PollPOKO>(poko.pluginSettings[this.prettyName]!!)

        if(curPOKO.pollChannels.contains(curChannelID)){
            curPOKO.pollChannels.remove(curChannelID)

            event.channel.sendInfoEmbed(bodyMessage = "The channel has been toggled off for polling.")
        } else {
            curPOKO.pollChannels.add(curChannelID)

            event.channel.sendInfoEmbed(bodyMessage = "The channel has been toggled on for polling.")
        }

        poko.pluginSettings[this.prettyName] = DiscordCore.mapper.writeValueAsString(curPOKO)

        DiscordCore.updateGuildConfigStore(event.guild.stringID)

        event.message.indicateSuccess()

    }
    passiveListener = end@{ event ->
        if(event !is MessageReceivedEvent)
            return@end

        val poko = DiscordCore.getConfigForGuild(event.guild)
        if(poko.pluginSettings.containsKey(this.prettyName)) {

            val curPOKO = DiscordCore.mapper.readValue<PollPOKO>(poko.pluginSettings[this.prettyName]!!)

            if(curPOKO.pollChannels.contains(event.channel.stringID)){

                RequestBuffer.request { event.message.addReaction(":thumbsup:") }.get()
                RequestBuffer.request { event.message.addReaction(":thumbsdown:") }.get()
                RequestBuffer.request { event.message.addReaction(":shrug:") }.get()

            }

        }

    }
}

