package dev.nohus.rift.jukebox.data

import dev.nohus.rift.jukebox.Source
import dev.nohus.rift.jukebox.Track
import io.ktor.http.encodeURLPath
import java.time.Duration
import java.util.UUID

object NetworkTracks {

    val soundtrack = getSoundtrackTracks()
    val trailer = getTrailerTracks()
    val permaband = getPermabandTracks()

    private data class NetworkTrack(
        val title: String,
        val duration: String,
        val filename: String,
    )

    private fun getSoundtrackTracks(): List<Track> {
        return listOf(
            NetworkTrack("Miner Stories", "6:12", "EVE Online - Miner Stories.mp3"),
            NetworkTrack("Hidden Mementos", "6:38", "EVE Online - Hidden Mementos.mp3"),
            NetworkTrack("War Relics", "6:14", "EVE Online - War Relics.mp3"),
            NetworkTrack("Red Glowing Dust", "5:37", "EVE Online - Red Glowing Dust.mp3"),
            NetworkTrack("Costs of Conflict", "6:01", "EVE Online - Costs of Conflict.mp3"),
            NetworkTrack("Jovian Riddle", "5:02", "EVE Online - Jovian Riddle.mp3"),
            NetworkTrack("Lost Wormhole", "6:01", "EVE Online - Lost Wormhole.mp3"),
            NetworkTrack("Below the Asteroids", "4:37", "EVE Online - Below the Asteroids.mp3"),
            NetworkTrack("Forsaken Ruins", "6:59", "EVE Online - Forsaken Ruins.mp3"),
            NetworkTrack("Surplus of Rare Artifacts", "8:13", "EVE Online - Surplus of Rare Artifacts.mp3"),
            NetworkTrack("The Green Nebula", "6:25", "EVE Online - The Green Nebula.mp3"),
            NetworkTrack("Primordial Star Clouds", "4:09", "EVE Online - Primordial Star Clouds.mp3"),
            NetworkTrack("The Jovian Front", "5:14", "EVE Online - The Jovian Front.mp3"),
            NetworkTrack("My Other Residency", "4:54", "EVE Online - My Other Residency.mp3"),
            NetworkTrack("Merchants, Looters and Ghosts", "6:35", "EVE Online - Merchants, Looters and Ghosts.mp3"),
            NetworkTrack("Safe Trade Routes", "5:44", "EVE Online - Safe Trade Routes.mp3"),
            NetworkTrack("Last Power Cell", "4:54", "EVE Online - Last Power Cell.mp3"),
            NetworkTrack("EVE of Battle", "5:40", "EVE Online - EVE of Battle.mp3"),
            NetworkTrack("Nouvelle Rouvenor Hero", "5:09", "EVE Online - Nouvelle Rouvenor Hero.mp3"),
            NetworkTrack("Inter-Spatial-Flexure", "6:06", "EVE Online - Inter-Spatial-Flexure.mp3"),
            NetworkTrack("Infinite Distortion of Space And Time", "7:24", "EVE Online - Infinite Distortion of Space And Time.mp3"),
            NetworkTrack("Tomorrows Neverending Yesterday", "5:54", "EVE Online - Tomorrows Neverending Yesterday.mp3"),
            NetworkTrack("Gallentean Refuge", "5:15", "EVE Online - Gallentean Refuge.mp3"),
            NetworkTrack("Love, Honour and Obey", "5:40", "EVE Online - Love, Honour and Obey.mp3"),
            NetworkTrack("Everyday Zone", "4:59", "EVE Online - Everyday Zone.mp3"),
            NetworkTrack("Object Almost Accomplished", "7:41", "EVE Online - Object Almost Accomplished.mp3"),
            NetworkTrack("Ride Out The Storm", "6:25", "EVE Online - Ride Out The Storm.mp3"),
            NetworkTrack("At the End of Your Journey", "4:56", "EVE Online - At the End of Your Journey.mp3"),
            NetworkTrack("Do You Know Where You Are", "5:49", "EVE Online - Do You Know Where You Are.mp3"),
            NetworkTrack("Home at Last", "4:28", "EVE Online - Home at Last.mp3"),
            NetworkTrack("Miners Heaven", "5:30", "EVE Online - Miners Heaven.mp3"),
            NetworkTrack("Quaesitum Finished", "4:37", "EVE Online - Quaesitum Finished.mp3"),
            NetworkTrack("My Own Binary System", "5:22", "EVE Online - My Own Binary System.mp3"),
            NetworkTrack("Something Old, Something New", "4:56", "EVE Online - Something Old, Something New.mp3"),
            NetworkTrack("I Saw Your Ship", "4:55", "EVE Online - I Saw Your Ship.mp3"),
            NetworkTrack("We Fight Proud For The Holder", "4:21", "EVE Online - We Fight Proud For The Holder.mp3"),
            NetworkTrack("The Hunt", "5:20", "EVE Online - The Hunt.mp3"),
            NetworkTrack("Amongst Allies", "5:36", "EVE Online - Amongst Allies.mp3"),
            NetworkTrack("Theme Of The Universe", "6:26", "EVE Online - Theme Of The Universe.mp3"),
            NetworkTrack("Defenders Of The Orphic World", "5:13", "EVE Online - Defenders Of The Orphic World.mp3"),
            NetworkTrack("Treasure the Obscure", "5:25", "EVE Online - Treasure the Obscure.mp3"),
            NetworkTrack("Rose of Victory", "5:46", "EVE Online - Rose of Victory.mp3"),
            NetworkTrack("It's Full Of Stars", "5:00", "EVE Online - It's Full Of Stars.mp3"),
            NetworkTrack("Smoke From Down Below", "5:23", "EVE Online - Smoke From Down Below.mp3"),
            NetworkTrack("...But Still We Go On", "8:08", "EVE Online - ...But Still We Go On.mp3"),
            NetworkTrack("In The Depth Of Space", "4:58", "EVE Online - In The Depth Of Space.mp3"),
            NetworkTrack("Minmatar Rebel Alliance", "5:01", "EVE Online - Minmatar Rebel Alliance.mp3"),
            NetworkTrack("All Which Was Lost Has Now Been Regained", "5:46", "EVE Online - All Which Was Lost Has Now Been Regained.mp3"),
            NetworkTrack("Borderlines", "4:56", "EVE Online - Borderlines.mp3"),
            NetworkTrack("Wonderlands Of The Mind", "5:00", "EVE Online - Wonderlands Of The Mind.mp3"),
            NetworkTrack("Omens", "5:04", "EVE Online - Omens.mp3"),
            NetworkTrack("The Solitary Pilot", "5:30", "EVE Online - The Solitary Pilot.mp3"),
            NetworkTrack("Times Of Sanguinity", "5:21", "EVE Online - Times Of Sanguinity.mp3"),
            NetworkTrack("Doomed Forever", "5:00", "EVE Online - Doomed Forever.mp3"),
            NetworkTrack("Path Of The Cursed", "5:25", "EVE Online - Path Of The Cursed.mp3"),
            NetworkTrack("On The Outskirts", "5:42", "EVE Online - On The Outskirts.mp3"),
            NetworkTrack("Forgotten Places", "5:36", "EVE Online - Forgotten Places.mp3"),
            NetworkTrack("Precious Ore", "5:01", "EVE Online - Precious Ore.mp3"),
            NetworkTrack("It Ends Here", "5:18", "EVE Online - It Ends Here.mp3"),
            NetworkTrack("Shifting The Balance Of Power", "5:00", "EVE Online - Shifting The Balance Of Power.mp3"),
            NetworkTrack("Where Evil Lurks", "5:22", "EVE Online - Where Evil Lurks.mp3"),
            NetworkTrack("Point Of No Return", "5:03", "EVE Online - Point Of No Return.mp3"),
            NetworkTrack("Theme From Jita", "5:18", "EVE Online - Theme From Jita.mp3"),
            NetworkTrack("New Moon", "5:21", "EVE Online - New Moon.mp3"),
            NetworkTrack("Seek And You Shall Find", "5:18", "EVE Online - Seek And You Shall Find.mp3"),
            NetworkTrack("Learning From The Past", "4:54", "EVE Onilne - Learning From The Past.mp3"),
            NetworkTrack("Close To A Holy Place", "5:45", "EVE Online - Close To A Holy Place.mp3"),
            NetworkTrack("Pirates Den", "5:06", "EVE Online - Pirates Den.mp3"),
            NetworkTrack("Unidentified Phenomenon", "4:32", "EVE Online - Unidentified Phenomenon.mp3"),
            NetworkTrack("Gas Giant", "4:54", "EVE Online - Gas Giant.mp3"),
            NetworkTrack("The Day After The Storm", "5:00", "EVE Online - The Day After The Storm.mp3"),
            NetworkTrack("Is Anybody Out There", "4:57", "EVE Online - Is Anybody Out There.mp3"),
            NetworkTrack("Redesigned Stars", "3:51", "EVE Online - Redesigned Stars.mp3"),
            NetworkTrack("Bonanza Excavation", "4:05", "EVE Online - Bonanza Excavation.mp3"),
            NetworkTrack("Stellar Shadows", "5:59", "EVE Online - Stellar Shadows.mp3"),
        ).map {
            val url = "https://riftforeve.online/media/jukebox/soundtrack/${it.filename}".encodeURLPath()
            Track(UUID.randomUUID(), it.title, parseDuration(it.duration), Source.Network(url))
        }
    }

