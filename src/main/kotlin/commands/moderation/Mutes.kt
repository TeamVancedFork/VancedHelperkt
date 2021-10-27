package commands.moderation

import config
import core.command.CommandContext
import core.command.base.BaseCommand
import core.database.moderatorRoleIds
import core.database.muteRoleId
import core.util.Infraction
import core.util.sendInfractionToModLogChannel
import core.wrapper.applicationcommand.CustomApplicationCommandCreateBuilder
import core.wrapper.applicationcommand.CustomApplicationCommandPermissionBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.core.any
import dev.kord.core.entity.interaction.string
import dev.kord.core.entity.interaction.user
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.builder.interaction.user

class Mutes : BaseCommand(
    commandName = "mute",
    commandDescription = "Mute actions",
    defaultPermissions = false
) {

    override suspend fun execute(
        ctx: CommandContext
    ) {
        val subCommand = ctx.subCommand ?: return

        when (subCommand.name) {
            "add" -> muteUser(ctx)
            "remove" -> unmuteUser(ctx)
        }
    }

    override suspend fun commandOptions() =
        CustomApplicationCommandCreateBuilder(
            arguments = {
                subCommand(
                    name = "add",
                    description = "Mute a member",
                ) {
                    user(
                        name = "user",
                        description = "Who to mute"
                    ) {
                        required = true
                    }
                    string(
                        name = "reason",
                        description = "Reason for the mute"
                    ) {
                        required = true
                    }
                }
                subCommand(
                    name = "remove",
                    description = "Unmute a member",
                ) {
                    user(
                        name = "user",
                        description = "User to unmute",
                        builder = {
                            required = true
                        }
                    )
                }
            }
        )

    override fun commandPermissions() =
        CustomApplicationCommandPermissionBuilder(
            permissions = {
                for (moderatorRoleId in moderatorRoleIds) {
                    role(
                        id = Snowflake(moderatorRoleId),
                        allow = true
                    )
                }
            }
        )

    private suspend fun muteUser(ctx: CommandContext) {
        val user = ctx.args["user"]!!.user()
        val reason = ctx.args["reasion"]!!.string()

        val userMention = user.mention

        val member = user.asMember(config.guildSnowflake)

        val muteId = Snowflake(muteRoleId)

        if (member.roles.any { it.id == muteId }) {
            ctx.respondEphemeral {
                content = "$userMention is already muted"
            }
            return
        }

        member.addRole(muteId, "Muted")
        ctx.respondPublic {
            content = "Successfully muted $userMention"
        }
        sendInfractionToModLogChannel(
            Infraction.Mute(user, ctx.author, reason)
        )
    }

    private suspend fun unmuteUser(ctx: CommandContext) {
        val user = ctx.args["user"]!!.user()

        val userMention = user.mention

        val member = user.asMember(config.guildSnowflake)

        val muteId = Snowflake(muteRoleId)

        if (!member.roles.any { it.id == muteId }) {
            ctx.respondEphemeral {
                content = "$userMention is not muted"
            }
            return
        }

        member.removeRole(muteId, "Unmuted")
        ctx.respondPublic {
            content = "Successfully unmuted $userMention"
        }
        sendInfractionToModLogChannel(
            Infraction.Unmute(user, ctx.author)
        )
    }

}