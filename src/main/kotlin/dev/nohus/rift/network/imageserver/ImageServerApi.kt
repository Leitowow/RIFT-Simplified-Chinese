package dev.nohus.rift.network.imageserver

import dev.nohus.rift.network.requests.Originator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Response
import retrofit2.Retrofit
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Single
class ImageServerApi(
    @Named("api") client: OkHttpClient,
) {

    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://images.evetech.net")
        .build()
    private val service = retrofit.create(ImageServerService::class.java)

    suspend fun headCharacterPortrait(originator: Originator, characterId: Int): Response<Void> {
        return service.headCharacterPortrait(originator, characterId)
    }

    suspend fun getCharacterPortrait(originator: Originator, size: Int, characterId: Int): BufferedImage? {
        val bytes = service.getCharacterPortrait(originator, characterId, size).body()?.bytes() ?: return null
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(bytes))
        }
    }

    suspend fun getCharacterPortraitOpenCv(originator: Originator, size: Int, characterId: Int): ByteArray? {
        return service.getCharacterPortrait(originator, characterId, size).body()?.bytes()
    }

    suspend fun getAllianceLogo(originator: Originator, allianceId: Int, size: Int): BufferedImage? {
        val bytes = service.getAllianceLogo(originator, allianceId, size).body()?.bytes() ?: return null
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(bytes))
        }
    }

    suspend fun getCorporationLogo(originator: Originator, corporationId: Int, size: Int): BufferedImage? {
        val bytes = service.getCorporationLogo(originator, corporationId, size).body()?.bytes() ?: return null
        return withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(bytes))
        }
    }
}
