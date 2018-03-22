'''
Credit: this code is a modified and refactored version of code found in the following repo:
https://github.com/dpressel/rude-carnie
'''
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from six.moves import xrange
import time
import json
from tensorflow.contrib.layers import *
from datetime import datetime
import os
import numpy as np
import tensorflow as tf

from distutils.version import LooseVersion

VERSION_GTE_0_12_0 = LooseVersion(tf.__version__) >= LooseVersion('0.12.0')

# Name change in TF v 0.12.0
if VERSION_GTE_0_12_0:
    standardize_image = tf.image.per_image_standardization
else:
    standardize_image = tf.image.per_image_whitening

LAMBDA = 0.01

tf.app.flags.DEFINE_string('pre_checkpoint_path', '',
                           """If specified, restore this pretrained model """
                           """before beginning any training.""")

tf.app.flags.DEFINE_string('train_dir', 'C:\\Users\\TOSHIBA\\Desktop\\172\\ICS_411_Senior_Project\\Training\\gender\\test_fold_is_0',
                           'Training directory')

tf.app.flags.DEFINE_boolean('log_device_placement', False,
                            """Whether to log device placement.""")

tf.app.flags.DEFINE_integer('num_preprocess_threads', 4,
                            'Number of preprocessing threads')

tf.app.flags.DEFINE_string('optim', 'Momentum',
                           'Optimizer')

tf.app.flags.DEFINE_integer('image_size', 227,
                            'Image size')

tf.app.flags.DEFINE_float('eta', 0.01,
                          'Learning rate')

tf.app.flags.DEFINE_float('pdrop', 0.,
                          'Dropout probability')

tf.app.flags.DEFINE_integer('max_steps', 1,
                            'Number of iterations')

tf.app.flags.DEFINE_integer('steps_per_decay', 10000,
                            'Number of steps before learning rate decay')
tf.app.flags.DEFINE_float('eta_decay_rate', 0.1,
                          'Learning rate decay')

tf.app.flags.DEFINE_integer('epochs', -1,
                            'Number of epochs')

tf.app.flags.DEFINE_integer('batch_size', 1,
                            'Batch size')

tf.app.flags.DEFINE_string('checkpoint', 'checkpoint',
                           'Checkpoint name')

tf.app.flags.DEFINE_string('model_type', 'default',
                           'Type of convnet')

FLAGS = tf.app.flags.FLAGS


def data_files(data_dir, subset):
    """Returns a python list of all (sharded) data subset files.
    Returns:
      python list of all (sharded) data set files.
    Raises:
      ValueError: if there are not data_files matching the subset.
    """
    if subset not in ['train', 'validation']:
        print('Invalid subset!')
        exit(-1)

    tf_record_pattern = os.path.join(data_dir, '%s-*' % subset)
    data_files = tf.gfile.Glob(tf_record_pattern)
    print(data_files)
    if not data_files:
        print('No files found for data dir %s at %s' % (subset, data_dir))

        exit(-1)
    return data_files


def decode_jpeg(image_buffer, scope=None):
    """Decode a JPEG string into one 3-D float image Tensor.
    Args:
      image_buffer: scalar string Tensor.
      scope: Optional scope for op_scope.
    Returns:
      3-D float Tensor with values ranging from [0, 1).
    """
    with tf.op_scope([image_buffer], scope, 'decode_jpeg'):
        # Decode the string as an RGB JPEG.
        # Note that the resulting image contains an unknown height and width
        # that is set dynamically by decode_jpeg. In other words, the height
        # and width of image is unknown at compile-time.
        image = tf.image.decode_jpeg(image_buffer, channels=3)

        # After this point, all image pixels reside in [0,1)
        # until the very end, when they're rescaled to (-1, 1).  The various
        # adjust_* ops all require this range for dtype float.
        image = tf.image.convert_image_dtype(image, dtype=tf.float32)
        return image


def distort_image(image, height, width):
    # Image processing for training the network. Note the many random
    # distortions applied to the image.

    distorted_image = tf.random_crop(image, [height, width, 3])
    # distorted_image = tf.image.resize_images(image, [height, width])
    # Randomly flip the image horizontally.
    distorted_image = tf.image.random_flip_left_right(distorted_image)
    # Because these operations are not commutative, consider randomizing
    # the order their operation.
    distorted_image = tf.image.random_brightness(distorted_image, max_delta=63)
    distorted_image = tf.image.random_contrast(distorted_image,lower=0.2, upper=1.8)
    return distorted_image


