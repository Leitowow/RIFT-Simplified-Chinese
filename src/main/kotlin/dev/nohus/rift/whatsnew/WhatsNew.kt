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
        return buildList {
            add(
                "2.2.0" description """
                -! New feature: Jump bridges!
                - Jump bridges are now shown on the map
                - Your jump bridge network can be either imported from clipboard, or found automatically through ESI. This feature requires a new ESI scope, so you might need to reauthenticate your characters.

                - Added button to the About screen to open the app data directory
                - Fixed some issues with the tray icon
                - Fixed some startup issues
                """.trimIndent(),
            )
            add(
                "2.3.0" description """
                -! Autopilot route on the map
                - When you set the destination from RIFT, you can now see the autopilot route on the map
                - Added options for setting the autopilot route. You can now select between letting EVE choose the route, or use the route calculated by RIFT.

                - Fixed scaling issues with the map on macOS
                - Added warning to the jump bridge search feature
                - Various smaller UI improvements
                """.trimIndent(),
            )
            add(
                "2.4.0" description """
                - As you travel along your route on the map, previous systems are now removed from the route
                - Decloaking notification now has an option of ignoring objects you don't want a notification for (like gates)
                """.trimIndent(),
            )
            add(
                "2.5.0" description """
                -! Notifications improvements
                - Notifications now have a close button, if you want to get rid of them faster
                - Notifications for Local chat messages will no longer notify for EVE System messages
                - Jabber DM notifications will no longer notify for bot messages

                - Added reminder to select configuration pack if you are in a supported alliance but haven't done so
                - Fixed a bunch of bugs
                """.trimIndent(),
            )
            add(
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
            )
            add(
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
            )
            add(
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
            )
            add(
                "2.9.0" description """
                -! Jump range on the map
                - The map can now color systems according to jump ranges and show indicators for reachable systems
                - Range can be shown from a specific system, or follow any of your characters
                - You can view the distance in light years for each system

                - The assets window will now show the character owning the asset when viewing assets from all characters
                """.trimIndent(),
            )
            add(
                "2.10.0" description """
                - Added the ability to lock windows in place
                - Optimized jump bridge search
                """.trimIndent(),
            )
            add(
                "2.11.0" description """
                - You can now disable characters that you don't want to use for anything in RIFT
                - The opened region map is now remembered across restarts
                """.trimIndent(),
            )
            add(
                "2.12.0" description """
                -! Planets on the map
                - You can now enable map indicators for planets
                - Planet types can be filtered, whether for Skyhook scouting or PI needs

                - Made it possible to set up combat alerts with no target filters
                - Added a warning if your EVE client is set to a language other than English
                """.trimIndent(),
            )
            add(
                "2.13.0" description """
                - Added EVE Online Partner badge
                - Added prioritisation of ambiguous system names in fleet pings, to choose systems with friendly sovereignty
                - Added support for multiple fleet formup locations in pings
                - Updated formup location distance counter to consider jump bridges
                - Added configuration pack with intel channels for The Initiative.
                - Added support for jump bridge list parsing when copying from Firefox
                - Updated assets browser with new hangar types
                """.trimIndent(),
            )
            add(
                "2.14.0" description """
                -! Combat finished alert
                - Added new alert type for when you are no longer in combat. Useful for ratting when AFK.

                - Updated settings file saving to be more resilient to filesystem failures
                """.trimIndent(),
            )
            add(
                "2.15.0" description """
                - Added Debug window to view logs, accessible from the About window
                - Added option to skip selecting the EVE installation during the setup wizard
                """.trimIndent(),
            )
            add(
                "2.16.0" description """
                -! Jove Observatories
                - You can now enable map indicators for Jove Observatories, to see systems where Unidentified Wormholes can spawn
                """.trimIndent(),
            )
            add(
                "2.17.0" description """
                - When an update is available, it can now be installed directly from the About window
                - Updated combat finished alerts to be per-character, for a better experience when multiboxing
                - Pings are now remembered for 48 hours and won't disappear when restarting
                - Added the EVE-KILL.com killmail stream to populate kills on the map along with the existing zKillboard integration
                - The Debug window now shows zKillboard, EVE-KILL, and Jabber connection status
                """.trimIndent(),
            )
            add(
                "2.18.0" description """
                -! Sovereignty logos and colors
                - You can now enable sovereignty indicators on the map, which will display sovereignty owner logos under systems
                - The logos will also display when viewing sovereignty in the system info box
                - System colors and backgrounds can now display the sovereignty using the dominant color from the owner's logo

                -! Autopilot for all
                - When setting the autopilot route, you can now set it for all your online characters at once
                """.trimIndent(),
            )
            add(
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
            )
            add(
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
            )
            add(
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
            )
            add(
                "4.1.0" description """
                - New view in Planetary Industry to show planets grouped into rows by character
                - Active clones with no implants are no longer shown in the Characters window
                - Jump clones display in the Characters window can now be toggled on and off
                """.trimIndent(),
            )
            add(
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
            )
            add(
                "4.3.0" description """
                - Region maps can now be zoomed out much further, which will also scale the systems down. This makes them usable with very small map window sizes.
                - You can now press Space to automatically resize the map view to fit the window size
                - Startup warnings now have a "don't show again" checkbox.
                - Improved PI alerts to show how much time is left to the triggering event
                """.trimIndent(),
            )
            add(
                "4.4.0" description """
                - New Null-Sec system coloring mode on the map. Unlike the normal security status colors that show all of Null-Sec in a single color, this one uses a color scale to show different levels of negative security status.
                - Crash window will now tell you if you are not running the latest version of RIFT, in case the problem is already fixed
                - Updated editing alert actions to allow choosing special actions like showing the PI window for PI alerts
                """.trimIndent(),
            )
            add(
                "4.5.0" description """
                -! Standings and rats on the map
                - New map coloring mode for standings, which will show your blue space and red space according to your standings towards the sovereignty owner. Also available as small colored indicators next to system names.
                - New map coloring mode and indicators for rat types. See what faction of rats is present in each system.

                - Added support for intel channels spanning multiple regions. Add the same channel multiple times with each region it's for.
                - Added a UI scale setting, enabling you to make everything in RIFT bigger or smaller.
                """.trimIndent(),
            )
            add(
                "4.6.0" description """
                - Tabs, like map regions, can now be closed with the middle mouse button
                - Added additional protection against settings corruption in case of power loss
                """.trimIndent(),
            )
            add(
                "4.7.0" description """
                - Added parsing "clr du" as an intel clear message
                - Added wrapping implants in the Characters window into multiple lines when there is not enough space to show all on one line
                - Added ellipsis when a context menu entry is too long to fit
                """.trimIndent(),
            )
            add(
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
            )
            add(
                "4.9.0" description """
                - Added the ability to maximize windows. It's available in the three-dots menu for most windows, and directly on the title bar for the map window.
                - Added map setting to not hide system name labels when zooming out region maps.
                - Alerts can now be created for intel up to 15 jumps away, up from the previous limit of 5 jumps
                - Updated map setting for showing automatic intel popups to allow disabling the popups completely
                - Updated default intel channel list for users of The Imperium configuration pack. If you'd like to use it, remove all intel channels from your list to return to the updated defaults.
                """.trimIndent(),
            )
            add(
                "4.10.0" description """
                -! New feature: Character Settings Copying
                - Copy both character-level and account-level EVE settings between your characters
                - Automatically detects which characters belong to which accounts
                - No manual file choosing required

                - Maximized windows now always show the unmaximize button on the toolbar for easy access
                - If you never interacted with the tray icon, then the Neocom window now always shows on startup and quits the app completely when closed
                - The Neocom window is now resizeable and the buttons adapt to the window size
                """.trimIndent(),
            )
            add(
                "4.11.0" description """
                - Message alerts now highlight the keyword that the alert was looking for
                - Characters that failed to load can now be added to the disabled characters list
                - The Jabber window now has an option for a bigger font size in the chats
                """.trimIndent(),
            )
            add(
                "4.12.0" description """
                -! New feature: Combined Region Maps
                - 14 new maps are now available showing related areas across multiple regions
                - You can switch between alternative maps from the map settings panel, when you are viewing regions that have alternative maps available
                """.trimIndent(),
            )
            add(
                "4.13.0" description """
                - Improved parsing intel messages to better handle characters that have names that are English words, module names, and similar. Such text will no longer be detected as a character name, if the given character is inactive (hasn't played for a long time).
                - zKillboard activity is also taken into account, so that if a character appears on any killmail within the last few months, it will never be ignored
                - Added integration with ntfy.sh for push notifications, in addition to the existing Pushover integration.
                - All dropdowns can now be quickly navigated with the keyboard by typing, which scrolls it to the matching item. Arrows keys and the Enter key can now also be used.
                """.trimIndent(),
            )
            add(
                "4.14.0" description """
                - Updated splash screen
                """.trimIndent(),
            )
            add(
                "4.15.0" description """
                - Solar system distances in intel now have a hover tooltip to show which of your characters is closest
                - Updated jump bridge parsing to work with an updated alliance wiki page
                - Account settings files backed up by other third party tools no longer interfere with RIFT
                """.trimIndent(),
            )
            add(
                "4.16.0" description """
                -! Industry Cost Indices on the map
                - New indicators for Copying, Invention, Manufacturing, Reactions, Material Efficiency Research and Time Efficiency Research Cost Indices
                - Each index can be shown or hidden separately
                - New system and background coloring modes for each index to see the relative values at a glance
                """.trimIndent(),
            )
            add(
                "4.17.0" description """
                -! New feature: Multiple map windows
                - Thanks to a major rework of RIFT's window management, you can now open multiple maps at the same time
                - The positions, sizes, and opened regions are remembered for each map window separately across restarts
                - When showing a system on the map from a context menu somewhere in the app, it will be opened in the window with that region already opened, if possible
                - When filtering by opened regions in the Intel Feed window, regions from all open maps will be considered

                -! New feature: Contacts list label alerts
                - You can now set up alerts for intel about characters from a contacts label of any of your characters, corporations, and alliances
                - For example, if you have a contacts list of hot droppers in-game, you can now setup an alert for that list
                - The alerts will automatically update when contacts are added or removed from the label either in-game or in RIFTs Contacts feature
                - This also works for labeled corporations and alliances, so you can create an alert for all characters from a specific corporation or alliance

                - Region names and constellation names can now be enabled to show for systems on the map, either always or on hover
                """.trimIndent(),
            )
            add(
                "4.18.0" description """
                -! New feature: Window transparency 
                - You can now make any RIFT window transparent, similar to the "light background" option in-game
                - This also switches to a new color scheme so that windows look properly in transparent mode

                - Your own corporations and alliances now show up in blue even if you don't have them explicitly set with positive standing
                - Chat message notifications now show the standing of the message author
                - Chat message notifications are no longer sent for "EVE System" messages and MOTDs, unless the alert is set specifically for "EVE System" messages
                - Optimized chat log file reading, reducing CPU usage
                - Window states are now managed per unique window, not per-window type, so you can now have one map window be always on-top and another one not, for example
                - Structure details failing to be loaded from ESI for the Assets window will no longer prevent you from seeing all assets. Instead, the relevant assets will just show to be in an unknown structure.
                - Updated Steam library detection on Linux to cover more installation types
                - Context menus will now scroll if there is not enough space to display all items
                """.trimIndent(),
            )
            add(
                "4.19.0" description """
                -! New Planetary Industry features
                - Colony icons now show the final product of the colony, and have circular progress bars showing the fill level of product storages, allowing you to see the status of your colonies at a glance and know if they need a pickup
                - The fill level only considers these storages where your final products accumulate, and shows products in blue and other commodities in orange, if they share storages
                - New animations visually show the colony work status, whether it's extracting, producing, idle, or having a problem
                - Data about your colonies can now be copied to paste into a spreadsheet. This supports Google Sheets, Excel, as well as Excel with the EVE Online add-in.
                - When sorting PI colonies by character, they are now also sorted by account – characters on the same account appear together
                - Added new animations when switching colony views and getting in and out of colony details

                - The window transparency feature now has a global toggle in settings. It defaults to disabled, so enable it to use it again.
                - Window transparency strength can now be controlled, so you can choose how much transparency you want
                """.trimIndent(),
            )
            add(
                "4.20.0" description """
                -! New "smart always above" setting
                - Enabling it will make the always above toggle on windows only apply when you have an EVE client focused. This means you can keep RIFT windows on top of EVE windows, but not on top of other windows when you switch away from the game.

                - RIFT can now follow conversations across multiple messages in intel channels, and understand when a message is an answer to an earlier relevant question
                - The Assets window now shows the total volume of your assets per station. The total uses the repackaged volumes.
                - The Planetary Industry spreadsheet export now includes average extracted per hour figures for every extracted commodity
                - Setting an autopilot destination in-game will now show your route on the RIFT map
                - Exequror and Brutix Navy Issues are now recognized in intel reports when reported as ENI and BNI
                - Optimized watching log files on Windows to prevent slowdowns for people with huge amounts of log files
                - Optimized reading game logs to be much faster, especially in longer game sessions
                - Updated duplicate chat message handling to work better when multiboxed chat logs that have inconsistent timestamps
                - Associating characters with accounts for the settings copying feature was improved to work in more cases
                """.trimIndent(),
            )
            add(
                "4.21.0" description """
                -! Locations in Intel Feed and on the Map
                - Intel coming from killmails now includes the location in system where it happened. Campers on a stargate or station? Someone got ganked in an asteroid belt? See exactly where it happened.

                - Push notifications for fleet pings now show details of the ping
                """.trimIndent(),
            )
            add(
                "4.22.0" description """
                -! Optional ESI scopes (permissions)
                - RIFT no longer requires all needed ESI scopes to be granted. The features needing a given scope just won't work for characters that don't have it.
                - This means that when new features come to RIFT requiring new scopes, you will no longer need to reauthenticate all your characters – unless you want to use the new feature.
                - Now that the app supports not having all scopes, you can choose which scopes to grant when authenticating a character, if you want. You can now also see what each scope is used for.

                -! Current ship and status
                - The Characters window now shows the current ship of each of your characters, alongside the ship name and docking status.
                - This requires a new ESI scope, which you can grant by authenticating a character again.

                - Killmail locations showing up in intel feeds now show the distance to the celestial's warp-in point, if applicable. For example, if when someone is killed near a planet, you will see the planet along with how far away it happened from that planet's warp-in point.
                - Updated the setup wizard to be clearer about what intel channels are and that adding them is optional
                - Your old chat and game log files are now moved to an "old" subdirectory as an optimization.
                """.trimIndent(),
            )
            add(
                "4.23.0" description """
                - When using the "move with character" map option, the system info box won't be expanded anymore
                - When clicking a system on the map, the map won't center on the system anymore, so you can click systems without ruining your map layout
                - Dragging the map won't hide the expanded system info boxes anymore, so you can look around without hiding information you want open
                """.trimIndent(),
            )
            add(
                "4.24.0" description """
                - Jump range text field now has autocomplete for system and character names
                - Map search now has autocomplete for system names
                - Map search now works for systems that aren't visible on the current map layout, and will automatically switch the map tab to show the result
                - Chat message alerts now support regular expressions for precise control of matching messages
                """.trimIndent(),
            )
            add(
                "4.25.0" description """
                -! New Map Markers feature
                - Do you want to add your own markers and notes to systems on the map? Now you can.
                - Add a marker to any system from it's right-click menu, or access your markers from the Map Settings window
                - Choose from a big list of icons, and give your marker a color if you want
                - View, edit, and delete your markers from the new Map Markers window

                -! Thera and Turnur connections on the map
                - The map can now show indicators for Thera and Turnur wormholes
                - These include more details about the connection on mouseover

                -! New alert types
                - You can now set up alerts for intel relating to ESS and Skyhooks
                - You can now set up an alert for modules running out of charges
                - You can now set up custom alerts for any game log message you wish, including regular expressions support

                -! Deleting characters
                - Do you have unused or biomassed characters whose data you no longer want to keep? You can now delete them from your EVE installation and RIFT's character list.

                - Added New Eden Encyclopedia as an option for viewing ship details
                """.trimIndent(),
            )
            add(
                "4.26.0" description """
                - The character settings copying feature now supports launcher profiles, including copying across profiles
                - When adding an intel channel, the text field now shows autocomplete suggestions of valid channel names
                - The Assets window now shows totals for all your assets from all locations
                """.trimIndent(),
            )
            add(
                "4.27.0" description """
                - Asset locations can now be pinned in the Assets window to always appear on top
                - They can also be hidden, which will put them below the main list
                """.trimIndent(),
            )
            add(
                "4.28.0" description """
                - Updated assets location pinning to work like in-game and show a pin icon
                - Updated assets location hiding to be grouped into a "Hidden" category like in-game
                - Updated many icons
                - Updated the look of map tabs
                - Replaced some dropdowns with a new slider component where relevant, like for volume in Settings
                - Performance improvements to startup time
                """.trimIndent(),
            )
            add(
                "4.29.0" description """
                - Updated the Character Settings Copy feature to support users with an extreme number of characters
                - Updated window management on Linux so that dragging RIFT windows participates in window snapping and other native window features
                """.trimIndent(),
            )
            add(
                "4.30.0" description """
                -! New feature: Jukebox!
                - In 2012, the in-game MP3 player was removed, which many consider an egregious error. After over a decade of silence, RIFT is bringing back the most essential feature EVE has ever lost – the legendary Jukebox!
                - Enjoy the original EVE soundtrack, with the ability to switch tracks at will
                - Blast Below the Asteroids on repeat while mining
                - Create and manage your own custom playlists, because space is better with your own MP3s
                - Relive the True EVE Jukebox Experience, just as CCP intended
                """.trimIndent(),
            )
            add(
                "4.31.0" description """
                -! New options in Map Settings
                - You can now choose if you prefer to show systems on region maps or the New Eden map. This is taken into account when clicking a system anywhere in RIFT, showing a system on the map from right-click menus, or searching for systems on the map.
                - You can now choose separately if you want the map to follow your character by moving within the opened map, switch the opened map if you left the currently visible regions, or both

                -! Alerts window
                - Alert groups can now be collapsed
                - Alert group headers now show the number of alerts and enabled alerts within the group
                """.trimIndent(),
            )
            add(
                "4.32.0" description """
                - Systems on the map that have some intel associated, but no characters or ships, are now highlighted with a grey circle. For example you can now easily see systems with bubbles reported.
                - The map no longer centers on the online character location whenever you open a map tab, if you have character following disabled
                - Added a warning when running the EVE client in fullscreen mode, to let you know it can interfere with always-on-top windows
                - The character settings copying feature now shows account IDs instead of giving accounts ordinal numbers that can change
                """.trimIndent(),
            )
            add(
                "4.33.0" description """
                -! New feature: Distance Map
                - This new map type shows nearby systems in bands grouped by distance from a central system
                - Monitor your neighborhood and see how far away are any hostiles
                - You can either choose a system as the origin, or have it automatically follow a character to always see what's around you
                - As with all maps, you can open multiple copies to show different systems at the same time
                - Keep it simple, or customize it to show any information you want
                - It also works great at small sizes, and with the transparent mode you can have it be a seamless part of your in-game layout
                """.trimIndent(),
            )
            add(
                "4.34.0" description """
                - New Legion splash screen
                - Intel from killmails now triggers alerts. Previously you could see ships and characters known from killmails on the map, but they wouldn't trigger alerts even when matching. Now they do.
                - The New Eden cluster map now highlights systems with intel even when zoomed out, so you can easily see where things are going on on a wider scale.
                """.trimIndent(),
            )
            add(
                "4.35.0" description """
                - The character settings copying feature now shows a warning if you have the game open
                - RIFT will now warn you on startup if your system clock is incorrect, which is a common cause of inaccurate intel timestamps and missing alerts
                """.trimIndent(),
            )
            add(
                "4.36.0" description """
                -! New feature: Sovereignty Upgrades
                - You can now import a list of Sovereignty Upgrades improving your solar systems
                - Your list can be exported out of RIFT as well
                - The upgrades are then shown on the map. As usual, you can choose to always display them, only display them on hover, or use their presence to color systems or their backgrounds on the map.
                - You can also choose to only see the types of upgrades you are interested in
                - All upgrades added in the Legion expansion are supported, including the new Exploration Detector upgrades. Their area of effect is shown on the map, so you can easily see where the additional exploration sites can spawn.
                - Systems affected by multiple Exploration Detector upgrades also have this indicated

                - The Settings window has been redesigned and organized into separate tabs
                - Jump Bridge Network settings have been redesigned, and you can now see a list of your imported jump bridge connections
                - You can now export your jump bridges out of RIFT
                - The Map Settings window has been removed. Map settings now have their own tab in the main Settings window.
                """.trimIndent(),
            )
            add(
                "4.37.0" description """
                - The map now shows active clones in addition to jump clones. This means you can now see your offline characters on the map.
                - Multiple killmails happening in the same system in quick succession will no longer trigger multiple alerts
                - The Assets feature now tells blueprint copies apart from originals for price calculation
                """.trimIndent(),
            )
            add(
                "4.38.0" description """
                -! New feature: Sovereignty Hub hacking import
                - When hacking a Sovereignty Hub in-game, you can now click the Copy Hacked Data button to instantly import the list of Sovereignty Upgrades into RIFT
                - This allows you to easily fill out your map with the information from the Hub you just hacked, and later copy and share the complete list of upgrades from all systems if needed

                - Updated intel channels and sovereignty upgrades link in a configuration pack
                """.trimIndent(),
            )
            add(
                "4.39.0" description """
                - You can now create alerts that only consider your undocked characters. If you don't want to receive alerts while docked, this is for you.
                - Characters in the Intel Reports window now show the corporation, alliance, and standing of the character, like in the Intel Feed window
                - Characters now show a label icon if they have a contact label assigned to them, their corporation, or their alliance. Hovering on the icon will show the label names. This works in Intel Reports, Intel Feed, the Map, and notifications.
                - Added EVE-KILL as option in the right-click menus of characters, corporations, alliances, ships, and systems
                - When you left-click on a character, corporation, alliance, or ship, it will open in the last used website, instead of being hardcoded to zKillboard
                - Systems in the Intel Feed window that contain only a single character now show the full character name instead of "1 hostile"
                """.trimIndent(),
            )
            add(
                "4.40.0" description """
                -! Asteroids and ice on the map
                - The map can now show indicators for asteroid belts, their counts, and ice field systems

                - You can now sort by character name in the Planetary Industry window
                - Viewing and copying fittings with charges is now supported in the Assets window, where supported by ESI
                """.trimIndent(),
            )
            add(
                "5.1.0" description """
                -! New feature: Corporation Projects
                - View active and historical projects from all your characters and corporations
                - Easily filter, sort, and search for projects
                - See all the details for all types of projects
                - Check your contribution progress from all characters at once
                - If you have the Project Manager role in your corporation, you can also see all the participants in a project and information about them
                
                -! New feature: Show Info
                - The familiar "Show Info" button from in-game context menus is now available in RIFT context menus
                - It shows when right-clicking on any character, corporation, alliance, ship, or system
                - Clicking it will open the info window in-game
                
                - New system, constellation, and region icons for every individual location that match in-game. These are now used for systems instead of the old sun icons.
                - Updated font sizes in the app to better match in-game. Some text is now slightly bigger.
                """.trimIndent(),
            )
            add(
                "5.3.0" description """
                -! New feature: PI colony time seek & fast-forward
                - You can now preview the state of your colonies at any time in the future up to the time they stop working
                - Set up a complex layout and want to see what will happen in 12 hours? Now you can.
                
                - Spreadsheet exports of colonies now include all stored commodities
                """.trimIndent(),
            )
            add(
                "5.4.0" description """
                -! New feature: Wallets
                - You can now view and analyze your character and corp wallets in one unified interface
                - View all your wallets at once or select the ones you are interested in
                - See an overview of your income and expenses by type with an interactive pie chart
                - Browse, filter and search through all your transactions at once, with a level of detail and context not available in-game
                - Get additional insights by transaction party. Who contributes the most to the corp wallet? Which of your alts is a money sink?
                - See statistics for ratting, showing all the ships you destroyed by type and system
                - Check your daily goal streaks and quickly find out which characters still haven't completed a goal today
                - See balance change activity per day, by character or transaction party
                
                - Added an info tooltip informing about the jump bridge import format
                - Locations, gates and stations (for example in Assets and Intel Feed) are now clickable to open them in-game
                """.trimIndent(),
            )
            add(
                "5.5.0" description """
                - Optimized wallets data loading
                - Wallets data now has a detailed progress UI showing what's being loaded
                - Wallets data is now only loaded when you open the Wallets window, and not on startup
                - Optimized killmail fetching, made possible by new capabilities of zKillboard's killmail stream
                """.trimIndent(),
            )
            add(
                "5.6.0" description """
                - Overhauled networking for better performance when sending requests to ESI and other APIs
                - For those interested, in the Debug window – accessible from the About window – there is now a Network Statistics tab with a live network requests chart breaking down sent requests by feature
                - In Planetary Industry spreadsheet exports, commodity-specific columns are now sorted by name
                - In Wallets there are now more options for visible timespans, including shorter timestamps like last 2 hours
                - In Jabber, longer channel MOTDs now scroll instead of taking a lot of space
                """.trimIndent(),
            )
            add(
                "5.7.0" description """
                -! New feature: Loyalty Points
                - In the Wallets window, there is now a Loyalty Points tab showing your LP and EverMarks for all characters
                - With each LP type, you can also see the closest LP store for that corporation, and view it on the map or set it as your autopilot destination
                
                - Added several optimizations to memory usage when using RIFT for longer periods of time.
                - Implemented handling for the newly introduced ESI rate limits which will become active in the coming weeks. This means RIFT will be staying well within them.
                """.trimIndent(),
            )
            add(
                "5.8.0" description """
                - Added Loyalty Points totals showing your LP summed up from all characters
                - Added Loyalty Points search to filter by name
                - Greatly improved the speed of processing wallet transactions, especially for people with hundreds of thousands of them
                - Added a popup informing of updates being available when using old versions of the app
                """.trimIndent(),
            )
            add(
                "5.9.0" description """
                - Your online characters are now shown in one row on the map system info boxes, instead of one per character, to save space
                """.trimIndent(),
            )
            add(
                "5.10.0" description """
                - Updated splash screen
                - Chat message alerts now have an option to exclude messages from your own characters
                - Intel Reports now has a new option to reverse the message order, so that new messages show at the top
                - Intel Reports and Intel Feed windows now have their settings directly in a context menu, instead of opening separate settings windows
                - Updated text and sizing of map system info boxes
                - Startup warnings now scroll if necessary
                """.trimIndent(),
            )
            add(
                "5.11.0" description """
                - The new 2D map layout introduced in-game in the Catalyst expansion is now available in RIFT's New Eden map, along with many map tweaks
                - Added new buttons on the map to focus on the current location, fit the map to the window, or switch to the 2D map layout. They are also quickly accessible with keyboard shortcuts.
                - Updated internal data for new Catalyst ships
                - Significantly reduced memory usage when running RIFT over time
                """.trimIndent(),
            )
            add(
                "5.12.0" description """
                -! New feature: Corporation Assets
                - The Assets window now includes corporation assets from all your corporations
                - Assets are shown in their corporation hangars and project delivery hangars
                - Hangars use their in-game division names, if you have access
                - New completely redesigned asset owner filtering allows you to choose which characters and corporations are you interested in. Choose one or any combination.
                """.trimIndent(),
            )
            add(
                "5.13.0" description """
                - Characters settings copying now can be used even when you can't load character details for any reason
                """.trimIndent(),
            )
            add(
                "5.14.0" description """
                -! New Feature: Freelance Jobs
                - The new Opportunities window allows you to see public Freelance Jobs, jobs from your corporations, and jobs you are participating in
                - Easily filter, sort, and search for jobs
                - See all the details for all types of jobs
                - Check your contribution progress from all characters at once
                - If you have the Project Manager role in your corporation, you can also see all the participants in a jobs and information about them
                - Corporation Projects are now also available in the same place
                
                - Jump range display on the map was updated to only consider valid jump destinations, and will now ignore high-sec systems, Pochven, and similar
                - ESI interactions were updated, which should result in an overall smoother experience, especially under heavy load
                """.trimIndent(),
            )
            add(
                "5.15.0" description """
                - Using a new computer vision pipeline, RIFT is now able to process character portraits to separate the characters from their backgrounds, allowing to display them in more creative ways
                - In many places, characters portraits are now shown with a subtle parallax effect
                - Characters in intel contexts now more clearly show their standings by coloring their portrait backgrounds, which makes it easier to tell friend from foe at a glance
                - The character portraits are cached and will load much faster for characters you are not seeing for the first time
                
                - Significant internal upgrades were made, improving performance and memory use
                - The Linux release of RIFT is now available as an AppImage, making it very easy run on any distribution
                """.trimIndent(),
            )
            add(
                "5.16.0" description """
                - Added new settings for character portraits, allowing to change the strength of the background parallax effect, choose when to highlight backgrounds for standings, and the intensity of the highlights
                - Added an option to add an intel channel that isn't specific to any region
                - Added an option to turn off zKillboard killmail monitoring
                - Added a Clipboard Import Tester in Settings, to troubleshoot importing jump bridges and sovereignty upgrades, and see if the format is correct, or why it isn't
                """.trimIndent(),
            )
            add(
                "5.17.0" description """
                - You can now set custom asset location names, to easily note which station is which
                - This also works for locations where ESI does not provide a name - like Skyhooks - allowing you to name them and be able to figure out which is which
                - Improved handling of corp assets in the Assets feature, including visibility of Impounded assets and a new Capsuleer Deliveries folder
                """.trimIndent(),
            )
            add(
                "5.18.0" description """
                - Everywhere standings are shown, RIFT now shows corp members and alliance members with their own star icons and colors, same as in-game
                - The new Exordium region is now visible on the Map
                - Several performance improvements
                """.trimIndent(),
            )
        }.reversed()
    }
}
