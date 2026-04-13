package preq.service

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import preq.config.CloudinaryProperties

@Service
class CloudinaryService(private val props: CloudinaryProperties) {
    private val cloudinary = Cloudinary(props.url)

    fun upload(file: MultipartFile): String {
        val result = cloudinary.uploader().upload(file.bytes, ObjectUtils.emptyMap())
        return result["secure_url"] as String
    }

    fun delete(imageUrl: String) {
        val publicId = imageUrl.substringAfterLast("/").substringBeforeLast(".")
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())
    }
}