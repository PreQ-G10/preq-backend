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
import java.nio.file.Paths

@Service
class ImageEmbeddingService {
    private lateinit var ortEnv: OrtEnvironment
    private lateinit var ortSession: OrtSession
    private lateinit var embeddingModel: ZooModel<Image, FloatArray>
    private lateinit var embeddingPredictor: Predictor<Image, FloatArray>

    @Value("\${u2net.model.path}")
    private lateinit var modelPath: String

    @Value("\${resnet.model.path}")
    private lateinit var resnetModelPath: String

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

        val resnetFile = java.io.File(resnetModelPath)
        if (!resnetFile.exists()) {
            resnetFile.parentFile.mkdirs()
            val gzUrl =
                java.net
                    .URI(
                        "https://djl-ai.s3.amazonaws.com/mlrepo/model/cv/image_classification/ai/djl/pytorch/resnet/0.0.1/traced_resnet50.pt.gz",
                    ).toURL()
            gzUrl.openStream().use { gzInput ->
                java.util.zip.GZIPInputStream(gzInput).use { input ->
                    resnetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }

        ortSession = ortEnv.createSession(modelPath, OrtSession.SessionOptions())

        val embeddingCriteria =
            Criteria
                .builder()
                .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                .setTypes(Image::class.java, FloatArray::class.java)
                .optEngine("PyTorch")
                .optModelPath(Paths.get(resnetModelPath).parent)
                .optModelName("traced_resnet50")
                .optTranslator(EmbeddingTranslator())
                .optProgress(ProgressBar())
                .build()

        embeddingModel = embeddingCriteria.loadModel()
        embeddingPredictor = embeddingModel.newPredictor()
    }

    private fun removeBackground(image: Image): Image {
        val originalImage = image.wrappedImage as BufferedImage
        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        val size = 320
        val resized = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val graphics = resized.createGraphics()
        graphics.drawImage(originalImage, 0, 0, size, size, null)
        graphics.dispose()

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        val inputData = FloatArray(3 * size * size)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val rgb = resized.getRGB(x, y)
                val red = ((rgb shr 16) and 0xFF) / 255f
                val green = ((rgb shr 8) and 0xFF) / 255f
                val blue = ((rgb) and 0xFF) / 255f
                inputData[0 + y * size + x] = (red - mean[0]) / std[0]
                inputData[1 * size * size + y * size + x] = (green - mean[1]) / std[1]
                inputData[2 * size * size + y * size + x] = (blue - mean[2]) / std[2]
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
            for (maskValue in row) {
                if (maskValue < minVal) minVal = maskValue
                if (maskValue > maxVal) maxVal = maskValue
            }
        }
        val range = maxVal - minVal

        val output = BufferedImage(originalWidth, originalHeight, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until originalHeight) {
            for (x in 0 until originalWidth) {
                val maskX = (x.toFloat() / originalWidth * size).toInt().coerceIn(0, size - 1)
                val maskY = (y.toFloat() / originalHeight * size).toInt().coerceIn(0, size - 1)
                val maskVal = (outputData[maskY][maskX] - minVal) / range
                if (maskVal > 0.5f) {
                    output.setRGB(x, y, originalImage.getRGB(x, y))
                } else {
                    output.setRGB(x, y, 0xFFFFFFFF.toInt())
                }
            }
        }

        val nonWhiteColumns =
            (0 until originalWidth).filter { x ->
                (0 until originalHeight).any { y -> output.getRGB(x, y) != 0xFFFFFFFF.toInt() }
            }
        val nonWhiteRows =
            (0 until originalHeight).filter { y ->
                (0 until originalWidth).any { x -> output.getRGB(x, y) != 0xFFFFFFFF.toInt() }
            }

        if (nonWhiteColumns.isEmpty() || nonWhiteRows.isEmpty()) return image

        val cropped =
            output.getSubimage(
                nonWhiteColumns.first(),
                nonWhiteRows.first(),
                nonWhiteColumns.last() - nonWhiteColumns.first() + 1,
                nonWhiteRows.last() - nonWhiteRows.first() + 1,
            )
        return ImageFactory.getInstance().fromImage(cropped)
    }

    fun generateEmbedding(file: MultipartFile): FloatArray {
        require(file.contentType?.startsWith("image/") == true) {
            "File must be an image, got: ${file.contentType}"
        }
        val originalImage =
            ByteArrayInputStream(file.bytes).use {
                ImageFactory.getInstance().fromInputStream(it)
            }
        val backgroundRemovedImage = removeBackground(originalImage)
        return embeddingPredictor.predict(backgroundRemovedImage)
    }

    @PreDestroy
    fun cleanup() {
        if (::ortSession.isInitialized) ortSession.close()
        if (::ortEnv.isInitialized) ortEnv.close()
        if (::embeddingPredictor.isInitialized) embeddingPredictor.close()
        if (::embeddingModel.isInitialized) embeddingModel.close()
    }
}
