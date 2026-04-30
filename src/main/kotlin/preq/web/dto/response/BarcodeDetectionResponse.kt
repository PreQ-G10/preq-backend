package preq.web.dto.response

import preq.enum.BarcodeDetectionStatus

data class BarcodeDetectionResponse(
    val status: BarcodeDetectionStatus,
    val product: ProductResponse? = null,
    val apiProduct: ProductResponse? = null,
    val existingProduct: ProductResponse? = null,
)
