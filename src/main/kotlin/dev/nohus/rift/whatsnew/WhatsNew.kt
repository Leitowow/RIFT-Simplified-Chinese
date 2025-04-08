package dev.nohus.rift.whatsnew

import dev.nohus.rift.whatsnew.WhatsNewViewModel.Point
import dev.nohus.rift.whatsnew.WhatsNewViewModel.Version

object WhatsNew {
    private infix fun String.description(text: String): Version {
        val points = text
            .split("""^-""".toRegex(RegexOption.MULTILINE))
            .mapNotNull { it.takeIf { it.isNotBlank() } }
            .map {
                val isHighlighted = it.startsWith("!")
                Point(
                    text = it.removePrefix("!").trimStart().removeSuffix("\n"),
                    isHighlighted = isHighlighted,
                )
            }
        return Version(
            version = this,
            points = points,
        )
    }

    fun getVersions(): List<Version> {
        return listOf(
            "2.2.0" description """
                -! New feature: Jump bridges!
                - Jump bridges are now shown on the map
                - Your jump bridge network can be either imported from clipboard, or found automatically through ESI. This feature requires a new ESI scope, so you might need to reauthenticate your characters.
                
                - Added button to the About screen to open the app data directory
                - Fixed some issues with the tray icon
                - Fixed some startup issues
            """.trimIndent(),
            "2.3.0" description """
                -! Autopilot route on the map
                - When you set the destination from RIFT, you can now see the autopilot route on the map
                - Added options for setting the autopilot route. You can now select between letting EVE choose the route, or use the route calculated by RIFT.
                
                - Fixed scaling issues with the map on macOS
                - Added warning to the jump bridge search feature
                - Various smaller UI improvements
            """.trimIndent(),
            "2.4.0" description """
                - As you travel along your route on the map, previous systems are now removed from the route
                - Decloaking notification now has an option of ignoring objects you don't want a notification for (like gates)
            """.trimIndent(),
            "2.5.0" description """
                -! Notifications improvements
                - Notifications now have a close button, if you want to get rid of them faster
                - Notifications for Local chat messages will no longer notify for EVE System messages
                - Jabber DM notifications will no longer notify for bot messages
                
                - Added reminder to select configuration pack if you are in a supported alliance but haven't done so
                - Fixed a bunch of bugs
            """.trimIndent(),
            "2.6.0" description """
                -! New feature: Assets!
                - You can now view your assets across all characters
                - Filter, sort and quickly search through your items
                - Copy or view the fittings of your ships
                - Right-click asset locations to view on the map or set autopilot
                - This feature requires a new ESI scope, so you might need to reauthenticate your characters
                
                -! What's new window
                - Added this window, which pops up when the app is updated to let you know of changes
            """.trimIndent(),
            "2.7.0" description """
                -! New feature: System stats and info on the map!
                - The map can now show the number of jumps, kills, NPC kills, stations, assets you own, incursion status, faction warfare status, and sovereignty
                - Information can be chosen as the system color, or to color to area around systems
                - Details are visible in the info box on hover
                - New collapsible panel on the map allows quickly changing the data to be shown
                - Map has been optimized and consumes less resources
                
                - Assets shown in the assets window now include their prices
                - Mumble is now opened immediately from pings, without going through the web browser
                - When there are multiple EVE installations, the newer character settings directory will be detected
                - Blueprints and skins in the assets window no longer show incorrect icons
            """.trimIndent(),
            "2.8.0" description """
                -! More map information
                - Indicators for selected types of information can now be shown next to systems
                - Selected types of information can now be shown in the system info boxes
                - Metaliminal Storms can now be shown on the map

                -! Assets
                - The total price of items in a location is now visible on the location header
                - Asset location can now be sorted by total price
                - Viewing fits from the assets window now includes the cargo contents
            """.trimIndent(),
            "2.9.0" description """
                -! Jump range on the map
                - The map can now color systems according to jump ranges and show indicators for reachable systems
                - Range can be shown from a specific system, or follow any of your characters
                - You can view the distance in light years for each system
                
                - The assets window will now show the character owning the asset when viewing assets from all characters
            """.trimIndent(),
            "2.10.0" description """
                - Added the ability to lock windows in place
                - Optimized jump bridge search
            """.trimIndent(),
            "2.11.0" description """
                - You can now disable characters that you don't want to use for anything in RIFT
                - The opened region map is now remembered across restarts
            """.trimIndent(),
            "2.12.0" description """
                -! Planets on the map
                - You can now enable map indicators for planets
                - Planet types can be filtered, whether for Skyhook scouting or PI needs
                
                - Made it possible to set up combat alerts with no target filters
                - Added a warning if your EVE client is set to a language other than English
            """.trimIndent(),
            "2.13.0" description """
                - Added EVE Online Partner badge
                - Added prioritisation of ambiguous system names in fleet pings, to choose systems with friendly sovereignty
                - Added support for multiple fleet formup locations in pings
                - Updated formup location distance counter to consider jump bridges
                - Added configuration pack with intel channels for The Initiative.
                - Added support for jump bridge list parsing when copying from Firefox
                - Updated assets browser with new hangar types
            """.trimIndent(),
            "2.14.0" description """
                -! Combat finished alert
                - Added new alert type for when you are no longer in combat. Useful for ratting when AFK.
                
                - Updated settings file saving to be more resilient to filesystem failures
            """.trimIndent(),
            "2.15.0" description """
                - Added Debug window to view logs, accessible from the About window
                - Added option to skip selecting the EVE installation during the setup wizard
            """.trimIndent(),
            "2.16.0" description """
                -! Jove Observatories
                - You can now enable map indicators for Jove Observatories, to see systems where Unidentified Wormholes can spawn
            """.trimIndent(),
            "2.17.0" description """
                - When an update is available, it can now be installed directly from the About window
                - Updated combat finished alerts to be per-character, for a better experience when multiboxing
                - Pings are now remembered for 48 hours and won't disappear when restarting
                - Added the EVE-KILL.com killmail stream to populate kills on the map along with the existing zKillboard integration
                - The Debug window now shows zKillboard, EVE-KILL, and Jabber connection status
            """.trimIndent(),
            "2.18.0" description """
                -! Sovereignty logos and colors
                - You can now enable sovereignty indicators on the map, which will display sovereignty owner logos under systems
                - The logos will also display when viewing sovereignty in the system info box
                - System colors and backgrounds can now display the sovereignty using the dominant color from the owner's logo
                
                -! Autopilot for all
                - When setting the autopilot route, you can now set it for all your online characters at once
            """.trimIndent(),
            "3.0.0" description """
                -! New feature: Intel Feed
                - In the new Intel Feed window, you can see all currently known intel
                - Filter by location type or sync the view with the currently opened map region
                - Filter by distance to your characters, or within region
                - Choose the displayed intel types; you can get a killmail feed by picking to only show killmails.
                - Sort by distance or time to get the freshest intel first
                - Search through the intel for anything you need
                - Characters are grouped together by alliance or corporation. Click any row to expand the info and see individual characters.
                - Compact mode is available for a denser layout
                
                - System names now also show the region. Wormhole systems show the wormhole class instead. Abyssal system names (triglavian) are now also supported.
                - Clicking a system will now navigate to it on the map
                - Intel in map info boxes will now group characters and switch to a compact mode if there are many items to show
            """.trimIndent(),
            "3.1.0" description """
                - Many performance optimizations
                - Improved layout of chat message notifications
                - Added animations in the Intel Feed window
                - Updated setting autopilot destination to only affect online characters
                - Updated system info boxes on the map to dynamically adjust to the window size
                - Added icons and updated the layout of information in the map system info boxes
                - Updated alerts with distance ranges to use jump bridge distances if enabled in settings
                - Added option to open wormhole systems on anoik.is in their context menu 
            """.trimIndent(),
            "4.0.0" description """
                -! New feature: Planetary Industry
                - You can now view all your PI colonies in one place
                - Each colony shows all the current details in real time
                - See colonies and their status in a list view, or a simplified grid overview
                - Check and sort by expiry time, including for production planets or storages getting full
                - Fast-forward to the future and see how you colony will look when it stops working
                - Colony location can now be selected to show on the map

                -! New feature: Jump clones
                - Characters window now shows your jump clones and implants, including in your active clone
                - Jump clone locations can be selected to show on the map

                -! Intel updates
                - Standings are now automatically updated from ESI, and characters in intel displays now show in red, orange, blue, and dark blue depending on standings
                - Intel Feed now groups characters in NPC corps together in one group
                - Improved parsing of intel messages

                - When creating an alert for a combat event, your recent combat targets will now be suggested for filtering
                - Updated logs directory detection to handle Windows installations with non-standard Documents directory location
                - Updated Dotlan icon in solar system context menu
                - Some of the new feature require new ESI scopes, so you will need to reauthenticate your characters
            """.trimIndent(),
            "4.1.0" description """
                - New view in Planetary Industry to show planets grouped into rows by character
                - Active clones with no implants are no longer shown in the Characters window
                - Jump clones display in the Characters window can now be toggled on and off
            """.trimIndent(),
            "4.2.0" description """
                -! Planetary Industry alerts
                - Create alerts for expired extractors, storages getting full, etc.
                - Choose how long in advance to receive them
                
                -! New feature: Mobile push notifications
                - Receive RIFT alerts on your phone
                - After initial setup in RIFT settings, they can be enabled for any alert
                
                - The New Eden map view now shows system backgrounds at lesser zoom, allowing for a better overview
                - Updated About window with Creator Code and Patreon info
            """.trimIndent(),
            "4.3.0" description """
                - Region maps can now be zoomed out much further, which will also scale the systems down. This makes them usable with very small map window sizes.
                - You can now press Space to automatically resize the map view to fit the window size
                - Startup warnings now have a "don't show again" checkbox.
                - Improved PI alerts to show how much time is left to the triggering event
            """.trimIndent(),
            "4.4.0" description """
                - New Null-Sec system coloring mode on the map. Unlike the normal security status colors that show all of Null-Sec in a single color, this one uses a color scale to show different levels of negative security status.
                - Crash window will now tell you if you are not running the latest version of RIFT, in case the problem is already fixed
                - Updated editing alert actions to allow choosing special actions like showing the PI window for PI alerts
            """.trimIndent(),
            "4.5.0" description """
                -! Standings and rats on the map
                - New map coloring mode for standings, which will show your blue space and red space according to your standings towards the sovereignty owner. Also available as small colored indicators next to system names.
                - New map coloring mode and indicators for rat types. See what faction of rats is present in each system.
                
                - Added support for intel channels spanning multiple regions. Add the same channel multiple times with each region it's for.
                - Added a UI scale setting, enabling you to make everything in RIFT bigger or smaller.
            """.trimIndent(),
            "4.6.0" description """
                - Tabs, like map regions, can now be closed with the middle mouse button
                - Added additional protection against settings corruption in case of power loss
            """.trimIndent(),
            "4.7.0" description """
                - Added parsing "clr du" as an intel clear message
                - Added wrapping implants in the Characters window into multiple lines when there is not enough space to show all on one line
                - Added ellipsis when a context menu entry is too long to fit
            """.trimIndent(),
            "4.8.0" description """
                -! New feature: Contacts
                - View all your in-game contacts across all your characters, including corporation and alliance contacts
                - Filter by character, corporation, alliance, standing level or any label
                - Search through your contacts and see their details
                - Add, edit or delete your contacts, assign standings, labels, and more
                - In the right-click menu of any character, corporation and alliance you see in the app, you will now find a new option to Add Contact
                
                -! New feature: Search
                - Search for anything from the game: characters, corporations, alliances, items, structures, and many more
                - See character affiliations
                - See standings for characters, corporations and alliances
                - Add to contacts
                - View on the map and set destination to found structures, stations and systems
                
                - Added a warning if you have chat logs disabled in EVE settings
            """.trimIndent(),
            "4.9.0" description """
                - Added the ability to maximize windows. It's available in the three-dots menu for most windows, and directly on the title bar for the map window.
                - Added map setting to not hide system name labels when zooming out region maps.
                - Alerts can now be created for intel up to 15 jumps away, up from the previous limit of 5 jumps
                - Updated map setting for showing automatic intel popups to allow disabling the popups completely
                - Updated default intel channel list for users of The Imperium configuration pack. If you'd like to use it, remove all intel channels from your list to return to the updated defaults.
            """.trimIndent(),
            "4.10.0" description """
                -! New feature: Character Settings Copying
                - Copy both character-level and account-level EVE settings between your characters
                - Automatically detects which characters belong to which accounts
                - No manual file choosing required
                
                - Maximized windows now always show the unmaximize button on the toolbar for easy access
                - If you never interacted with the tray icon, then the Neocom window now always shows on startup and quits the app completely when closed
                - The Neocom window is now resizeable and the buttons adapt to the window size
            """.trimIndent(),
            "4.11.0" description """
                - Message alerts now highlight the keyword that the alert was looking for
                - Characters that failed to load can now be added to the disabled characters list
                - The Jabber window now has an option for a bigger font size in the chats
            """.trimIndent(),
            "4.12.0" description """
                -! New feature: Combined Region Maps
                - 14 new maps are now available showing related areas across multiple regions
                - You can switch between alternative maps from the map settings panel, when you are viewing regions that have alternative maps available
            """.trimIndent(),
            "4.13.0" description """
                - Improved parsing intel messages to better handle characters that have names that are English words, module names, and similar. Such text will no longer be detected as a character name, if the given character is inactive (hasn't played for a long time).
                - zKillboard activity is also taken into account, so that if a character appears on any killmail within the last few months, it will never be ignored
                - Added integration with ntfy.sh for push notifications, in addition to the existing Pushover integration.
                - All dropdowns can now be quickly navigated with the keyboard by typing, which scrolls it to the matching item. Arrows keys and the Enter key can now also be used.
            """.trimIndent(),
            "4.14.0" description """
                - Updated splash screen
            """.trimIndent(),
            "4.15.0" description """
                - Solar system distances in intel now have a hover tooltip to show which of your characters is closest
                - Updated jump bridge parsing to work with an updated alliance wiki page
                - Account settings files backed up by other third party tools no longer interfere with RIFT
            """.trimIndent(),
            "4.16.0" description """
                -! Industry Cost Indices on the map
                - New indicators for Copying, Invention, Manufacturing, Reactions, Material Efficiency Research and Time Efficiency Research Cost Indices
                - Each index can be shown or hidden separately
                - New system and background coloring modes for each index to see the relative values at a glance
            """.trimIndent(),
        ).reversed()
    }
}
