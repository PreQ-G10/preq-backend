package preq.config

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "cloudinary")
@org.springframework.stereotype.Component
data class CloudinaryProperties(
    var url: String = ""
)