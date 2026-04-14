package preq.service

import ai.djl.Application
import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.modality.cv.ImageFactory
import ai.djl.modality.cv.transform.Normalize
import ai.djl.modality.cv.transform.Resize
import ai.djl.modality.cv.transform.ToTensor
import ai.djl.ndarray.NDList
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.FloatBuffer

@Service
class ImageEmbeddingService {
    private lateinit var ortEnv: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private lateinit var embeddingModel: ZooModel<Image, FloatArray>
    private lateinit var embeddingPredictor: Predictor<Image, FloatArray>

    @Value("\${u2net.model.path}")
    private lateinit var modelPath: String

    private class EmbeddingTranslator : Translator<Image, FloatArray> {
        override fun processInput(
            ctx: TranslatorContext,
            input: Image,
        ): NDList {
            var array = input.toNDArray(ctx.ndManager, Image.Flag.COLOR)
            array = Resize(224, 224).transform(array)
            array = ToTensor().transform(array)
            array =
                Normalize(
                    floatArrayOf(0.485f, 0.456f, 0.406f),
                    floatArrayOf(0.229f, 0.224f, 0.225f),
                ).transform(array)
            return NDList(array)
        }

        override fun processOutput(
            ctx: TranslatorContext,
            list: NDList,
        ): FloatArray = list.singletonOrThrow().toFloatArray()
    }

    @PostConstruct
    fun init() {
        ortEnv = OrtEnvironment.getEnvironment()
        val modelFile = java.io.File(modelPath)

        if (!modelFile.exists()) {
            modelFile.parentFile.mkdirs()
            java.net
                .URI("https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2net.onnx")
                .toURL()
                .openStream()
                .use { input -> modelFile.outputStream().use { output -> input.copyTo(output) } }
        }

        ortSession = ortEnv.createSession(modelPath, OrtSession.SessionOptions())

        val embeddingCriteria =
            Criteria
                .builder()
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .setTypes(Image::class.java, FloatArray::class.java)
                .optEngine("PyTorch")
                .optArgument("name", "traced_resnet50")
                .optTranslator(EmbeddingTranslator())
                .optProgress(ProgressBar())
                .build()

        embeddingModel = embeddingCriteria.loadModel()
        embeddingPredictor = embeddingModel.newPredictor()
    }

    private fun removeBackground(image: Image): Image {
        val buf = image.wrappedImage as BufferedImage
        val ow = buf.width
        val oh = buf.height

        val size = 320
        val resized = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val g = resized.createGraphics()
        g.drawImage(buf, 0, 0, size, size, null)
        g.dispose()

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        val inputData = FloatArray(3 * size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val rgb = resized.getRGB(x, y)
                val r = ((rgb shr 16) and 0xFF) / 255f
                val gr = ((rgb shr 8) and 0xFF) / 255f
                val b = ((rgb) and 0xFF) / 255f
                inputData[0 * size * size + y * size + x] = (r - mean[0]) / std[0]
                inputData[1 * size * size + y * size + x] = (gr - mean[1]) / std[1]
                inputData[2 * size * size + y * size + x] = (b - mean[2]) / std[2]
            }
        }

        val inputTensor =
            OnnxTensor.createTensor(
                ortEnv,
                FloatBuffer.wrap(inputData),
                longArrayOf(1, 3, size.toLong(), size.toLong()),
            )
        val results = ortSession.run(mapOf("input.1" to inputTensor))
        val outputData = (results[0].value as Array<Array<Array<FloatArray>>>)[0][0]

        var minVal = Float.MAX_VALUE
        var maxVal = -Float.MAX_VALUE
        for (row in outputData) {
            for (v in row) {
                if (v < minVal) minVal = v
                if (v > maxVal) maxVal = v
            }
        }
        val range = maxVal - minVal

        val output = BufferedImage(ow, oh, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until oh) {
            for (x in 0 until ow) {
                val mx = (x.toFloat() / ow * size).toInt().coerceIn(0, size - 1)
                val my = (y.toFloat() / oh * size).toInt().coerceIn(0, size - 1)
                val maskVal = (outputData[my][mx] - minVal) / range
                if (maskVal > 0.5f) {
                    output.setRGB(x, y, buf.getRGB(x, y))
                } else {
                    output.setRGB(x, y, 0xFFFFFFFF.toInt())
                }
            }
        }

        val xs =
            (0 until ow).filter { x ->
                (0 until oh).any { y -> output.getRGB(x, y) != 0xFFFFFFFF.toInt() }
            }
        val ys =
            (0 until oh).filter { y ->
                (0 until ow).any { x -> output.getRGB(x, y) != 0xFFFFFFFF.toInt() }
            }

        if (xs.isEmpty() || ys.isEmpty()) return image

        val cropped =
            output.getSubimage(
                xs.first(),
                ys.first(),
                xs.last() - xs.first() + 1,
                ys.last() - ys.first() + 1,
            )
        return ImageFactory.getInstance().fromImage(cropped)
    }

    fun generateEmbedding(file: MultipartFile): FloatArray {
        require(file.contentType?.startsWith("image/") == true) {
            "File must be an image, got: ${file.contentType}"
        }
        val image =
            ByteArrayInputStream(file.bytes).use {
                ImageFactory.getInstance().fromInputStream(it)
            }
        val segmented = removeBackground(image)
        return embeddingPredictor.predict(segmented)
    }

    @PreDestroy
    fun cleanup() {
        if (::ortSession.isInitialized) ortSession.close()
        if (::ortEnv.isInitialized) ortEnv.close()
        if (::embeddingPredictor.isInitialized) embeddingPredictor.close()
        if (::embeddingModel.isInitialized) embeddingModel.close()
    }
}
