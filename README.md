
# Lunatech-Scala-Anonymization  

#### Scala implementation of the [understand.ai Anonymizer](https://github.com/understand-ai/anonymizer)
 
## Table of contents
1. [Introduction](#introduction)
2. [Global Architecture](#globalArchitecture)
3. [Configuration](#configuration)
4. [Build](#build)
5. [Run](#run)
6. [Attributions](#attributions)

## Prerequisites
#### 1. Download models
You have two solutions to download models:

- Download pre-trained models from Google drive 
    - [face.pb](https://docs.google.com/uc?export=download&id=1CwChAYxJo3mON6rcvXsl82FMSKj82vxF)
    - [plate.pb](https://docs.google.com/uc?export=download&id=1Fls9FYlQdRlLAtw-GVS_ie1oQUYmci9g)
    
- Use your own models

**You have to put both models in the folder** */src/main/resources/models* with the following names:
- **face.pb**: detection model for face
- **plate.pb**: detection model for plate
#### 2. Protocol Buffers compiler
To use Tensorflow for Scala, you need to install the Protocol Buffers compiler (at least version 3).

With **apt**: `apt-get install protobuf-compiler`

With **brew**: `brew install protoc`

Feel free to check this [link](http://platanios.org/tensorflow_scala/installation.html) for more information.

### Introduction <a name="introduction"></a>
Anonymization allows you to blur face and plate to improve the privacy of each other.
See an exemple below of an image presenting several cars and faces before and after anonymization.
![Image example cars before](/images/input/cars.jpg)
![Image example cars after](/images/out/anonymized_cars.jpg)

### Global Architecture <a name="globalArchitecture"></a>
Four important steps to anonymize our images:
1. First of all, we use [Tensorflow Scala](https://github.com/eaplatanios/tensorflow_scala) to detect plates and faces.
2. We create a mask with image size. This mask is the area who need to be blurred.
3. We use a convolution with gaussian kernel and our image to blur everything.
4. Using our previous mask we keep only the detection boxes area blurred and all other parts as initial.

In addition, we use a mean kernel to have smooth border around the blurred area.
![Diagram Global architecture](/images/documentation/diagramGeneral.png)

## Configuration <a name="configuration"></a>
There are multiple options to run the anonymizer.
For the documentation, please use: `--help`.

| Name                   |                          Value                                                     |  Required |
|:----------------------:|:----------------------------------------------------------------------------------:|:---------:|
| `--input`              | Path to the input folder                                                           |    X      |
| `--output`             | Path to the output folder                                                          |    X      |
| `--image-extensions`   | Comma-separated list of file types that will be anonymized (e.g. jpg, png)         |           |
| `--face-threshold`     | Detection confidence needed to anonymize a detected face. Must be in [0.001, 1.0]  |           |
| `--plate-threshold`    | Detection confidence needed to anonymize a license plate. Must be in [0.001, 1.0]  |           |
| `--kernel-size`        | Size of the gaussian kernel. Must be odd                                           |           |
| `--sigma`              | Standard deviation.                                                                |           |
| `--box-kernel-size`    | Size of the kernel used to smooth transitions. Must be odd                         |           |


## Build <a name="build"></a>
Inside the root folder, use `sbt assembly`.

## Run <a name="run"></a>
Once you have built your jar, use `java -jar <name_of_your_jar> <options>`

Otherwise, if you want to test quickly, in the root folder, you can use `sbt "run --input <inputPath> --output <outputPath>"
`
## Attributions <a name="attributions"></a>
As said in the [initial python project](https://github.com/understand-ai/anonymizer):

"An image for one of the test cases was taken from the COCO dataset.
 The pictures in this README are under an [Attribution 4.0 International license](https://creativecommons.org/licenses/by/4.0/legalcode)."
