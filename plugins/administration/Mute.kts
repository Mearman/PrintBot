
import com.github.decyg.command.CommandCore
import com.github.decyg.core.AuditLog
import com.github.decyg.core.indicateSuccess
import com.github.decyg.core.sendInfoEmbed
import com.github.decyg.tokenizer.UserToken
import sx.blah.discord.handle.impl.events.guild.channel.ChannelCreateEvent
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IRole
import sx.blah.discord.handle.obj.Permissions
import sx.blah.discord.util.RequestBuffer
import java.util.*

val mutedRoleName = "bot_muted_role"

fun getMutedRole(guild : IGuild) : IRole {

    val foundRoles = guild.getRolesByName(mutedRoleName)

    return if (foundRoles.isEmpty()){
        val newRole = guild.createRole()
        newRole.changeName(mutedRoleName)

        newRole
    } else {
        foundRoles[0]
    }
}

CommandCore.command {
    commandAliases = listOf("mute")
    prettyName = "Mute"
    description = "Textually mutes or unmutes a user"
    requiredPermission = Permissions.MANAGE_CHANNELS
    argumentParams = listOf(
            UserToken("a user to toggle mute/unmute on") isOptional false isGreedy false
    )
    behaviour = end@{ event, tokens ->

        val userToken = tokens[0] as UserToken
        val userObj = userToken.mentionedUser!!


        if(event.guild.getRolesByName(mutedRoleName).isEmpty()){

            val newRole = getMutedRole(event.guild)

            event.guild.channels.forEach {
                if(it.getModifiedPermissions(newRole).contains(Permissions.SEND_MESSAGES)){
                    RequestBuffer.request {
                        it.overrideRolePermissions(newRole, EnumSet.noneOf(Permissions::class.java), EnumSet.of(Permissions.SEND_MESSAGES))
                    }
                }
            }

        }

        val mutedRole = getMutedRole(event.guild)

        if(userObj.getRolesForGuild(event.guild).contains(mutedRole)){
            RequestBuffer.request { userObj.removeRole(mutedRole) }
            event.channel.sendInfoEmbed(bodyMessage = "User ${userObj.name} has been unmuted.")

            AuditLog.log(
                    event.guild,
                    "User ${event.author} (${event.author.stringID}) has unmuted user $userObj (${userObj.stringID})"
            )
        } else {
            RequestBuffer.request { userObj.addRole(mutedRole) }
            event.channel.sendInfoEmbed(bodyMessage = "User ${userObj.name} has been muted.")

            AuditLog.log(
                    event.guild,
                    "User ${event.author} (${event.author.stringID}) has muted user $userObj (${userObj.stringID})"
            )
        }

        event.message.indicateSuccess()

    }
    passiveListener = end@{ event ->
        if(event !is ChannelCreateEvent)
            return@end

        val newRole = getMutedRole(event.guild)

        if(event.channel.getModifiedPermissions(newRole).contains(Permissions.SEND_MESSAGES)){
            RequestBuffer.request {
                event.channel.overrideRolePermissions(newRole, EnumSet.noneOf(Permissions::class.java), EnumSet.of(Permissions.SEND_MESSAGES))
            }
        }


    }
}