def image_preprocessing(image_buffer, image_size, train, thread_id=0):
    """Decode and preprocess one image for evaluation or training.
    Args:
    image_buffer: JPEG encoded string Tensor
    train: boolean
    thread_id: integer indicating preprocessing thread
    Returns:
    3-D float Tensor containing an appropriately scaled image
    Raises:
    ValueError: if user does not provide bounding box
    """

    image = decode_jpeg(image_buffer)

    if train:
        image = distort_image(image, image_size, image_size)
    else:
        image = tf.image.resize_images(image, [image_size, image_size])
    # data normalization
    image = standardize_image(image)
    return image


def parse_example_proto(example_serialized):
    # Dense features in Example proto.
    feature_map = {
        'image/encoded': tf.FixedLenFeature([], dtype=tf.string,
                                            default_value=''),
        'image/filename': tf.FixedLenFeature([], dtype=tf.string,
                                             default_value=''),

        'image/class/label': tf.FixedLenFeature([1], dtype=tf.int64,
                                                default_value=-1),
        'image/class/text': tf.FixedLenFeature([], dtype=tf.string,
                                               default_value=''),
        'image/height': tf.FixedLenFeature([1], dtype=tf.int64,
                                           default_value=-1),
        'image/width': tf.FixedLenFeature([1], dtype=tf.int64,
                                          default_value=-1),

    }

    features = tf.parse_single_example(example_serialized, feature_map)
    label = tf.cast(features['image/class/label'], dtype=tf.int32)
    return features['image/encoded'], label, features['image/filename']



def optimizer(optim, eta, loss_fn, at_step, decay_rate):
    global_step = tf.Variable(0, trainable=False)

    optz = lambda lr: tf.train.MomentumOptimizer(lr, 0.9)
    lr_decay_fn = lambda lr,global_step: tf.train.exponential_decay(lr, global_step, at_step, decay_rate, staircase=True)

    return tf.contrib.layers.optimize_loss(loss_fn, global_step, eta, optz, clip_gradients=4.,
                                           learning_rate_decay_fn=lr_decay_fn)


def batch_inputs(data_dir, batch_size, image_size, train, num_preprocess_threads=4,
                 num_readers=1, input_queue_memory_factor=16):
    with tf.name_scope('batch_processing'):
        if train:
            files = data_files(data_dir, 'train')
            filename_queue = tf.train.string_input_producer(files, shuffle=True, capacity=16)
        else:
            files = data_files(data_dir, 'validation')
            filename_queue = tf.train.string_input_producer(files, shuffle=False, capacity=1)

        if num_preprocess_threads % 4:
            raise ValueError('Please make num_preprocess_threads a multiple '
                             'of 4 (%d % 4 != 0).', num_preprocess_threads)

        if num_readers < 1:
            raise ValueError('Please make num_readers at least 1')

        # Approximate number of examples per shard.
        examples_per_shard = 1024
        # Size the random shuffle queue to balance between good global
        # mixing (more examples) and memory use (fewer examples).
        # 1 image uses 299*299*3*4 bytes = 1MB
        # The default input_queue_memory_factor is 16 implying a shuffling queue
        # size: examples_per_shard * 16 * 1MB = 17.6GB
        min_queue_examples = examples_per_shard * input_queue_memory_factor
        if train:
            examples_queue = tf.RandomShuffleQueue(
                capacity=min_queue_examples + 3 * batch_size,
                min_after_dequeue=min_queue_examples,
                dtypes=[tf.string])
        else:
            examples_queue = tf.FIFOQueue(
                capacity=examples_per_shard + 3 * batch_size,
                dtypes=[tf.string])

        # Create multiple readers to populate the queue of examples.
        if num_readers > 1:
            enqueue_ops = []
            for _ in range(num_readers):
                reader = tf.TFRecordReader()
                _, value = reader.read(filename_queue)
                enqueue_ops.append(examples_queue.enqueue([value]))

            tf.train.queue_runner.add_queue_runner(
                tf.train.queue_runner.QueueRunner(examples_queue, enqueue_ops))
            example_serialized = examples_queue.dequeue()
        else:
            reader = tf.TFRecordReader()
            _, example_serialized = reader.read(filename_queue)

        images_labels_fnames = []
        for thread_id in range(num_preprocess_threads):
            # Parse a serialized Example proto to extract the image and metadata.
            image_buffer, label_index, fname = parse_example_proto(example_serialized)

            image = image_preprocessing(image_buffer, image_size, train, thread_id)
            images_labels_fnames.append([image, label_index, fname])

        images, label_index_batch, fnames = tf.train.batch_join(
            images_labels_fnames,
            batch_size=batch_size,
            capacity=2 * num_preprocess_threads * batch_size)

        images = tf.cast(images, tf.float32)
        images = tf.reshape(images, shape=[batch_size, image_size, image_size, 3])

        # Display the training images in the visualizer.
        tf.summary.image('images', images, 20)

        return images, tf.reshape(label_index_batch, [batch_size]), fnames


