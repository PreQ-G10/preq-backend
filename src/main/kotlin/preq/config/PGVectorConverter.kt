package preq.config

import com.pgvector.PGvector
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class PGVectorConverter : AttributeConverter<FloatArray, PGvector> {
    override fun convertToDatabaseColumn(attribute: FloatArray?): PGvector? {
        return attribute?.let { PGvector(it) }
    }

    override fun convertToEntityAttribute(dbData: PGvector?): FloatArray? {
        return dbData?.toArray()
    }
}