package preq.web.dto.projection

interface SimilarProductResult {
    fun getProductId(): Long

    fun getSimilarity(): Double
}
