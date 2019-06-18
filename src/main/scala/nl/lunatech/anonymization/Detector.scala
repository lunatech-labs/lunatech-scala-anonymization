package nl.lunatech.anonymization

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}

import org.platanios.tensorflow.api.core.{Graph, NewAxis}
import org.platanios.tensorflow.api.core.client.{FeedMap, Session}
import org.platanios.tensorflow.api.{Tensor, UByte}
import org.tensorflow.framework.GraphDef
import org.platanios.tensorflow.api.---


/** A Detector to found detection boxes in function of the input kind.
  *
  *  @constructor create a new detector with a detection kind and threshold
  *  @param kind the person's name
  *  @param detectionThreshold detections above this threshold is conserved. Need to be between [0.001, 1.0]
  */
class Detector(private val kind: String, private val detectionThreshold: Double = 0.7) {

    private val modelGraphPath: InputStream = kind match {
        case "FACE" => getClass.getResourceAsStream(s"/models/face.pb")
        case _ => getClass.getResourceAsStream(s"/models/plate.pb")
    }

    private val graph: Graph = Graph.fromGraphDef(GraphDef.parseFrom(new BufferedInputStream(modelGraphPath)))
    private val session: Session = Session(graph)
    private val imagePlaceholder = graph.getOutputByName("image_tensor:0").toUByte
    private val detectionBoxes = graph.getOutputByName("detection_boxes:0").toFloat
    private val detectionScores = graph.getOutputByName("detection_scores:0").toFloat
    private val detectionClasses = graph.getOutputByName("detection_classes:0").toFloat
    private val numDetections = graph.getOutputByName("num_detections:0").toFloat

    /** Combined detection results
      *
      *  @param boxes detection boxes with relative positions, between [0.0, 1.0]
      *  @param scores detection scores between [0.0, 1.0]
      *  @param classes detection classes (e.g. 1=pedestrian, 2=cyclist, ...)
      *  @param num number of detection
      *  @param height of the image used for the detection
      *  @param width of the image used for the detection
      *  @param detectionThreshold all detections under this threshold will be discard. Between [0.0, 1.0]
      *
      *  @return list of results with the following format: [label, score, position]. The position is represented by the upper left corner and the lower right corner [yMin, xMin, yMax, xMax]
      */
    private def convertBoxes(boxes: Tensor[Float], scores: Tensor[Float], classes: Tensor[Float], num: Tensor[Float], height: Int, width: Int, detectionThreshold: Double): IndexedSeq[(Int, Float, (Int, Int, Int, Int))] = {
        for {
            i <- 0 until num(0).scalar.toInt
            box = boxes(0, i).entriesIterator.toSeq
            yMin = (box.head * height).toInt
            xMin = (box(1) * width).toInt
            yMax = (box(2) * height).toInt
            xMax = (box(3) * width).toInt
            labelBox = (yMin, xMin, yMax, xMax)
            labelId = classes(0, i).scalar.toInt
            score = scores(0, i).scalar
            if score > detectionThreshold
        } yield (labelId, score, labelBox)
    }

    /** Run the detection for one image with the current graph
      *
      *  @param image image used for the detection. Need to be a Tensor of UByte
      *  @param detectionThreshold all detections under this threshold will be discard. Between [0.0, 1.0]
      *
      *  @return list of results with the following format: [label, score, position]. The position is represented by the upper left corner and the lower right corner [yMin, xMin, yMax, xMax]
      */
    def detect(image: Tensor[UByte], detectionThreshold: Double = this.detectionThreshold): IndexedSeq[(Int, Float, (Int, Int, Int, Int))] = {
        val feeds = FeedMap(Map(imagePlaceholder -> image.slice(NewAxis, ---)))
        val Seq(boxes, scores, classes, num) = session.run(fetches = Seq(detectionBoxes, detectionScores, detectionClasses, numDetections), feeds = feeds)
        convertBoxes(boxes, scores, classes, num, image.shape(0), image.shape(1), detectionThreshold)
    }
    
}
