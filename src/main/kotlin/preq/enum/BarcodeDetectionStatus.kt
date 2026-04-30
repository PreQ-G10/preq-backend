package preq.enum

enum class BarcodeDetectionStatus {
    FOUND, // barcode already in DB
    CREATED, // no collision, created from API
    COLLISION, // manual product matches API result
    NOT_FOUND, // not in DB nor API
    INCOMPLETE_DATA, // found in API but data is unreliable
}