    private fun getTrailerTracks(): List<Track> {
        return listOf(
            NetworkTrack("Dominion Trailer", "3:42", "EVE Online - Dominion Trailer Music (Adam Skorupa and Pawel Blaszczak).mp3"),
            NetworkTrack("Causality Video", "2:55", "EVE Online - Causality Video Music (Adam Skorupa and Pawel Blaszczak).mp3"),
            NetworkTrack("Tyrannis Trailer", "1:24", "EVE Online - Tyrannis Trailer Music (Adam Skorupa and Pawel Blaszczak).mp3"),
            NetworkTrack("Trinity Trailer", "2:14", "EVE Online - Trinity Trailer Music (Adam Skorupa and Pawel Blaszczak).mp3"),
            NetworkTrack("Crucible Trailer", "1:21", "EVE Online - Crucible Trailer Music (HaZaR).mp3"),
            NetworkTrack("Alliance Tournament X", "0:57", "EVE Online Alliance Tournament X  Intro Music (Adam Skorupa).mp3"),
            NetworkTrack("Odyssey Trailer", "1:30", "EVE Online - Odyssey Trailer Music (Adam Skorupa).mp3"),
            NetworkTrack("Empyrean Age Trailer", "2:11", "EVE Online - Empyrean Age Trailer Music (Adam Skorupa and Pawel Blaszczak).mp3"),
            NetworkTrack("Inferno Trailer", "1:15", "EVE Online - Inferno Trailer Music (HaZaR).mp3"),
            NetworkTrack("Revelations I", "1:37", "EVE Online - Revelations I Trailer Music (Adam Skorupa and Pawel Blaszczak).mp3"),
            NetworkTrack("CDIA Video Music", "1:48", "EVE Online - CDIA Video Music (Adam Skorupa and Pawel Blaszczak).mp3"),
            NetworkTrack("Origins Trailer", "3:34", "EVE Universe - Origins Trailer Music (Adam Skorupa).mp3"),
            NetworkTrack("Birth Of The Capsuleer", "2:12", "EVE Online - Birth Of The Capsuleer (Joshua Crispin).mp3"),
            NetworkTrack("Crimson Harvest", "4:00", "EVE Online - Crimson Harvest (Dark Vibra).mp3"),
            NetworkTrack("Emergent Threats - EVE Fanfest 2015", "2:09", "EVE Online - Emergent Threats - EVE Fanfest 2015 Trailer Music.mp3"),
            NetworkTrack("This is EVE (2014)", "3:42", "EVE Online - This is EVE (2014) - Trailer Music (Music Imaginary).mp3"),
        ).map {
            val url = "https://riftforeve.online/media/jukebox/trailer/${it.filename}".encodeURLPath()
            Track(UUID.randomUUID(), it.title, parseDuration(it.duration), Source.Network(url))
        }
    }

    private fun getPermabandTracks(): List<Track> {
        return listOf(
            NetworkTrack("HTFU", "3:59", "Permaband - HTFU.mp3"),
            NetworkTrack("Keep Clickin", "3:23", "Permaband - Keep Clickin.mp3"),
            NetworkTrack("Killing Is Just A Means", "4:41", "Permaband - Killing Is Just A Means.mp3"),
            NetworkTrack("Wrecking Machine", "3:56", "Permaband - Wrecking Machine.mp3"),
            NetworkTrack("Warp To The Dance Floor", "2:23", "Permaband – Warp To The Dance Floor.mp3"),
            NetworkTrack("Fly With Us", "4:29", "Permaband - Fly With Us.mp3"),
        ).map {
            val url = "https://riftforeve.online/media/jukebox/permaband/${it.filename}".encodeURLPath()
            Track(UUID.randomUUID(), it.title, parseDuration(it.duration), Source.Network(url))
        }
    }

    private fun parseDuration(duration: String): Duration {
        val (minutes, seconds) = duration.split(":")
        val totalSeconds = (minutes.toInt() * 60) + seconds.toInt()
        return Duration.ofSeconds(totalSeconds.toLong())
    }
}
