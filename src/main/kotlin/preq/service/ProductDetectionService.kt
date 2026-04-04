package preq.service

import ai.djl.Application
import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.output.DetectedObjects
import ai.djl.modality.cv.output.Mask
import ai.djl.modality.cv.transform.Normalize
import ai.djl.modality.cv.transform.Resize
import ai.djl.modality.cv.transform.ToTensor
import ai.djl.ndarray.NDList
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import kotlin.math.sqrt

@Service
class ProductDetectionService() {

    private lateinit var detectionModel: ZooModel<Image, DetectedObjects>
    private lateinit var detectionPredictor: Predictor<Image, DetectedObjects>
    private lateinit var embeddingModel: ZooModel<Image, FloatArray>
    private lateinit var embeddingPredictor: Predictor<Image, FloatArray>

    private inner class EmbeddingTranslator : Translator<Image, FloatArray> {
        override fun processInput(ctx: TranslatorContext, input: Image): NDList {
            var array = input.toNDArray(ctx.ndManager, Image.Flag.COLOR)
            array = Resize(224, 224).transform(array)
            array = ToTensor().transform(array)
            array = Normalize(
                floatArrayOf(0.485f, 0.456f, 0.406f),
                floatArrayOf(0.229f, 0.224f, 0.225f)
            ).transform(array)
            return NDList(array)
        }

        override fun processOutput(ctx: TranslatorContext, list: NDList): FloatArray {
            return list.singletonOrThrow().toFloatArray()
        }
    }

    @PostConstruct
    fun init() {
        val detectionCriteria = Criteria.builder()
            .optApplication(Application.CV.OBJECT_DETECTION)
            .setTypes(Image::class.java, DetectedObjects::class.java)
            .optEngine("PyTorch")
            .optArgument("translatorFactory", "ai.djl.modality.cv.translator.YoloV8TranslatorFactory")
            .optProgress(ProgressBar())
            .build()

        detectionModel = detectionCriteria.loadModel()
        detectionPredictor = detectionModel.newPredictor()

        val embeddingCriteria = Criteria.builder()
            .optApplication(Application.CV.IMAGE_CLASSIFICATION)
            .setTypes(Image::class.java, FloatArray::class.java)
            .optEngine("PyTorch")
            .optArgument("translatorFactory", "ai.djl.modality.cv.translator.ImageFeatureExtractorFactory")
            .optTranslator(EmbeddingTranslator())
            .optProgress(ProgressBar())
            .build()

        embeddingModel = embeddingCriteria.loadModel()
        embeddingPredictor = embeddingModel.newPredictor()
    }

    fun generateEmbedding(file: MultipartFile): FloatArray {
        require(file.contentType?.startsWith("image/") == true) {
            "File must be an image, got: ${file.contentType}"
        }
        val image = ByteArrayInputStream(file.bytes).use {
            ImageFactory.getInstance().fromInputStream(it)
        }
        val cropped = cropProduct(image)
        cropped.save(java.io.FileOutputStream("${System.getProperty("user.home")}/Desktop/output.jpeg"), "jpeg")
        return embeddingPredictor.predict(cropped)
    }

    private fun cropProduct(image: Image): Image {
        val detections = detectionPredictor.predict(image)
        if (detections.numberOfObjects == 0) return image

        val best = detections.items<DetectedObjects.DetectedObject>()
            .maxBy { it.probability }

        val buf = image.wrappedImage as BufferedImage
        val bounds = best.boundingBox.bounds

        val x = (bounds.x * buf.width).toInt().coerceAtLeast(0)
        val y = (bounds.y * buf.height).toInt().coerceAtLeast(0)
        val w = (bounds.width * buf.width).toInt().coerceAtMost(buf.width - x)
        val h = (bounds.height * buf.height).toInt().coerceAtMost(buf.height - y)

        // white out everything outside the bounding box
        val output = BufferedImage(buf.width, buf.height, BufferedImage.TYPE_INT_RGB)
        val g = output.createGraphics()
        g.color = java.awt.Color.WHITE
        g.fillRect(0, 0, buf.width, buf.height)
        g.drawImage(buf.getSubimage(x, y, w, h), x, y, null)
        g.dispose()

        // tight crop
        val cropped = output.getSubimage(x, y, w, h)
        return ImageFactory.getInstance().fromImage(cropped)
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size)
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return dot / (sqrt(normA) * sqrt(normB))
    }

    @PreDestroy
    fun cleanup() {
        if (::detectionPredictor.isInitialized) detectionPredictor.close()
        if (::embeddingPredictor.isInitialized) embeddingPredictor.close()
        if (::detectionModel.isInitialized) detectionModel.close()
        if (::embeddingModel.isInitialized) embeddingModel.close()
    }
}