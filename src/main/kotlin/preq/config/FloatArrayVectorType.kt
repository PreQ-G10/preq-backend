package preq.config

import com.pgvector.PGvector
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType
import org.postgresql.util.PGobject
import java.io.Serializable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class FloatArrayVectorType : UserType<FloatArray> {
    override fun getSqlType() = Types.OTHER

    override fun returnedClass() = FloatArray::class.java

    override fun nullSafeGet(
        rs: ResultSet,
        position: Int,
        session: SharedSessionContractImplementor,
        owner: Any?,
    ): FloatArray? {
        val obj = rs.getObject(position) ?: return null
        return when (obj) {
            is PGvector -> obj.toArray()
            is PGobject -> PGvector(obj.value).toArray()
            else -> throw ClassCastException("Cannot convert ${obj::class.java} to PGvector")
        }
    }

    override fun nullSafeSet(
        st: PreparedStatement,
        value: FloatArray?,
        index: Int,
        session: SharedSessionContractImplementor,
    ) {
        if (value == null) {
            st.setNull(index, Types.OTHER)
        } else {
            st.setObject(index, PGvector(value), Types.OTHER)
        }
    }

    override fun equals(
        x: FloatArray?,
        y: FloatArray?,
    ): Boolean {
        return x?.contentEquals(y ?: return x == null) ?: (y == null)
    }

    override fun hashCode(x: FloatArray) = x.contentHashCode()

    override fun deepCopy(value: FloatArray?) = value?.copyOf()

    override fun isMutable() = true

    override fun disassemble(value: FloatArray?) = value?.copyOf()

    override fun assemble(
        cached: Serializable?,
        owner: Any?,
    ) = cached as? FloatArray
}
