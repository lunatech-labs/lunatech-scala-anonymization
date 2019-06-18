package nl.lunatech.anonymization

import java.io.{BufferedOutputStream, File, FileOutputStream}
import org.platanios.tensorflow.api.{Tensor, tf}
import org.platanios.tensorflow.api.core.client.Session
import org.platanios.tensorflow.api.core.types.UByte
import org.platanios.tensorflow.api.ops.{Files, Image}

/** Anonymizer which apply detection with all the detectors in parameters and blurring thanks to its obfuscator.
  *
  *  @constructor create a new anonymizer with a list of detectors and an obfuscator
  *  @param detectors list of detectors used one after the other
  *  @param obfuscator use to blur the image
  */
class Anonymizer(private val detectors: IndexedSeq[Detector], private val obfuscator: Obfuscator) {

    /** Open an image file and transform it into a UByte tensor
      *
      *  @param imageFilePath path to the image to transform
      *  @param numChannel channel number of the image (e.g. 1=grayscale, 3=color, ...)
      *  @return tensor UByte corresponding to the parameter path image
      */
    private def transformImages(imageFilePath: File, numChannel: Int = 3): Tensor[UByte] = {
        val tensorImageOuts = Image.decodePng(Files.readFile(imageFilePath.getAbsolutePath), numChannel)
        Session().run(fetches = tensorImageOuts).toUByte
    }

    /** Anonymized an images folder and save results to outputPath folder
      *
      *  @param imagesPath path to the images folder to anonymize
      *  @param outputPath path where all results will be saved
      */
    def anonymizeImages(imagesPath: IndexedSeq[File], outputPath: File): Unit = {
        imagesPath.zipWithIndex.foreach { case (imagePath, index) =>
            println(s"Anonymization: ${imagePath.getName} - Image ${index+1}/${imagesPath.length}")
            val tensorImage = transformImages(imagePath)
            val boxes = detectors.flatMap(_.detect(tensorImage))
            val tensorAnonymizedImage = obfuscator.blur(tensorImage, boxes)
            val anonymizedImage = tf.createWith() {
                val exampleImage = tf.decodeRaw[Byte](tf.image.encodePng(tensorAnonymizedImage))
                Session().run(fetches = exampleImage)
            }
            val out = new BufferedOutputStream(new FileOutputStream(new File(s"$outputPath/anonymized_${imagePath.getName}")))
            out.write(anonymizedImage.entriesIterator.toArray)
        }
        println(s"Results saved into $outputPath")
    }
}

