package nl.lunatech.anonymization

import java.io.{File, FileFilter}
import scopt.OParser

object Main extends App {

    val builder = OParser.builder[ConfigParserCLI]
    val parser1 = {
        import builder._
        OParser.sequence(
            head("Anonymize faces and license plates in a series of images."),
            programName("anonymization"),
            help("help").text("prints this usage text"),

            opt[String]("input")
                .required()
                .valueName("/path/to/input_folder")
                .action((x, c) => c.copy(inputPath = x))
                .text("Path to the folder that contains the images that should be anonymized."),

            opt[String]("output")
                .required()
                .valueName("/path/to/output_folder")
                .action((x, c) => c.copy(outputPath = x))
                .text("Path to the folder the anonymized images should be written to."),

            opt[Seq[String]]("image-extensions")
                .optional()
                .valueName("jpg,png...")
                .action((x, c) => c.copy(imageExtensions = x))
                .text("[Optional] Comma-separated list of file types that will be anonymized."),

            opt[Double]("face-threshold")
                .optional()
                .valueName("0.7")
                .action((x, c) => c.copy(faceThreshold = x))
                .validate( x =>
                    if (x >= 0.001 && x <= 1.0) success
                    else failure("Value <face-threshold> must be > 0.001 and < 1.0"))
                .text("[Optional] Detection confidence needed to anonymize a detected face. Must be in [0.001, 1.0]."),

            opt[Double]("plate-threshold")
                .optional()
                .valueName("0.7")
                .action((x, c) => c.copy(plateThreshold = x))
                .validate( x =>
                    if (x >= 0.001 && x <= 1.0) success
                    else failure("Value <plate-threshold> must be > 0.001 and < 1.0"))
                .text("[Optional] Detection confidence needed to anonymize a license plate. Must be in [0.001, 1.0].")
        )
    }

    OParser.parse(parser1, args, ConfigParserCLI()) match {
        case Some(config) =>
            new File(config.outputPath).mkdirs
            val filter = new FileFilter() {
                def accept(file: File): Boolean = { config.imageExtensions.exists( extension => file.getName.toLowerCase.endsWith(extension)) }
            }
            Option(new File(config.inputPath).listFiles(filter)) match {
                case Some(images) => {
                    val obfuscator = new Obfuscator(kernel_size = 65, sigma = 3, box_kernel_size = 19)
                    val detectors = Vector(new Detector("FACE", config.faceThreshold), new Detector("PLATE", config.plateThreshold))
                    new Anonymizer(detectors, obfuscator).anonymizeImages(images, new File(config.outputPath))
                }
                case None => println("Error with your input Path")
            }
        case _ =>  // parser1 will display error message
    }
}