def distorted_inputs(data_dir, batch_size=128, image_size=227, num_preprocess_threads=4):
    # Force all input processing onto CPU in order to reserve the GPU for
    # the forward inference and back-propagation.
    with tf.device('/cpu:0'):
        images, labels, filenames = batch_inputs(
            data_dir, batch_size, image_size, train=True,
            num_preprocess_threads=num_preprocess_threads,
            num_readers=1)
    return images, labels, filenames


def levi_hassner(nlabels, images, pkeep, is_training):
    weight_decay = 0.0005
    # Returns a function that can be used to apply L2 regularization to weights.
    # Small values of L2 can help prevent overfitting the training data.
    weights_regularizer = tf.contrib.layers.l2_regularizer(weight_decay)
    # A context manager for defining ops that creates variables (layers).
    # This context manager validates that the (optional) values are from the same graph,
    #  ensures that graph is the default graph, and pushes a name scope and a variable scope.
    with tf.variable_scope("LeviHassner", [images]) as scope:
        # TF-Slim is a lightweight library for defining, training and evaluating complex models in TensorFlow.
        # Components of tf-slim can be freely mixed with native tensorflow, as well as other frameworks,
        # such as tf.contrib.learn.
        with tf.contrib.slim.arg_scope(
                [convolution2d, fully_connected],
                weights_regularizer=weights_regularizer,
                biases_initializer=tf.constant_initializer(1.),
                weights_initializer=tf.random_normal_initializer(stddev=0.005),
                trainable=True):
            with tf.contrib.slim.arg_scope(
                    [convolution2d],
                    weights_initializer=tf.random_normal_initializer(stddev=0.01)):
                conv1 = convolution2d(images, 96, [7, 7], [4, 4], padding='VALID',
                                      biases_initializer=tf.constant_initializer(0.), scope='conv1')
                pool1 = max_pool2d(conv1, 3, 2, padding='VALID', scope='pool1')
                norm1 = tf.nn.local_response_normalization(pool1, 5, alpha=0.0001, beta=0.75, name='norm1')
                conv2 = convolution2d(norm1, 256, [5, 5], [1, 1], padding='SAME', scope='conv2')
                pool2 = max_pool2d(conv2, 3, 2, padding='VALID', scope='pool2')
                norm2 = tf.nn.local_response_normalization(pool2, 5, alpha=0.0001, beta=0.75, name='norm2')
                conv3 = convolution2d(norm2, 384, [3, 3], [1, 1], biases_initializer=tf.constant_initializer(0.),
                                      padding='SAME', scope='conv3')
                pool3 = max_pool2d(conv3, 3, 2, padding='VALID', scope='pool3')
                flat = tf.reshape(pool3, [-1, 384 * 6 * 6], name='reshape')
                full1 = fully_connected(flat, 512, scope='full1')
                drop1 = tf.nn.dropout(full1, pkeep, name='drop1')
                full2 = fully_connected(drop1, 512, scope='full2')
                drop2 = tf.nn.dropout(full2, pkeep, name='drop2')

    with tf.variable_scope('output') as scope:
        weights = tf.Variable(tf.random_normal([512, nlabels], mean=0.0, stddev=0.01), name='weights')
        biases = tf.Variable(tf.constant(0.0, shape=[nlabels], dtype=tf.float32), name='biases')
        output = tf.add(tf.matmul(drop2, weights), biases, name=scope.name)
        softmax_output = tf.nn.softmax(output)

    return output, softmax_output


