'''
Credit:
https://omid.al/posts/2017-02-20-Tutorial-Build-Your-First-Tensorflow-Android-App.html
https://github.com/dpressel/rude-carnie



according the blog referenced above, the first referece, this code will do the following:

- Removing training-only operations like checkpoint saving.
- Stripping out parts of the graph that are never reached.
- Removing debug operations like CheckNumerics.
- Folding batch normalization ops into the pre-calculated weights.
- Fusing common operations into unified versions.


Usage: python frozen_graphs_optimizer.py --input [you graph file name] --output [your name of the output file]

'''


import tensorflow as tf
from tensorflow.contrib.layers import *
from tensorflow.python.tools import optimize_for_inference_lib
import numpy as np
import sys, getopt

opt = optimize_for_inference_lib

def optmizer(graph, output_file):
    input_graph_def = tf.GraphDef()
    with tf.gfile.Open(graph, "r") as f:
        data = f.read()
        input_graph_def.ParseFromString(data)

    output_graph_def = opt.optimize_for_inference(
            input_graph_def,
            ["batch_processing/Reshape"], # an array of the input node(s)
            ["output/output"], # an array of output nodes
            tf.float32.as_datatype_enum)

    # Save the optimized graph

    f = tf.gfile.FastGFile(output_file, "w")
    f.write(output_graph_def.SerializeToString())




def main(argv):
   inputfile = ''
   outputfile = ''
   try:
      opts, args = getopt.getopt(argv,"i:o:",["input=","output=", "input_nodes=", "output_nodes="])
   except getopt.GetoptError:
      print 'test.py --input <inputfile> --output <outputfile> --input_nodes [input_nodes] output_nodes [output_nodes]'
      sys.exit(2)
   for opt, arg in opts:
      if opt in ("--input"):
         inputfile = arg
      elif opt in ("--output"):
         outputfile = arg
      elif opt in ("--input_nodes"):
      
   print 'Input file is "', inputfile
   print 'Output file is "', outputfile
   optmizer(inputfile, outputfile)

if __name__ == "__main__":
   main(sys.argv[1:])
