'''
Credit:
https://gist.github.com/tokestermw/795cc1fd6d0c9069b20204cbd133e36b
https://blog.metaflow.fr/tensorflow-how-to-freeze-a-model-and-serve-it-with-a-python-api-d4f3596b3adc
https://github.com/dpressel/rude-carnie



To run it using the cmd: python connect.py --filename 2.jpg"
'''




from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import tensorflow as tf
from tensorflow.contrib.layers import *
import numpy as np
from distutils.version import LooseVersion

RESIZE_AOI = 256
RESIZE_FINAL = 227
VERSION_GTE_0_12_0 = LooseVersion(tf.__version__) >= LooseVersion('0.12.0')

if VERSION_GTE_0_12_0:
    standardize_image = tf.image.per_image_standardization
else:
    standardize_image = tf.image.per_image_whitening


tf.app.flags.DEFINE_string('filename', '',
                           'File (Image) or File list (Text/No header TSV) to process')
tf.app.flags.DEFINE_string('checkpoint', 'checkpoint',
                          'Checkpoint basename')

FLAGS = tf.app.flags.FLAGS

def decode_jpeg(image_data):
    config = tf.ConfigProto(allow_soft_placement=True)
    sess = tf.Session(config=config)
    decode_jpeg_data = tf.placeholder(dtype=tf.string)
    decode_jpeg = tf.image.decode_jpeg(decode_jpeg_data, channels=3)
    crop = tf.image.resize_images(decode_jpeg, (RESIZE_AOI, RESIZE_AOI))
    image = sess.run(crop, feed_dict={decode_jpeg_data: image_data})
    return image


def load_graph(frozen_graph_filename):
    # We load the protobuf file from the disk and parse it to retrieve the
    # unserialized graph_def
    with tf.gfile.GFile(frozen_graph_filename, "rb") as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())

    # Then, we import the graph_def into a new Graph and returns it
    with tf.Graph().as_default() as graph:
        # The name var will prefix every op/nodes in your graph
        # Since we load everything in a new graph, this is not needed
        tf.import_graph_def(graph_def, name="prefix")
    return graph



def main(argv=None):
    with tf.gfile.GFile('./frozen_model.pb','rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())

    with tf.Graph().as_default() as graph:
        images = tf.placeholder(tf.float32, [None, RESIZE_FINAL, RESIZE_FINAL, 3])
        tf.import_graph_def(
            graph_def,
            # usually, during training you use queues, but at inference time use placeholders
            # this turns into "input
            input_map={"batch_processing/Reshape:0": images},
            return_elements=None,
            # if input_map is not None, needs a name
            name="prefix",

            producer_op_list=None
        )

    with tf.Session(graph=graph) as sess:
        label_list = ['(0, 2)', '(4, 6)', '(8, 12)', '(15, 20)', '(25, 32)', '(38, 43)', '(48, 53)', '(60, 100)']
        with tf.gfile.FastGFile(FLAGS.filename, 'rb') as f:
            image_data = f.read()
        image = decode_jpeg(image_data)
        crops = []
        crop = tf.image.resize_images(image, (RESIZE_FINAL, RESIZE_FINAL))
        crops.append(standardize_image(crop))

        image_batch = tf.stack(crops)
        outputnode = graph.get_tensor_by_name('prefix/output/output:0')
        softmax_output = tf.nn.softmax(outputnode)
        batch_results = sess.run(softmax_output, feed_dict={images: image_batch.eval()})

        print(batch_results)

        output = batch_results[0]
        batch_sz = batch_results.shape[0]
        for i in range(1, batch_sz):
            output = output + batch_results[i]
        output /= batch_sz
        best = np.argmax(output)
        best_choice = (label_list[best], output[best])
        print('My Guess %s, prob = %.2f' % best_choice)

if __name__ == '__main__':
    tf.app.run()
