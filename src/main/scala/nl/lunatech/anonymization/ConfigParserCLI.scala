package nl.lunatech.anonymization

/** Configuration for CLI parser
  *
  *  @constructor create a new config file.
  *  @param inputPath       path to input folder with all images to anonymized
  *  @param outputPath      path to save all the results
  *  @param imageExtensions image's extensions allowed
  *  @param faceThreshold   threshold for face detection
  *  @param plateThreshold  threshold for plate detection
  */
case class ConfigParserCLI(inputPath: String = "",
                           outputPath: String = "",
                           imageExtensions: Seq[String] = Seq("png", "jpg"),
                           faceThreshold: Double = 0.7,
                           plateThreshold: Double = 0.7,
                           kernelSize: Int = 21,
                           sigma: Int = 2,
                           boxKernelSize: Int = 9)