def loss(logits, labels):
    labels = tf.cast(labels, tf.int32)
    cross_entropy = tf.nn.sparse_softmax_cross_entropy_with_logits(
        logits=logits, labels=labels, name='cross_entropy_per_example')
    cross_entropy_mean = tf.reduce_mean(cross_entropy, name='cross_entropy')
    tf.add_to_collection('losses', cross_entropy_mean)
    losses = tf.get_collection('losses')
    regularization_losses = tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES)
    total_loss = cross_entropy_mean + LAMBDA * sum(regularization_losses)
    tf.summary.scalar('tl (raw)', total_loss)
    # total_loss = tf.add_n(losses + regularization_losses, name='total_loss')
    loss_averages = tf.train.ExponentialMovingAverage(0.9, name='avg')
    loss_averages_op = loss_averages.apply(losses + [total_loss])
    for l in losses + [total_loss]:
        tf.summary.scalar(l.op.name + ' (raw)', l)
        tf.summary.scalar(l.op.name, loss_averages.average(l))
    with tf.control_dependencies([loss_averages_op]):
        total_loss = tf.identity(total_loss)
    return total_loss


def main(arg=None):
    # next line will create a new graph and use it in the context that follows
    with tf.Graph().as_default():
        # Open the metadata file and figure out nlabels, and size of epoch
        # md.json is created using preproc.py file
        input_file = os.path.join(FLAGS.train_dir, 'md.json')
        print(input_file)
        with open(input_file, 'r') as f:
            md = json.load(f)

        # images and labels refer to a batch of training samples pair (x,y)
        images, labels, _ = distorted_inputs(FLAGS.train_dir, FLAGS.batch_size, FLAGS.image_size,
                                             FLAGS.num_preprocess_threads)
        # logits are the predicted output from the forward pass, it is used in the loss function
        logits, softmax = levi_hassner(md['nlabels'], images, pkeep=1 - 0., is_training=True)
        total_loss = loss(logits, labels)
        print (softmax)

        global_step = tf.Variable(0, trainable=False)
        optz = lambda lr: tf.train.MomentumOptimizer(lr, 0.9)
        lr_decay_fn = lambda lr, global_step: tf.train.exponential_decay(lr, global_step, 10000, 0.1,
                                                                         staircase=True)
        train_op = tf.contrib.layers.optimize_loss(total_loss, global_step, FLAGS.eta, optz, clip_gradients=4.,
                                               learning_rate_decay_fn=lr_decay_fn)

        saver = tf.train.Saver(tf.global_variables())
        summary_op = tf.summary.merge_all()

        sess = tf.Session(config=tf.ConfigProto(log_device_placement=FLAGS.log_device_placement))
        tf.global_variables_initializer().run(session=sess)
        if FLAGS.pre_checkpoint_path:
            if tf.gfile.Exists(FLAGS.pre_checkpoint_path) is True:
                print('Trying to restore checkpoint from %s' % FLAGS.pre_checkpoint_path)
                restorer = tf.train.Saver()
                tf.train.latest_checkpoint(FLAGS.pre_checkpoint_path)
                print('%s: Pre-trained model restored from %s' %
                      (datetime.now(), FLAGS.pre_checkpoint_path))

        run_dir = '%s/run-%d' % (FLAGS.train_dir, os.getpid())

        checkpoint_path = '%s/%s' % (run_dir, FLAGS.checkpoint)
        if tf.gfile.Exists(run_dir) is False:
            print('Creating %s' % run_dir)
            tf.gfile.MakeDirs(run_dir)

        tf.train.write_graph(sess.graph_def, run_dir, 'model.pb', as_text=True)
        tf.train.start_queue_runners(sess=sess)
        summary_writer = tf.summary.FileWriter(run_dir, sess.graph)
        steps_per_train_epoch = int(md['train_counts'] / FLAGS.batch_size)
        num_steps = FLAGS.max_steps if FLAGS.epochs < 1 else FLAGS.epochs * steps_per_train_epoch
        print('Requested number of steps [%d]' % num_steps)

        for step in xrange(num_steps):
            start_time = time.time()
            _, loss_value = sess.run([train_op, total_loss])
            duration = time.time() - start_time

            assert not np.isnan(loss_value), 'Model diverged with loss = NaN'

            if step % 10 == 0:
                num_examples_per_step = FLAGS.batch_size
                examples_per_sec = num_examples_per_step / duration
                sec_per_batch = float(duration)

                format_str = ('%s: step %d, loss = %.3f (%.1f examples/sec; %.3f ' 'sec/batch)')
                print(format_str % (datetime.now(), step, loss_value,
                                    examples_per_sec, sec_per_batch))

            # Loss only actually evaluated every 100 steps?
            if step % 1 == 0:
                summary_str = sess.run(summary_op)
                summary_writer.add_summary(summary_str, step)

            if step % 1 == 0 or (step + 1) == num_steps:
                saver.save(sess, checkpoint_path, global_step=step)


if __name__ == '__main__':
    tf.app.run()