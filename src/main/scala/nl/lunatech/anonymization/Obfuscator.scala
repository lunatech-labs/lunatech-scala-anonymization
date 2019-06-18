package nl.lunatech.anonymization

import org.platanios.tensorflow.api.core.client.Session
import org.platanios.tensorflow.api.core.{NewAxis, Shape}
import org.platanios.tensorflow.api.tensors.Tensor
import org.platanios.tensorflow.api.{---, Output, UByte, tf}
import smile.stat.distribution.GaussianDistribution
import java.util.Arrays.fill
import scala.math.{abs, sqrt}

/** An Obfuscator used to blur images
  *
  *  @constructor create a new obfuscator with blurring depending on its parameters
  *  @param kernel_size size of the gaussian kernel used to blur the image. Need to be an odd number
  *  @param sigma standard deviation for our gaussian kernel
  *  @param box_kernel_size size of the other kernel used to obtain smooth results. Need to be an odd number
  */
class Obfuscator(private val kernel_size: Int = 21, private val sigma: Int = 2, private val box_kernel_size: Int = 9) {
    import Obfuscator._
    private val pad = (kernel_size - 1) / 2
    private val paddings = Tensor(Tensor(pad, pad), Tensor(pad, pad), Tensor(0, 0))
    // Kernel used to blur the image
    private val kernelGaussian: Tensor[Float] = gaussianKernel(kernel_size, sigma)
    // Kernel used to smooth the transition between blur areas and normal areas
    private val kernelMean: Tensor[Float] = smoothingKernel(box_kernel_size)

    /** Blur an image according to a list of detection boxes
      *
      *  @param image UByte tensor representing the current image
      *  @param boxes list of detection boxes found by the detectors
      *
      *  @return UByte tensor representing the image blurred
      */
    def blur(image: Tensor[UByte], boxes: IndexedSeq[(Int, Float, (Int, Int, Int, Int))]): Tensor[UByte] = {
        val mask = generateMask(image.shape(0), image.shape(1), boxes)
        val imageWithPadding = tf.pad(image, paddings=paddings, mode=tf.ReflectivePadding).toFloat
        val tabImages = (0 to 2).map(i => tf.slice(imageWithPadding, Tensor(0, 0, i), Tensor(-1, -1, 1)).toFloat)
        val tabImages4D = tabImages.map(img => img.slice(NewAxis, ---))
        val tabConvolution = tabImages4D.map(colors => tf.conv2D(input = colors, filter = kernelGaussian, stride1 = 1, stride2 = 1, padding=org.platanios.tensorflow.api.ops.NN.ValidConvPadding))
        val imageAfterConvolution = tf.concatenate(inputs = tabConvolution, axis = 3)
        val smoothedMask = tf.reshape(tf.conv2D(input = mask.toFloat, filter = kernelMean, stride1 = 1, stride2 = 1, padding=org.platanios.tensorflow.api.ops.NN.SameConvPadding, name="smooth_mask"), Shape(image.shape(0), image.shape(1), 1))
        val reshapeBlur = tf.squeeze(imageAfterConvolution)
        val imageWithoutBox = image.toFloat * (Tensor(1.0f) - smoothedMask)
        val imageCombined = ((reshapeBlur * smoothedMask) + imageWithoutBox).toUByte

        Session().run(fetches = imageCombined)
    }

}

object Obfuscator {

    // Normal Distribution used to generate our gaussian kernel
    private val normalDistribution = new GaussianDistribution(0, 1)

    /** Generate a gaussian kernel. This kernel is independent of the image dimensions so with just need to generate it once.
      *
      *  @param kernelSize size of the kernel (e.g. 21 = 21 pixels). Need to be an odd number
      *  @param sigma standard deviation
      *
      *  @return Float tensor containing our gaussian kernel.
      */
    private def gaussianKernel(kernelSize: Int, sigma: Int): Tensor[Float] = {
        val interval: Double = (2 * sigma + 1.0) / kernelSize
        val start = -sigma -interval/2.0
        val step = (2.0 * sigma + interval) / kernelSize
        val cdfStep = (0 to kernelSize).map(i => normalDistribution.cdf(start + i*step))
        val kernel1d = cdfStep.sliding(2).map { case Seq(x, y) => y - x }.toVector
        val kernel1dSqrt = for {
            x <- kernel1d
            y <- kernel1d
        } yield sqrt(x*y)
        val sumKernel1d = kernel1dSqrt.sum
        val kernel = kernel1dSqrt.map(_ / sumKernel1d)
        Session().run(fetches=tf.reshape(kernel, Shape(kernelSize, kernelSize, 1, 1)).toFloat)
    }

    /** Generate a mean kernel used to smooth the transition between blur and normal area.
      * This kernel is independant of the image dimensions so with just need to generate it once.
      *
      *  @param kernelSize size of the kernel. Need to be an odd number
      *
      *  @return Float tensor containing our mean kernel.
      */
    private def smoothingKernel(kernelSize: Int): Tensor[Float] = {
        val factor = (kernelSize + 1) / 2
        val center = if (kernelSize % 2 == 1) factor - 1 else factor - 0.5

        val meanKernel = for {
            i <- 0 until kernelSize
            j <- 0 until kernelSize
        } yield (1.0f - abs(i - center).toFloat / factor.toFloat) * (1.0f - abs(j - center).toFloat / factor.toFloat)

        val sumSmoothing = meanKernel.sum
        val kernel = Tensor(meanKernel.map(element => element / sumSmoothing))
        Session().run(fetches = tf.reshape(kernel, Shape(kernelSize, kernelSize, 1, 1)))
    }

    /** Generate a mask.
      * The size of this mask is the same than the current image.
      * The mask is filled with only 0 or 1.
      * 0 represents an area who doesn't need to be blur.
      * 1 is the opposite.
      *
      *  @param height of the current image
      *  @param width of the current image
      *  @param boxes list with detection boxes for the current image
      *
      *  @return Float Output containing our mask.
      */
    private def generateMask(height: Int, width: Int, boxes: IndexedSeq[(Int, Float, (Int, Int, Int, Int))]): Output[Int] = {
        val maskBlur = Array.fill(height, width)(0)
        boxes.foreach { case (_, _, (x1,y1,x2,y2)) => (x1 until x2).foreach(xi => fill(maskBlur(xi), y1, y2, 1)) }
        val maskBlurFlatten = maskBlur.flatten
        val tensorMaskFlatten = Tensor(maskBlurFlatten)
        val tensorMaskReshape = tf.reshape(tensorMaskFlatten, Shape(height, width))
        tensorMaskReshape.slice(NewAxis, ---, NewAxis)
    }
}
