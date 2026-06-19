package preq.exceptions

class ProductNotInCatalogueException(
    productIds: Set<Long>,
) : RuntimeException("Products not found in catalogue: $productIds")
