package preq.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import preq.web.dto.response.OpenFoodFactsResponse
import reactor.core.publisher.Mono

@Service
class BarcodeService(
    private val webClient: WebClient =
        WebClient
            .builder()
            .baseUrl("https://world.openfoodfacts.org/api/v2")
            .build(),
) {
    fun getProduct(barcode: String): OpenFoodFactsResponse? =
        webClient
            .get()
            .uri("/product/$barcode.json")
            .retrieve()
            .onStatus({ it.value() == 404 }) {
                Mono.error { NoSuchElementException() }
            }.bodyToMono(OpenFoodFactsResponse::class.java)
            .block()
}
