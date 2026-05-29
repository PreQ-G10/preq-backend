package preq.web.dto.request

import preq.enum.FieldType

data class ContestProductFieldRequest(
    val fieldType: FieldType,
    val fieldValue: String,
)
